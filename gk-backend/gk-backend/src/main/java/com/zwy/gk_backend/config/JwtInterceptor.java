package com.zwy.gk_backend.config;

import com.zwy.gk_backend.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 放行浏览器的预检请求 (OPTIONS)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 从请求头获取 Authorization
        String token = request.getHeader("Authorization");

        // 如果 token 存在，且以 "Bearer " 开头
        if (token != null && token.startsWith("Bearer ")) {
            try {
                // 截取真正的 token 字符串并解析
                jwtUtils.parseToken(token.substring(7));
                return true; // 验证通过，放行！
            } catch (Exception e) {
                // Token 过期或被篡改
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }
        }

        // 没有 Token，直接拒绝
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return false;
    }
}