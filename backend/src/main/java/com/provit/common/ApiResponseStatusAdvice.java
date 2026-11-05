package com.provit.common;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.provit.dto.response.ApiResponse;

@RestControllerAdvice(basePackages = "com.provit.controller")
public class ApiResponseStatusAdvice implements ResponseBodyAdvice<ApiResponse<?>> {
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return ApiResponse.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public ApiResponse<?> beforeBodyWrite(ApiResponse<?> body, MethodParameter returnType, MediaType contentType,
            Class<? extends HttpMessageConverter<?>> converterType, ServerHttpRequest request,
            ServerHttpResponse response) {
        if (body != null && body.getResponseCode() != null) {
            switch (body.getResponseCode()) {
                case AUTH_UNAUTHORIZED, AUTH_TOKEN_INVALID, AUTH_TOKEN_EXPIRED -> response.setStatusCode(HttpStatus.UNAUTHORIZED);
                case AUTH_FORBIDDEN -> response.setStatusCode(HttpStatus.FORBIDDEN);
                default -> { }
            }
        }
        return body;
    }
}
