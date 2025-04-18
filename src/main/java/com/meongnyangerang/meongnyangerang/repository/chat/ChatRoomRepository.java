package com.meongnyangerang.meongnyangerang.repository.chat;

import com.meongnyangerang.meongnyangerang.domain.chat.ChatRoom;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

  Page<ChatRoom> findAllByUser_Id(Long userId, Pageable pageable);

  Page<ChatRoom> findAllByHost_Id(Long hostId, Pageable pageable);

  Optional<ChatRoom> findByUser_IdAndHost_Id(Long userId, Long hostId);
}
