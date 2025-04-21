package com.meongnyangerang.meongnyangerang.repository;

import com.meongnyangerang.meongnyangerang.domain.review.ReviewReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {

}
