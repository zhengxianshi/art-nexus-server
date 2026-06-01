package com.artnexus.modules.relay.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RelayRoomCreateRequest {

    @NotBlank(message = "房间标题不能为空")
    @Size(max = 50, message = "标题最长50个字符")
    private String title;

    @Min(value = 5, message = "最少5人")
    @Max(value = 10, message = "最多10人")
    private Integer maxMembers;

    @Size(max = 200, message = "提示词最长200个字符")
    private String prompt;
}
