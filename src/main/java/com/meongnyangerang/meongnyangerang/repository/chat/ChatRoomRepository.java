package com.meongnyangerang.meongnyangerang.repository.chat;

import com.meongnyangerang.meongnyangerang.domain.chat.ChatRoom;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

  // 사용자 ID로 채팅방 목록 조회
  Page<ChatRoom> findAllByUser_Id(Long userId, Pageable pageable);

  // 호스트 ID로 채팅방 목록 조회
  Page<ChatRoom> findAllByHost_Id(Long hostId, Pageable pageable);

  Optional<ChatRoom> findByUser_IdAndHost_Id(Long userId, Long hostId);
}
