package com.meongnyangerang.meongnyangerang.repository.accommodation;

import com.meongnyangerang.meongnyangerang.domain.accommodation.AccommodationImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AccommodationImageRepository extends JpaRepository<AccommodationImage, Long> {

  List<AccommodationImage> findAllByAccommodationId(Long accommodationId);

  void deleteAllByAccommodationId(Long accommodationId);

  @Modifying
  @Query("DELETE FROM AccommodationImage ai "
      + "WHERE ai.imageUrl IN :urls")
  void deleteAllByImageUrl(@Param("urls") List<String> deleteImageUrls);

  int countByAccommodationId(Long id);
}
