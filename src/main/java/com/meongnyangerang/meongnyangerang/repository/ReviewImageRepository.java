package com.meongnyangerang.meongnyangerang.repository;

import com.meongnyangerang.meongnyangerang.domain.review.ReviewImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {

  List<ReviewImage> findAllByReviewId(Long reviewId);

  List<ReviewImageProjection> findByReview_IdIn(List<Long> reviewIds);

  ReviewImage findFirstByReviewIdOrderByIdAsc(Long reviewId);
}
