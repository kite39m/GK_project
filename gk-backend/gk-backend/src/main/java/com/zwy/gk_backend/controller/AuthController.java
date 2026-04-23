package com.zwy.gk_backend.controller;

import com.zwy.gk_backend.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> loginData) {
        String username = loginData.get("username");
        String password = loginData.get("password");

        Map<String, Object> result = new HashMap<>();

        // 🚨 新手阶段先写死账号密码。等你后续建了 User 表，这里再改成去数据库查
        if ("admin".equals(username) && "123456".equals(password)) {
            // 账号密码正确，颁发 Token
            String token = jwtUtils.createToken(1, "admin");
            result.put("code", 200);
            result.put("token", token);
            result.put("message", "登录成功");
        } else {
            // 账号密码错误
            result.put("code", 401);
            result.put("message", "账号或密码错误");
        }
        return result;
    }
}