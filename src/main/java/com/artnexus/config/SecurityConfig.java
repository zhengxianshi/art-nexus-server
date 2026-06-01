package com.artnexus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 安全配置 —— 注册 PasswordEncoder
 * JWT Filter 不在此处注册（避免与 SecurityFilterChain 冲突），
 * 实际认证流程由 com.artnexus.security 包中的 SecurityConfig 管理。
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
