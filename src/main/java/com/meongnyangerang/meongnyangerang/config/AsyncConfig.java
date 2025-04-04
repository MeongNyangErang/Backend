package com.meongnyangerang.meongnyangerang.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

  @Bean(name = "taskExecutor")
  public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2); // 기본 스레드 풀 크기
    executor.setMaxPoolSize(5); // 최대 스레드 풀 크기
    executor.setQueueCapacity(100); // 작업 대기열
    executor.setThreadNamePrefix("Async-"); // 스레드 접두사 이름
    executor.initialize();
    return executor;
  }
}
