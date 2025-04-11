package com.meongnyangerang.meongnyangerang.config;

import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Slf4j
@RequiredArgsConstructor
@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
  // WebSocket과 함께 메시지 브로커를 활성화

  private final JwtTokenProvider jwtTokenProvider;

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    // WebSocket 연결을 위한 엔드포인트 설정
    registry.addEndpoint("/ws")
        .setAllowedOriginPatterns("*") // CORS 설정 (모든 도메인 접근 허용)
        .withSockJS(); // WebSocket을 지원하지 않을 때 자동으로 대체 기술 사용
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    // 서버 -> 클라이언트 전송 prefix
    registry.enableSimpleBroker("/subscribe");

    // 클라이언트 -> 서버 전송 prefix
    registry.setApplicationDestinationPrefixes("/app");

    // 1:1 개인 메시지 prefix
    registry.setUserDestinationPrefix("/user");
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    log.debug("WebSocket Inbound Channel");

    registration.interceptors(new ChannelInterceptor() {
      @Override
      public Message<?> preSend(Message<?> message, MessageChannel channel) {
        log.debug("WebSocket preSend");
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
            message, StompHeaderAccessor.class);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
          log.debug("STOMP CONNECT 요청... 사용자 검증...");
          String authorizationHeader = accessor.getFirstNativeHeader("Authorization");

          if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);

            Authentication authentication = jwtTokenProvider.getAuthentication(token);
            accessor.setUser(authentication); // WebSocket 세션에 인증 정보 저장
          } else {
            log.error("STOMP CONNECT 요청에서 토큰을 찾을 수 없습니다.");
            throw new MeongnyangerangException(ErrorCode.WEBSOCKET_SERVER_ERROR);
          }
        }
        return message;
      }
    });
  }
}
