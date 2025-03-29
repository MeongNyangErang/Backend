package com.meongnyangerang.meongnyangerang.repository.accommodation;

import com.meongnyangerang.meongnyangerang.domain.accommodation.facility.AccommodationFacility;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccommodationFacilityRepository extends
    JpaRepository<AccommodationFacility, Long> {

  List<AccommodationFacility> findAllByAccommodationId(Long accommodationId);
}
