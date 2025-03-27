package com.meongnyangerang.meongnyangerang.jwt;

import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.*;

import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.JwtCustomException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JwtTokenProvider {

  @Value("${jwt.secret}")
  private String secretKey;

  private Key key;

  // 암호화 알고리즘
  private final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

  private final long tokenTime = 1000L * 60 * 60; // 1시간

  // Bean 생성 후 자동 실행 (secretKey를 Key 객체로 변환하여 저장)
  @PostConstruct
  public void init() {
    key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
  }

  // 토큰 생성
  public String createToken(String email, String role, String userStatus) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + tokenTime);

    return Jwts.builder()
        .setSubject(email) // JWT payload의 subject(email)
        .claim("role", role) // 권한 정보
        .claim("status", userStatus) // UserStatus 추가
        .setIssuedAt(now) // 발급 시간
        .setExpiration(expiryDate) // 만료 시간
        .signWith(key, signatureAlgorithm) // 암호화 알고리즘
        .compact();
  }

  // JWT 토큰 유효성 검사
  public boolean validateToken(String token) {
    try {
      // JWT 검증 수행(검증 실패 시 JwtException 발생)
      Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
      return true;
    } catch (ExpiredJwtException e) {
      log.error("JWT 토큰이 만료되었습니다: {}", e.getMessage());
      throw new JwtCustomException(EXPIRED_JWT);
    } catch (MalformedJwtException e) {
      log.error("JWT 형식이 올바르지 않습니다: {}", e.getMessage());
      throw new JwtCustomException(INVALID_JWT_FORMAT);
    } catch (UnsupportedJwtException e) {
      log.error("지원되지 않는 JWT 토큰입니다: {}", e.getMessage());
      throw new JwtCustomException(UNSUPPORTED_JWT);
    } catch (SignatureException e) {
      log.error("JWT 서명이 유효하지 않습니다: {}", e.getMessage());
      throw new JwtCustomException(INVALID_JWT_SIGNATURE);
    } catch (Exception e) {
      log.error("JWT 검증 중 알 수 없는 오류 발생: {}", e.getMessage());
      throw new JwtCustomException(JWT_VALIDATION_ERROR);
    }
  }
}
