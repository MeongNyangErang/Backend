package com.meongnyangerang.meongnyangerang.dto.portone;

import com.meongnyangerang.meongnyangerang.dto.ReservationRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class PaymentReservationRequest {
  @NotBlank(message = "결제 고유번호(impUid)는 필수입니다.")
  private String impUid;

  @NotBlank(message = "주문 번호(merchantUid)는 필수입니다.")
  private String merchantUid;

  @Valid
  @NotNull(message = "예약 정보는 필수입니다.")
  private ReservationRequest reservationRequest;
}

