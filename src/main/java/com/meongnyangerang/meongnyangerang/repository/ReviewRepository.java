package com.meongnyangerang.meongnyangerang.repository;

import com.meongnyangerang.meongnyangerang.domain.review.Review;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

  boolean existsByUserIdAndReservationId(Long userId, Long reservationId);

  Page<Review> findByUserId(Long userId, Pageable pageable);

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

  Page<Review> findAllByAccommodationIdAndReportCountLessThan(
      Long accommodationId, Integer reportCount, Pageable pageable);

  int countByAccommodationId(Long accommodationId);

  boolean existsByReservationId(Long reservationId);

  List<Review> findTop5ByAccommodationIdOrderByCreatedAtDesc(Long accommodationId);

  List<Review> findTop10ByOrderByCreatedAtDesc();
}
