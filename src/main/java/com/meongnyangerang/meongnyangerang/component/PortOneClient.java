package com.meongnyangerang.meongnyangerang.component;

import com.meongnyangerang.meongnyangerang.dto.portone.PaymentInfo;
import com.meongnyangerang.meongnyangerang.dto.portone.PaymentResponse;
import com.meongnyangerang.meongnyangerang.dto.portone.TokenResponse;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class PortOneClient {

  private final RestTemplate restTemplate = new RestTemplate();

  @Value("${PORTONE_API_KEY}")
  private String apiKey;

  @Value("${PORTONE_API_SECRET}")
  private String apiSecret;

  private static final String TOKEN_URL = "https://api.iamport.kr/users/getToken";
  private static final String PAYMENT_URL = "https://api.iamport.kr/payments/";

  private String getAccessToken() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    Map<String, String> body = Map.of(
        "imp_key", apiKey,
        "imp_secret", apiSecret
    );

    HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

    ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
        TOKEN_URL, request, TokenResponse.class);

    if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
      throw new MeongnyangerangException(ErrorCode.PAYMENT_AUTHORIZATION_FAILED);
    }

    return response.getBody().getResponse().getAccessToken();
  }

  public PaymentInfo getPaymentByImpUid(String impUid) {
    String accessToken = getAccessToken();

    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", accessToken);

    HttpEntity<Void> request = new HttpEntity<>(headers);

    ResponseEntity<PaymentResponse> response = restTemplate.exchange(
        PAYMENT_URL + impUid,
        HttpMethod.GET,
        request,
        PaymentResponse.class
    );

    if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
      throw new MeongnyangerangException(ErrorCode.PAYMENT_NOT_FOUND);
    }

    return response.getBody().getResponse();
  }

  public void cancelPayment(String impUid, String reason, Long amount) {
    String accessToken = getAccessToken();

    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", accessToken);
    headers.setContentType(MediaType.APPLICATION_JSON);

    Map<String, Object> body = new HashMap<>();
    body.put("imp_uid", impUid);
    body.put("reason", reason);
    body.put("checksum", amount); // 실제 결제 금액과 일치해야 함

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

    ResponseEntity<String> response = restTemplate.postForEntity(
        "https://api.iamport.kr/payments/cancel", request, String.class);

    if (!response.getStatusCode().is2xxSuccessful()) {
      throw new MeongnyangerangException(ErrorCode.PAYMENT_CANCELLATION_FAILED);
    }
  }

}

