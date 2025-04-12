package com.meongnyangerang.meongnyangerang.repository.room;

import com.meongnyangerang.meongnyangerang.domain.room.Room;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

  @Query("SELECT r FROM Room r " +
      "WHERE r.accommodation.id = :accommodationId " +
      "AND (:cursorId IS NULL OR r.id < :cursorId) " +
      "ORDER BY r.id DESC")
  List<Room> findByAccommodationIdWithCursor(
      @Param("accommodationId") Long accommodationId,
      @Param("cursorId") Long cursorId,
      Pageable pageable
  );

  List<Room> findAllByAccommodationId(Long id);

  List<Room> findAllByAccommodationIdOrderByPriceAsc(Long accommodationId);
}
