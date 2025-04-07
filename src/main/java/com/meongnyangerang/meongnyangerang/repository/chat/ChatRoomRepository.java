package com.meongnyangerang.meongnyangerang.repository.chat;

import com.meongnyangerang.meongnyangerang.domain.chat.ChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

  // 사용자 ID로 채팅방 목록 조회
  Page<ChatRoom> findAllByUser_IdOrderByUpdatedAtDesc(Long userId, Pageable pageable);

  // 호스트 ID로 채팅방 목록 조회
  Page<ChatRoom> findAllByHost_IdOrderByUpdatedAtDesc(Long hostId, Pageable pageable);
}
