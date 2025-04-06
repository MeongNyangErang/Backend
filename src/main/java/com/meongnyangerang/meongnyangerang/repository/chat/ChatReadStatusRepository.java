package com.meongnyangerang.meongnyangerang.repository.chat;

import com.meongnyangerang.meongnyangerang.domain.chat.ChatReadStatus;
import com.meongnyangerang.meongnyangerang.domain.chat.SenderType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatReadStatusRepository extends JpaRepository<ChatReadStatus, Long> {

  // 참여자의 읽음 상태 조회
  Optional<ChatReadStatus> findByChatRoomIdAndParticipantIdAndParticipantType(
      Long chatRoomId, Long participantId, SenderType participantType);
}
