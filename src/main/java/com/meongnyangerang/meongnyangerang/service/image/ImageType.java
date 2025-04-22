package com.meongnyangerang.meongnyangerang.service.image;

import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@RequiredArgsConstructor
public enum ImageType {

  JPG("image/jpg", "jpg"),
  JPEG("image/jpeg", "jpeg"),
  PNG("image/png", "png");

  private final String mimeType;
  private final String extension;

  // mimeType 을 key 로 하는 Map 생성
  private static final Map<String, ImageType> MIME_TYPE_MAP = Arrays.stream(values())
      .collect(Collectors.toMap(ImageType::getMimeType, type -> type));

  // extension 을 key 로 하는 Map 생성
  private static final Map<String, ImageType> EXTENSION_MAP = Arrays.stream(values())
      .collect(Collectors.toMap(ImageType::getExtension, type -> type));

  public static boolean isSupported(String mimeType, String filename) {
    String extension = extractExtension(filename);
    // mimeType 과 확장자가 모두 지원되는지, 서로 일치하는지 확인
    return MIME_TYPE_MAP.containsKey(mimeType.toLowerCase())
        && EXTENSION_MAP.containsKey(extension.toLowerCase());
  }

  private static String extractExtension(String filename) {
    if (filename == null || !filename.contains(".")) {
      log.error("이미지 파일 확장자 오류");
      throw new MeongnyangerangException(ErrorCode.INVALID_EXTENSION);
    }
    return filename.substring(filename.lastIndexOf(".") + 1); // 확장자만 추출
  }
}
