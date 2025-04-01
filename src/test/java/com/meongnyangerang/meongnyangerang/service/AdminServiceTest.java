package com.meongnyangerang.meongnyangerang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meongnyangerang.meongnyangerang.component.MailComponent;
import com.meongnyangerang.meongnyangerang.domain.host.Host;
import com.meongnyangerang.meongnyangerang.domain.host.HostStatus;
import com.meongnyangerang.meongnyangerang.dto.CustomApplicationResponse;
import com.meongnyangerang.meongnyangerang.dto.PendingHostDetailResponse;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.HostRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

  @Mock
  private HostRepository hostRepository;

  @Mock
  private MailComponent mailComponent;

  @InjectMocks
  private AdminService adminService;

  @Test
  @DisplayName("관리자는 호스트 가입 신청 목록을 조회할 수 있다.")
  void getPendingHostList_success() {
    // given
    List<Host> hosts = List.of(
        Host.builder().id(1L).status(HostStatus.PENDING).createdAt(LocalDateTime.now()).build(),
        Host.builder().id(2L).status(HostStatus.ACTIVE).createdAt(LocalDateTime.now()).build(),
        Host.builder().id(3L).status(HostStatus.PENDING).createdAt(LocalDateTime.now()).build()
    );

    when(hostRepository.findAllByStatus(0L, 5 + 1, HostStatus.PENDING.name()))
        .thenReturn(hosts.stream().filter(host -> host.getStatus() == HostStatus.PENDING)
            .collect(Collectors.toList()));

    // when
    CustomApplicationResponse response = adminService.getPendingHostList(0L, 5);

    // then
    assertEquals(2, response.getContent().size());
    assertEquals(1L, response.getContent().get(0).getHostId());
  }

  @Test
  @DisplayName("가입 신청 목록 조회 후, 해당 신청 중 하나를 선택하면 상세 정보를 볼 수 있다.")
  void getPendingHostDetail_success() {
    // given
    Long hostId = 1L;

    List<Host> hosts = List.of(
        Host.builder().id(1L).status(HostStatus.PENDING)
            .email("test1@gmail.com")
            .name("test1")
            .phoneNumber("010-1111-1111")
            .businessLicenseImageUrl("https://business1.com")
            .submitDocumentImageUrl("https://sbubmit1.com")
            .build(),
        Host.builder().id(2L).status(HostStatus.ACTIVE)
            .email("test2@gmail.com")
            .name("test2")
            .phoneNumber("010-2222-2222")
            .businessLicenseImageUrl("https://business2.com")
            .submitDocumentImageUrl("https://sbubmit2.com")
            .build(),
        Host.builder().id(3L).status(HostStatus.PENDING)
            .email("test3@gmail.com")
            .name("test3")
            .phoneNumber("010-3333-3333")
            .businessLicenseImageUrl("https://business3.com")
            .submitDocumentImageUrl("https://sbubmit3.com")
            .build()
    );

    when(hostRepository.findByIdAndStatus(hostId, HostStatus.PENDING))
        .thenAnswer(invocation -> {
          Long id = invocation.getArgument(0);
          HostStatus status = invocation.getArgument(1);

          return hosts.stream()
              .filter(host -> host.getId().equals(id) && host.getStatus() == status)
              .findFirst();
        });

    // when
    PendingHostDetailResponse response = adminService.getPendingHostDetail(hostId);

    // then
    assertEquals("test1", response.getName());
    assertEquals("test1@gmail.com", response.getEmail());
    assertEquals("010-1111-1111", response.getPhoneNumber());
  }

  @Test
  @DisplayName("호스트 정보를 가져오면서 호스트의 상태가 PENDING 인지 확인 후 아니면, HOST_ALREADY_PROCESSED 예외가 발생해야 한다.")
  void getPendingHostDetail_host_already_processed() {
    // given
    Long hostId = 2L;

    when(hostRepository.findByIdAndStatus(hostId, HostStatus.PENDING)).thenReturn(
        Optional.empty());

    // when
    MeongnyangerangException e = assertThrows(MeongnyangerangException.class,
        () -> adminService.getPendingHostDetail(hostId));

    // then
    assertEquals(ErrorCode.HOST_ALREADY_PROCESSED, e.getErrorCode());
  }

  @Test
  @DisplayName("관리자는 호스트 가입 요청에 대해 승인을 할 수 있고, 승인 성공 메일을 발송해야 한다.")
  void approveHost_success() {
    // given
    Host host = Host.builder()
        .id(1L)
        .email("test@gmail.com")
        .status(HostStatus.PENDING)
        .build();

    when(hostRepository.findById(host.getId())).thenReturn(Optional.of(host));

    // when
    adminService.approveHost(host.getId());

    // then
    assertEquals(HostStatus.ACTIVE, host.getStatus());
    verify(mailComponent, times(1)).sendMail("test@gmail.com",
        "[멍랑이랑] 요청하신 호스트 가입이 승인되었습니다!",
        """
            <div>
              <h2>안녕하세요, 멍랑이랑입니다.</h2>
              <p>요청하신 <strong>호스트 가입</strong>이 승인되었습니다!</p>
              <p>앞으로 좋은 서비스로 보답하겠습니다.</p>
              <p>감사합니다.</p>
            </div>
            """);
  }

  @Test
  @DisplayName("호스트가 없는 경우, NOT_EXISTS_HOST 예외가 발생해야 한다.")
  void approveHost_not_exists_host() {
    // given
    Host host = Host.builder()
        .id(1L)
        .email("test@gmail.com")
        .status(HostStatus.PENDING)
        .build();

    when(hostRepository.findById(host.getId())).thenReturn(Optional.empty());

    // when & then
    MeongnyangerangException e = assertThrows(MeongnyangerangException.class,
        () -> adminService.approveHost(host.getId()));

    assertEquals(ErrorCode.NOT_EXISTS_HOST, e.getErrorCode());
  }

  @Test
  @DisplayName("이미 처리(승인/거절)된 경우, HOST_ALREADY_PROCESSED 예외가 발생해야 한다.")
  void approveHost_host_already_processed() {
    // given
    Host host = Host.builder()
        .id(1L)
        .email("test@gmail.com")
        .status(HostStatus.ACTIVE)
        .build();

    when(hostRepository.findById(host.getId())).thenReturn(Optional.of(host));

    // when & then
    MeongnyangerangException e = assertThrows(MeongnyangerangException.class,
        () -> adminService.approveHost(host.getId()));

    assertEquals(ErrorCode.HOST_ALREADY_PROCESSED, e.getErrorCode());
  }
}