package com.meongnyangerang.meongnyangerang.service.image;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.service.adptor.ImageStorage;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

  private static final String IMAGE_PATH_PREFIX = "image/";

  @Mock
  private ImageStorage imageStorage;

  @InjectMocks
  private ImageService imageService;

  @Test
  @DisplayName("단일 이미지 업로드 실패 - 지원하지 않는 파일 유형")
  void storeImage_WhenNotSupportedFileType_ThrowException() {
    // given
    MockMultipartFile image = new MockMultipartFile(
        "file",
        "test.PNGE",
        "image/pnge",
        "test image content".getBytes()
    );

    String filename = createFilename(image.getOriginalFilename());

    // when, then
    assertThatThrownBy(() -> imageService.storeImage(image, filename))
        .isInstanceOf(MeongnyangerangException.class)
        .hasFieldOrPropertyWithValue("ErrorCode", ErrorCode.NOT_SUPPORTED_TYPE);
  }

  private static String createFilename(String originalFilename) {
    String fileName = UUID.randomUUID() + getExtension(originalFilename);
    return IMAGE_PATH_PREFIX + fileName;
  }

  private static String getExtension(String originalFileName) {
    try {
      return originalFileName.substring(originalFileName.lastIndexOf("."));
    } catch (StringIndexOutOfBoundsException e) {
      throw new MeongnyangerangException(ErrorCode.INVALID_EXTENSION);
    }
  }
}