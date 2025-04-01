package com.meongnyangerang.meongnyangerang.repository.room;

import com.meongnyangerang.meongnyangerang.domain.room.facility.RoomFacility;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomFacilityRepository extends JpaRepository<RoomFacility, Long> {

  List<RoomFacility> findAllByRoomId(Long roomId);

  @Modifying
  @Query("DELETE FROM RoomFacility f WHERE f.room.id = :roomId")
  void deleteAllByRoomId(Long roomId);
}
