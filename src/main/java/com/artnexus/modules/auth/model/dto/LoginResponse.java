package com.artnexus.modules.auth.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {

    private Long userId;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String accessToken;
    private String refreshToken;
}
