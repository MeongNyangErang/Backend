package com.meongnyangerang.meongnyangerang.service;

import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.*;

import com.meongnyangerang.meongnyangerang.component.PortOneClient;
import com.meongnyangerang.meongnyangerang.dto.portone.PaymentInfo;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PortOneService {

  private final PortOneClient portOneClient;

  public void verifyPayment(String impUid, Long expectedAmount) {
    PaymentInfo payment = portOneClient.getPaymentByImpUid(impUid);

    if (!payment.getStatus().equals("paid")) {
      throw new MeongnyangerangException(PAYMENT_NOT_COMPLETED);
    }

    if (!payment.getAmount().equals(expectedAmount)) {
      throw new MeongnyangerangException(PAYMENT_AMOUNT_MISMATCH);
    }
  }

  public void cancelPayment(String impUid, Long amount, String reason) {
    portOneClient.cancelPayment(impUid, reason, amount);
  }

}
