package com.meongnyangerang.meongnyangerang.service.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest.KeyVersion;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class S3FileServiceTest {

  @Mock
  private AmazonS3 amazonS3;

  @InjectMocks
  private S3FileService s3FileService;

  private static final String IMAGE_PATH_PREFIX = "image";

  private static final UUID MOCK_UUID =
      UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

  private static final String MOCK_FILE_URL =
      "https://test-bucket.s3.amazonaws.com/image/" + MOCK_UUID + ".jpg";

  private final String bucket = "test-bucket";

  private MockMultipartFile mockImage;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(s3FileService, "bucket", bucket);

    mockImage = new MockMultipartFile(
        "image",
        "test.jpg",
        "image/jpg",
        "test image content".getBytes()
    );
  }

  @Test
  @DisplayName("단일 이미지 업로드 성공")
  void uploadImage_Success() throws MalformedURLException {
    // given
    String filename = createFilename(mockImage);

    ArgumentCaptor<ByteArrayInputStream> inputStreamCaptor = ArgumentCaptor
        .forClass(ByteArrayInputStream.class);
    ArgumentCaptor<ObjectMetadata> metadataCaptor = ArgumentCaptor.forClass(ObjectMetadata.class);

    // UUID 모킹
    try (MockedStatic<UUID> mockedUUID = mockStatic(UUID.class)) {
      mockedUUID.when(UUID::randomUUID).thenReturn(MOCK_UUID);

      // URL 모킹
      URL mockUrl = new URL(MOCK_FILE_URL);
      when(amazonS3.getUrl(bucket, "image/" + MOCK_UUID + ".jpg")).thenReturn(mockUrl);

      // when
      String response = s3FileService.uploadFile(mockImage);

      // then
      assertEquals(MOCK_FILE_URL, response);
      verify(amazonS3, times(1)).putObject(
          eq(bucket), eq(filename), inputStreamCaptor.capture(), metadataCaptor.capture());
    }
  }

  @Test
  @DisplayName("파일 삭제 성공")
  void deleteFile_Success() throws MalformedURLException {
    // given
    String key = extractKeyFromUrl(MOCK_FILE_URL);

    when(amazonS3.getUrl(bucket, ""))
        .thenReturn(new URL("https://test-bucket.s3.amazonaws.com/"));
    when(amazonS3.doesObjectExist(bucket, key)).thenReturn(true);

    // when
    s3FileService.deleteFile(MOCK_FILE_URL);

    // then
    verify(amazonS3, times(1)).doesObjectExist(bucket, key);
    verify(amazonS3, times(1)).deleteObject(bucket, key);
  }

  @Test
  @DisplayName("다중 파일 삭제 성공")
  void deleteFiles_Success() throws MalformedURLException {
    // given
    List<String> fileUrls = List.of(
        "https://test-bucket.s3.amazonaws.com/image/file1.jpg",
        "https://test-bucket.s3.amazonaws.com/image/file2.jpg");

    List<String> expectedKeys = List.of(
        "image/file1.jpg",
        "image/file2.jpg"
    );

    when(amazonS3.getUrl(bucket, ""))
        .thenReturn(new URL("https://test-bucket.s3.amazonaws.com/"));

    // 키의 존재 여부 모킹
    expectedKeys.forEach(key -> when(amazonS3.doesObjectExist(bucket, key)).thenReturn(true));

    // when
    s3FileService.deleteFiles(fileUrls);

    // then
    // 각 키에 대해 존재 여부 확인 메서드 호출 검증

    // DeleteObjectsRequest 검증
    ArgumentCaptor<DeleteObjectsRequest> deleteRequestCaptor = ArgumentCaptor
        .forClass(DeleteObjectsRequest.class);

    verify(amazonS3).deleteObjects(deleteRequestCaptor.capture());
  }



  private static String createFilename(MultipartFile file) {
    String originalFilename = file.getOriginalFilename();
    String fileName = MOCK_UUID + getExtension(originalFilename);
    return IMAGE_PATH_PREFIX + "/" + fileName;
  }

  private static String getExtension(String originalFileName) {
    try {
      return originalFileName.substring(originalFileName.lastIndexOf("."));
    } catch (StringIndexOutOfBoundsException e) {
      throw new MeongnyangerangException(ErrorCode.INVALID_EXTENSION);
    }
  }

  private String extractKeyFromUrl(String fileUrl) {
    // 직접 URL에서 키 추출 (".com/" 이후부터 끝까지 추출)
    return fileUrl.substring(fileUrl.lastIndexOf(".com/") + 5);
  }
}