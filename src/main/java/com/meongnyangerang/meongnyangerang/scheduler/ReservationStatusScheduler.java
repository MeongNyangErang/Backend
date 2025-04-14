package com.meongnyangerang.meongnyangerang.scheduler;

import com.meongnyangerang.meongnyangerang.domain.reservation.Reservation;
import com.meongnyangerang.meongnyangerang.domain.reservation.ReservationStatus;
import com.meongnyangerang.meongnyangerang.repository.ReservationRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReservationStatusScheduler {

  private final ReservationRepository reservationRepository;

  // 매일 새벽 2시에 실행
  @Scheduled(cron = "0 0 2 * * ?")
//  @Scheduled(cron = "*/10 * * * * *")
  public void updateReservationStatus() {
    LocalDate today = LocalDate.now();

    List<Reservation> reservations = reservationRepository.findByCheckOutDate(today);

    for (Reservation reservation : reservations) {
      reservation.setStatus(ReservationStatus.COMPLETED);
    }

    reservationRepository.saveAll(reservations);
  }

}
