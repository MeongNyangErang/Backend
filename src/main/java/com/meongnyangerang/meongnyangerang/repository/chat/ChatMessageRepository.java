package com.meongnyangerang.meongnyangerang.repository.chat;

import com.meongnyangerang.meongnyangerang.domain.chat.ChatMessage;
import com.meongnyangerang.meongnyangerang.domain.chat.SenderType;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

  // 마지막 메시지 조회
  ChatMessage findTopByChatRoomIdOrderByCreatedAtDesc(Long roomId);

  int countByChatRoomIdAndSenderTypeAndCreatedAtGreaterThan(
      Long roomId,
      SenderType senderType,
      LocalDateTime lastReadTime
  );

  Page<ChatMessage> findByChatRoomId(Long chatRoomId, Pageable pageable);
}
