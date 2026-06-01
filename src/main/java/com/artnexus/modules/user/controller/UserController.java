package com.artnexus.modules.user.controller;

import com.artnexus.common.Result;
import com.artnexus.modules.user.model.dto.*;
import com.artnexus.modules.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile/{userId}")
    public Result<UserProfileVO> getProfile(@PathVariable Long userId) {
        return Result.success(userService.getProfile(userId));
    }

    @PutMapping("/profile")
    public Result<UserProfileVO> updateProfile(@RequestAttribute("userId") Long userId,
                                                @Valid @RequestBody UserUpdateRequest request) {
        return Result.success(userService.updateProfile(userId, request));
    }

    @PutMapping("/password")
    public Result<Void> updatePassword(@RequestAttribute("userId") Long userId,
                                        @Valid @RequestBody PasswordUpdateRequest request) {
        userService.updatePassword(userId, request);
        return Result.success();
    }

    @PutMapping("/avatar")
    public Result<String> updateAvatar(@RequestAttribute("userId") Long userId,
                                        @RequestBody java.util.Map<String, String> body) {
        return Result.success(userService.updateAvatar(userId, body.get("avatarUrl")));
    }
}
