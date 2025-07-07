package com.meongnyangerang.meongnyangerang.repository;

import com.meongnyangerang.meongnyangerang.domain.review.Review;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

  boolean existsByUserIdAndReservationId(Long userId, Long reservationId);

  Page<Review> findByUserIdAndHiddenFalse(Long userId, Pageable pageable);

  Page<Review> findByAccommodationIdAndHiddenFalse(Long accommodationId, Pageable pageable);

  Page<Review> findAllByAccommodationIdAndReportCountLessThan(
      Long accommodationId, Integer reportCount, Pageable pageable);

  int countByAccommodationId(Long accommodationId);

  boolean existsByReservationId(Long reservationId);

  List<Review> findTop5ByAccommodationIdOrderByCreatedAtDesc(Long accommodationId);

  List<Review> findTop10ByOrderByCreatedAtDesc();

  List<Review> findByHiddenTrueAndHiddenAtBefore(LocalDateTime cutoff);
}
