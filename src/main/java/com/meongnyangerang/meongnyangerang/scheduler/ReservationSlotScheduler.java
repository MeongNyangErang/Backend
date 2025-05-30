package com.meongnyangerang.meongnyangerang.scheduler;

import com.meongnyangerang.meongnyangerang.domain.reservation.ReservationSlot;
import com.meongnyangerang.meongnyangerang.repository.ReservationSlotRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationSlotScheduler {

  private final ReservationSlotRepository reservationSlotRepository;

  /**
   * 만료된 hold 슬롯 초기화 (5분마다 실행)
   */
  @Scheduled(cron = "0 */5 * * * *") // 매 5분마다 실행
  @Transactional
  public void releaseExpiredHoldSlots() {
    LocalDateTime now = LocalDateTime.now();
    List<ReservationSlot> expiredSlots = reservationSlotRepository.findAllExpiredHoldSlots(now);

    for (ReservationSlot slot : expiredSlots) {
      slot.setHold(false);
      slot.setExpiredAt(null);
    }

    reservationSlotRepository.saveAll(expiredSlots);

    if (!expiredSlots.isEmpty()) {
      log.info("만료된 hold 슬롯 {}건 초기화 완료", expiredSlots.size());
    }
  }
}
