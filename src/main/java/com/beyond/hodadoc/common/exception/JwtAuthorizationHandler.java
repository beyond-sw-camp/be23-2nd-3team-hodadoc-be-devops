package com.beyond.hodadoc.common.exception;

import com.beyond.hodadoc.common.dtos.CommonErrorDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;

@Component
public class JwtAuthorizationHandler implements AccessDeniedHandler {
    private final ObjectMapper objectMapper;

    public JwtAuthorizationHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 상태코드 세팅.
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        CommonErrorDto dto = CommonErrorDto.builder()
                .status_code(403)
                .error_message("권한이 없습니다.")
                .build();
        String data = objectMapper.writeValueAsString(dto);
        PrintWriter printWriter = response.getWriter();
        printWriter.write(data);
        printWriter.flush();

    }
}
