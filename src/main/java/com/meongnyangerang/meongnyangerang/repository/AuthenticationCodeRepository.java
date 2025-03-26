package com.meongnyangerang.meongnyangerang.repository;

import com.meongnyangerang.meongnyangerang.domain.auth.AuthenticationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthenticationCodeRepository extends JpaRepository<AuthenticationCode, Long> {

  void deleteAllByEmail(String email);
}
