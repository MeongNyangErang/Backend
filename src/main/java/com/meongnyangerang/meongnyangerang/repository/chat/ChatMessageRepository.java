package com.meongnyangerang.meongnyangerang.repository.chat;

import com.meongnyangerang.meongnyangerang.domain.chat.ChatMessage;
import com.meongnyangerang.meongnyangerang.domain.chat.SenderType;
import java.time.LocalDateTime;
import java.util.List;
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

  /**
   * 특정 시간 이전의 메시지를 페이징하여 최신순으로 조회 (커서 기반 페이징)
   */
  List<ChatMessage> findByChatRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(
      Long chatRoomId, LocalDateTime cursor, Pageable pageable);

  @Query("SELECT m FROM ChatMessage m "
      + "WHERE m.chatRoom.id = :chatRoomId "
      + "AND (:cursorId IS NULL OR m.id < :cursorId) "
      + "ORDER BY m.id DESC")
  List<ChatMessage> findByChatRoomIdWithCursor(
      @Param("chatRoomId") Long chatRoomId,
      @Param("cursorId") Long cursorId,
      Pageable pageable
  );
}
