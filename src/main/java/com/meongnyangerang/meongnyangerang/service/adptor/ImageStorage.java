package com.meongnyangerang.meongnyangerang.service.adptor;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface ImageStorage {

  void uploadFile(MultipartFile file, String fileUrl);

  void deleteFile(String fileUrl);

  void deleteFiles(List<String> fileUrls);
}
