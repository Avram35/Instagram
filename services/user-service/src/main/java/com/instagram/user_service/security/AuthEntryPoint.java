package com.instagram.user_service.security;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
@Component
public class AuthEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence
    (
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException
    )
        throws IOException, ServletException 
        {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "401 Unauthorized");
        }

}
