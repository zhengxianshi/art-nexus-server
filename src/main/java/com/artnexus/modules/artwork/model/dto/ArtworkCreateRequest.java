package com.artnexus.modules.artwork.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ArtworkCreateRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题最长100个字符")
    private String title;

    @Size(max = 2000, message = "描述最长2000个字符")
    private String description;

    @NotBlank(message = "图片不能为空")
    private String imageUrl;

    private String thumbnailUrl;

    private Integer width;

    private Integer height;

    private Long imageSize;
}
