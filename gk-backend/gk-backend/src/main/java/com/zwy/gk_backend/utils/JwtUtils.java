package com.zwy.gk_backend.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {
    // 生成一个安全的签名密钥
    private static final Key KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    // Token过期时间：10小时 (10 * 60 * 60 * 1000 毫秒)
    private static final long EXPIRATION = 36000000L;

    // 1. 生成 Token
    public String createToken(Integer userId, String username) {
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(KEY)
                .compact();
    }

    // 2. 验证并解析 Token
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}