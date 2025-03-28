package com.meongnyangerang.meongnyangerang.jwt;

import com.meongnyangerang.meongnyangerang.exception.JwtCustomException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider jwtTokenProvider;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    // 토큰 추출
    String token = resolveToken(request);

    if (token != null) {
      try {
        // 토큰 유효성 검사
        if (jwtTokenProvider.validateToken(token)) {
          // 유효한 토큰이면 사용자 인증 정보 생성
          Authentication authentication = jwtTokenProvider.getAuthentication(token);
          // 사용자 정보를 SecurityContext에 저장
          SecurityContextHolder.getContext().setAuthentication(authentication);
        }
      } catch (JwtCustomException e) {
        SecurityContextHolder.clearContext(); // 잘못된 토큰일 경우 초기화
        log.error("JWT 인증 실패: {}", e.getMessage());
        response.sendError(e.getHttpStatus().value(), e.getMessage());
        return;
      }
    }

    filterChain.doFilter(request, response);
  }

  // HTTP 요청의 Authorization 헤더에서 토큰 추출
  private String resolveToken(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }
    return null;
  }
}
