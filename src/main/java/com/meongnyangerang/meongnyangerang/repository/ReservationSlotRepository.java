package com.meongnyangerang.meongnyangerang.repository;

import com.meongnyangerang.meongnyangerang.domain.reservation.ReservationSlot;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationSlotRepository extends JpaRepository<ReservationSlot, Long> {

  // 특정 객실이 날짜 범위(startDate ~ endDate)내에서 예약이 존재하는지 확인
  boolean existsByRoomIdAndReservedDateBetweenAndIsReserved(Long roomId, LocalDate startDate, LocalDate endDate, boolean isReserved);

  // 특정 날짜에 대한 예약 존재하는지 찾거나 생성하는 로직에 활용
  Optional<ReservationSlot> findByRoomIdAndReservedDate(Long roomId, LocalDate reservedDate);
}
