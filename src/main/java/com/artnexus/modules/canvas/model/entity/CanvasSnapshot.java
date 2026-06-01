package com.artnexus.modules.canvas.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("canvas_snapshot")
public class CanvasSnapshot {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long roomId;

    /** JSON: { layers: [...] } */
    private String snapshotData;

    /** 生成快照时的操作序号 */
    private Long lastOpSeq;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
