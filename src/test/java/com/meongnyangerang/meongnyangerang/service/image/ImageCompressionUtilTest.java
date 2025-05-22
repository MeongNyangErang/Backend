package com.meongnyangerang.meongnyangerang.service.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.meongnyangerang.meongnyangerang.dto.image.CompressedImageData;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class ImageCompressionUtilTest {

  @InjectMocks
  private ImageCompressionUtil compressionUtil;

  @Test
  @DisplayName("이미지 압축 성공")
  void compressImage_Success() throws IOException {
    // given
    final long capacityThreshold = 512000;
    final int maxWidth = 1080;
    final int maxHeight = 1080;

    MultipartFile largeJpegFile = createTestImageFile(
        "large.jpeg", "image/jpeg", 3000, 2000);
    long originalSize = largeJpegFile.getSize();

    // when
    CompressedImageData result = compressionUtil.compressImage(largeJpegFile);
    byte[] imageData = result.imageData();
    long compressedSize = imageData.length;

    // then
    assertNotNull(imageData);
    assertEquals("large.jpeg", result.filename());
    assertEquals("image/jpeg", result.contentType());

    // 압축된 이미지가 원본보다 작은지 확인
    assertTrue(compressedSize < largeJpegFile.getSize());

    // 압축된 이미지가 유효한 이미지인지 확인
    BufferedImage compressedImage = ImageIO.read(new ByteArrayInputStream(imageData));
    assertNotNull(compressedImage);

    System.out.println("compressedSize : " + compressedSize);
    System.out.println("compressedImage.getWidth() : " + compressedImage.getWidth());
    System.out.println("compressedImage.getHeight() : " + compressedImage.getHeight());
    System.out.println("maxWidth : " + maxWidth);
    System.out.println("maxHeight : " + maxHeight);

    // 용량이 임계값(500KB) 이하로 압축되었는지 확인
    assertTrue(compressedSize <= capacityThreshold);

    // 리사이즈가 적용되었는지 확인 (1920x1080 이하)
    assertTrue(compressedImage.getWidth() <= maxWidth);
    assertTrue(compressedImage.getHeight() <= maxHeight);
  }

  @Test
  @DisplayName("PNG 이미지 압축 성공 - 리사이즈만 적용")
  void compressImage_Png_Success() throws IOException {
    // given
    MultipartFile largePngFile = createTestImageFile(
        "large.png", "image/png", 2500, 1800);

    // when
    CompressedImageData result = compressionUtil.compressImage(largePngFile);
    byte[] imageData = result.imageData();

    // then
    assertNotNull(imageData);
    assertEquals("large.png", result.filename());
    assertEquals("image/png", result.contentType());

    // 압축된 이미지가 원본보다 작은지 확인 (리사이즈로 인해)
    assertTrue(imageData.length < largePngFile.getSize());

    // 압축된 이미지의 해상도 확인
    BufferedImage compressedImage = ImageIO.read(new ByteArrayInputStream(imageData));
    assertNotNull(compressedImage);
    assertTrue(compressedImage.getWidth() <= 1920);
    assertTrue(compressedImage.getHeight() <= 1080);
  }

  private MockMultipartFile createTestImageFile(
      String filename, String contentType, int width, int height
  ) throws IOException {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2d = image.createGraphics();

    // 간단한 패턴 그리기 (테스트용)
    g2d.setColor(Color.BLUE);
    g2d.fillRect(0, 0, width, height);
    g2d.setColor(Color.WHITE);
    g2d.fillOval(width / 4, height / 4, width / 2, height / 2);
    g2d.dispose();

    // byte[]로 변환
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    String format = contentType.equals("image/jpeg") ? "jpeg" : "png";
    ImageIO.write(image, format, baos);

    return new MockMultipartFile("file", filename, contentType, baos.toByteArray());
  }
}