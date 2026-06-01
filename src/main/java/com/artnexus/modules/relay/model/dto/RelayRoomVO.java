package com.artnexus.modules.relay.model.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RelayRoomVO {

    private Long id;
    private Long creatorId;
    private String creatorName;
    private String title;
    private String coverUrl;
    private Integer maxMembers;
    private Integer currentCount;
    private String status;
    private Integer currentTurn;
    private String prompt;
    private List<RelayMemberVO> members;
    private LocalDateTime createdAt;
}
