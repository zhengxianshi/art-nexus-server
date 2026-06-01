package com.artnexus.modules.relay.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RelaySegmentSubmitRequest {

    @NotBlank(message = "图片不能为空")
    private String imageUrl;

    private String hint;
}
