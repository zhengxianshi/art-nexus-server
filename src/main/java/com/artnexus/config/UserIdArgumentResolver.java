package com.artnexus.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 自动解析 Controller 方法中标记了 @RequestAttribute("userId") 的参数，
 * 从 request attribute 读取（由 JwtAuthenticationFilter 设置）
 */
@Component
@RequiredArgsConstructor
public class UserIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(
                org.springframework.web.bind.annotation.RequestAttribute.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        String attrName = parameter.getParameterAnnotation(
                org.springframework.web.bind.annotation.RequestAttribute.class).value();
        if ("userId".equals(attrName)) {
            HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
            if (request != null) {
                return request.getAttribute("userId");
            }
        }
        return null;
    }
}
