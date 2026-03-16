package com.beyond.hodadoc.common.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final String secretKey;
    private final int expiration;
    private final int refreshExpiration;  // 추가
    private Key SECRET_KEY;

    public JwtTokenProvider(
            @Value("${jwt.secretKey}") String secretKey,
            @Value("${jwt.expiration}") int expiration,
            @Value("${jwt.refresh-expiration}") int refreshExpiration  // 추가
    ) {
        this.secretKey = secretKey;
        this.expiration = expiration;
        this.refreshExpiration = refreshExpiration;
    }

    @PostConstruct
    public void init() {
        byte[] keyBytes = Base64.getDecoder().decode(secretKey);
        this.SECRET_KEY = new SecretKeySpec(keyBytes, SignatureAlgorithm.HS512.getJcaName());
    }

    // 기존 Access Token 생성 (변경 없음)
    public String createToken(Long accountId, String role) {
        Claims claims = Jwts.claims().setSubject(String.valueOf(accountId));
        claims.put("role", role);

        Date now = new Date();
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expiration * 60 * 1000L))
                .signWith(SECRET_KEY, SignatureAlgorithm.HS512)
                .compact();
    }

    // Refresh Token 생성 (새로 추가) - subject만 담고 만료 기간 길게
    public String createRefreshToken(Long accountId) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(String.valueOf(accountId))
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + refreshExpiration * 60 * 1000L))
                .signWith(SECRET_KEY, SignatureAlgorithm.HS512)
                .compact();
    }

    // 토큰에서 accountId 추출 (새로 추가)
    public Long getAccountId(String token) {
        String subject = Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
        return Long.parseLong(subject);
    }

    // 토큰에서 role 추출 (새로 추가)
    public String getRole(String token) {
        return (String) Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role");
    }

    // 토큰 유효성 검사 (새로 추가)
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // Refresh Token 만료 시간(초) 반환 - Redis TTL 설정용
    public long getRefreshExpirationSeconds() {
        return refreshExpiration * 60L;
    }
}