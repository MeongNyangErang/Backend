package com.meongnyangerang.meongnyangerang.repository;

import com.meongnyangerang.meongnyangerang.domain.reservation.Reservation;
import com.meongnyangerang.meongnyangerang.domain.reservation.ReservationStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

  Page<Reservation> findByUserIdAndStatus(Long user_id, ReservationStatus status,
      Pageable pageable);

  @Query("SELECT r FROM Reservation r " +
      "WHERE r.room.accommodation.host.id = :hostId " +
      "AND r.status = :status")
  Page<Reservation> findByHostIdAndStatus(@Param("hostId") Long hostId,
      @Param("status") ReservationStatus status,
      Pageable pageable);

  boolean existsByUserIdAndStatus(Long userId, ReservationStatus status);

  @Query("""
          SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
          FROM Reservation r
          WHERE r.room.accommodation.host.id = :hostId
          AND r.status = :status
      """)
  boolean existsByHostIdAndStatus(Long hostId, ReservationStatus status);

  List<Reservation> findByCheckOutDate(LocalDate checkOutDate);

  List<Reservation> findByCheckInDateAndStatus(
      LocalDate date, ReservationStatus reservationStatus);
}
