package com.meongnyangerang.meongnyangerang.repository.accommodation;

import com.meongnyangerang.meongnyangerang.domain.accommodation.facility.AccommodationPetFacility;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccommodationPetFacilityRepository extends
    JpaRepository<AccommodationPetFacility, Long> {

  List<AccommodationPetFacility> findAllByAccommodationId(Long accommodationId);
}
