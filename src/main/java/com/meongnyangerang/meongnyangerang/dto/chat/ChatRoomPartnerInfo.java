package com.meongnyangerang.meongnyangerang.dto.chat;

import com.meongnyangerang.meongnyangerang.domain.chat.SenderType;

public record ChatRoomPartnerInfo(
    Long partnerId,
    String partnerName,
    String partnerImageUrl,
    SenderType partnerType
) {

}
