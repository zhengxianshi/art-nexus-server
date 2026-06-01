package com.artnexus.modules.artwork.model.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentVO {

    private Long id;
    private Long userId;
    private String username;
    private String userAvatar;
    private String content;
    private Long replyToUserId;
    private String replyToUsername;
    private List<CommentVO> replies;
    private LocalDateTime createdAt;
}
