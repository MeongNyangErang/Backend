package com.meongnyangerang.meongnyangerang.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meongnyangerang.meongnyangerang.domain.host.Host;
import com.meongnyangerang.meongnyangerang.domain.host.HostStatus;
import com.meongnyangerang.meongnyangerang.domain.user.User;
import com.meongnyangerang.meongnyangerang.domain.user.UserStatus;
import com.meongnyangerang.meongnyangerang.repository.HostRepository;
import com.meongnyangerang.meongnyangerang.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountCleanupServiceTest {

  @InjectMocks
  private AccountCleanupService accountCleanupService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private HostRepository hostRepository;

  @Test
  @DisplayName("30일 경과된 사용자 및 호스트 하드 삭제")
  void deleteExpiredSoftDeletedAccounts() {
    // given
    LocalDateTime cutoff = LocalDateTime.now().minusDays(30);

    List<User> expiredUsers = List.of(User.builder().id(1L).build());
    List<Host> expiredHosts = List.of(Host.builder().id(1L).build());

    when(userRepository.findAllByStatusAndDeletedAtBefore(UserStatus.DELETED, cutoff))
        .thenReturn(expiredUsers);
    when(hostRepository.findAllByStatusAndDeletedAtBefore(HostStatus.DELETED, cutoff))
        .thenReturn(expiredHosts);

    // when
    accountCleanupService.deleteExpiredSoftDeletedUsers();

    // then
    verify(userRepository).deleteAll(expiredUsers);
    verify(hostRepository).deleteAll(expiredHosts);
  }
}
