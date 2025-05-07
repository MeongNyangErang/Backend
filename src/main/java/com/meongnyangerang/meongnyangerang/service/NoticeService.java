package com.meongnyangerang.meongnyangerang.service;

import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.NOT_EXIST_ACCOUNT;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.NOT_EXIST_NOTICE;

import com.meongnyangerang.meongnyangerang.domain.admin.Admin;
import com.meongnyangerang.meongnyangerang.domain.admin.Notice;
import com.meongnyangerang.meongnyangerang.dto.NoticeRequest;
import com.meongnyangerang.meongnyangerang.dto.NoticeSimpleResponse;
import com.meongnyangerang.meongnyangerang.dto.chat.PageResponse;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.AdminRepository;
import com.meongnyangerang.meongnyangerang.repository.NoticeRepository;
import com.meongnyangerang.meongnyangerang.service.image.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class NoticeService {

  private final NoticeRepository noticeRepository;
  private final AdminRepository adminRepository;
  private final ImageService imageService;

  // 공지사항 등록
  @Transactional
  public void createNotice(Long adminId, NoticeRequest request, MultipartFile imageFile) {

    Admin admin = adminRepository.findById(adminId)
        .orElseThrow(() -> new MeongnyangerangException(NOT_EXIST_ACCOUNT));

    String imageUrl = null;
    if (imageFile != null && !imageFile.isEmpty()) {
      imageUrl = imageService.storeImage(imageFile);
    }

    noticeRepository.save(Notice.builder()
        .admin(admin)
        .title(request.getTitle())
        .content(request.getContent())
        .imageUrl(imageUrl)
        .build());
  }

  // 공지사항 수정
  @Transactional
  public void updateNotice(Long adminId, Long noticeId, NoticeRequest request,
      MultipartFile newImage) {
    Admin admin = adminRepository.findById(adminId)
        .orElseThrow(() -> new MeongnyangerangException(NOT_EXIST_ACCOUNT));

    Notice notice = noticeRepository.findById(noticeId)
        .orElseThrow(() -> new MeongnyangerangException(NOT_EXIST_NOTICE));

    // 이미지가 새로 첨부되었을 경우
    if (newImage != null && !newImage.isEmpty()) {
      // 기존 이미지가 있다면 삭제 등록
      if (notice.getImageUrl() != null) {
        imageService.deleteImageAsync(notice.getImageUrl());
      }

      notice.setImageUrl(imageService.storeImage(newImage));
    }

    notice.setTitle(request.getTitle());
    notice.setContent(request.getContent());
  }

  // 공지사항 삭제
  @Transactional
  public void deleteNotice(Long adminId, Long noticeId) {
    Admin admin = adminRepository.findById(adminId)
        .orElseThrow(() -> new MeongnyangerangException(NOT_EXIST_ACCOUNT));

    Notice notice = noticeRepository.findById(noticeId)
        .orElseThrow(() -> new MeongnyangerangException(NOT_EXIST_NOTICE));

    // 이미지 삭제 등록
    if (notice.getImageUrl() != null) {
      imageService.deleteImageAsync(notice.getImageUrl());
    }

    noticeRepository.delete(notice);
  }

  // 공지사항 목록 조회
  public PageResponse<NoticeSimpleResponse> getNoticeList(Pageable pageable) {
    Page<Notice> notices = noticeRepository.findAll(pageable);

    return PageResponse.from(notices.map(NoticeSimpleResponse::from));
  }
}
