package com.meongnyangerang.meongnyangerang.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NicknameUpdateRequest(

    @Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하여야 합니다.")
    @NotBlank(message = "새로운 닉네임을 입력해주세요.")
    String newNickname
) {

}
