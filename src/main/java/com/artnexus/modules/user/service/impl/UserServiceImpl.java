package com.artnexus.modules.user.service.impl;

import com.artnexus.common.BusinessException;
import com.artnexus.common.ErrorCode;
import com.artnexus.modules.artwork.mapper.ArtworkMapper;
import com.artnexus.modules.artwork.mapper.LikeRecordMapper;
import com.artnexus.modules.artwork.model.entity.Artwork;
import com.artnexus.modules.artwork.model.entity.LikeRecord;
import com.artnexus.modules.user.mapper.UserMapper;
import com.artnexus.modules.user.model.dto.PasswordUpdateRequest;
import com.artnexus.modules.user.model.dto.UserProfileVO;
import com.artnexus.modules.user.model.dto.UserUpdateRequest;
import com.artnexus.modules.user.model.entity.User;
import com.artnexus.modules.user.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final ArtworkMapper artworkMapper;
    private final LikeRecordMapper likeRecordMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserProfileVO getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        UserProfileVO vo = toVO(user);
        // 统计作品数
        vo.setArtworkCount(Math.toIntExact(
                artworkMapper.selectCount(new LambdaQueryWrapper<Artwork>()
                        .eq(Artwork::getUserId, userId)
                        .eq(Artwork::getStatus, 1))));
        // 统计获赞总数
        Long totalLikes = likeRecordMapper.selectCount(new LambdaQueryWrapper<LikeRecord>()
                .eq(LikeRecord::getUserId, userId));
        vo.setLikeCount(Math.toIntExact(totalLikes));
        return vo;
    }

    @Override
    public UserProfileVO updateProfile(Long userId, UserUpdateRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        userMapper.updateById(user);
        return toVO(user);
    }

    @Override
    public void updatePassword(Long userId, PasswordUpdateRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.WRONG_PASSWORD);
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
    }

    @Override
    public String updateAvatar(Long userId, String avatarUrl) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        user.setAvatarUrl(avatarUrl);
        userMapper.updateById(user);
        return avatarUrl;
    }

    private UserProfileVO toVO(User user) {
        UserProfileVO vo = new UserProfileVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setBio(user.getBio());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }
}
