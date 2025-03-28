package com.meongnyangerang.meongnyangerang.security;

import com.meongnyangerang.meongnyangerang.domain.host.HostStatus;
import com.meongnyangerang.meongnyangerang.domain.user.Role;
import com.meongnyangerang.meongnyangerang.domain.user.UserStatus;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * User, Host, Admin을 처리할 수 있도록 role을 포함하여 관리
 * GrantedAuthority 목록을 role 기반으로 설정
 */

@Getter
public class UserDetailsImpl implements UserDetails {

  private final Long id;
  private final String email;
  private final String password;
  private final Role role;
  private final String nickname;
  private final Enum<?> status; // UserStatus or HostStatus를 받을 수 있도록 Enum의 상위 타입 사용
  private final Collection<? extends GrantedAuthority> authorities;

  public UserDetailsImpl(Long id, String email, String password, Role role, String nickname, Enum<?> status) {
    this.id = id;
    this.email = email;
    this.password = password;
    this.role = role;
    this.nickname = nickname;
    this.status = status;
    this.authorities = List.of(new SimpleGrantedAuthority(role.name())); // ROLE_XXX 형태로 권한 부여
  }

  @Override
  public String getUsername() {
    return email;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  // 계정 잠금
  @Override
  public boolean isAccountNonLocked() {
    if (role == Role.ROLE_USER && status instanceof UserStatus userStatus) {
      return userStatus != UserStatus.DELETED;
    }
    if (role == Role.ROLE_HOST && status instanceof HostStatus hostStatus) {
      return hostStatus != HostStatus.DELETED;
    }
    return true; // admin은 상태 없음
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  // 계정 활성화
  @Override
  public boolean isEnabled() {
    if (role == Role.ROLE_HOST && status instanceof HostStatus hostStatus) {
      return hostStatus == HostStatus.ACTIVE; // 승인된 상태만 로그인 허용
    }
    if (role == Role.ROLE_USER && status instanceof UserStatus userStatus) {
      return userStatus == UserStatus.ACTIVE;
    }
    return true;
  }
}
