package com.meongnyangerang.meongnyangerang.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meongnyangerang.meongnyangerang.domain.host.Host;
import com.meongnyangerang.meongnyangerang.domain.host.HostStatus;
import com.meongnyangerang.meongnyangerang.domain.reservation.ReservationStatus;
import com.meongnyangerang.meongnyangerang.dto.HostSignupRequest;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.HostRepository;
import com.meongnyangerang.meongnyangerang.repository.ReservationRepository;
import com.meongnyangerang.meongnyangerang.service.image.ImageService;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class HostServiceTest {

  @InjectMocks
  private HostService hostService;

  @Mock
  private HostRepository hostRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private ImageService imageService;

  @Mock
  private ReservationRepository reservationRepository;

  @Test
  @DisplayName("호스트 회원가입 성공 테스트")
  void registerHostSuccess() throws IOException {
    // given
    String email = "host@example.com";
    String password = "password123";
    String encodedPassword = "encodedPassword";

    HostSignupRequest request = new HostSignupRequest();
    request.setEmail(email);
    request.setName("호스트");
    request.setNickname("호스트닉네임");
    request.setPassword(password);
    request.setPhoneNumber("010-1234-5678");

    MultipartFile profileImage = new MockMultipartFile("profile", "profile.jpg", "image/jpeg", new byte[0]);
    MultipartFile businessLicense = new MockMultipartFile("license", "license.jpg", "image/jpeg", new byte[0]);
    MultipartFile submitDocument = new MockMultipartFile("document", "document.jpg", "image/jpeg", new byte[0]);

    when(hostRepository.existsByEmail(email)).thenReturn(false);
    when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
    when(imageService.storeImage(profileImage)).thenReturn("http://s3.com/profile.jpg");
    when(imageService.storeImage(businessLicense)).thenReturn("http://s3.com/license.jpg");
    when(imageService.storeImage(submitDocument)).thenReturn("http://s3.com/document.jpg");

    // when
    assertDoesNotThrow(() -> hostService.registerHost(request, profileImage, businessLicense, submitDocument));

    // then
    verify(hostRepository).save(any(Host.class));
  }

  @Test
  @DisplayName("중복 이메일 호스트 회원가입 실패 테스트")
  void registerHostDuplicateEmail() {
    // given
    String email = "existing@example.com";

    HostSignupRequest request = new HostSignupRequest();
    request.setEmail(email);

    when(hostRepository.existsByEmail(email)).thenReturn(true);

    // when & then
    assertThrows(MeongnyangerangException.class,
        () -> hostService.registerHost(request, null, null, null));
    verify(hostRepository, never()).save(any(Host.class));
  }

  @Test
  @DisplayName("호스트 탈퇴 성공")
  void deleteHostSuccess() {
    // given
    Long hostId = 1L;

    Host host = Host.builder()
        .id(hostId)
        .email("host@example.com")
        .status(HostStatus.ACTIVE)
        .build();

    when(hostRepository.findById(hostId)).thenReturn(Optional.of(host));
    when(reservationRepository.existsByHostIdAndStatus(hostId, ReservationStatus.RESERVED)).thenReturn(false);

    // when
    hostService.deleteHost(hostId);

    // then
    assertEquals(HostStatus.DELETED, host.getStatus());
    assertNotNull(host.getDeletedAt());
  }

  @Test
  @DisplayName("호스트 탈퇴 실패 - 예약 존재")
  void deleteHostFailDueToReservation() {
    // given
    Long hostId = 1L;

    when(hostRepository.findById(hostId)).thenReturn(Optional.of(new Host()));
    when(reservationRepository.existsByHostIdAndStatus(hostId, ReservationStatus.RESERVED)).thenReturn(true);

    // when & then
    assertThrows(MeongnyangerangException.class, () -> hostService.deleteHost(hostId));
  }
}
