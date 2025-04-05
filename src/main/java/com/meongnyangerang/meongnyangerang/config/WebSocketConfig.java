package com.meongnyangerang.meongnyangerang.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
  // WebSocket과 함께 메시지 브로커를 활성화

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    // WebSocket 연결을 위한 엔드포인트 설정
    registry.addEndpoint("/ws")
        .setAllowedOriginPatterns("*")  // CORS 설정 (모든 도메인 접근 허용)
        .withSockJS();  // SockJS 지원 활성화 (WebSocket을 지원하지 않는 브라우저에서 폴백(fallback) 옵션을 제공)
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    // 클라이언트로 메시지를 보낼 때 사용할 prefix
    registry.enableSimpleBroker("/topic", "/queue");

    // 서버로 메시지를 보낼 때 사용할 prefix
    registry.setApplicationDestinationPrefixes("/app");

    // 사용자별 구독 경로에 사용될 prefix
    registry.setUserDestinationPrefix("/user");
  }
}
