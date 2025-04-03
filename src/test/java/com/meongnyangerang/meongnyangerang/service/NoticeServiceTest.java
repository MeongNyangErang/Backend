package com.meongnyangerang.meongnyangerang.service;

import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.NOT_EXIST_ACCOUNT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meongnyangerang.meongnyangerang.domain.admin.Admin;
import com.meongnyangerang.meongnyangerang.domain.admin.Notice;
import com.meongnyangerang.meongnyangerang.dto.NoticeRequest;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.AdminRepository;
import com.meongnyangerang.meongnyangerang.repository.NoticeRepository;
import com.meongnyangerang.meongnyangerang.service.image.ImageService;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
public class NoticeServiceTest {

  @InjectMocks
  private NoticeService noticeService;

  @Mock
  private NoticeRepository noticeRepository;

  @Mock
  private AdminRepository adminRepository;

  @Mock
  private ImageService imageService;

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

  @Test
  @DisplayName("공지사항 등록 성공 - 이미지 포함")
  void createNoticeWithImageSuccess() {
    // given
    Long adminId = 1L;
    Admin admin = Admin.builder().id(adminId).email("admin@meongnyang.com").build();
    NoticeRequest request = new NoticeRequest("공지 제목", "공지 내용");

    MockMultipartFile imageFile = new MockMultipartFile(
        "image", "notice.jpg", "image/jpeg", "fake-image".getBytes());

    when(adminRepository.findById(adminId)).thenReturn(Optional.of(admin));
    when(imageService.storeImage(imageFile)).thenReturn("https://s3.bucket/notice.jpg");

    ArgumentCaptor<Notice> captor = ArgumentCaptor.forClass(Notice.class);

    // when
    noticeService.createNotice(adminId, request, imageFile);

    // then
    verify(noticeRepository).save(captor.capture());
    Notice savedNotice = captor.getValue();

    assertEquals("공지 제목", savedNotice.getTitle());
    assertEquals("공지 내용", savedNotice.getContent());
    assertEquals("https://s3.bucket/notice.jpg", savedNotice.getImageUrl());
    assertEquals(admin, savedNotice.getAdmin());
  }

  @Test
  @DisplayName("공지사항 등록 실패 - 존재하지 않는 관리자")
  void createNoticeFailAdminNotFound() {
    // given
    Long adminId = 999L;
    NoticeRequest request = new NoticeRequest("제목", "내용");
    MultipartFile imageFile = null;

    when(adminRepository.findById(adminId)).thenReturn(Optional.empty());

    // when & then
    MeongnyangerangException exception = assertThrows(MeongnyangerangException.class,
        () -> noticeService.createNotice(adminId, request, imageFile));

    assertEquals(NOT_EXIST_ACCOUNT, exception.getErrorCode());
    verify(noticeRepository, never()).save(Mockito.any()); // 이건 호출 방지 검증용이라 예외적으로 사용해도 OK
  }

  @Test
  @DisplayName("공지사항 수정 성공 - 기존 이미지 있음")
  void updateNoticeSuccessWithOldImage() {
    // given
    Long adminId = 1L;
    Long noticeId = 1L;

    Admin admin = Admin.builder().id(adminId).build();
    Notice notice = Notice.builder()
        .id(noticeId)
        .admin(admin)
        .title("old title")
        .content("old content")
        .imageUrl("http://s3.com/old-image.jpg")
        .build();

    NoticeRequest request = new NoticeRequest("새로운 제목", "새로운 내용");

    MockMultipartFile newImage = new MockMultipartFile(
        "image", "new.jpg", "image/jpeg", "new image".getBytes()
    );

    given(adminRepository.findById(adminId)).willReturn(Optional.of(admin));
    given(noticeRepository.findById(noticeId)).willReturn(Optional.of(notice));
    given(imageService.storeImage(newImage)).willReturn("http://s3.com/new-image.jpg");

    // when
    noticeService.updateNotice(adminId, noticeId, request, newImage);

    // then
    assertEquals("새로운 제목", notice.getTitle());
    assertEquals("새로운 내용", notice.getContent());
    assertEquals("http://s3.com/new-image.jpg", notice.getImageUrl());
    verify(imageService).registerImagesForDeletion("http://s3.com/old-image.jpg");
  }
}
