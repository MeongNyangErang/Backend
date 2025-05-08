package com.meongnyangerang.meongnyangerang.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CommonApiMetricsConfig implements WebMvcConfigurer {

  private final MeterRegistry meterRegistry;
  private final String API_URL_PREFIX = "/api/v1";

  // 경로 패턴 상수화 및 맵 구성
  private final Map<Pattern, String> URI_PATTERNS = Map.of(
      Pattern.compile(API_URL_PREFIX + "/users/reservations/\\d+"),
      API_URL_PREFIX + "/users/reservations/{id}",

      Pattern.compile(API_URL_PREFIX + "/accommodations/\\d+"),
      API_URL_PREFIX + "/accommodations/{id}"
      // 추가 패턴은 여기에...
  );

  // 메트릭 캐싱을 위한 맵
  private final ConcurrentMap<String, Timer> timerCache = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, Counter> counterCache = new ConcurrentHashMap<>();


  @Bean
  public HandlerInterceptor apiMetricsInterceptor() {
    return new HandlerInterceptor() {
      @Override
      public boolean preHandle(
          HttpServletRequest request,
          HttpServletResponse response,
          Object handler
      ) {
        request.setAttribute("requestStartTime", System.currentTimeMillis());
        return true;
      }

      @Override
      public void afterCompletion(
          HttpServletRequest request,
          HttpServletResponse response,
          Object handler,
          Exception ex
      ) {
        try {
          Long startTime = (Long) request.getAttribute("requestStartTime");
          if (startTime == null) {
            log.warn("[부하 테스트] 요청 시간 측정되지 않음");
            return;
          }

          // 표준화된 경로와 메타데이터 추출
          String path = getStandardizedRequestPath(request);
          String method = request.getMethod();
          String statusGroup = getStatusGroup(response.getStatus());
          boolean isSuccess = response.getStatus() < 400;

          Tags tags = Tags.of(
              "path", path,
              "method", method,
              "status", statusGroup
          );

          // 1. 처리 시간 기록 (Timer 사용)
          long duration = System.currentTimeMillis() - startTime;
          String timerKey = "timer:" + path + ":" + method + ":" + statusGroup;
          Timer timer = timerCache.computeIfAbsent(timerKey, k ->
              Timer.builder("api.request.duration")
                  .tags(tags)
                  .description("API request duration")
                  .register(meterRegistry)
          );
          timer.record(duration, TimeUnit.MILLISECONDS);

          // 2. 요청 총 수 기록
          String counterKey = "counter:" + path + ":" + method + ":" + statusGroup;
          Counter counter = counterCache.computeIfAbsent(counterKey, k ->
              Counter.builder("api.request.total")
                  .tags(tags)
                  .description("Total API requests")
                  .register(meterRegistry)
          );
          counter.increment();

          // 3. 성공/실패 요청 구분
          String resultKey =
              "result:" + path + ":" + method + ":" + (isSuccess ? "success" : "failure");
          Counter resultCounter = counterCache.computeIfAbsent(resultKey, k ->
              Counter.builder("api.request.result")
                  .tags(tags.and("success", String.valueOf(isSuccess)))
                  .description("API request results (success/failure)")
                  .register(meterRegistry)
          );
          resultCounter.increment();
        } catch (Exception e) {
          log.error("[부하 테스트] Error recording metrics: {}", e.getMessage());
        }
      }

      /**
       * HTTP 상태 코드를 그룹화하여 카디널리티 감소
       */
      private String getStatusGroup(int status) {
        if (status < 200) {
          return "1xx";
        }
        if (status < 300) {
          return "2xx";
        }
        if (status < 400) {
          return "3xx";
        }
        if (status < 500) {
          return "4xx";
        }
        return "5xx";
      }
    };
  }

  /**
   * 요청 경로를 표준화하여 메트릭 카디널리티를 줄임
   */
  private String getStandardizedRequestPath(HttpServletRequest request) {
    String uri = request.getRequestURI();

    for (Map.Entry<Pattern, String> entry : URI_PATTERNS.entrySet()) {
      if (entry.getKey().matcher(uri).matches()) {
        return entry.getValue();
      }
    }
    return uri; // 매칭되는 패턴이 없으면 원본 URI 반환
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(apiMetricsInterceptor())
        .addPathPatterns("/api/**");
  }
}
