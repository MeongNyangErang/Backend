package com.meongnyangerang.meongnyangerang.service.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meongnyangerang.meongnyangerang.domain.image.ImageDeletionQueue;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.ImageDeletionQueueRepository;
import com.meongnyangerang.meongnyangerang.service.adptor.ImageStorage;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

  @Mock
  private ImageStorage imageStorage;

  @Mock
  private ImageDeletionQueueRepository imageDeletionQueueRepository;

  @Spy
  @InjectMocks
  private ImageService imageService;

  private List<String> imageUrls;
  private final String IMAGE_URL_1 = "http://example.com/image1.jpg";
  private final String IMAGE_URL_2 = "http://example.com/image2.jpg";


  @BeforeEach
  void setUp() {
    imageUrls = Arrays.asList(IMAGE_URL_1, IMAGE_URL_2);
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
  @DisplayName("이미지 URL 등록 - 성공")
  void registerImagesForDeletion_Success() {
    // given
    // when
    imageService.registerImagesForDeletion(imageUrls);

    // then
    ArgumentCaptor<List<ImageDeletionQueue>> queueCaptor = ArgumentCaptor.forClass(List.class);
    verify(imageDeletionQueueRepository).saveAll(queueCaptor.capture());

    List<ImageDeletionQueue> capturedEntries = queueCaptor.getValue();
    assertThat(capturedEntries).hasSize(2);
    assertThat(capturedEntries.get(0).getImageUrl()).isEqualTo(IMAGE_URL_1);
    assertThat(capturedEntries.get(1).getImageUrl()).isEqualTo(IMAGE_URL_2);
  }

  @Test
  @DisplayName("이미지 URL 등록 - 빈 문자열 필터링")
  void registerImagesForDeletion_FilterEmptyStrings() {
    // given
    List<String> imageUrls = Arrays.asList(
        IMAGE_URL_1,
        "",
        IMAGE_URL_2
    );

    // when
    imageService.registerImagesForDeletion(imageUrls);

    // then
    ArgumentCaptor<List<ImageDeletionQueue>> queueCaptor = ArgumentCaptor.forClass(List.class);
    verify(imageDeletionQueueRepository).saveAll(queueCaptor.capture());

    List<ImageDeletionQueue> capturedEntries = queueCaptor.getValue();
    assertThat(capturedEntries).hasSize(2); // 빈 문자열은 필터링됨
    assertThat(capturedEntries.get(0).getImageUrl())
        .isEqualTo(IMAGE_URL_1);
    assertThat(capturedEntries.get(1).getImageUrl())
        .isEqualTo(IMAGE_URL_2);
  }

  @Test
  @DisplayName("이미지 삭제 큐 처리 - 성공")
  void processImageDeletionQueue_Success() {
    // given
    List<ImageDeletionQueue> mockImages = Arrays.asList(
        createMockImageDeletionQueue(IMAGE_URL_1),
        createMockImageDeletionQueue(IMAGE_URL_2)
    );

    when(imageDeletionQueueRepository
        .findAllByOrderByRegisteredAtAsc(PageRequest.of(0, 100)))
        .thenReturn(mockImages);

    // when
    imageService.processImageDeletionQueue();

    // then
    ArgumentCaptor<List<String>> urlsCaptor = ArgumentCaptor.forClass(List.class);
    verify(imageService, times(1)).deleteImages(urlsCaptor.capture());

    List<String> capturedUrls = urlsCaptor.getValue();
    assertThat(capturedUrls).containsExactly(IMAGE_URL_1, IMAGE_URL_2);
    verify(imageDeletionQueueRepository, times(1)).deleteAll(mockImages);
  }

  @Test
  @DisplayName("이미지 삭제 큐 처리 - 빈 큐")
  void processImageDeletionQueue_EmptyQueue() {
    // given
    List<ImageDeletionQueue> mockImages = Collections.emptyList();

    when(imageDeletionQueueRepository
        .findAllByOrderByRegisteredAtAsc(PageRequest.of(0, 100)))
        .thenReturn(mockImages);

    // when
    imageService.processImageDeletionQueue();

    // then
    verify(imageService, never()).deleteImages(Collections.emptyList());
    verify(imageDeletionQueueRepository, never()).deleteAll(mockImages);
  }

  @Test
  @DisplayName("이미지 삭제 큐 처리 - 삭제 실패해도 큐에서 제거")
  void processImageDeletionQueue_DeleteFailedButStillRemoveFromQueue() {
    // given
    List<ImageDeletionQueue> mockImages = List.of(
        createMockImageDeletionQueue(IMAGE_URL_1),
        createMockImageDeletionQueue(IMAGE_URL_2)
    );

    when(imageDeletionQueueRepository
        .findAllByOrderByRegisteredAtAsc(PageRequest.of(0, 100)))
        .thenReturn(mockImages);

    doThrow(new MeongnyangerangException(ErrorCode.FILE_NOT_EMPTY))
        .when(imageService).deleteImages(imageUrls);

    // when
    imageService.processImageDeletionQueue();

    // then
    verify(imageService, times(1)).deleteImages(imageUrls);
    verify(imageDeletionQueueRepository, never()).deleteAll(mockImages);
  }

  private ImageDeletionQueue createMockImageDeletionQueue(String imageUrl) {
    return ImageDeletionQueue.builder()
        .imageUrl(imageUrl)
        .registeredAt(LocalDateTime.now())
        .build();
  }
}