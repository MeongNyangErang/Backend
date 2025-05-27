package com.meongnyangerang.meongnyangerang.repository;

import com.meongnyangerang.meongnyangerang.domain.reservation.ReservationSlot;
import com.meongnyangerang.meongnyangerang.domain.room.Room;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationSlotRepository extends JpaRepository<ReservationSlot, Long> {

  // 특정 객실이 날짜 범위(startDate ~ endDate)내에서 예약이 존재하는지 확인
  boolean existsByRoomIdAndReservedDateBetweenAndIsReserved(Long roomId, LocalDate startDate,
      LocalDate endDate, boolean isReserved);

  boolean existsByRoomIdAndReservedDateBetweenAndHoldTrue(Long roomId, LocalDate start, LocalDate end);

  // 특정 날짜에 대한 예약 존재하는지 찾거나 생성하는 로직에 활용
  Optional<ReservationSlot> findByRoomIdAndReservedDate(Long roomId, LocalDate reservedDate);

  @Query("""
          SELECT DISTINCT rs.room.id
          FROM ReservationSlot rs
          WHERE rs.isReserved = true
            AND rs.reservedDate BETWEEN :checkInDate AND :checkOutDate
      """)
  List<Long> findReservedRoomIdsBetweenDates(@Param("checkInDate") LocalDate checkInDate,
      @Param("checkOutDate") LocalDate checkOutDate);

  List<ReservationSlot> findByRoomAndReservedDateBetween(Room room, LocalDate startDate,
      LocalDate endDate);

  @Modifying
  @Query("DELETE FROM ReservationSlot rs WHERE rs.room.id = :roomId")
  void deleteAllByRoomId(@Param("roomId") Long roomId);
}
