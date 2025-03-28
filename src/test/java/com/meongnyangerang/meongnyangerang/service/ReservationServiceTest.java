package com.meongnyangerang.meongnyangerang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meongnyangerang.meongnyangerang.domain.reservation.ReservationSlot;
import com.meongnyangerang.meongnyangerang.domain.room.Room;
import com.meongnyangerang.meongnyangerang.domain.user.User;
import com.meongnyangerang.meongnyangerang.dto.ReservationRequest;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.ReservationRepository;
import com.meongnyangerang.meongnyangerang.repository.ReservationSlotRepository;
import com.meongnyangerang.meongnyangerang.repository.RoomRepository;
import com.meongnyangerang.meongnyangerang.repository.UserRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

  @Mock
  private ReservationRepository reservationRepository;

  @Mock
  private ReservationSlotRepository reservationSlotRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private RoomRepository roomRepository;

  @InjectMocks
  private ReservationService reservationService;

  @Test
  @DisplayName("사용자가 예약 등록 시, 예약 등록에 성공해야 한다.")
  void createReservation_success() {
    // given
    Long userId = 1L;
    Long roomId = 101L;
    LocalDate checkInDate = LocalDate.of(2025, 1, 1);
    LocalDate checkOutDate = LocalDate.of(2025, 1, 3);
    ReservationRequest request = ReservationRequest.builder()
        .roomId(roomId)
        .checkInDate(checkInDate)
        .checkOutDate(checkOutDate)
        .build();

    User user = User.builder()
        .id(userId)
        .build();

    Room room = Room.builder()
        .id(roomId)
        .build();

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
    when(reservationSlotRepository.existsByRoomIdAndReservedDateBetweenAndIsReserved(
        roomId, checkInDate, checkOutDate.minusDays(1), true)).thenReturn(false);
    when(reservationSlotRepository.findByRoomIdAndReservedDate(
        roomId, checkInDate)).thenReturn(Optional.empty());
    when(reservationSlotRepository.findByRoomIdAndReservedDate(
        roomId, checkOutDate.minusDays(1))).thenReturn(Optional.empty());

    // when
    reservationService.createReservation(userId, request);

    // then
    verify(reservationSlotRepository, times(1)).saveAll(any());
    verify(reservationRepository, times(1)).save(any());
  }

  @Test
  @DisplayName("사용자가 존재하지 않을 경우, USER_NOT_FOUND 예외가 발생해야 한다.")
  void createReservation_user_not_found() {
    // given
    Long userId = 1L;
    Long roomId = 101L;
    LocalDate checkInDate = LocalDate.of(2025, 1, 1);
    LocalDate checkOutDate = LocalDate.of(2025, 1, 3);
    ReservationRequest request = ReservationRequest.builder()
        .roomId(roomId)
        .checkInDate(checkInDate)
        .checkOutDate(checkOutDate)
        .build();

    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    // when & then
    MeongnyangerangException e = assertThrows(MeongnyangerangException.class,
        () -> {
          reservationService.createReservation(userId, request);
        });

    assertEquals(ErrorCode.USER_NOT_FOUND, e.getErrorCode());
  }

  @Test
  @DisplayName("객실이 존재하지 않을 경우, ROOM_NOT_FOUND 예외가 발생해야 한다.")
  void createReservation_room_not_found() {
    // given
    Long userId = 1L;
    Long roomId = 101L;
    LocalDate checkInDate = LocalDate.of(2025, 1, 1);
    LocalDate checkOutDate = LocalDate.of(2025, 1, 3);
    ReservationRequest request = ReservationRequest.builder()
        .roomId(roomId)
        .checkInDate(checkInDate)
        .checkOutDate(checkOutDate)
        .build();

    User user = User.builder()
        .id(userId)
        .build();

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

    // when & then
    MeongnyangerangException e = assertThrows(MeongnyangerangException.class,
        () -> {
          reservationService.createReservation(userId, request);
        });

    assertEquals(ErrorCode.ROOM_NOT_FOUND, e.getErrorCode());
  }

  @Test
  @DisplayName("객실이 이미 예약되어 있을 경우, ROOM_ALREADY_RESERVED 예외가 발생해야 한다.")
  void createReservation_room_already_reserved() {
    // given
    Long userId = 1L;
    Long roomId = 101L;
    LocalDate checkInDate = LocalDate.of(2025, 1, 1);
    LocalDate checkOutDate = LocalDate.of(2025, 1, 3);
    ReservationRequest request = ReservationRequest.builder()
        .roomId(roomId)
        .checkInDate(checkInDate)
        .checkOutDate(checkOutDate)
        .build();

    User user = User.builder()
        .id(userId)
        .build();

    Room room = Room.builder()
        .id(roomId)
        .build();

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
    when(reservationSlotRepository.existsByRoomIdAndReservedDateBetweenAndIsReserved(
        roomId, checkInDate, checkOutDate.minusDays(1), true)).thenReturn(true);

    // when & then
    MeongnyangerangException e = assertThrows(MeongnyangerangException.class,
        () -> {
          reservationService.createReservation(userId, request);
        });

    assertEquals(ErrorCode.ROOM_ALREADY_RESERVED, e.getErrorCode());
  }

  @Test
  @DisplayName("이미 예약된 슬롯이라면, ROOM_ALREADY_RESERVED 예외가 발생해야 한다.")
  void createReservation_slot_already_reserved() {
    // given
    Long userId = 1L;
    Long roomId = 101L;
    LocalDate checkInDate = LocalDate.of(2025, 1, 1);
    LocalDate checkOutDate = LocalDate.of(2025, 1, 3);
    ReservationRequest request = ReservationRequest.builder()
        .roomId(roomId)
        .checkInDate(checkInDate)
        .checkOutDate(checkOutDate)
        .build();

    User user = User.builder()
        .id(userId)
        .build();

    Room room = Room.builder()
        .id(roomId)
        .build();

    ReservationSlot existingSlot = new ReservationSlot(room, checkInDate, true);

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
    when(reservationSlotRepository.existsByRoomIdAndReservedDateBetweenAndIsReserved(
        roomId, checkInDate, checkOutDate.minusDays(1), true)).thenReturn(false);
    when(reservationSlotRepository.findByRoomIdAndReservedDate(
        roomId, checkInDate)).thenReturn(Optional.of(existingSlot));

    // when & then
    MeongnyangerangException e = assertThrows(MeongnyangerangException.class,
        () -> {
          reservationService.createReservation(userId, request);
        });

    assertEquals(ErrorCode.ROOM_ALREADY_RESERVED, e.getErrorCode());
  }

  @Test
  @DisplayName("같은 날짜, 같은 객실에 여러 사람이 예약을 시도하는 경우, 하나는 성공하고 나머지는 예외가 발생해야 한다.")
  void createReservation_optimisticLockException() throws InterruptedException {
    // given
    Long userId = 1L;
    Long roomId = 101L;
    LocalDate checkInDate = LocalDate.of(2025, 1, 1);
    LocalDate checkOutDate = LocalDate.of(2025, 1, 3);
    ReservationRequest request = ReservationRequest.builder()
        .roomId(roomId)
        .checkInDate(checkInDate)
        .checkOutDate(checkOutDate)
        .build();

    User user = User.builder()
        .id(userId)
        .build();

    Room room = Room.builder()
        .id(roomId)
        .build();

    // 예약 슬롯 객체 생성 -> 이 객실만 조회되게 해서 1개인 걸 가정해서 충돌 발생시키기
    ReservationSlot reservationSlot = new ReservationSlot(room, checkInDate, false);

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
    when(reservationSlotRepository.existsByRoomIdAndReservedDateBetweenAndIsReserved(
        roomId, checkInDate, checkOutDate.minusDays(1), true)).thenReturn(false);
    when(reservationSlotRepository.findByRoomIdAndReservedDate(
        roomId, checkInDate)).thenReturn(Optional.of(reservationSlot));

    ExecutorService executorService = Executors.newFixedThreadPool(3);  // 3개의 스레드 실행
    CountDownLatch latch = new CountDownLatch(3); // 3개의 예약이 끝날 때까지 대기
    AtomicInteger successCount = new AtomicInteger(); // 성공한 예약의 개수 세기
    AtomicInteger failCount = new AtomicInteger();  // 실패한 예약의 개수 세기

    // 3개의 스레드 각각 예약 시도
    for (int i = 0; i < 3; i++) {
      executorService.submit(() -> {  // 스레드 제출
        try {
          // 예약 시도 -> 성공하면 성공 예약 개수 증가
          reservationService.createReservation(userId, request);
          successCount.incrementAndGet();
        } catch (MeongnyangerangException e) {
          // OptimisticLockException 잡아서 MeongnyangerangException 던지기 때문에 실패 개수 증가
          failCount.incrementAndGet();
        } finally {
          // 작업이 끝나면 latch 를 하나 줄여서 작업 완료를 알림
          latch.countDown();
        }
      });
    }

    // 모든 스레드가 끝날 때까지 대기
    latch.await();

    // then
    assertEquals(1, successCount.get());
    assertEquals(2, failCount.get());
  }
}