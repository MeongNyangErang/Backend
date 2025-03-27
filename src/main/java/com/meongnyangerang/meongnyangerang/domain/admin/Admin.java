package com.meongnyangerang.meongnyangerang.domain.admin;

import com.meongnyangerang.meongnyangerang.domain.user.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@EntityListeners(AuditingEntityListener.class)
public class Admin {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 100, unique = true)
  private String email;

  @Column(nullable = false)
  private String password;

  @Column(nullable = false, length = 50)
  private String name;

  @Enumerated(EnumType.STRING)
  private Role role;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;
}
