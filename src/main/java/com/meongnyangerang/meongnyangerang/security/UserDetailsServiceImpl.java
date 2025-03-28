package com.meongnyangerang.meongnyangerang.security;

import com.meongnyangerang.meongnyangerang.domain.admin.Admin;
import com.meongnyangerang.meongnyangerang.domain.host.Host;
import com.meongnyangerang.meongnyangerang.domain.user.User;
import com.meongnyangerang.meongnyangerang.repository.AdminRepository;
import com.meongnyangerang.meongnyangerang.repository.HostRepository;
import com.meongnyangerang.meongnyangerang.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

  private final UserRepository userRepository;
  private final HostRepository hostRepository;
  private final AdminRepository adminRepository;

  /**
   * UserRepository, HostRepository, AdminRepository를 이용하여 email을 기준으로 사용자 정보를 검색
   * 해당하는 엔티티를 찾아 UserDetailsImpl 객체로 변환
   */
  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    // User 조회
    Optional<User> userOptional = userRepository.findByEmail(email);
    if (userOptional.isPresent()) {
      User user = userOptional.get();
      return new UserDetailsImpl(user.getId(), user.getEmail(), user.getPassword(), user.getRole(),
          user.getNickname(), user.getStatus());
    }

    // Host 조회
    Optional<Host> hostOptional = hostRepository.findByEmail(email);
    if (hostOptional.isPresent()) {
      Host host = hostOptional.get();
      return new UserDetailsImpl(host.getId(), host.getEmail(), host.getPassword(), host.getRole(),
          host.getNickname(), host.getStatus());
    }

    // Admin 조회
    Optional<Admin> adminOptional = adminRepository.findByEmail(email);
    if (adminOptional.isPresent()) {
      Admin admin = adminOptional.get();
      return new UserDetailsImpl(admin.getId(), admin.getEmail(), admin.getPassword(), admin.getRole(),
          null, null); // Admin 에는 nickname, status 없음
    }

    // 사용자를 찾을 수 없을 때 예외 발생
    throw new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email);
  }
}
