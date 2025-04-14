package com.meongnyangerang.meongnyangerang.scheduler;

import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AccommodationViewCountScheduler {

  private final AccommodationRepository accommodationRepository;

  @Scheduled(cron = "0 0 0 1 * ?") // 매달 1일 자정
//  @Scheduled(cron = "*/10 * * * * *")
  @Transactional
  public void resetViewCounts() {
    accommodationRepository.resetAllViewCounts();
  }
}
