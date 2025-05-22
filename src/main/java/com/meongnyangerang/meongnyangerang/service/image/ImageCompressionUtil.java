package com.meongnyangerang.meongnyangerang.service.image;

import com.meongnyangerang.meongnyangerang.dto.image.CompressedImageData;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.imgscalr.Scalr;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
public class ImageCompressionUtil {

  @Value("${image.compression.capacity-threshold}")
  private long capacityThreshold;

  @Value("${image.compression.size-threshold}")
  private long sizeThreshold;

  @Value("${image.compression.quality}")
  private float quality;

  @Value("${image.compression.max-width}")
  private int maxWidth;

  @Value("${image.compression.max-height}")
  private int maxHeight;

  /**
   * 이미지 압축 필요 여부 확인 (해상도 + 용량 기준)
   */
  public boolean shouldCompress(MultipartFile file) {
    boolean capacityExceeded = file.getSize() > capacityThreshold; // 용량 체크
    boolean sizeExceeded = isSizeExceeded(file); // 해상도 체크
    return capacityExceeded || sizeExceeded;
  }

  /**
   * 이미지 압축 및 변환 처리
   */
  public CompressedImageData compressImage(MultipartFile file) {
    try {
      byte[] originalData = file.getBytes();
      long originalSize = originalData.length;

      BufferedImage originalImage = Optional.ofNullable(
              ImageIO.read(new ByteArrayInputStream(originalData)))
          .orElseThrow(() -> new MeongnyangerangException(ErrorCode.MISSING_IMAGE_FILE));

      // 이미지 처리
      BufferedImage processedImage = processImageSize(originalImage);
      String targetFormat = getFileExtension(file.getOriginalFilename());
      byte[] compressedData = compressImageByFormat(processedImage, targetFormat);

      // 압축 결과 로깅
      log.debug("이미지 압축 완료 - {}KB → {}KB ({}% 감소)",
          originalSize / 1024,
          compressedData.length / 1024,
          String.format("%.1f", (1.0 - (double) compressedData.length / originalSize) * 100));

      // 압축된 데이터 반환
      return new CompressedImageData(
          compressedData,
          file.getOriginalFilename(),
          file.getContentType()
      );
    } catch (IOException e) {
      throw new MeongnyangerangException(ErrorCode.INVALID_IO_ERROR);
    }
  }

  private boolean isSizeExceeded(MultipartFile file) {
    try {
      BufferedImage image = Optional.ofNullable(ImageIO.read(file.getInputStream()))
          .orElseThrow(() -> new MeongnyangerangException(ErrorCode.MISSING_IMAGE_FILE));
      int maxDimension = Math.max(image.getWidth(), image.getHeight());

      return maxDimension > sizeThreshold;
    } catch (IOException e) {
      log.error("이미지 해상도 가져오던 중 에러 발생: {}", e.getMessage());
      throw new MeongnyangerangException(ErrorCode.INVALID_IO_ERROR);
    }
  }

  /**
   * 이미지 크기 처리 (리사이즈)
   */
  private BufferedImage processImageSize(BufferedImage originalImage) {
    int originalWidth = originalImage.getWidth();
    int originalHeight = originalImage.getHeight();

    // 리사이즈가 필요한지 확인
    if (originalWidth <= maxWidth && originalHeight <= maxHeight) {
      return originalImage;
    }

    // 비율 유지하여 리사이즈
    double widthRatio = (double) maxWidth / originalWidth;
    double heightRatio = (double) maxHeight / originalHeight;
    double resizeRatio = Math.min(widthRatio, heightRatio);

    int targetWidth = (int) (originalWidth * resizeRatio);
    int targetHeight = (int) (originalHeight * resizeRatio);

    log.debug("이미지 리사이즈: {}x{} → {}x{}",
        originalWidth, originalHeight, targetWidth, targetHeight);

    return Scalr.resize(originalImage, Scalr.Method.QUALITY, targetWidth, targetHeight);
  }

  /**
   * 형식별 압축 처리
   */
  private byte[] compressImageByFormat(BufferedImage image, String format) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    String lowerFormat = format.toLowerCase();

    // JPG/JPEG 둘 다 "jpeg" Writer 사용 (동일한 형식이므로)
    if ("jpg".equals(lowerFormat) || "jpeg".equals(lowerFormat)) {
      compressJpegImage(image, outputStream);
    } else if ("png".equals(lowerFormat)) {
      ImageIO.write(image, "png", outputStream);
    } else {
      log.error("압축을 지원하지 않는 파일 형식입니다. {}", format);
      throw new MeongnyangerangException(ErrorCode.NOT_SUPPORTED_TYPE);
    }

    return outputStream.toByteArray();
  }

  private void compressJpegImage(BufferedImage image, ByteArrayOutputStream outputStream)
      throws IOException {
    Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
    if (!writers.hasNext()) {
      throw new IllegalStateException("JPEG Writer를 찾을 수 없습니다.");
    }

    ImageWriter writer = writers.next();
    try (ImageOutputStream ios = ImageIO.createImageOutputStream(outputStream)) {
      writer.setOutput(ios);
      ImageWriteParam param = writer.getDefaultWriteParam();

      if (param.canWriteCompressed()) {
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);
      }
      writer.write(null, new IIOImage(image, null, null), param);
    } finally {
      writer.dispose();
    }
  }

  private String getFileExtension(String fileName) {
    if (fileName == null || !fileName.contains(".")) {
      throw new IllegalArgumentException("유효하지 않은 파일명입니다: " + fileName);
    }
    return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
  }
}
