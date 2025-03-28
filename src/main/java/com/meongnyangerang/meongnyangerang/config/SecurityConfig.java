package com.meongnyangerang.meongnyangerang.config;

import com.meongnyangerang.meongnyangerang.jwt.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable()) // CSRF 보호 비활성화
        // 세션 비활성화(세션을 사용하지 않고 JWT 인증 방식 사용)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            // 인증 없이 접근 허용할 엔드포인트
            .requestMatchers(
                "/api/v1/email/**",
                "/api/v1/nickname/**",
                "/api/v1/users/signup",
                "/api/v1/hosts/signup"
            ).permitAll()
            .requestMatchers("/api/v1/users/**").hasAuthority("ROLE_USER")
            .requestMatchers("/api/v1/hosts/**").hasAuthority("ROLE_HOST")
            .requestMatchers("/api/v1/admin/**").hasAuthority("ROLE_ADMIN")
            .anyRequest().authenticated() // 그 외 모든 요청은 인증 필요
        )
        .addFilterBefore(jwtAuthenticationFilter,
            UsernamePasswordAuthenticationFilter.class) // JWT 필터 추가(로그인한 사용자의 JWT를 검증하여 인증된 사용자로 설정)
        // 예외 처리 추가(추후 더 자세하게 수정 예정)
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint((request, response, authException) -> {
              response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                  "Unauthorized");  // 인증 실패(401)
            })
            .accessDeniedHandler((request, response, accessDeniedException) -> {
              response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");  // 권한 부족(403)
            })
        );
    return http.build();
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
      throws Exception {
    return configuration.getAuthenticationManager();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

}
