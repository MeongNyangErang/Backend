package com.meongnyangerang.meongnyangerang.service;

import com.meongnyangerang.meongnyangerang.domain.review.ReviewReport;
import com.meongnyangerang.meongnyangerang.dto.chat.PageResponse;
import com.meongnyangerang.meongnyangerang.repository.ReviewReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReviewReportService {

  private final ReviewReportRepository reviewReportRepository;

  public PageResponse<ReviewReport> getReviews(Pageable pageable) {
    return PageResponse.from(reviewReportRepository.findAll(pageable));
  }
}
