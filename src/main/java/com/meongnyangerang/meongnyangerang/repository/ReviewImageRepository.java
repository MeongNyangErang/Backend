package com.meongnyangerang.meongnyangerang.repository;

import com.meongnyangerang.meongnyangerang.domain.review.ReviewImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {

  ReviewImage findByReviewId(Long reviewId);

  List<ReviewImage> findAllByReviewId(Long reviewId);

  /**
   * 리뷰 ID와 이미지 URL만 조회하는 쿼리
   */
  @Query("SELECT " +
      "ri.review.id as reviewId, " +
      "ri.imageUrl as imageUrl " +
      "FROM ReviewImage ri " +
      "WHERE ri.review.id IN :reviewIds")
  List<ReviewImageProjection> findByReviewIds(@Param("reviewIds") List<Long> reviewIds);
}
