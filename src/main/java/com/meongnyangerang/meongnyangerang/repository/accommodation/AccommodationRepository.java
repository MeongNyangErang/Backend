package com.meongnyangerang.meongnyangerang.repository.accommodation;

import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccommodationRepository extends JpaRepository<Accommodation, Long> {

  boolean existsByHostId(Long hostId);

  Optional<Accommodation> findByHostId(Long hostId);
}
