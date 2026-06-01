package com.artnexus.modules.user.service;

import com.artnexus.modules.user.model.dto.PasswordUpdateRequest;
import com.artnexus.modules.user.model.dto.UserProfileVO;
import com.artnexus.modules.user.model.dto.UserUpdateRequest;

public interface UserService {

    UserProfileVO getProfile(Long userId);

    UserProfileVO updateProfile(Long userId, UserUpdateRequest request);

    void updatePassword(Long userId, PasswordUpdateRequest request);

    String updateAvatar(Long userId, String avatarUrl);
}
