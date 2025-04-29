package com.meongnyangerang.meongnyangerang.repository.auth;

import com.meongnyangerang.meongnyangerang.domain.auth.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

}
