package com.meongnyangerang.meongnyangerang.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
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
}
