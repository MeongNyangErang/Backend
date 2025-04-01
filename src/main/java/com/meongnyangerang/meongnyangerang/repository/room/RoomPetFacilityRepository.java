package com.meongnyangerang.meongnyangerang.repository.room;

import com.meongnyangerang.meongnyangerang.domain.room.facility.RoomPetFacility;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomPetFacilityRepository extends JpaRepository<RoomPetFacility, Long> {

  List<RoomPetFacility> findAllByRoomId(Long roomId);

  @Modifying
  @Query("DELETE FROM RoomPetFacility pf WHERE pf.room.id = :roomId")
  void deleteAllByRoomId(Long roomId);
}
