package com.meongnyangerang.meongnyangerang.repository;

import com.meongnyangerang.meongnyangerang.domain.reservation.Reservation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

  @Query(value = "SELECT * FROM reservation r " +
      "WHERE r.user_id = :userId AND r.status = :status " +
      "AND (:cursorId = 0 OR r.id <= :cursorId) " +
      "ORDER BY r.created_at DESC LIMIT :size",
      nativeQuery = true)
  List<Reservation> findByUserIdAndStatus(
      @Param("userId") Long userId,
      @Param("cursorId") Long cursorId,
      @Param("size") int size,
      @Param("status") String status);

  @Query(value = "SELECT r.* FROM reservation r " +
      "JOIN room rm ON r.room_id = rm.id " +
      "JOIN accommodation a ON rm.accommodation_id = a.id " +
      "JOIN host h ON a.host_id = h.id " +
      "WHERE h.id = :userId " +
      "AND r.status = :status " +
      "AND (:cursorId = 0 OR r.id <= :cursorId) " +
      "ORDER BY r.created_at DESC LIMIT :size",
      nativeQuery = true)
  List<Reservation> findByHostIdAndStatus(
      @Param("userId") Long userId,
      @Param("cursorId") Long cursorId,
      @Param("size") int size,
      @Param("status") String status);
}
