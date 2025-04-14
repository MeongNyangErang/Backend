package com.meongnyangerang.meongnyangerang.service.notification;

import com.meongnyangerang.meongnyangerang.domain.reservation.Reservation;
import com.meongnyangerang.meongnyangerang.domain.reservation.ReservationStatus;
import com.meongnyangerang.meongnyangerang.repository.ReservationRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationReminderScheduler {

  private final ReservationRepository reservationRepository;
  private final NotificationService notificationService;

  @Scheduled(cron = "0 0 9 * * ?")  // 매일 오전 9시에 실행
  //@Scheduled(cron = "*/10 * * * * *") // 10초마다 (테스트)
  public void sendReservationReminders() {
    log.info("예약 알림 스케줄러 실행 시작");
    LocalDate tomorrow = LocalDate.now().plusDays(1);

    List<Reservation> tomorrowReservations = reservationRepository
        .findByCheckInDateAndStatus(tomorrow, ReservationStatus.RESERVED);

    log.info("내일({}) 체크인 예약 {}건 발견", tomorrow, tomorrowReservations.size());

    for (Reservation reservation : tomorrowReservations) {
      try {
        notificationService.sendReservationReminderNotification(reservation);
        log.info("예약 ID: {} 리마인더 알림 발송 완료", reservation.getId());
      } catch (Exception e) {
        log.error("예약 ID: {} 알림 발송 실패: {}", reservation.getId(), e.getMessage());
      }
    }
    log.info("예약 알림 스케줄러 실행 완료");
  }
}
