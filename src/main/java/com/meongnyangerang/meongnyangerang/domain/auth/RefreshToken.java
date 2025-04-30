package com.meongnyangerang.meongnyangerang.domain.auth;

import com.meongnyangerang.meongnyangerang.domain.user.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class RefreshToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String refreshToken;

  @Column(nullable = false)
  private Long userId;

  @Enumerated(EnumType.STRING)
  private Role role;

  @Column(nullable = false)
  private LocalDateTime expiryDate;
}
