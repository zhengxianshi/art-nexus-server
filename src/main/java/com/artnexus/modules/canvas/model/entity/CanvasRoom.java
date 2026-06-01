package com.artnexus.modules.canvas.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("canvas_room")
public class CanvasRoom {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long creatorId;

    private String title;

    private Integer maxMembers;

    private Integer currentCount;

    /** WAITING / ACTIVE / CLOSED */
    private String status;

    /** 最近一次快照ID */
    private Long lastSnapshotId;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
