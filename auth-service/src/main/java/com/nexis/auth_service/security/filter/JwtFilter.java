package com.nexis.auth_service.security.filter;


import com.nexis.auth_service.entity.UserEntity;
import com.nexis.auth_service.repository.UserRepository;
import com.nexis.auth_service.util.AuthUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final AuthUtil authUtil;
    private final UserRepository userRepository;
    private final HandlerExceptionResolver handlerExceptionResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

       try {
           log.info("Incoming request : {}", request.getRequestURI());

           String requestHeader = request.getHeader("Authorization");

           if (requestHeader == null || !requestHeader.startsWith("Bearer ")) {
               filterChain.doFilter(request, response);
               return;
           }

           String token = requestHeader.split("Bearer ")[1];
           String email = authUtil.getEmailFromToken(token);

           if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

               UserEntity user = userRepository.findByEmail(email).orElseThrow();

               UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                       user, null
               );

               SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);

           }
           filterChain.doFilter(request, response);
       } catch (Exception e) {
           handlerExceptionResolver.resolveException(request,response,null,e);
       }

    }
}
