package com.meongnyangerang.meongnyangerang.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http)throws Exception {
    http
        .csrf(csrf -> csrf.disable()) // CSRF 보호 비활성화
        .authorizeHttpRequests(auth -> auth
            // 인증 없이 접근 허용할 엔드포인트
            .requestMatchers(
                "/api/v1/email/**",
                "/api/v1/nickname/**",
                "/api/v1/users/signup",
                "/api/v1/hosts/signup"
            ).permitAll()
            .requestMatchers("/api/v1/users/**").hasRole("ROLE_USER")
            .requestMatchers("/api/v1/hosts/**").hasRole("ROLE_HOST")
            .requestMatchers("/api/v1/admin/**").hasRole("ROLE_ADMIN")
            .anyRequest().authenticated() // 그 외 모든 요청은 인증 필요
        )
        .formLogin(Customizer.withDefaults()) // 기본 로그인 폼 사용(추후 JWT 도입 시 제거 예정)
        .logout(LogoutConfigurer::permitAll // 모든 사용자가 로그아웃 가능하도록 허용(일시)
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
