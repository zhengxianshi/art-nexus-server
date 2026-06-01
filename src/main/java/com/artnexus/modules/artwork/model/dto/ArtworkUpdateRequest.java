package com.artnexus.modules.artwork.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ArtworkUpdateRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 100)
    private String title;

    @Size(max = 2000)
    private String description;
}
