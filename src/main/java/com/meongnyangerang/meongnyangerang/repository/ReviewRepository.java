package com.meongnyangerang.meongnyangerang.repository;

import com.meongnyangerang.meongnyangerang.domain.review.Review;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

  boolean existsByUserIdAndReservationId(Long userId, Long reservationId);

  @Query(value = "SELECT * FROM review r " +
      "WHERE r.user_id = :userId " +
      "AND (:cursorId = 0 OR r.id <= :cursorId) " +
      "AND r.report_count < 20 " +
      "ORDER BY r.created_at DESC LIMIT :size",
      nativeQuery = true)
  List<Review> findByUserId(
      @Param("userId") Long userId,
      @Param("cursorId") Long cursorId,
      @Param("size") int size);

  @Query(value = "SELECT * FROM review r " +
      "WHERE r.accommodation_id = :accommodationId " +
      "AND (:cursorId = 0 OR r.id <= :cursorId) " +
      "AND r.report_count < 20 " +
      "ORDER BY r.created_at DESC LIMIT :size",
      nativeQuery = true)
  List<Review> findByAccommodationId(
      @Param("accommodationId") Long accommodationId,
      @Param("cursorId") Long cursorId,
      @Param("size") int size);

  @Query("SELECT r FROM Review r " +
      "JOIN FETCH r.user " +
      "JOIN FETCH r.reservation " +
      "JOIN r.reservation res JOIN res.room rm " +
      "WHERE rm.accommodation.id = :accommodationId " +
      "AND (:cursorId IS NULL OR r.id < :cursorId) " +
      "AND r.reportCount < 20 " +
      "ORDER BY r.id DESC")
  List<Review> findByAccommodationIdWithCursor(
      @Param("accommodationId") Long accommodationId,
      @Param("cursorId") Long cursorId,
      Pageable pageable
  );

  int countByAccommodationId(Long accommodationId);
}
