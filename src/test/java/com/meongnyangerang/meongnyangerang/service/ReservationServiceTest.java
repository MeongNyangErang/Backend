package com.meongnyangerang.meongnyangerang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import com.meongnyangerang.meongnyangerang.domain.host.Host;
import com.meongnyangerang.meongnyangerang.domain.reservation.Reservation;
import com.meongnyangerang.meongnyangerang.domain.reservation.ReservationSlot;
import com.meongnyangerang.meongnyangerang.domain.reservation.ReservationStatus;
import com.meongnyangerang.meongnyangerang.domain.room.Room;
import com.meongnyangerang.meongnyangerang.domain.user.User;
import com.meongnyangerang.meongnyangerang.dto.CustomReservationResponse;
import com.meongnyangerang.meongnyangerang.dto.HostReservationResponse;
import com.meongnyangerang.meongnyangerang.dto.ReservationRequest;
import com.meongnyangerang.meongnyangerang.dto.UserReservationResponse;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.ReservationRepository;
import com.meongnyangerang.meongnyangerang.repository.ReservationSlotRepository;
import com.meongnyangerang.meongnyangerang.repository.RoomRepository;
import com.meongnyangerang.meongnyangerang.repository.UserRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
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
  private AccommodationRepository accommodationRepository;

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

  @Test
  @DisplayName("해당 유저가 예약한 내역만 볼 수 있고 상태에 따라 조회를 할 수 있다.")
  void getUserReservation_success() {
    Long userId = 1L;
    Long cursorId = 0L;
    Long accommodationId = 1L;
    int size = 20;
    ReservationStatus status = ReservationStatus.RESERVED;

    User user = User.builder().id(1L).build();
    User user2 = User.builder().id(2L).build();
    Host host = Host.builder().id(1L).build();
    Accommodation accommodation = Accommodation.builder().id(1L).host(host).build();
    Room room1 = Room.builder().id(101L).accommodation(accommodation).name("room").
        checkInTime(LocalTime.parse("11:00")).checkOutTime(LocalTime.parse("15:00")).build();
    Room room2 = Room.builder().id(102L).accommodation(accommodation).name("room").
        checkInTime(LocalTime.parse("11:00")).checkOutTime(LocalTime.parse("15:00")).build();
    Room room3 = Room.builder().id(103L).accommodation(accommodation).name("room").
        checkInTime(LocalTime.parse("11:00")).checkOutTime(LocalTime.parse("15:00")).build();

    List<Reservation> list = new ArrayList<>();

    Reservation r1 = Reservation.builder()
        .id(1L)
        .status(ReservationStatus.RESERVED)
        .user(user)
        .room(room1)
        .checkInDate(LocalDate.of(2025, 1, 1))
        .checkOutDate(LocalDate.of(2025, 1, 3))
        .peopleCount(2)
        .petCount(1)
        .totalPrice(30000L)
        .createdAt(LocalDateTime.now())
        .build();

    Reservation r2 = Reservation.builder()
        .id(2L)
        .status(ReservationStatus.RESERVED)
        .user(user)
        .room(room2)
        .checkInDate(LocalDate.of(2025, 1, 1))
        .checkOutDate(LocalDate.of(2025, 1, 3))
        .peopleCount(2)
        .petCount(1)
        .totalPrice(30000L)
        .createdAt(LocalDateTime.now())
        .build();

    Reservation r3 = Reservation.builder()
        .id(3L)
        .status(ReservationStatus.COMPLETED)
        .user(user)
        .room(room3)
        .checkInDate(LocalDate.of(2025, 1, 1))
        .checkOutDate(LocalDate.of(2025, 1, 3))
        .peopleCount(2)
        .petCount(1)
        .totalPrice(30000L)
        .createdAt(LocalDateTime.now())
        .build();

    Reservation r4 = Reservation.builder()
        .id(3L)
        .status(ReservationStatus.RESERVED)
        .user(user2)
        .room(room3)
        .checkInDate(LocalDate.of(2025, 1, 1))
        .checkOutDate(LocalDate.of(2025, 1, 3))
        .peopleCount(2)
        .petCount(1)
        .totalPrice(30000L)
        .createdAt(LocalDateTime.now())
        .build();

    list.add(r1);
    list.add(r2);
    list.add(r3);
    list.add(r4);

    when(reservationRepository.findByUserIdAndStatus(userId, cursorId, size + 1,
        String.valueOf(status)))
        .thenReturn(list.stream()
            .filter(reservation -> reservation.getStatus() == status &&
                reservation.getUser().getId().equals(userId))
            .collect(Collectors.toList()));

    when(roomRepository.findById(101L)).thenReturn(Optional.of(room1));
    when(roomRepository.findById(102L)).thenReturn(Optional.of(room2));

    when(accommodationRepository.findById(accommodationId)).thenReturn(Optional.of(accommodation));

    CustomReservationResponse<UserReservationResponse> response = reservationService.getUserReservations(
        userId, cursorId, size,
        status);

    assertEquals(2, response.getContent().size());
    assertFalse(response.isHasNext());
  }

  @Test
  @DisplayName("유저는 이용 전 상태인 예약을 취소할 수 있다.")
  void cancelReservation_success() {
    // given
    User user = User.builder().id(1L).build();
    Room room = Room.builder().id(1L).build();

    Reservation reservation = Reservation.builder()
        .id(1L)
        .status(ReservationStatus.RESERVED)
        .user(user)
        .room(room)
        .checkInDate(LocalDate.of(2025, 1, 1))
        .checkOutDate(LocalDate.of(2025, 1, 3))
        .peopleCount(2)
        .petCount(1)
        .totalPrice(30000L)
        .createdAt(LocalDateTime.now())
        .build();

    when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));

    // when
    reservationService.cancelReservation(user.getId(), reservation.getId());

    // then
    verify(reservationRepository, times(1)).findById(reservation.getId());
    assertEquals(ReservationStatus.CANCELED, reservation.getStatus());
  }

  @Test
  @DisplayName("예약 정보가 없을 경우, RESERVATION_NOT_FOUND 예외가 발생해야 한다.")
  void cancelReservation_reservation_not_found() {
    // given
    User user = User.builder().id(1L).build();
    Room room = Room.builder().id(1L).build();

    Reservation reservation = Reservation.builder()
        .id(1L)
        .status(ReservationStatus.RESERVED)
        .user(user)
        .room(room)
        .checkInDate(LocalDate.of(2025, 1, 1))
        .checkOutDate(LocalDate.of(2025, 1, 3))
        .peopleCount(2)
        .petCount(1)
        .totalPrice(30000L)
        .createdAt(LocalDateTime.now())
        .build();

    when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.empty());

    // when
    MeongnyangerangException e = assertThrows(MeongnyangerangException.class, () ->
        reservationService.cancelReservation(user.getId(), reservation.getId()));

    // then
    assertEquals(ErrorCode.RESERVATION_NOT_FOUND, e.getErrorCode());
  }

  @Test
  @DisplayName("예약 유저 ID와 로그인한 유저 ID가 같지 않은 경우, INVALID_AUTHORIZED 예외가 발생해야 한다.")
  void cancelReservation_invalid_authorized() {
    // given
    User user = User.builder().id(1L).build();
    Room room = Room.builder().id(1L).build();

    Reservation reservation = Reservation.builder()
        .id(1L)
        .status(ReservationStatus.RESERVED)
        .user(user)
        .room(room)
        .checkInDate(LocalDate.of(2025, 1, 1))
        .checkOutDate(LocalDate.of(2025, 1, 3))
        .peopleCount(2)
        .petCount(1)
        .totalPrice(30000L)
        .createdAt(LocalDateTime.now())
        .build();

    when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));

    // when
    MeongnyangerangException e = assertThrows(MeongnyangerangException.class, () ->
        reservationService.cancelReservation(100L, reservation.getId()));

    // then
    assertEquals(ErrorCode.INVALID_AUTHORIZED, e.getErrorCode());
  }

  @Test
  @DisplayName("이미 취소된 예약인 경우, RESERVATION_ALREADY_CANCELED 예외가 발생해야 한다.")
  void cancelReservation_reservation_already_canceled() {
    // given
    User user = User.builder().id(1L).build();
    Room room = Room.builder().id(1L).build();

    Reservation reservation = Reservation.builder()
        .id(1L)
        .status(ReservationStatus.CANCELED)
        .user(user)
        .room(room)
        .checkInDate(LocalDate.of(2025, 1, 1))
        .checkOutDate(LocalDate.of(2025, 1, 3))
        .peopleCount(2)
        .petCount(1)
        .totalPrice(30000L)
        .createdAt(LocalDateTime.now())
        .build();

    when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));

    // when
    MeongnyangerangException e = assertThrows(MeongnyangerangException.class, () ->
        reservationService.cancelReservation(user.getId(), reservation.getId()));

    // then
    assertEquals(ErrorCode.RESERVATION_ALREADY_CANCELED, e.getErrorCode());
  }

  @Test
  @DisplayName("로그인한 호스트가 자신이 등록한 숙소의 예약 내역을 상태별로 조회할 수 있다.")
  void getHostReservation_success() {
    Long cursorId = 0L;
    int size = 20;
    ReservationStatus status = ReservationStatus.COMPLETED;

    User user = User.builder().id(1L).build();
    User user2 = User.builder().id(2L).build();
    Host host = Host.builder().id(1L).build();
    Accommodation accommodation = Accommodation.builder().id(1L).host(host).build();
    Room room1 = Room.builder().id(101L).accommodation(accommodation).name("room").
        checkInTime(LocalTime.parse("11:00")).checkOutTime(LocalTime.parse("15:00")).build();
    Room room2 = Room.builder().id(102L).accommodation(accommodation).name("room").
        checkInTime(LocalTime.parse("11:00")).checkOutTime(LocalTime.parse("15:00")).build();
    Room room3 = Room.builder().id(103L).accommodation(accommodation).name("room").
        checkInTime(LocalTime.parse("11:00")).checkOutTime(LocalTime.parse("15:00")).build();

    List<Reservation> list = new ArrayList<>();

    Reservation r1 = Reservation.builder()
        .id(1L)
        .status(ReservationStatus.RESERVED)
        .user(user)
        .room(room1)
        .checkInDate(LocalDate.of(2025, 1, 1))
        .checkOutDate(LocalDate.of(2025, 1, 3))
        .peopleCount(2)
        .petCount(1)
        .hasVehicle(true)
        .totalPrice(30000L)
        .createdAt(LocalDateTime.now())
        .build();

    Reservation r2 = Reservation.builder()
        .id(2L)
        .status(ReservationStatus.COMPLETED)
        .user(user)
        .room(room2)
        .checkInDate(LocalDate.of(2025, 1, 1))
        .checkOutDate(LocalDate.of(2025, 1, 3))
        .peopleCount(2)
        .petCount(1)
        .totalPrice(30000L)
        .hasVehicle(false)
        .createdAt(LocalDateTime.now())
        .build();

    Reservation r3 = Reservation.builder()
        .id(3L)
        .status(ReservationStatus.COMPLETED)
        .user(user)
        .room(room3)
        .checkInDate(LocalDate.of(2025, 1, 1))
        .checkOutDate(LocalDate.of(2025, 1, 3))
        .peopleCount(2)
        .petCount(1)
        .totalPrice(30000L)
        .hasVehicle(true)
        .createdAt(LocalDateTime.now())
        .build();

    Reservation r4 = Reservation.builder()
        .id(3L)
        .status(ReservationStatus.COMPLETED)
        .user(user2)
        .room(room3)
        .checkInDate(LocalDate.of(2025, 1, 1))
        .checkOutDate(LocalDate.of(2025, 1, 3))
        .peopleCount(2)
        .petCount(1)
        .totalPrice(30000L)
        .hasVehicle(true)
        .createdAt(LocalDateTime.now())
        .build();

    list.add(r1);
    list.add(r2);
    list.add(r3);
    list.add(r4);

    when(reservationRepository.findByHostIdAndStatus(host.getId(), cursorId, size + 1,
        String.valueOf(status)))
        .thenReturn(list.stream()
            .filter(reservation -> reservation.getStatus() == status &&
                Objects.equals(reservation.getRoom().getAccommodation().getHost().getId(),
                    host.getId()))
            .collect(Collectors.toList()));

    CustomReservationResponse<HostReservationResponse> response = reservationService.getHostReservation(
        host.getId(), cursorId, size,
        status);

    assertEquals(3, response.getContent().size());
    assertFalse(response.isHasNext());
  }
}