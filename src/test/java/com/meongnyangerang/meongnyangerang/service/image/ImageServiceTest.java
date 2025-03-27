package com.meongnyangerang.meongnyangerang.service.image;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.service.adptor.ImageStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

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

    // when, then
    assertThatThrownBy(() -> imageService.storeImage(image))
        .isInstanceOf(MeongnyangerangException.class)
        .hasFieldOrPropertyWithValue("ErrorCode", ErrorCode.NOT_SUPPORTED_TYPE);
  }
}