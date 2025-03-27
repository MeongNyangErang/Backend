package com.meongnyangerang.meongnyangerang.repository;

import com.meongnyangerang.meongnyangerang.domain.host.Host;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HostRepository extends JpaRepository<Host, Long> {
  boolean existsByEmail(String email);

  boolean existsByNickname(String nickname);

  Optional<Host> findByEmail(String email);
}
