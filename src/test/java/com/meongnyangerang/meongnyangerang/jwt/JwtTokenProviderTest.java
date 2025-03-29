package com.meongnyangerang.meongnyangerang.jwt;

import com.meongnyangerang.meongnyangerang.domain.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class JwtTokenProviderTest {

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  private String token;

  @BeforeEach
  void setUp() {
    // JWT 토큰 생성 (ROLE_USER 권한)
    token = jwtTokenProvider.createToken(1L, "test@example.com", "ROLE_USER", UserStatus.ACTIVE);
  }

}
