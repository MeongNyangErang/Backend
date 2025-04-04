package com.meongnyangerang.meongnyangerang.service.image;

import com.meongnyangerang.meongnyangerang.domain.image.ImageDeletionQueue;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.ImageDeletionQueueRepository;
import com.meongnyangerang.meongnyangerang.service.adptor.ImageStorage;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

  private final ImageStorage imageStorage;
  private final ImageDeletionQueueRepository imageDeletionQueueRepository;

  private static final int BATCH_SIZE = 100;

  // TODO: 젠킨스 사용 고려
  //@Scheduled(cron = "0 0/10 * * * ?") // 10분마다 실행
//  @Scheduled(cron = "0/10 * * * * ?") // 10초마다 실행 (테스트)
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

      imageDeletionQueueRepository.deleteAll(images);
      log.info("이미지 삭제 큐 처리 완료");
    } catch (Exception e) {
      log.error("이미지 삭제 큐 처리 중 오류 발생: {}", e.getMessage(), e);
    }
  }

  /**
   * 이미지 업로드
   */
  public String storeImage(MultipartFile image) {
    validateImage(image);
    return imageStorage.uploadFile(image);
  }

  /**
   * 이미지 삭제
   */
  public void deleteImage(String imageUrl) {
    validateImageUrl(imageUrl);
    imageStorage.deleteFile(imageUrl);
  }

  /**
   * 다중 이미지 삭제
   */
  public void deleteImages(List<String> imageUrls) {
    List<String> validUrls = validateImageUrls(imageUrls);
    imageStorage.deleteFiles(validUrls);
  }

  /**
   * 이미지 삭제를 배치 처리하기 위해 삭제할 데이터 저장 (단일)
   */
  public void registerImagesForDeletion(String imageUrl) {
    if (imageUrl == null || imageUrl.isEmpty()) {
      log.info("등록할 이미지 URL이 없습니다.");
    }
    ImageDeletionQueue imageDeletionQueue = ImageDeletionQueue.from(imageUrl);
    imageDeletionQueueRepository.save(imageDeletionQueue);
    log.info("이미지 삭제 큐에 단일 항목 등록 완료");
  }

  /**
   * 이미지 삭제를 배치 처리하기 위해 삭제할 데이터 저장 (다중)
   */
  public void registerImagesForDeletion(List<String> imageUrls) {
    List<String> validUrls = validateImageUrls(imageUrls);

    if (validUrls.isEmpty()) {
      log.info("등록할 유효한 이미지 URL이 없습니다.");
      return;
    }

    List<ImageDeletionQueue> deletionEntries = validUrls.stream()
        .map(ImageDeletionQueue::from)
        .toList();

    imageDeletionQueueRepository.saveAll(deletionEntries);
    log.info("이미지 삭제 큐에 {}개 항목 등록 완료", deletionEntries.size());
  }

  public void deregisterImageForDeletion(ImageDeletionQueue imageDeletionQueueId) {
    imageDeletionQueueRepository.delete(imageDeletionQueueId);
    log.info("등록한 삭제 큐에서 항목 제거 완료: {}", imageDeletionQueueId.getImageUrl());
  }

  private List<ImageDeletionQueue> fetchPendingImages() {
    return imageDeletionQueueRepository
        .findAllByOrderByRegisteredAtAsc(PageRequest.of(0, BATCH_SIZE));
  }

  private void validateImage(MultipartFile image) {
    if (image == null || image.isEmpty()) {
      throw new MeongnyangerangException(ErrorCode.MISSING_IMAGE_FILE);
    }
    validateImageFormat(image.getContentType(), image.getOriginalFilename());
  }

  private void validateImageFormat(String contentType, String imageName) {
    if (!ImageType.isSupported(contentType, imageName)) {
      throw new MeongnyangerangException(ErrorCode.NOT_SUPPORTED_TYPE);
    }
  }

  private void validateImageUrl(String imageUrl) {
    if (imageUrl == null || imageUrl.isEmpty()) {
      throw new MeongnyangerangException(ErrorCode.MISSING_IMAGE_URL);
    }
  }

  private List<String> validateImageUrls(List<String> imageUrls) {
    if (imageUrls == null) {
      return Collections.emptyList();
    }

    return imageUrls.stream()
        .filter(url -> url != null && !url.isEmpty())
        .collect(Collectors.toList());
  }
}
