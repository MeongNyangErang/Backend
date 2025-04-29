package com.meongnyangerang.meongnyangerang.repository.auth;

import com.meongnyangerang.meongnyangerang.domain.auth.RefreshToken;
import com.meongnyangerang.meongnyangerang.domain.user.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  void deleteByUserIdAndRole(Long userId, Role role);

  Optional<RefreshToken> findByRefreshToken(String refreshToken);
}
