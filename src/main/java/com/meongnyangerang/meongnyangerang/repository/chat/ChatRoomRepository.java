package com.meongnyangerang.meongnyangerang.repository.chat;

import com.meongnyangerang.meongnyangerang.domain.chat.ChatRoom;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

  // 사용자 ID로 채팅방 목록 조회
  List<ChatRoom> findAllByUserIdOrderByUpdatedAtDesc(@Param("userId") Long userId);

  // 호스트 ID로 채팅방 목록 조회
  List<ChatRoom> findAllByHostIdOrderByUpdatedAtDesc(@Param("hostId") Long hostId);
}
