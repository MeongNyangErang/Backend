package com.meongnyangerang.meongnyangerang.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meongnyangerang.meongnyangerang.jwt.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {

  @Value("${CORS_ALLOWED_ORIGINS}")
  private String allowedOrigins;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .cors(cors -> cors.configurationSource(corsConfigurationSource())) // CORS 설정
        .csrf(csrf -> csrf.disable()) // CSRF 보호 비활성화
        // 세션 비활성화(세션을 사용하지 않고 JWT 인증 방식 사용)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            // 인증 없이 접근 허용할 엔드포인트
            .requestMatchers(
                "/api/v1/email/**",
                "/api/v1/nickname/**",
                "/api/v1/users/signup",
                "/api/v1/hosts/signup",
                "/api/v1/users/login",
                "/api/v1/hosts/login",
                "/api/v1/admin/login",
                "/api/v1/recommendations/default/**",
                "/ws/**",
                "/ws/info",
                "/api/v1/accommodations/{accommodationId}/reviews",
                "/api/v1/search/accommodations",
                "/api/v1/accommodations/{accommodationId}",
                "/api/v1/rooms/{roomId}",
                "/api/v1/auth/reissue",
                "/health"
            ).permitAll()
            .requestMatchers("/api/v1/users/**").hasAuthority("ROLE_USER")
            .requestMatchers("/api/v1/chats/users/create").hasAuthority("ROLE_USER")
            .requestMatchers("/api/v1/recommendations/user-pet/**").hasAuthority("ROLE_USER")
            .requestMatchers("/api/v1/recommendations/most-viewed").hasAuthority("ROLE_USER")
            .requestMatchers("/api/v1/hosts/**").hasAuthority("ROLE_HOST")
            .requestMatchers("/api/v1/admin/**", "/api/v1/dummy-data/**").hasAuthority("ROLE_ADMIN")
            .requestMatchers("/api/v1/account/**").hasAnyAuthority("ROLE_USER", "ROLE_HOST")
            .requestMatchers("/api/v1/reviews/**").hasAnyAuthority("ROLE_USER", "ROLE_HOST")
            .anyRequest().authenticated() // 그 외 모든 요청은 인증 필요
        )
        .addFilterBefore(jwtAuthenticationFilter,
            UsernamePasswordAuthenticationFilter.class) // JWT 필터 추가(로그인한 사용자의 JWT를 검증하여 인증된 사용자로 설정)
        // 예외 처리 추가(추후 더 자세하게 수정 예정)
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint((request, response, authException) -> {
              log.error("[401 Unauthorized] 인증 실패: {}", authException.getMessage());

              response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
              response.setContentType("application/json");
              response.setCharacterEncoding("UTF-8");

              Map<String, String> error = new HashMap<>();
              error.put("message", "인증되지 않은 사용자입니다. (401)");
              new ObjectMapper().writeValue(response.getWriter(), error);
            })
            .accessDeniedHandler((request, response, accessDeniedException) -> {
              log.error("[403 Forbidden] 권한 부족: {}", accessDeniedException.getMessage());

              response.setStatus(HttpServletResponse.SC_FORBIDDEN);
              response.setContentType("application/json");
              response.setCharacterEncoding("UTF-8");

              Map<String, String> error = new HashMap<>();
              error.put("message", "접근 권한이 없습니다. (403)");
              new ObjectMapper().writeValue(response.getWriter(), error);
            })
        );
    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOriginPatterns(Arrays.asList(allowedOrigins.split(",")));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
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
