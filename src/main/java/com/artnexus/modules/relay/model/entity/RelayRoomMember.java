package com.artnexus.modules.relay.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("relay_room_member")
public class RelayRoomMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long roomId;

    private Long userId;

    /** 加入顺序 (1-based) */
    private Integer joinOrder;

    /** 是否已提交本轮作品 */
    private Boolean submitted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
