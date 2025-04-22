package com.meongnyangerang.meongnyangerang.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record HostNameUpdateRequest(
    @NotBlank(message = "새로운 이름을 입력하세요.")
    @Size(min = 1, max = 20, message = "이름은 1자 이상 20자 이하여야 합니다.")
    @Pattern(regexp = "^[가-힣a-zA-Z]+$", message = "이름은 한글 또는 영문만 입력 가능합니다.")
    String name
) {

}
