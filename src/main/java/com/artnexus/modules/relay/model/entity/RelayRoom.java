package com.artnexus.modules.relay.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("relay_room")
public class RelayRoom {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long creatorId;

    private String title;

    private String coverUrl;

    private Integer maxMembers;

    private Integer currentCount;

    /** WAITING / IN_PROGRESS / FINISHED */
    private String status;

    /** 当前轮到第几个成员 (1-based) */
    private Integer currentTurn;

    /** 第一轮的提示词(由房主设定) */
    private String prompt;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
