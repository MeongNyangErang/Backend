package com.meongnyangerang.meongnyangerang.service;

import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.NOT_EXIST_ACCOUNT;

import com.meongnyangerang.meongnyangerang.domain.admin.Admin;
import com.meongnyangerang.meongnyangerang.domain.admin.Notice;
import com.meongnyangerang.meongnyangerang.dto.NoticeRequest;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.AdminRepository;
import com.meongnyangerang.meongnyangerang.repository.NoticeRepository;
import com.meongnyangerang.meongnyangerang.service.image.ImageService;
import lombok.RequiredArgsConstructor;
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
}
