package com.beyond.hodadoc.common.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Component
public class JwtTokenFilter extends GenericFilter {

    @Value("${jwt.secretKey}")
    private String secretKeyBase64;

    private Key SIGN_KEY;

    @Override
    public void init(FilterConfig filterConfig) {
        // ✅ Provider와 동일하게 Base64 decode해서 키 구성
        byte[] keyBytes = Base64.getDecoder().decode(secretKeyBase64);
        this.SIGN_KEY = new SecretKeySpec(keyBytes, SignatureAlgorithm.HS512.getJcaName());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        String token = null;

        // 1) Authorization 헤더에서 토큰 추출
        String authHeader = req.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        // 2) SSE 등 EventSource용: query parameter에서 토큰 추출
        // SSE 연결할 때 EventSource를 쓰는데-> 헤더를 커스텀할 수가 없음
        //  헤더에 토큰 없으면 URL 쿼리파라미터에서 찾아보라는 뜻
        if (token == null) {
            token = req.getParameter("token");
        }

        if (token == null) {
            chain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(SIGN_KEY)   // Key로 검증
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // ✅ subject는 이제 accountId
            Long accountId = Long.parseLong(claims.getSubject());

            List<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + claims.get("role")));

            Authentication authentication =
                    new UsernamePasswordAuthenticationToken(accountId, "", authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            // 토큰이 이상하면 인증 없이 통과(또는 여기서 401 처리하도록 변경 가능)
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response);
    }
}
