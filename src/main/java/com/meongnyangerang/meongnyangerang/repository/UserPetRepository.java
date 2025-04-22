package com.meongnyangerang.meongnyangerang.repository;

import com.meongnyangerang.meongnyangerang.domain.user.UserPet;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPetRepository extends JpaRepository<UserPet, Long> {

  long countByUserId(Long userId);

  List<UserPet> findAllByUserId(Long userId);
}
