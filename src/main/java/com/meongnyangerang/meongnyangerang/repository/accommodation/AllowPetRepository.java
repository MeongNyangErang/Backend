package com.meongnyangerang.meongnyangerang.repository.accommodation;

import com.meongnyangerang.meongnyangerang.domain.accommodation.AllowPet;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AllowPetRepository extends JpaRepository<AllowPet, Long> {

  List<AllowPet> findAllByAccommodationId(Long accommodationId);
}
