package com.meongnyangerang.meongnyangerang.jwt;

import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.EXPIRED_JWT;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.INVALID_AUTHORIZED;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.INVALID_JWT_FORMAT;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.INVALID_JWT_SIGNATURE;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.JWT_VALIDATION_ERROR;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.UNSUPPORTED_JWT;

import com.meongnyangerang.meongnyangerang.domain.admin.AdminStatus;
import com.meongnyangerang.meongnyangerang.domain.host.HostStatus;
import com.meongnyangerang.meongnyangerang.domain.user.Role;
import com.meongnyangerang.meongnyangerang.domain.user.UserStatus;
import com.meongnyangerang.meongnyangerang.exception.JwtCustomException;
import com.meongnyangerang.meongnyangerang.security.UserDetailsImpl;
import io.jsonwebtoken.Claims;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JwtTokenProvider {

  @Value("${JWT_SECRET}")
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
  public String createToken(Long id, String email, String role, Enum<?> status) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + tokenTime);

    return Jwts.builder()
        .setSubject(email) // JWT payload의 subject(email)
        .claim("id", id) // 사용자 ID 추가
        .claim("role", role) // 권한 정보
        .claim("status", status.name())
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

  // 사용자 인증 정보 생성
  public Authentication getAuthentication(String token) {
    Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
        .parseClaimsJws(token)
        .getBody();

    Long id = claims.get("id", Long.class); // JWT에서 ID 추출
    String email = claims.getSubject(); // JWT에서 email 추출
    String roleString = claims.get("role", String.class); // JWT에서 role을 String으로 가져옴
    String statusString = claims.get("status", String.class); // JWT에서 status를 String으로 가져옴

    // String -> UserRole 변환
    Role role = Role.valueOf(roleString);

    // JWT 토큰에서 추출한 statusString -> Enum<?> 으로 형변환
    Enum<?> status = switch (role) {
      case ROLE_USER -> UserStatus.valueOf(statusString);
      case ROLE_HOST -> HostStatus.valueOf(statusString);
      case ROLE_ADMIN -> AdminStatus.valueOf(statusString);
    };

    // 추가 검증 (호스트 - 승인 대기중, 사용자 - 삭제 상태, 호스트 - 삭제 상태) -> 예외 처리
    if ((role == Role.ROLE_HOST && status == HostStatus.PENDING) ||
        (status == UserStatus.DELETED || status == HostStatus.DELETED)) {
      throw new JwtCustomException(INVALID_AUTHORIZED);
    }

    // UserDetailsImpl에 JWT 정보만 전달
    UserDetails userDetails = new UserDetailsImpl(id, email, "", role, status);

    return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
  }
}
