package com.meongnyangerang.meongnyangerang.service;

import com.meongnyangerang.meongnyangerang.domain.review.ReportStatus;
import com.meongnyangerang.meongnyangerang.domain.review.ReviewReport;
import com.meongnyangerang.meongnyangerang.dto.ReviewReportResponse;
import com.meongnyangerang.meongnyangerang.dto.chat.PageResponse;
import com.meongnyangerang.meongnyangerang.repository.ReviewReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReviewReportService {

  private final ReviewReportRepository reviewReportRepository;

  public PageResponse<ReviewReportResponse> getReviews(Pageable pageable) {
    Page<ReviewReport> reviewReportResponse = reviewReportRepository.findAllByStatus(
        pageable, ReportStatus.PENDING);
    Page<ReviewReportResponse> responses = reviewReportResponse.map(ReviewReportResponse::from);
    return PageResponse.from(responses);
  }
}
