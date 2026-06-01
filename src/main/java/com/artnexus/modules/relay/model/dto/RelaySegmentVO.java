package com.artnexus.modules.relay.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RelaySegmentVO {

    private Long id;
    private Integer turnNumber;
    private Long userId;
    private String username;
    private String avatarUrl;
    private String imageUrl;
    private String hint;
    private LocalDateTime createdAt;
}
