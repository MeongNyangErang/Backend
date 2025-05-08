package com.meongnyangerang.meongnyangerang.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.HashSet;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

@Configuration
public class WebSocketMetricsConfig {

  private final MeterRegistry meterRegistry;
  private final Counter messageErrorCounter;
  private final Counter webSocketErrorCounter;
  private final Set<WebSocketSession> sessions = new HashSet<>();

  public WebSocketMetricsConfig(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;

    this.messageErrorCounter = Counter.builder("stomp.messages.errors")
        .description("Counter of STOMP message errors")
        .register(meterRegistry);

    this.webSocketErrorCounter = Counter.builder("websocket.errors")
        .description("Counter of WebSocket transport errors")
        .register(meterRegistry);
  }

  @Bean
  public WebSocketHandlerDecoratorFactory webSocketMetricsDecorator() {
    return handler -> new WebSocketHandlerDecorator(handler) {
      private final Counter sessionsCounter = meterRegistry.counter("websocket.sessions.total");
      private final Gauge activeSessionsGauge = Gauge.builder(
          "websocket.sessions.active", sessions::size).register(meterRegistry);

      @Override
      public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        sessionsCounter.increment();
        super.afterConnectionEstablished(session);
      }

      @Override
      public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus)
          throws Exception {
        sessions.remove(session);
        super.afterConnectionClosed(session, closeStatus);
      }

      @Override
      public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        // WebSocket 전송 에러 카운팅
        webSocketErrorCounter.increment();
        super.handleTransportError(session, exception);
      }
    };
  }

  @Bean
  public ChannelInterceptor stompMetricsInterceptor() {
    return new ChannelInterceptor() {
      private final Counter messageSentCounter = meterRegistry.counter("stomp.messages.sent");
      private final Timer messageProcessingTimer = meterRegistry.timer(
          "stomp.messages.processing.time");

      @Override
      public Message<?> preSend(Message<?> message, MessageChannel channel) {
        if (StompHeaderAccessor.wrap(message).getCommand() == StompCommand.SEND) {
          messageSentCounter.increment();
          return messageProcessingTimer.record(() -> message);
        }
        return message;
      }

      @Override
      public void afterSendCompletion(
          Message<?> message, MessageChannel channel, boolean sent, Exception ex
      ) {
        if (StompHeaderAccessor.wrap(message).getCommand() == StompCommand.SEND && ex != null) {
          messageErrorCounter.increment(); // 에러 발생 시 카운터 증가
        }
      }
    };
  }
}
