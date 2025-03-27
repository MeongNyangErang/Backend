package com.meongnyangerang.meongnyangerang.service.adptor;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface ImageStorage {

  String uploadFile(MultipartFile file);

  void deleteFile(String fileUrl);

  void deleteFiles(List<String> fileUrls);
}
