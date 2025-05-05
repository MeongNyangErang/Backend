package com.meongnyangerang.meongnyangerang.controller;

import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.INVALID_REQUEST;

import com.meongnyangerang.meongnyangerang.domain.user.Role;
import com.meongnyangerang.meongnyangerang.dto.LoginResponse;
import com.meongnyangerang.meongnyangerang.dto.auth.KakaoUserInfoResponse;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.service.HostService;
import com.meongnyangerang.meongnyangerang.service.KakaoOAuthService;
import com.meongnyangerang.meongnyangerang.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/oauth")
public class KakaoOAuthController {

  private final KakaoOAuthService kakaoOAuthService;
  private final UserService userService;
  private final HostService hostService;

  @GetMapping("/kakao/callback")
  public ResponseEntity<LoginResponse> kakaoCallback(@RequestParam String code,
      @RequestParam Role role) {

    // 1. 카카오 Access Token 요청
    String kakaoAccessToken = kakaoOAuthService.getAccessToken(code);

    // 2. 사용자 정보 요청
    KakaoUserInfoResponse kakaoUserInfo = kakaoOAuthService.getUserInfo(kakaoAccessToken);

    // 3. 소셜 로그인 처리
    return ResponseEntity.ok(switch (role) {
      case ROLE_USER -> userService.loginWithKakao(kakaoUserInfo);
      case ROLE_HOST -> hostService.loginWithKakao(kakaoUserInfo);
      default -> throw new MeongnyangerangException(INVALID_REQUEST);
    });
  }
}
