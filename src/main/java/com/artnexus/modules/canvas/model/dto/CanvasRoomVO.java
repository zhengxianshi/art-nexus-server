package com.artnexus.modules.canvas.model.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CanvasRoomVO {

    private Long id;
    private Long creatorId;
    private String creatorName;
    private String title;
    private Integer maxMembers;
    private Integer currentCount;
    private String status;
    private List<CanvasMemberVO> members;
    private LocalDateTime createdAt;
}
