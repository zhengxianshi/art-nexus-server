package com.artnexus.modules.canvas.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CanvasRoomCreateRequest {

    @NotBlank(message = "房间标题不能为空")
    @Size(max = 50, message = "标题最长50个字符")
    private String title;
}
