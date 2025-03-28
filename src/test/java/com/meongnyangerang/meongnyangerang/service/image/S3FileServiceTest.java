package com.meongnyangerang.meongnyangerang.service.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import java.net.URL;
import java.util.List;
import software.amazon.awssdk.core.sync.RequestBody;
import java.net.MalformedURLException;
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
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@ExtendWith(MockitoExtension.class)
class S3FileServiceTest {

  @Mock
  private S3Client s3Client;

  @Mock
  private S3Utilities s3Utilities;

  @InjectMocks
  private S3FileService s3FileService;

  private static final String IMAGE_PATH_PREFIX = "image";
  private static final String BUCKET = "test-bucket";
  private static final UUID MOCK_UUID =
      UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
  private static final String EXPECTED_KEY = IMAGE_PATH_PREFIX + MOCK_UUID + ".jpg";
  private static final String MOCK_FILE_URL =
      "https://" + BUCKET + ".s3.amazonaws.com/image/" + MOCK_UUID + ".jpg";

  private MockMultipartFile mockImage;

  @BeforeEach
  void setUp() throws MalformedURLException {
    ReflectionTestUtils.setField(s3FileService, "bucket", BUCKET);

    mockImage = new MockMultipartFile(
        "image",
        "test.jpg",
        "image/jpg",
        "test image content".getBytes()
    );

    when(s3Client.utilities()).thenReturn(s3Utilities);
    doReturn(new URL(MOCK_FILE_URL)).when(s3Utilities).getUrl(any(GetUrlRequest.class));
  }

  @Test
  @DisplayName("단일 이미지 업로드 성공")
  void uploadImage_Success() throws MalformedURLException {
    // given
    // 업로드 요청 캡처
    ArgumentCaptor<PutObjectRequest> putObjectRequestCaptor = ArgumentCaptor
        .forClass(PutObjectRequest.class);

    // 업로드 실제 파일 데이터 캡처
    ArgumentCaptor<RequestBody> requestBodyCaptor = ArgumentCaptor.forClass(RequestBody.class);

    // UUID 생성을 모킹하기 위해 정적 메서드 모킹
    try (MockedStatic<UUID> mockedUUID = mockStatic(UUID.class)) {
      mockedUUID.when(UUID::randomUUID).thenReturn(MOCK_UUID);

      // when
      String fileUrl = s3FileService.uploadFile(mockImage);

      // then
      verify(s3Client, times(1)).putObject(
          putObjectRequestCaptor.capture(),
          requestBodyCaptor.capture()
      );

      PutObjectRequest capturedRequest = putObjectRequestCaptor.getValue();
      assertEquals(BUCKET, capturedRequest.bucket());
      assertEquals(fileUrl, generateFileUrl(capturedRequest.key()));
    }
  }

  @Test
  @DisplayName("파일 삭제 성공")
  void deleteFile_Success() throws MalformedURLException {
    // given
    String key = extractKeyFromUrl(MOCK_FILE_URL);

    // 삭제 요청 캡처
    ArgumentCaptor<DeleteObjectRequest> deleteRequestCaptor = ArgumentCaptor
        .forClass(DeleteObjectRequest.class);

    // when
    s3FileService.deleteFile(MOCK_FILE_URL);

    // then
    verify(s3Client, times(1))
        .deleteObject(deleteRequestCaptor.capture());

    // 캡처된 DeleteObjectRequest 객체 가져오기
    DeleteObjectRequest capturedRequest = deleteRequestCaptor.getValue();
    assertEquals(BUCKET, capturedRequest.bucket());
    assertEquals(key, generateFileUrl(capturedRequest.key()));
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

    // 삭제 요청 캡처
    ArgumentCaptor<DeleteObjectsRequest> deleteRequestCaptor = ArgumentCaptor
        .forClass(DeleteObjectsRequest.class);

    // when
    s3FileService.deleteFiles(fileUrls);

    // then
    verify(s3Client).deleteObjects(deleteRequestCaptor.capture());

    // 캡처된 DeleteObjectRequest 객체 가져오기
    DeleteObjectsRequest capturedRequest = deleteRequestCaptor.getValue();
    assertEquals(BUCKET, capturedRequest.bucket());

    List<ObjectIdentifier> capturedKeys = capturedRequest.delete().objects();
    assertThat(capturedKeys)
        .hasSize(expectedKeys.size())
        .extracting(ObjectIdentifier::key)
        .containsExactlyElementsOf(expectedKeys);
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

  private String generateFileUrl(String key) {
    return s3Client.utilities()
        .getUrl(GetUrlRequest.builder()
            .bucket(BUCKET)
            .key(key)
            .build())
        .toString();
  }
}