package com.meongnyangerang.meongnyangerang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.meongnyangerang.meongnyangerang.domain.review.ReportStatus;
import com.meongnyangerang.meongnyangerang.domain.review.ReporterType;
import com.meongnyangerang.meongnyangerang.domain.review.ReviewReport;
import com.meongnyangerang.meongnyangerang.dto.chat.PageResponse;
import com.meongnyangerang.meongnyangerang.repository.ReviewReportRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class ReviewReportServiceTest {

  @Mock
  private ReviewReportRepository reviewReportRepository;

  @InjectMocks
  private ReviewReportService reviewReportService;

  private Pageable pageable;

  private final static int PAGE_SIZE = 20;

  @BeforeEach
  public void setUp() {
    pageable = PageRequest.of(0, PAGE_SIZE, Sort.by("createdAt")
        .descending());
  }

  @Test
  public void testGetReviews() {
    ReviewReport report1 = ReviewReport.builder()
        .id(1L)
        .reporterId(10L)
        .type(ReporterType.USER)
        .reason("욕설")
        .status(ReportStatus.PENDING)
        .build();

    ReviewReport report2 = ReviewReport.builder()
        .id(2L)
        .reporterId(20L)
        .type(ReporterType.HOST)
        .reason("광고")
        .status(ReportStatus.COMPLETED)
        .build();

    List<ReviewReport> reviewReports = List.of(report1, report2);
    Page<ReviewReport> page = new PageImpl<>(reviewReports, pageable, reviewReports.size());

    when(reviewReportRepository.findAll(pageable)).thenReturn(page);

    // Then: 서비스 메서드 호출 후 결과 검증
    PageResponse<ReviewReport> response = reviewReportService.getReviews(pageable);

    // 페이지 내용 검증
    assertEquals(2, response.content().size());
    assertEquals(0, response.page());
    assertEquals(PAGE_SIZE, response.size());
    assertEquals(1, response.totalPages());
    assertTrue(response.first());
    assertTrue(response.last());
  }
}