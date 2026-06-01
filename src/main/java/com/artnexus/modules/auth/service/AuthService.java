package com.artnexus.modules.auth.service;

import com.artnexus.modules.auth.model.dto.*;

public interface AuthService {

    LoginResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    LoginResponse refresh(RefreshRequest request);

    void logout(String accessToken, String refreshToken);
}
