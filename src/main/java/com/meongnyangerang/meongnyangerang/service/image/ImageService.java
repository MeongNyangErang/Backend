package com.meongnyangerang.meongnyangerang.service.image;

import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.service.adptor.ImageStorage;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

  private final ImageStorage imageStorage;

  /**
   * 이미지 업로드
   */
  public String storeImage(MultipartFile image) {
    validateImage(image);
    return imageStorage.uploadFile(image);
  }

  /**
   * 단일 이미지 비동기 삭제
   */
  @Async
  public void deleteImageAsync(String imageUrl) {
    try {
      String threadName = Thread.currentThread().getName();
      log.info("비동기 이미지 삭제 시작 - 스레드: {}, 이미지: {}", threadName, imageUrl);

      validateImageUrl(imageUrl);
      imageStorage.deleteFile(imageUrl);
      log.info("이미지 비동기 삭제 완료: {}", imageUrl);
    } catch (Exception e) {
      log.error("이미지 비동기 삭제 중 오류 발생: {}", e.getMessage(), e);
    }
  }

  /**
   * 다중 이미지 비동기 삭제
   */
  @Async
  public void deleteImagesAsync(List<String> imageUrls) {
    try{
      List<String> validUrls = validateImageUrls(imageUrls);
      imageStorage.deleteFiles(validUrls);
      log.info("다중 이미지 비동기 삭제 완료: {}", imageUrls);
    } catch (Exception e) {
      log.error("이미지 비동기 삭제 중 오류 발생: {}", e.getMessage(), e);
    }
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
