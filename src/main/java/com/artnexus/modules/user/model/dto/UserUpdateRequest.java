package com.artnexus.modules.user.model.dto;

import lombok.Data;

@Data
public class UserUpdateRequest {

    private String nickname;
    private String bio;
}
