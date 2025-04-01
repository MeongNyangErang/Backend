package com.meongnyangerang.meongnyangerang.repository.room;

import com.meongnyangerang.meongnyangerang.domain.room.facility.Hashtag;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HashtagRepository extends JpaRepository<Hashtag, Long> {

  List<Hashtag> findAllByRoomId(Long roomId);

  @Modifying
  @Query("DELETE FROM Hashtag h WHERE h.room.id = :roomId")
  void deleteAllByRoomId(@Param("roomId") Long id);
}
