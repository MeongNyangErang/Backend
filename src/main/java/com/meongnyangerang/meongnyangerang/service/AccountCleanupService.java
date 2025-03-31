package com.meongnyangerang.meongnyangerang.service;

import com.meongnyangerang.meongnyangerang.domain.host.Host;
import com.meongnyangerang.meongnyangerang.domain.host.HostStatus;
import com.meongnyangerang.meongnyangerang.domain.user.User;
import com.meongnyangerang.meongnyangerang.domain.user.UserStatus;
import com.meongnyangerang.meongnyangerang.repository.HostRepository;
import com.meongnyangerang.meongnyangerang.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountCleanupService {

  private final UserRepository userRepository;
  private final HostRepository hostRepository;

  @Scheduled(cron = "0 0 3 * * ?") // 매일 새벽 3시에 실행
  @Transactional
  public void deleteExpiredSoftDeletedUsers() {
    LocalDateTime cutoff = LocalDateTime.now().minusDays(30);

    List<User> usersToDelete = userRepository.findAllByStatusAndDeletedAtBefore(UserStatus.DELETED, cutoff);
    userRepository.deleteAll(usersToDelete);
    log.info("하드 삭제된 사용자 수: {}", usersToDelete.size());

    List<Host> hostsToDelete = hostRepository.findAllByStatusAndDeletedAtBefore(HostStatus.DELETED, cutoff);
    hostRepository.deleteAll(hostsToDelete);
    log.info("하드 삭제된 호스트 수: {}", hostsToDelete.size());
  }

}
