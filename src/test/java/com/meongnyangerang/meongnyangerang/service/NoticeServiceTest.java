package com.meongnyangerang.meongnyangerang.service;

import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.NOT_EXIST_ACCOUNT;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meongnyangerang.meongnyangerang.domain.admin.Admin;
import com.meongnyangerang.meongnyangerang.domain.admin.Notice;
import com.meongnyangerang.meongnyangerang.dto.NoticeDetailResponse;
import com.meongnyangerang.meongnyangerang.dto.NoticeRequest;
import com.meongnyangerang.meongnyangerang.dto.NoticeSimpleResponse;
import com.meongnyangerang.meongnyangerang.dto.chat.PageResponse;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.AdminRepository;
import com.meongnyangerang.meongnyangerang.repository.NoticeRepository;
import com.meongnyangerang.meongnyangerang.service.image.ImageService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    verify(imageService).deleteImageAsync("http://s3.com/old-image.jpg");
  }

  @Test
  @DisplayName("공지사항 수정 성공 - 기존 이미지 없음")
  void updateNotice_success_withoutOldImage() {
    // given
    Long adminId = 2L;
    Long noticeId = 20L;

    Admin admin = Admin.builder().id(adminId).email("admin2@example.com").build();
    Notice notice = Notice.builder()
        .id(noticeId)
        .admin(admin)
        .title("공지 제목")
        .content("공지 내용")
        .imageUrl(null) // 기존 이미지 없음
        .build();

    NoticeRequest request = new NoticeRequest("수정 제목", "수정 내용");

    MockMultipartFile newImage = new MockMultipartFile(
        "image", "newfile.jpg", "image/jpeg", "image-content".getBytes());

    given(adminRepository.findById(2L)).willReturn(Optional.of(admin));
    given(noticeRepository.findById(20L)).willReturn(Optional.of(notice));
    given(imageService.storeImage(newImage)).willReturn("http://s3.com/newfile.jpg");

    // when
    noticeService.updateNotice(adminId, noticeId, request, newImage);

    // then
    assertEquals("수정 제목", notice.getTitle());
    assertEquals("수정 내용", notice.getContent());
    assertEquals("http://s3.com/newfile.jpg", notice.getImageUrl());
    verify(imageService).storeImage(newImage);
    verify(imageService, never()).deleteImageAsync(anyString()); // 삭제 등록 안 됨
  }

  @Test
  @DisplayName("공지사항 수정 실패 - 존재하지 않는 관리자")
  void updateNotice_fail_adminNotFound() {
    // given
    Long adminId = 999L;
    Long noticeId = 1L;
    NoticeRequest request = new NoticeRequest("제목", "내용");

    given(adminRepository.findById(adminId)).willReturn(Optional.empty());

    // when & then
    assertThrows(MeongnyangerangException.class, () ->
        noticeService.updateNotice(adminId, noticeId, request, null));
  }

  @Test
  @DisplayName("공지사항 수정 실패 - 존재하지 않는 공지사항")
  void updateNotice_fail_noticeNotFound() {
    // given
    Long adminId = 3L;
    Long noticeId = 300L;

    Admin admin = Admin.builder().id(adminId).email("admin3@example.com").build();
    NoticeRequest request = new NoticeRequest("제목", "내용");

    given(adminRepository.findById(adminId)).willReturn(Optional.of(admin));
    given(noticeRepository.findById(noticeId)).willReturn(Optional.empty());

    // when & then
    assertThrows(MeongnyangerangException.class, () ->
        noticeService.updateNotice(adminId, noticeId, request, null));
  }

  @Test
  @DisplayName("공지사항 삭제 성공")
  void deleteNoticeSuccess() {
    Long adminId = 1L;
    Long noticeId = 10L;
    Admin admin = Admin.builder().id(adminId).build();
    Notice notice = Notice.builder()
        .id(noticeId)
        .admin(admin)
        .imageUrl("https://s3.com/image/notice.jpg")
        .build();

    given(adminRepository.findById(adminId)).willReturn(Optional.of(admin));
    given(noticeRepository.findById(noticeId)).willReturn(Optional.of(notice));

    // when
    assertDoesNotThrow(() -> noticeService.deleteNotice(adminId, noticeId));

    // then
    verify(imageService).deleteImageAsync("https://s3.com/image/notice.jpg");
    verify(noticeRepository).delete(notice);
  }

  @Test
  @DisplayName("공지사항 삭제 실패 - 존재하지 않는 관리자")
  void deleteNoticeFailNotExistAdmin() {
    // given
    given(adminRepository.findById(1L)).willReturn(Optional.empty());

    // when
    MeongnyangerangException exception = assertThrows(MeongnyangerangException.class,
        () -> noticeService.deleteNotice(1L, 10L));

    // then
    assertEquals(ErrorCode.NOT_EXIST_ACCOUNT, exception.getErrorCode());
    assertEquals("해당 계정은 존재하지 않습니다.", exception.getMessage());
  }

  @Test
  @DisplayName("공지사항 삭제 실패 - 존재하지 않는 공지사항")
  void deleteNoticeFailNotExistNotice() {
    // given
    Long adminId = 1L;
    given(adminRepository.findById(adminId)).willReturn(
        Optional.of(Admin.builder().id(adminId).build()));
    given(noticeRepository.findById(99L)).willReturn(Optional.empty());

    // when
    MeongnyangerangException exception = assertThrows(MeongnyangerangException.class,
        () -> noticeService.deleteNotice(adminId, 99L));

    // then
    assertEquals(ErrorCode.NOT_EXIST_NOTICE, exception.getErrorCode());
    assertEquals("존재하지 않는 공지사항입니다.", exception.getMessage());
  }

  @Test
  @DisplayName("공지사항 목록 조회 성공")
  void getNoticeList_success() {
    // given
    Pageable pageable = PageRequest.of(0, 2, Sort.by("createdAt").descending());

    Notice notice1 = Notice.builder()
        .id(1L)
        .title("첫 번째 공지")
        .content("내용1")
        .createdAt(LocalDateTime.now().minusDays(1))
        .build();

    Notice notice2 = Notice.builder()
        .id(2L)
        .title("두 번째 공지")
        .content("내용2")
        .createdAt(LocalDateTime.now())
        .build();

    Page<Notice> mockPage = new PageImpl<>(List.of(notice2, notice1), pageable, 2);

    given(noticeRepository.findAll(pageable)).willReturn(mockPage);

    // when
    PageResponse<NoticeSimpleResponse> result = noticeService.getNoticeList(pageable);

    // then
    assertEquals(2, result.content().size());
    assertEquals("두 번째 공지", result.content().get(0).title());
    assertEquals(0, result.page());
    assertEquals(2, result.totalElements());
    verify(noticeRepository).findAll(pageable);
  }

  @Test
  @DisplayName("공지사항 상세 조회 성공")
  void getNoticeDetailSuccess() {
    // given
    Long noticeId = 1L;
    Notice notice = Notice.builder()
        .id(noticeId)
        .title("공지사항 제목입니다")
        .content("공지사항 내용입니다")
        .imageUrl("https://s3.bucket/notice.jpg")
        .createdAt(LocalDateTime.of(2025, 5, 8, 10, 31, 45))
        .build();

    given(noticeRepository.findById(noticeId)).willReturn(Optional.of(notice));

    // when
    NoticeDetailResponse response = noticeService.getNoticeDetail(noticeId);

    // then
    assertEquals(noticeId, response.noticeId());
    assertEquals("공지사항 제목입니다", response.title());
    assertEquals("공지사항 내용입니다", response.content());
    assertEquals("https://s3.bucket/notice.jpg", response.noticeImageUrl());
    assertEquals(LocalDateTime.of(2025, 5, 8, 10, 31, 45), response.createdAt());
  }


  @Test
  @DisplayName("공지사항 상세 조회 실패 - 존재하지 않는 공지사항")
  void getNoticeDetailFail_notExist() {
    // given
    Long noticeId = 999L;
    given(noticeRepository.findById(noticeId)).willReturn(Optional.empty());

    // when & then
    MeongnyangerangException exception = assertThrows(MeongnyangerangException.class,
        () -> noticeService.getNoticeDetail(noticeId));

    assertEquals(ErrorCode.NOT_EXIST_NOTICE, exception.getErrorCode());
  }
}
