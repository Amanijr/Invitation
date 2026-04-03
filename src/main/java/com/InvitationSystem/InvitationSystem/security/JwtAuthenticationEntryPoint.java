package com.InvitationSystem.InvitationSystem.security;


import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


@Component
public class  JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("status", HttpStatus.UNAUTHORIZED.value());
        errorBody.put("error", "Unauthorized");
        errorBody.put("message", "Authentication required. Please provide a valid JWT token.");
        errorBody.put("path", request.getRequestURI());
        errorBody.put("timestamp", LocalDateTime.now().toString());

        objectMapper.writeValue(response.getOutputStream(), errorBody);
    }
}

