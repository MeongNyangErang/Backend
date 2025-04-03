package com.meongnyangerang.meongnyangerang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meongnyangerang.meongnyangerang.domain.admin.Admin;
import com.meongnyangerang.meongnyangerang.domain.admin.Notice;
import com.meongnyangerang.meongnyangerang.dto.NoticeRequest;
import com.meongnyangerang.meongnyangerang.repository.AdminRepository;
import com.meongnyangerang.meongnyangerang.repository.NoticeRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
public class NoticeServiceTest {

  @InjectMocks
  private NoticeService noticeService;

  @Mock
  private NoticeRepository noticeRepository;

  @Mock
  private AdminRepository adminRepository;

  @Test
  @DisplayName("공지사항 등록 성공 - 이미지 X")
  void createNoticeWithoutImageSuccess() {
    // given
    Long adminId = 1L;
    Admin admin = Admin.builder()
        .id(adminId)
        .email("admin@meongnyang.com")
        .build();

    NoticeRequest request = new NoticeRequest("제목", "내용");
    MultipartFile imageFile = null;

    when(adminRepository.findById(adminId)).thenReturn(Optional.of(admin));

    ArgumentCaptor<Notice> captor = ArgumentCaptor.forClass(Notice.class);

    // when
    noticeService.createNotice(adminId, request, imageFile);

    // then
    verify(noticeRepository).save(captor.capture());
    Notice savedNotice = captor.getValue();

    assertEquals("제목", savedNotice.getTitle());
    assertEquals("내용", savedNotice.getContent());
    assertEquals(admin, savedNotice.getAdmin());
    assertNull(savedNotice.getImageUrl());
  }
}
