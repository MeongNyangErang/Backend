package com.meongnyangerang.meongnyangerang.service.image;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.service.adptor.ImageStorage;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

  @Mock
  private ImageStorage imageStorage;

  @InjectMocks
  private ImageService imageService;

  private MockMultipartFile validImageFile;
  private String validImageUrl;
  private List<String> validImageUrls;

  @BeforeEach
  void setUp() {
    // 유효한 이미지 파일 생성
    validImageFile = new MockMultipartFile(
        "image",
        "test-image.jpg",
        "image/jpeg",
        "image content".getBytes()
    );

    // 유효한, 이미지 URL 설정
    validImageUrl = "https://storage.example.com/images/test-image.jpg";
    validImageUrls = Arrays.asList(
        "https://storage.example.com/images/image1.jpg",
        "https://storage.example.com/images/image2.jpg",
        "https://storage.example.com/images/image3.jpg"
    );
  }

  @Test
  @DisplayName("단일 이미지 업로드 성공")
  void storeImage_Success() {
    // given
    when(imageStorage.uploadFile(validImageFile)).thenReturn(validImageUrl);

    // when
    String result = imageService.storeImage(validImageFile);

    // then
    assertEquals(validImageUrl, result);
    verify(imageStorage, times(1)).uploadFile(validImageFile);
  }

  @Test
  @DisplayName("단일 이미지 업로드 실패 - 이미지 파일이 없음")
  void storeImage_WithNullImage_ThrowsException() {
    // given
    MultipartFile nullFile = null;

    // when
    // then
    assertThatThrownBy(() -> imageService.storeImage(nullFile))
        .isInstanceOf(MeongnyangerangException.class)
        .hasFieldOrPropertyWithValue("ErrorCode", ErrorCode.MISSING_IMAGE_FILE);
  }

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

    // when
    // then
    assertThatThrownBy(() -> imageService.storeImage(image))
        .isInstanceOf(MeongnyangerangException.class)
        .hasFieldOrPropertyWithValue("ErrorCode", ErrorCode.NOT_SUPPORTED_TYPE);
  }

  @Test
  @DisplayName("단일 이미지 비동기 삭제 성공")
  void deleteImageAsync_Success() {
    // given
    doNothing().when(imageStorage).deleteFile(validImageUrl);

    // when
    // then
    assertDoesNotThrow(() -> {
      imageService.deleteImageAsync(validImageUrl);
      Thread.sleep(100); // 비동기 메서드가 완료될 시간을 주기 위해 대기
    });

    // 메서드가 호출되었는지 확인
    verify(imageStorage, times(1)).deleteFile(validImageUrl);
  }

  @Test
  @DisplayName("다중 이미지 비동기 삭제 성공")
  void deleteImagesAsync_Success() {
    // given
    doNothing().when(imageStorage).deleteFiles(validImageUrls);

    // when
    // then
    assertDoesNotThrow(() -> {
      imageService.deleteImagesAsync(validImageUrls);
      Thread.sleep(100); // 비동기 메서드가 완료될 시간을 주기 위한 짧은 대기
    });

    // 메서드가 호출되었는지 확인
    verify(imageStorage, times(1)).deleteFiles(validImageUrls);
  }
}