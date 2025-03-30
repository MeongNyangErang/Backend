package com.meongnyangerang.meongnyangerang.repository.accommodation;

import com.meongnyangerang.meongnyangerang.domain.accommodation.AccommodationImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccommodationImageRepository extends JpaRepository<AccommodationImage, Long> {

  List<AccommodationImage> findAllByAccommodationId(Long accommodationId);

  void deleteAllByAccommodationId(Long accommodationId);
}
