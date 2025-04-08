package com.meongnyangerang.meongnyangerang.dto.chat;

import java.util.List;

public record ChatMessagesResponse(
    List<ChatMessageResponse> messages,
    Long nextCursorId,
    boolean hasNext
) {

}
