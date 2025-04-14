package com.meongnyangerang.meongnyangerang.repository.accommodation;

import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AccommodationRepository extends JpaRepository<Accommodation, Long> {

  boolean existsByHostId(Long hostId);

  Optional<Accommodation> findByHostId(Long hostId);

  List<Accommodation> findTop10ByOrderByViewCountDesc();

  @Modifying
  @Query("UPDATE Accommodation a SET a.viewCount = 0")
  void resetAllViewCounts();
}
