package com.artnexus.modules.auth.service.impl;

import com.artnexus.common.BusinessException;
import com.artnexus.common.ErrorCode;
import com.artnexus.modules.auth.model.dto.*;
import com.artnexus.modules.auth.service.AuthService;
import com.artnexus.modules.user.mapper.UserMapper;
import com.artnexus.modules.user.model.entity.User;
import com.artnexus.security.JwtTokenProvider;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public LoginResponse register(RegisterRequest request) {
        // 检查用户名是否已存在
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (count > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setStatus(1);
        userMapper.insert(user);

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getUsername());

        return new LoginResponse(user.getId(), user.getUsername(),
                user.getNickname(), user.getAvatarUrl(), accessToken, refreshToken);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.WRONG_PASSWORD);
        }
        if (user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getUsername());

        return new LoginResponse(user.getId(), user.getUsername(),
                user.getNickname(), user.getAvatarUrl(), accessToken, refreshToken);
    }

    @Override
    public LoginResponse refresh(RefreshRequest request) {
        if (!jwtTokenProvider.validateToken(request.getRefreshToken())) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        // 检查 refresh token 是否在黑名单
        String jti = jwtTokenProvider.getJti(request.getRefreshToken());
        if (Boolean.TRUE.equals(redisTemplate.hasKey("token:blacklist:" + jti))) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        Long userId = jwtTokenProvider.getUserId(request.getRefreshToken());
        String username = jwtTokenProvider.getUsername(request.getRefreshToken());

        // 旧的 refresh token 加入黑名单
        long remaining = jwtTokenProvider.getRemainingTime(request.getRefreshToken());
        if (remaining > 0) {
            redisTemplate.opsForValue().set("token:blacklist:" + jti, "1", Duration.ofMillis(remaining));
        }

        User user = userMapper.selectById(userId);
        String newAccessToken = jwtTokenProvider.generateAccessToken(userId, username);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId, username);

        return new LoginResponse(user.getId(), user.getUsername(),
                user.getNickname(), user.getAvatarUrl(), newAccessToken, newRefreshToken);
    }

    @Override
    public void logout(String accessToken, String refreshToken) {
        // 将 access token 和 refresh token 加入黑名单
        if (accessToken != null && jwtTokenProvider.validateToken(accessToken)) {
            String jti = jwtTokenProvider.getJti(accessToken);
            long remaining = jwtTokenProvider.getRemainingTime(accessToken);
            if (remaining > 0) {
                redisTemplate.opsForValue().set("token:blacklist:" + jti, "1", Duration.ofMillis(remaining));
            }
        }
        if (refreshToken != null && jwtTokenProvider.validateToken(refreshToken)) {
            String jti = jwtTokenProvider.getJti(refreshToken);
            long remaining = jwtTokenProvider.getRemainingTime(refreshToken);
            if (remaining > 0) {
                redisTemplate.opsForValue().set("token:blacklist:" + jti, "1", Duration.ofMillis(remaining));
            }
        }
    }
}
