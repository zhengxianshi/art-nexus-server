package com.artnexus.modules.user.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserProfileVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String bio;
    private Integer artworkCount;
    private Integer likeCount;
    private LocalDateTime createdAt;
}
