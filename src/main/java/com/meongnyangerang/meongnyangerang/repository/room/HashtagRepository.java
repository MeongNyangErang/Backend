package com.meongnyangerang.meongnyangerang.repository.room;

import com.meongnyangerang.meongnyangerang.domain.room.facility.Hashtag;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HashtagRepository extends JpaRepository<Hashtag, Long> {

  List<Hashtag> findAllByRoomId(Long roomId);
}
