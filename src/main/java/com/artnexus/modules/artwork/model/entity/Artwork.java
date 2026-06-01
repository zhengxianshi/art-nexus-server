package com.artnexus.modules.artwork.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("artwork")
public class Artwork {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String description;

    private String imageUrl;

    private String thumbnailUrl;

    private Integer width;

    private Integer height;

    private Long imageSize;

    private Integer likeCount;

    private Integer commentCount;

    private Integer viewCount;

    private Integer status;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
