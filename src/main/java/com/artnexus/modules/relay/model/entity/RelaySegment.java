package com.artnexus.modules.relay.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("relay_segment")
public class RelaySegment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long roomId;

    private Long userId;

    /** 第几轮 (1-based) */
    private Integer turnNumber;

    private String imageUrl;

    /** 留给下一人的提示词 */
    private String hint;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
