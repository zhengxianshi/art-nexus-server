package com.artnexus.modules.auth.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshRequest {

    @NotBlank(message = "Refresh Token 不能为空")
    private String refreshToken;
}
