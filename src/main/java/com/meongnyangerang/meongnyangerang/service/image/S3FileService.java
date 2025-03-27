package com.meongnyangerang.meongnyangerang.service.image;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest.KeyVersion;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class S3FileService implements ImageStorage {

  private static final String IMAGE_PATH_PREFIX = "image";
  private final AmazonS3 amazonS3;

  @Value("${AWS_BUCKET}")
  private String bucket;

  /**
   * 단일 파일 업로드
   *
   * @param file 업로드할 파일
   * @return 업로드된 파일의 URL
   */
  @Override
  public String uploadFile(MultipartFile file) {
    validateFileName(file);
    String filename = createFilename(file);

    try {
      ObjectMetadata metadata = createObjectMetadata(file);
      amazonS3.putObject(bucket, filename, file.getInputStream(), metadata);

      log.info("파일 업로드 성공");
      return generateFileUrl(filename);

    } catch (AmazonServiceException e) {
      log.error("파일 업로드 도중 아마존 서비스 에러 발생 : {}", e.getMessage(), e);
      throw new MeongnyangerangException(ErrorCode.FILE_NOT_EMPTY);
    } catch (IOException e) {
      log.error("파일 업로드 도중 IO 에러 발생 : {}", e.getMessage(), e);
      throw new MeongnyangerangException(ErrorCode.INVALID_IO_ERROR);
    }
  }

  /**
   * 파일 삭제
   *
   * @param fileUrl 삭제할 파일의 URL
   */
  @Override
  public void deleteFile(String fileUrl) {
    String key = extractKeyFromUrl(fileUrl);

    try {
      existKey(key);
      amazonS3.deleteObject(bucket, key);
      log.info("파일 삭제 성공 bucket: {}, key: {}", bucket, key);
    } catch (AmazonServiceException e) {
      log.error("파일 삭제 도중 아마존 서비스 에러 발생 : {}", e.getMessage(), e);
      throw new MeongnyangerangException(ErrorCode.AMAZON_SERVICE_ERROR);
    }
  }

  /**
   * 다중 파일 삭제
   *
   * @param fileUrls 삭제할 파일들의 URL 목록
   */
  @Override
  public void deleteFiles(List<String> fileUrls) {
    List<String> keys = fileUrls.stream()
        .map(this::extractKeyFromUrl)
        .toList();

    keys.forEach(this::existKey);

    // 여러 파일 일괄 삭제
    DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(bucket)
        .withKeys(keys.stream()
            .map(KeyVersion::new)
            .collect(Collectors.toList()));

    try {
      amazonS3.deleteObjects(deleteObjectsRequest);
      log.info("{}개의 파일이 성공적으로 삭제되었습니다", keys.size());
    } catch (AmazonServiceException e) {
      log.error("다중 파일 삭제 중 아마존 서비스 에러 발생: {}", e.getMessage());
      throw new MeongnyangerangException(ErrorCode.AMAZON_SERVICE_ERROR);
    }
  }

  /**
   * S3 URL에서 키(경로) 추출
   */
  private String extractKeyFromUrl(String fileUrl) {
    String bucketUrl = amazonS3.getUrl(bucket, "").toString();
    return fileUrl.replace(bucketUrl, "");
  }

  private void existKey(String key) {
    if (!amazonS3.doesObjectExist(bucket, key)) {
      log.warn("S3에 해당 객체가 존재하지 않습니다. Bucket : {}, key: {}", bucket, key);
      throw new MeongnyangerangException(ErrorCode.FILE_NOT_FOUND);
    }
  }

  private String generateFileUrl(String fileName) {
    return amazonS3.getUrl(bucket, fileName).toString();
  }

  private static String createFilename(MultipartFile file) {
    String originalFilename = file.getOriginalFilename();
    String fileName = UUID.randomUUID() + getExtension(originalFilename);
    return IMAGE_PATH_PREFIX + "/" + fileName;
  }

  private static String getExtension(String originalFileName) {
    try {
      return originalFileName.substring(originalFileName.lastIndexOf("."));
    } catch (StringIndexOutOfBoundsException e) {
      log.error("파일의 확장자가 없습니다.");
      throw new MeongnyangerangException(ErrorCode.INVALID_EXTENSION);
    }
  }

  private static ObjectMetadata createObjectMetadata(MultipartFile file) {
    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentType(file.getContentType());
    metadata.setContentLength(file.getSize());
    return metadata;
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
}
