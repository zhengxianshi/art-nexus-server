package com.artnexus.modules.artwork.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentCreateRequest {

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论最长500个字符")
    private String content;

    /** 父评论ID（楼中楼回复） */
    private Long parentId;

    /** 回复的目标用户ID */
    private Long replyToUserId;
}
