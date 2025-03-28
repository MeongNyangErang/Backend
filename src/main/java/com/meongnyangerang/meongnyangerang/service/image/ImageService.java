package com.meongnyangerang.meongnyangerang.service.image;

import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.service.adptor.ImageStorage;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

  private final ImageStorage imageStorage;
  int i = 0;

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
   * 지원하는 포맷인지 검증
   */
  private void validateImageFormat(String contentType, String imageName) {
    if (!ImageType.isSupported(contentType, imageName)) {
      throw new MeongnyangerangException(ErrorCode.NOT_SUPPORTED_TYPE);
    }
  }
}
