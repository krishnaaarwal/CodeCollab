package com.nexis.auth_service.security.filter;


import com.nexis.auth_service.entity.UserEntity;
import com.nexis.auth_service.repository.UserRepository;
import com.nexis.auth_service.security.user_principal.UserPrincipal;
import com.nexis.auth_service.util.AuthUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
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
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

       try {
           log.debug("Incoming request : {}", request.getRequestURI());

           String requestHeader = request.getHeader("Authorization");

           if (requestHeader == null || !requestHeader.startsWith("Bearer ") || requestHeader.length() <= 7) {
               filterChain.doFilter(request, response);
               return;
           }


           String token = requestHeader.substring(7);

            // --- THE REDIS CHECK ---
            // If the token exists as a Key in Redis, it means it's on the Blacklist!
           if (Boolean.TRUE.equals(redisTemplate.hasKey(token))) {
               log.warn("Blocked request! Attempted to use a blacklisted token.");
               // Return a 401 Unauthorized immediately. Do not process the request further.
               response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
               response.getWriter().write("Token has been revoked. Please log in again.");
               return;
           }

           String email = authUtil.getEmailFromToken(token);

           if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

               UserEntity userEntity = userRepository.findByEmail(email).orElseThrow();

               // 1. Create the Principal
               UserPrincipal principal = new UserPrincipal(userEntity);

               // 2. Put the Principal (not the entity) into the Authentication Token
               UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                       principal, null, principal.getAuthorities()
               );

               SecurityContextHolder.getContext().setAuthentication(authenticationToken);
           }
           filterChain.doFilter(request, response);
       } catch (Exception e) {
           handlerExceptionResolver.resolveException(request,response,null,e);
       }

    }
}
