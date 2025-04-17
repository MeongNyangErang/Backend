package com.meongnyangerang.meongnyangerang.repository.chat;

import com.meongnyangerang.meongnyangerang.domain.chat.ChatMessage;
import com.meongnyangerang.meongnyangerang.domain.chat.SenderType;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

  // 마지막 메시지 조회
  ChatMessage findTopByChatRoomIdOrderByCreatedAtDesc(Long roomId);

  // 읽지 않은 메시지 수 조회
  @Query("SELECT COUNT(cm) FROM ChatMessage cm " +
      "WHERE cm.chatRoom.id = :roomId " +
      "AND cm.senderType = :senderType " +
      "AND cm.createdAt > :lastReadTime")
  int countUnreadMessages(@Param("roomId") Long roomId,
      @Param("senderType") SenderType senderType,
      @Param("lastReadTime") LocalDateTime lastReadTime
  );

  Page<ChatMessage> findByChatRoomId(Long chatRoomId, Pageable pageable);
}
