package com.meongnyangerang.meongnyangerang.domain.host;

import com.meongnyangerang.meongnyangerang.domain.user.AuthProvider;
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
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@EntityListeners(AuditingEntityListener.class)
public class Host {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(length = 100, nullable = false, unique = true)
  private String email;

  @Column(length = 50, nullable = false)
  private String name;

  @Column(length = 50, nullable = false, unique = true)
  private String nickname;

  @Column(nullable = false)
  private String password;

  private String profileImageUrl;

  @Column(nullable = false)
  private String businessLicenseImageUrl;

  @Column(nullable = false)
  private String submitDocumentImageUrl;

  @Column(length = 20, nullable = false)
  private String phoneNumber;

  @Enumerated(EnumType.STRING)
  @Column(length = 50, nullable = false)
  private HostStatus status;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AuthProvider provider; // LOCAL or KAKAO

  private String oauthId; // 소셜 로그인 고유 ID

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(nullable = false)
  private LocalDateTime updatedAt;

  private LocalDateTime deletedAt;

  public void updatePhoneNumber(String newPhoneNumber) {
    this.phoneNumber = newPhoneNumber;
  }

  public void updateName(String newName) {
    this.name = newName;
  }

  public void updatePassword(String newPassword) {
    this.password = newPassword;
  }

  public void updateNickname(String newNickname) {
    this.nickname = newNickname;
  }

  public void updateProfileImage(String newProfileImage) {
    this.profileImageUrl = newProfileImage;
  }
}
