package com.artnexus.modules.relay.model.dto;

import lombok.Data;

@Data
public class RelayMemberVO {

    private Long userId;
    private String username;
    private String avatarUrl;
    private Integer joinOrder;
    private Boolean submitted;
}
