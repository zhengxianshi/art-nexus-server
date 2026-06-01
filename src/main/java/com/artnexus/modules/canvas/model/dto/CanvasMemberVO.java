package com.artnexus.modules.canvas.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CanvasMemberVO {

    private Long userId;
    private String username;
    private String avatarUrl;
    private LocalDateTime joinedAt;
}
