package com.meongnyangerang.meongnyangerang.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PasswordUpdateRequest(

    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$])[A-Za-z\\d!@#$]{8,}$",
        message = "비밀번호는 최소 8자 이상이며, 영문, 숫자, 특수문자(!,@,#,$)를 각각 하나 이상 포함해야 합니다.")
    @NotBlank(message = "기존 비밀번호를 입력해주세요.")
    String currentPassword,

    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$])[A-Za-z\\d!@#$]{8,}$",
        message = "비밀번호는 최소 8자 이상이며, 영문, 숫자, 특수문자(!,@,#,$)를 각각 하나 이상 포함해야 합니다.")
    @NotBlank(message = "새로운 비밀번호를 입력해주세요.")
    String newPassword

) {

}


