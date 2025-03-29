package com.meongnyangerang.meongnyangerang.service.image;

import com.meongnyangerang.meongnyangerang.domain.image.ImageDeletionQueue;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.ImageDeletionQueueRepository;
import com.meongnyangerang.meongnyangerang.service.adptor.ImageStorage;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

  private final ImageStorage imageStorage;
  private final ImageDeletionQueueRepository imageDeletionQueueRepository;

  private static final int BATCH_SIZE = 100;

  /**
   * 이미지 업로드
   */
  public String storeImage(MultipartFile image) {
    validateImageFormat(image.getContentType(), image.getOriginalFilename());
    return imageStorage.uploadFile(image);
  }

  /**
   * 이미지 삭제
   */
  public void deleteImage(String fileUrl) {
    if (fileUrl == null || fileUrl.isEmpty()) {
      log.error("삭제할 파일이 비어있습니다.");
      throw new MeongnyangerangException(ErrorCode.FILE_NOT_EMPTY);
    }
    imageStorage.deleteFile(fileUrl);
  }

  /**
   * 다중 이미지 삭제
   */
  public void deleteImages(List<String> fileUrls) {
    if (fileUrls == null || fileUrls.isEmpty()) {
      log.error("삭제할 파일 목록이 비어있습니다.");
      throw new MeongnyangerangException(ErrorCode.FILE_NOT_EMPTY);
    }
    imageStorage.deleteFiles(fileUrls);
  }

  /**
   * 이미지 삭제를 배치 처리하기 위해 삭제할 데이터 저장
   */
  public void registerImagesForDeletion(List<String> imageUrls) {
    List<ImageDeletionQueue> deletionEntries = imageUrls.stream()
        .filter(url -> !url.isEmpty())
        .map(url -> ImageDeletionQueue.builder()
            .imageUrl(url)
            .registeredAt(LocalDateTime.now())
            .build())
        .toList();

    imageDeletionQueueRepository.saveAll(deletionEntries);
    log.info("이미지 삭제 큐에 {}개 항목 등록 완료", deletionEntries.size());
  }

  // TODO: 젠킨스 사용 고려
  @Scheduled(cron = "0 0/10 * * * ?") // 10분마다 실행
  @Transactional
  public void processImageDeletionQueue() {
    log.info("이미지 삭제 큐 처리 시작");

    try {
      List<ImageDeletionQueue> images = fetchPendingImages();

      if (images.isEmpty()) {
        log.info("처리할 삭제 대기 이미지가 없습니다.");
        return;
      }

      List<String> imageUrls = images.stream()
          .map(ImageDeletionQueue::getImageUrl)
          .toList();

      deleteImages(imageUrls);
      deleteImageQueue(images);

      log.info("이미지 삭제 큐 처리 완료");
    } catch (Exception e) {
      log.error("이미지 삭제 큐 처리 중 오류 발생: {}", e.getMessage(), e);
    }
  }

  private List<ImageDeletionQueue> fetchPendingImages() {
    return imageDeletionQueueRepository
        .findAllByOrderByRegisteredAtAsc(PageRequest.of(0, BATCH_SIZE));
  }

  private void deleteImageQueue(List<ImageDeletionQueue> images) {
    imageDeletionQueueRepository.deleteAll(images);
  }

  /**
   * 지원하는 포맷인지 검증
   */
  private void validateImageFormat(String contentType, String imageName) {
    if (!ImageType.isSupported(contentType, imageName)) {
      throw new MeongnyangerangException(ErrorCode.NOT_SUPPORTED_TYPE);
    }
  }
}
