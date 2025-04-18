package com.meongnyangerang.meongnyangerang.repository.room;

import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import com.meongnyangerang.meongnyangerang.domain.room.Room;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

  Page<Room> findAllByAccommodationId(Long accommodationId, Pageable pageable);

  List<Room> findAllByAccommodationId(Long id);

  List<Room> findAllByAccommodationIdOrderByPriceAsc(Long accommodationId);

  Room findFirstByAccommodationOrderByPriceAsc(Accommodation accommodation);
}
