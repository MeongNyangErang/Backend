package com.meongnyangerang.meongnyangerang.dto.notification;

import com.meongnyangerang.meongnyangerang.domain.chat.SenderType;
import com.meongnyangerang.meongnyangerang.domain.host.Host;
import com.meongnyangerang.meongnyangerang.domain.user.User;

public record NotificationReceiverInfo(
    Long receiverId,
    SenderType receiverType,
    User user,
    Host host,
    String receiverEmail
) {

}
