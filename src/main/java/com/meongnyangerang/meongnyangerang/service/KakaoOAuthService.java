package com.meongnyangerang.meongnyangerang.service;

import com.meongnyangerang.meongnyangerang.dto.auth.KakaoTokenResponse;
import com.meongnyangerang.meongnyangerang.dto.auth.KakaoUserInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class KakaoOAuthService {

  private final RestTemplate restTemplate = new RestTemplate();

  @Value("${kakao.client-id}")
  private String clientId;

  @Value("${kakao.redirect-uri}")
  private String redirectUri;

  @Value("${kakao.token-uri}")
  private String tokenUri;

  @Value("${kakao.user-info-uri}")
  private String userInfoUri;

  public String getAccessToken(String code) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("grant_type", "authorization_code");
    params.add("client_id", clientId);
    params.add("redirect_uri", redirectUri);
    params.add("code", code);

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

    ResponseEntity<KakaoTokenResponse> response = restTemplate.postForEntity(
        tokenUri, request, KakaoTokenResponse.class);

    return response.getBody().getAccessToken();
  }

  public KakaoUserInfoResponse getUserInfo(String accessToken) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);

    HttpEntity<Void> request = new HttpEntity<>(headers);

    ResponseEntity<KakaoUserInfoResponse> response = restTemplate.exchange(
        userInfoUri, HttpMethod.GET, request, KakaoUserInfoResponse.class);

    return response.getBody();
  }
}
