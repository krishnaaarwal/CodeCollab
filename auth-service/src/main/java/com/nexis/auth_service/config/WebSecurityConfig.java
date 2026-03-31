package com.nexis.auth_service.config;

import com.nexis.auth_service.security.filter.JwtFilter;
import com.nexis.auth_service.security.oauth2.Oauth2SuccessHandler;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
@Configuration
@EnableMethodSecurity
public class WebSecurityConfig{

    private final JwtFilter jwtFilter;
    private final Oauth2SuccessHandler oauth2SuccessHandler;
    private final HandlerExceptionResolver handlerExceptionResolver;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception{
         httpSecurity
                .csrf(csrf->csrf.disable()) //CSRF(CROSS SITE REQUEST FORGERY) DISABLE
                .sessionManagement(session->session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth->auth.requestMatchers(
                                 "/public/**",
                                "/api/auth/register",    // Only signup is public
                                "/api/auth/login",       // Only login is public
                                "/api/auth/refresh",     // Public, because access token is dead!
                                "/api/auth/forgot-password",
                                 "/api/auth/reset-password",
                                 "/v3/api-docs/**",       // Let Swagger generate the JSON
                                 "/swagger-ui/**",        // Let Swagger serve the UI CSS/JS
                                 "/swagger-ui.html"       // Let Swagger serve the main page
                         ).permitAll()
                        .anyRequest().authenticated()
                ).addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                 .oauth2Login(oauth2->oauth2.failureHandler(new AuthenticationFailureHandler() {
                     @Override
                     public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
                         log.error("Oauth2 error: ",exception.getMessage());
                         handlerExceptionResolver.resolveException(request,response,null,exception);
                     }
                 }).successHandler(oauth2SuccessHandler)
                 ).exceptionHandling(exceptConfig->exceptConfig.accessDeniedHandler(
                         new AccessDeniedHandler() {
                             @Override
                             public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
                                 handlerExceptionResolver.resolveException(request,response,null,accessDeniedException);
                             }
                         }
                 ))
         ;

        return httpSecurity.build();
    }
}
