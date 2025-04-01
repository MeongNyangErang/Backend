package com.meongnyangerang.meongnyangerang.dto;

import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;

@Getter
@Builder
public class UpdateReviewRequest {

  @NotNull
  private Double userRating;

  @NotNull
  private Double petRating;

  private String content;

  @Default // 삭제할 이미지 Id가 없으면 빈 리스트 기본 값 설정
  private List<Long> deletedImageId = new ArrayList<>();

}
