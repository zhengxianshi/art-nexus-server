package com.artnexus.modules.artwork.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ArtworkVO {

    private Long id;
    private Long userId;
    private String username;
    private String userAvatar;
    private String title;
    private String description;
    private String imageUrl;
    private String thumbnailUrl;
    private Integer width;
    private Integer height;
    private Integer likeCount;
    private Integer commentCount;
    private Integer viewCount;
    private Boolean liked;       // 当前用户是否已点赞
    private LocalDateTime createdAt;
}
