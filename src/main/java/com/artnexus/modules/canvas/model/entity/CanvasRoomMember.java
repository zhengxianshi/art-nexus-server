package com.artnexus.modules.canvas.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("canvas_room_member")
public class CanvasRoomMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long roomId;

    private Long userId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime joinedAt;
}
