package com.meongnyangerang.meongnyangerang.dto.image;

public record CompressedImageData(
    byte[] imageData,
    String filename,
    String contentType
) {

}
