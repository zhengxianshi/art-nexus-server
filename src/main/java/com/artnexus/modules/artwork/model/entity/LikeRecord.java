package com.artnexus.modules.artwork.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("like_record")
public class LikeRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long artworkId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
