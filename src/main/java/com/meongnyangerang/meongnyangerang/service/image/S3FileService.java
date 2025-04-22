package com.meongnyangerang.meongnyangerang.service.image;

import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.service.adptor.ImageStorage;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3FileService implements ImageStorage {

  private static final String IMAGE_PATH_PREFIX = "image/";

  private final S3Client s3Client;

  @Value("${AWS_BUCKET}")
  private String bucket;

  /**
   * 단일 파일 업로드
   * @return 업로드된 파일의 URL
   */
  @Override
  public String uploadFile(MultipartFile file) {
    validateFileName(file);
    String filename = createFilename(file.getOriginalFilename());

    try {
      PutObjectRequest putObjectRequest = PutObjectRequest.builder()
          .bucket(bucket)
          .key(filename)
          .contentType(file.getContentType())
          .build();

      s3Client.putObject(
          putObjectRequest,
          RequestBody.fromInputStream(file.getInputStream(), file.getSize())
      );

      log.info("파일 업로드 성공");
      return generateFileUrl(filename);
    } catch (SdkException e) {
      log.error("파일 업로드 도중 아마존 서비스 에러 발생 : {}", e.getMessage(), e);
      throw new MeongnyangerangException(ErrorCode.FILE_IS_EMPTY);
    } catch (IOException e) {
      log.error("파일 업로드 도중 IO 에러 발생 : {}", e.getMessage(), e);
      throw new MeongnyangerangException(ErrorCode.INVALID_IO_ERROR);
    }
  }

  /**
   * 파일 삭제
   */
  @Override
  public void deleteFile(String fileUrl) {
    String key = extractKeyFromUrl(fileUrl);

    try {
      existKey(key);
      DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
          .bucket(bucket)
          .key(key)
          .build();

      s3Client.deleteObject(deleteObjectRequest);

      log.info("파일 삭제 성공 bucket: {}, key: {}", bucket, key);
    } catch (SdkException e) {
      log.error("파일 삭제 도중 아마존 서비스 에러 발생 : {}", e.getMessage(), e);
      throw new MeongnyangerangException(ErrorCode.AMAZON_SERVICE_ERROR);
    }
  }

  /**
   * 다중 파일 삭제
   */
  @Override
  public void deleteFiles(List<String> fileUrls) {
    List<String> keys = fileUrls.stream()
        .map(this::extractKeyFromUrl)
        .toList();

    List<String> existingKeys = filterExistingKeys(keys); // 존재하는 키만 필터링

    if (existingKeys.isEmpty()) {
      log.info("삭제할 S3 객체가 없습니다.");
      return;
    }

    List<ObjectIdentifier> objectsToDelete = keys.stream()
        .map(key -> ObjectIdentifier.builder()
            .key(key)
            .build())
        .collect(Collectors.toList());

    DeleteObjectsRequest deleteObjectsRequest = DeleteObjectsRequest.builder()
        .bucket(bucket)
        .delete(Delete.builder()
            .objects(objectsToDelete)
            .quiet(false) // 삭제 결과를 개별로 받음
            .build())
        .build();

    try {
      DeleteObjectsResponse response = s3Client.deleteObjects(deleteObjectsRequest);
      responseResultMessage(response);
    } catch (Exception e) {
      log.error("다중 파일 삭제 중 에러 발생: {}", e.getMessage());
      throw new MeongnyangerangException(ErrorCode.AMAZON_SERVICE_ERROR);
    }
  }

  /**
   * S3에 저장된 모든 이미지 URL 가져오기
   */
  public List<String> getAllImageUrls() {
    // S3 버킷에 있는 객체 목록을 가져오기 위한 요청 객체 생성
    ListObjectsV2Request request = ListObjectsV2Request.builder()
        .bucket(bucket)
        .build();

    ListObjectsV2Response response = s3Client.listObjectsV2(request); // 객체 목록 결과

    if (response.keyCount() < 1){
      throw new MeongnyangerangException(ErrorCode.NOT_FOUND_IMAGE);
    }

    return response.contents().stream()
        .filter(obj -> isImage(obj.key()))
        .map(obj -> generateFileUrl(obj.key()))
        .toList();
  }

  private boolean isImage(String key) {
    return key.endsWith(".jpg") ||
        key.endsWith(".jpeg") ||
        key.endsWith(".png");
  }

  /**
   * 존재하는 S3 키만 필터링
   */
  private List<String> filterExistingKeys(List<String> keys) {
    return keys.stream()
        .filter(this::objectExists)
        .collect(Collectors.toList());
  }

  /**
   * S3 객체 존재 여부 확인
   */
  private boolean objectExists(String key) {
    try {
      HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
          .bucket(bucket)
          .key(key)
          .build();

      s3Client.headObject(headObjectRequest);
      return true;
    } catch (S3Exception e) {
      return false; // S3 관련 예외는 객체가 없음을 의미
    }
  }

  /**
   * S3 URL에서 키(경로) 추출
   */
  private String extractKeyFromUrl(String fileUrl) {
    // 전체 경로에서 "image/" 이후의 파일명 추출
    int index = fileUrl.indexOf(IMAGE_PATH_PREFIX);
    return index != -1
        ? fileUrl.substring(index)
        : fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
  }

  private void existKey(String key) {
    HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .build();

    try {
      log.info("S3에 해당 객체가 존재하는지 확인: {}", key);
      s3Client.headObject(headObjectRequest);
    } catch (NoSuchKeyException e) {
      log.warn("S3에 해당 객체가 존재하지 않습니다. Bucket : {}, key: {}", bucket, key);
      throw new MeongnyangerangException(ErrorCode.FILE_NOT_FOUND);
    }
  }


  private static void validateFileName(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      log.error("파일이 존재하지 않습니다.");
      throw new MeongnyangerangException(ErrorCode.FILE_NOT_FOUND);
    }

    String filename = file.getOriginalFilename();
    if (filename == null || filename.trim().isEmpty()) {
      log.error("파일명이 유효하지 않습니다.");
      throw new MeongnyangerangException(ErrorCode.INVALID_FILENAME);
    }
  }

  private String createFilename(String originalFilename) {
    String fileName = UUID.randomUUID() + getExtension(originalFilename);
    return IMAGE_PATH_PREFIX + fileName;
  }

  private String getExtension(String originalFileName) {
    try {
      return originalFileName.substring(originalFileName.lastIndexOf("."));
    } catch (StringIndexOutOfBoundsException e) {
      log.error("파일의 확장자가 없습니다.");
      throw new MeongnyangerangException(ErrorCode.INVALID_EXTENSION);
    }
  }

  private String generateFileUrl(String key) {
    return s3Client.utilities()
        .getUrl(GetUrlRequest.builder()
            .bucket(bucket)
            .key(key)
            .build())
        .toString();
  }

  private static void responseResultMessage(DeleteObjectsResponse response) {
    if (response != null) {
      log.info("S3 다중 삭제 결과: 성공 {}, 실패 {}",
          response.deleted() != null ? response.deleted().size() : 0,
          response.errors() != null ? response.errors().size() : 0);
    } else {
      log.warn("S3 응답이 null입니다 - 삭제 상태를 확인할 수 없습니다");
    }
  }
}
