package com.meongnyangerang.meongnyangerang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import com.meongnyangerang.meongnyangerang.domain.host.Host;
import com.meongnyangerang.meongnyangerang.domain.notification.NotificationType;
import com.meongnyangerang.meongnyangerang.domain.reservation.Reservation;
import com.meongnyangerang.meongnyangerang.domain.reservation.ReservationSlot;
import com.meongnyangerang.meongnyangerang.domain.reservation.ReservationStatus;
import com.meongnyangerang.meongnyangerang.domain.room.Room;
import com.meongnyangerang.meongnyangerang.domain.user.User;
import com.meongnyangerang.meongnyangerang.dto.HostReservationResponse;
import com.meongnyangerang.meongnyangerang.dto.ReservationRequest;
import com.meongnyangerang.meongnyangerang.dto.ReservationResponse;
import com.meongnyangerang.meongnyangerang.dto.UserReservationResponse;
import com.meongnyangerang.meongnyangerang.dto.chat.PageResponse;
import com.meongnyangerang.meongnyangerang.dto.portone.PaymentReservationRequest;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.ReservationRepository;
import com.meongnyangerang.meongnyangerang.repository.ReservationSlotRepository;
import com.meongnyangerang.meongnyangerang.repository.ReviewRepository;
import com.meongnyangerang.meongnyangerang.repository.UserRepository;
import com.meongnyangerang.meongnyangerang.repository.room.RoomRepository;
import com.meongnyangerang.meongnyangerang.service.notification.NotificationService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

  @Mock
  private ReservationRepository reservationRepository;

  @Mock
  private ReservationSlotRepository reservationSlotRepository;

  @Mock
  private ReviewRepository reviewRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private RoomRepository roomRepository;

  @Mock
  private NotificationService notificationService;

  @Mock
  private PortOneService portOneService;

  @InjectMocks
  private ReservationService reservationService;

  private static final String RESERVATION_CONFIRMED_CONTENT = "%s 숙소 예약이 확정되었습니다.";
  private static final String RESERVATION_REGISTERED_CONTENT = "%s 님이 예약하였습니다.";
  private static final String RESERVATION_CANCELED_SUCCESS_CONTENT =
      "%s 숙소 예약이 성공적으로 취소되었습니다.";
  private static final String RESERVATION_CANCELED_CONTENT = "%s님이 예약을 취소하였습니다.";

  @Test
  @DisplayName("사용자가 예약 등록 시, 예약 등록에 성공해야 한다.")
  void createReservation_success() {
    // given
    Long userId = 1L;
    Long roomId = 101L;
    final String TEST_ACCOMMODATION_NAME = "테스트 숙소 이름";
    LocalDate checkInDate = LocalDate.of(2025, 1, 1);
    LocalDate checkOutDate = LocalDate.of(2025, 1, 3);

    ReservationRequest request = ReservationRequest.builder()
        .roomId(roomId)
        .accommodationName(TEST_ACCOMMODATION_NAME)
        .checkInDate(checkInDate)
        .checkOutDate(checkOutDate)
        .peopleCount(2)
        .petCount(1)
        .reserverName("홍길동")
        .reserverPhoneNumber("01012345678")
        .hasVehicle(true)
        .totalPrice(100000L)
        .build();

    PaymentReservationRequest paymentRequest = new PaymentReservationRequest();
    ReflectionTestUtils.setField(paymentRequest, "impUid", "imp_1234567890");
    ReflectionTestUtils.setField(paymentRequest, "merchantUid", "merchant_9876543210");
    ReflectionTestUtils.setField(paymentRequest, "reservationRequest", request);

    User user = User.builder().id(userId).nickname("홍길동").build();
    Host host = Host.builder().id(1L).build();
    Accommodation accommodation = Accommodation.builder().id(1L).host(host)
        .name(TEST_ACCOMMODATION_NAME).build();
    Room room = Room.builder().id(roomId).accommodation(accommodation).build();

    ReservationSlot slot1 = new ReservationSlot(room, checkInDate, false);
    slot1.setHold(true);
    slot1.setExpiredAt(LocalDateTime.now().plusMinutes(5));

    ReservationSlot slot2 = new ReservationSlot(room, checkInDate.plusDays(1), false);
    slot2.setHold(true);
    slot2.setExpiredAt(LocalDateTime.now().plusMinutes(5));

    Reservation reservation = Reservation.builder()
        .id(1L)
        .user(user)
        .room(room)
        .accommodationName(TEST_ACCOMMODATION_NAME)
        .checkInDate(checkInDate)
        .checkOutDate(checkOutDate)
        .peopleCount(2)
        .petCount(1)
        .totalPrice(100000L)
        .impUid("imp_1234567890")
        .merchantUid("merchant_9876543210")
        .build();
    
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
    when(reservationSlotRepository.findByRoomIdAndReservedDate(roomId, checkInDate)).thenReturn(Optional.of(slot1));
    when(reservationSlotRepository.findByRoomIdAndReservedDate(roomId, checkInDate.plusDays(1))).thenReturn(Optional.of(slot2));
    when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);
    doNothing().when(portOneService).verifyPayment("imp_1234567890", 100000L);

    // when
    ReservationResponse response = reservationService.createReservationAfterPayment(userId, paymentRequest);

    // then
    assertNotNull(response.getOrderNumber());
    assertEquals("merchant_9876543210", response.getOrderNumber());

    verify(reservationSlotRepository, times(1)).saveAll(List.of(slot1, slot2));
    verify(reservationRepository, times(1)).save(any(Reservation.class));

    verify(notificationService).sendReservationNotification(
        reservation.getId(),
        user,
        host,
        String.format(RESERVATION_CONFIRMED_CONTENT, TEST_ACCOMMODATION_NAME),
        String.format(RESERVATION_REGISTERED_CONTENT, "홍길동"),
        NotificationType.RESERVATION_CONFIRMED
    );
  }

  @Test
  @DisplayName("사용자가 존재하지 않을 경우, USER_NOT_FOUND 예외가 발생해야 한다.")
  void createReservation_user_not_found() {
    // given
    Long userId = 1L;
    Long roomId = 101L;
    LocalDate checkInDate = LocalDate.of(2025, 1, 1);
    LocalDate checkOutDate = LocalDate.of(2025, 1, 3);
    ReservationRequest request = ReservationRequest.builder().roomId(roomId)
        .checkInDate(checkInDate).checkOutDate(checkOutDate).build();

    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    // when & then
    MeongnyangerangException e = assertThrows(MeongnyangerangException.class, () -> {
      reservationService.validateAndHoldSlots(userId, request);
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
    ReservationRequest request = ReservationRequest.builder().roomId(roomId)
        .checkInDate(checkInDate).checkOutDate(checkOutDate).build();

    User user = User.builder().id(userId).build();

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

    // when & then
    MeongnyangerangException e = assertThrows(MeongnyangerangException.class, () -> {
      reservationService.validateAndHoldSlots(userId, request);
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
    ReservationRequest request = ReservationRequest.builder().roomId(roomId)
        .checkInDate(checkInDate).checkOutDate(checkOutDate).build();

    User user = User.builder().id(userId).build();

    Room room = Room.builder().id(roomId).build();

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
    when(reservationSlotRepository.existsByRoomIdAndReservedDateBetweenAndIsReserved(roomId,
        checkInDate, checkOutDate.minusDays(1), true)).thenReturn(true);

    // when & then
    MeongnyangerangException e = assertThrows(MeongnyangerangException.class, () -> {
      reservationService.validateAndHoldSlots(userId, request);
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
    ReservationRequest request = ReservationRequest.builder().roomId(roomId)
        .checkInDate(checkInDate).checkOutDate(checkOutDate).build();

    User user = User.builder().id(userId).build();

    Room room = Room.builder().id(roomId).build();

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
    when(reservationSlotRepository.existsByRoomIdAndReservedDateBetweenAndIsReserved(
        roomId, checkInDate, checkOutDate.minusDays(1), true)).thenReturn(true);

    // when & then
    MeongnyangerangException e = assertThrows(MeongnyangerangException.class, () -> {
      reservationService.validateAndHoldSlots(userId, request);
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
        .accommodationName("테스트 숙소")
        .checkInDate(checkInDate)
        .checkOutDate(checkOutDate)
        .peopleCount(2)
        .petCount(1)
        .reserverName("홍길동")
        .reserverPhoneNumber("01012345678")
        .hasVehicle(true)
        .totalPrice(100000L)
        .build();


    PaymentReservationRequest paymentRequest = new PaymentReservationRequest();
    ReflectionTestUtils.setField(paymentRequest, "impUid", "imp_test_1234");
    ReflectionTestUtils.setField(paymentRequest, "merchantUid", "order_test_1234");
    ReflectionTestUtils.setField(paymentRequest, "reservationRequest", request);

    User user = User.builder().id(userId).build();

    Room room = Room.builder().id(roomId).build();

    // 예약 슬롯 객체 생성 -> 이 객실만 조회되게 해서 1개인 걸 가정해서 충돌 발생시키기
    ReservationSlot reservationSlot = new ReservationSlot(room, checkInDate, false);

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
    when(reservationSlotRepository.existsByRoomIdAndReservedDateBetweenAndIsReserved(roomId,
        checkInDate, checkOutDate.minusDays(1), true)).thenReturn(false);
    when(reservationSlotRepository.findByRoomIdAndReservedDate(roomId, checkInDate)).thenReturn(
        Optional.of(reservationSlot));

    doNothing().when(portOneService).verifyPayment("imp_test_1234", 100000L);
    ExecutorService executorService = Executors.newFixedThreadPool(3);  // 3개의 스레드 실행
    CountDownLatch latch = new CountDownLatch(3); // 3개의 예약이 끝날 때까지 대기
    AtomicInteger successCount = new AtomicInteger(); // 성공한 예약의 개수 세기
    AtomicInteger failCount = new AtomicInteger();  // 실패한 예약의 개수 세기

    // 3개의 스레드 각각 예약 시도
    for (int i = 0; i < 3; i++) {
      executorService.submit(() -> {  // 스레드 제출
        try {
          // 예약 시도 -> 성공하면 성공 예약 개수 증가
          reservationService.createReservationAfterPayment(userId, paymentRequest);
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
  @DisplayName("checkRoomHoldStatus()에서 hold=true인 슬롯이 존재할 경우 예외 발생")
  void validateAndHoldSlots_shouldThrowWhenRoomHoldExists() {
    // given
    Long userId = 1L;
    Long roomId = 101L;
    LocalDate checkIn = LocalDate.of(2025, 1, 1);
    LocalDate checkOut = LocalDate.of(2025, 1, 3);

    ReservationRequest request = ReservationRequest.builder()
        .roomId(roomId)
        .checkInDate(checkIn)
        .checkOutDate(checkOut)
        .build();

    User user = User.builder().id(userId).build();
    Room room = Room.builder().id(roomId).build();

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
    when(reservationSlotRepository.existsByRoomIdAndReservedDateBetweenAndIsReserved(
        roomId, checkIn, checkOut.minusDays(1), true)).thenReturn(false);
    when(reservationSlotRepository.existsByRoomIdAndReservedDateBetweenAndHoldTrue(
        roomId, checkIn, checkOut.minusDays(1))).thenReturn(true);

    // when & then
    MeongnyangerangException exception = assertThrows(MeongnyangerangException.class, () ->
        reservationService.validateAndHoldSlots(userId, request));

    assertEquals(ErrorCode.ROOM_TEMPORARILY_HELD, exception.getErrorCode());
  }

  @Test
  @DisplayName("해당 유저가 예약한 내역만 볼 수 있고 상태에 따라 조회를 할 수 있다.")
  void getUserReservation_success() {
    Long userId = 1L;
    int page = 0;
    int size = 20;
    Pageable pageable = PageRequest.of(page, size);
    ReservationStatus status = ReservationStatus.RESERVED;

    User user = User.builder().id(1L).build();
    User user2 = User.builder().id(2L).build();
    Host host = Host.builder().id(1L).build();
    Accommodation accommodation = Accommodation.builder().id(1L).host(host).build();
    Room room1 = Room.builder().id(101L).accommodation(accommodation).name("room")
        .checkInTime(LocalTime.parse("11:00")).checkOutTime(LocalTime.parse("15:00")).build();
    Room room2 = Room.builder().id(102L).accommodation(accommodation).name("room")
        .checkInTime(LocalTime.parse("11:00")).checkOutTime(LocalTime.parse("15:00")).build();
    Room room3 = Room.builder().id(103L).accommodation(accommodation).name("room")
        .checkInTime(LocalTime.parse("11:00")).checkOutTime(LocalTime.parse("15:00")).build();

    List<Reservation> list = List.of(
        Reservation.builder().id(1L).status(ReservationStatus.RESERVED).user(user).room(room1)
            .checkInDate(LocalDate.of(2025, 1, 1)).checkOutDate(LocalDate.of(2025, 1, 3))
            .peopleCount(2).petCount(1).totalPrice(30000L).createdAt(LocalDateTime.now()).build(),

        Reservation.builder().id(2L).status(ReservationStatus.RESERVED).user(user).room(room2)
            .checkInDate(LocalDate.of(2025, 1, 1)).checkOutDate(LocalDate.of(2025, 1, 3))
            .peopleCount(2).petCount(1).totalPrice(30000L).createdAt(LocalDateTime.now()).build(),

        Reservation.builder().id(3L).status(ReservationStatus.COMPLETED).user(user).room(room3)
            .checkInDate(LocalDate.of(2025, 1, 1)).checkOutDate(LocalDate.of(2025, 1, 3))
            .peopleCount(2).petCount(1).totalPrice(30000L).createdAt(LocalDateTime.now()).build(),

        Reservation.builder().id(4L).status(ReservationStatus.RESERVED).user(user2).room(room3)
            .checkInDate(LocalDate.of(2025, 1, 1)).checkOutDate(LocalDate.of(2025, 1, 3))
            .peopleCount(2).petCount(1).totalPrice(30000L).createdAt(LocalDateTime.now()).build());

    List<Reservation> filteredList = list.stream().filter(
        reservation -> reservation.getStatus() == status && reservation.getUser().getId()
            .equals(userId)).toList();

    Page<Reservation> reservationPage = new PageImpl<>(filteredList, pageable, filteredList.size());

    when(reservationRepository.findByUserIdAndStatus(userId, status, pageable)).thenReturn(
        reservationPage);

    when(reviewRepository.existsByReservationId(1L)).thenReturn(false);
    when(reviewRepository.existsByReservationId(2L)).thenReturn(true);

    PageResponse<UserReservationResponse> response = reservationService.getUserReservations(userId,
        pageable, status);

    assertEquals(2, response.content().size());
    assertEquals(accommodation.getId(), response.content().get(0).getAccommodationId());
    assertEquals(accommodation.getId(), response.content().get(1).getAccommodationId());
    assertFalse(response.content().get(0).isReviewWritten());
    assertTrue(response.content().get(1).isReviewWritten());
  }

  @Test
  @DisplayName("findAndValidateHoldSlots()에서 hold가 false인 경우 예외 발생")
  void createReservationAfterPayment_shouldThrowWhenHoldIsFalse() {
    // given
    Long userId = 1L;
    Long roomId = 101L;
    LocalDate checkIn = LocalDate.of(2025, 1, 1);
    LocalDate checkOut = LocalDate.of(2025, 1, 2);

    User user = User.builder().id(userId).build();
    Room room = Room.builder().id(roomId).build();

    ReservationRequest req = ReservationRequest.builder()
        .roomId(roomId)
        .checkInDate(checkIn)
        .checkOutDate(checkOut)
        .build();

    ReservationSlot slot = new ReservationSlot(room, checkIn, false);
    slot.setHold(false);
    slot.setExpiredAt(LocalDateTime.now().plusMinutes(5));

    PaymentReservationRequest paymentReq = new PaymentReservationRequest();
    ReflectionTestUtils.setField(paymentReq, "impUid", "imp_test");
    ReflectionTestUtils.setField(paymentReq, "merchantUid", "merchant_test");
    ReflectionTestUtils.setField(paymentReq, "reservationRequest", req);

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
    when(reservationSlotRepository.findByRoomIdAndReservedDate(roomId, checkIn)).thenReturn(Optional.of(slot));

    doNothing().when(portOneService).verifyPayment("imp_test", null);

    // when & then
    MeongnyangerangException e = assertThrows(MeongnyangerangException.class, () ->
        reservationService.createReservationAfterPayment(userId, paymentReq));

    assertEquals(ErrorCode.RESERVATION_SLOT_EXPIRED, e.getErrorCode());
  }

  @Test
  @DisplayName("findAndValidateHoldSlots()에서 expiredAt이 null인 경우 예외 발생")
  void createReservationAfterPayment_shouldThrowWhenExpiredAtIsNull() {
    // given
    Long userId = 1L;
    Long roomId = 101L;
    LocalDate checkIn = LocalDate.of(2025, 1, 1);
    LocalDate checkOut = LocalDate.of(2025, 1, 2);

    User user = User.builder().id(userId).build();
    Room room = Room.builder().id(roomId).build();

    ReservationRequest req = ReservationRequest.builder()
        .roomId(roomId)
        .checkInDate(checkIn)
        .checkOutDate(checkOut)
        .build();

    ReservationSlot slot = new ReservationSlot(room, checkIn, false);
    slot.setHold(true);
    slot.setExpiredAt(null);

    PaymentReservationRequest paymentReq = new PaymentReservationRequest();
    ReflectionTestUtils.setField(paymentReq, "impUid", "imp_test");
    ReflectionTestUtils.setField(paymentReq, "merchantUid", "merchant_test");
    ReflectionTestUtils.setField(paymentReq, "reservationRequest", req);

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
    when(reservationSlotRepository.findByRoomIdAndReservedDate(roomId, checkIn)).thenReturn(Optional.of(slot));

    doNothing().when(portOneService).verifyPayment("imp_test", null);

    // when & then
    MeongnyangerangException e = assertThrows(MeongnyangerangException.class, () ->
        reservationService.createReservationAfterPayment(userId, paymentReq));

    assertEquals(ErrorCode.RESERVATION_SLOT_EXPIRED, e.getErrorCode());
  }

  @Test
  @DisplayName("findAndValidateHoldSlots()에서 expiredAt이 현재보다 이전인 경우 예외 발생")
  void createReservationAfterPayment_shouldThrowWhenExpiredAtIsPast() {
    // given
    Long userId = 1L;
    Long roomId = 101L;
    LocalDate checkIn = LocalDate.of(2025, 1, 1);
    LocalDate checkOut = LocalDate.of(2025, 1, 2);

    User user = User.builder().id(userId).build();
    Room room = Room.builder().id(roomId).build();

    ReservationRequest req = ReservationRequest.builder()
        .roomId(roomId)
        .checkInDate(checkIn)
        .checkOutDate(checkOut)
        .build();

    ReservationSlot slot = new ReservationSlot(room, checkIn, false);
    slot.setHold(true);
    slot.setExpiredAt(LocalDateTime.now().minusMinutes(1)); // 과거

    PaymentReservationRequest paymentReq = new PaymentReservationRequest();
    ReflectionTestUtils.setField(paymentReq, "impUid", "imp_test");
    ReflectionTestUtils.setField(paymentReq, "merchantUid", "merchant_test");
    ReflectionTestUtils.setField(paymentReq, "reservationRequest", req);

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
    when(reservationSlotRepository.findByRoomIdAndReservedDate(roomId, checkIn)).thenReturn(Optional.of(slot));

    doNothing().when(portOneService).verifyPayment("imp_test", null);

    // when & then
    MeongnyangerangException e = assertThrows(MeongnyangerangException.class, () ->
        reservationService.createReservationAfterPayment(userId, paymentReq));

    assertEquals(ErrorCode.RESERVATION_SLOT_EXPIRED, e.getErrorCode());
  }

  @Test
  @DisplayName("유저는 이용 전 상태인 예약을 취소할 수 있다.")
  void cancelReservation_success() {
    // given
    User user = User.builder().id(1L).build();
    Host host = Host.builder()
        .id(1L)
        .build();
    Accommodation accommodation = Accommodation.builder()
        .id(1L)
        .host(host)
        .build();
    Room room = Room.builder()
        .id(1L)
        .accommodation(accommodation)
        .build();

    Reservation reservation = Reservation.builder().id(1L).status(ReservationStatus.RESERVED)
        .user(user).room(room).checkInDate(LocalDate.of(2025, 1, 1))
        .checkOutDate(LocalDate.of(2025, 1, 3)).peopleCount(2).petCount(1).totalPrice(30000L)
        .createdAt(LocalDateTime.now()).build();

    ReservationSlot slot1 = new ReservationSlot(room, reservation.getCheckInDate(), true);
    ReservationSlot slot2 = new ReservationSlot(room, reservation.getCheckInDate().plusDays(1),
        true);

    List<ReservationSlot> slots = new ArrayList<>();
    slots.add(slot1);
    slots.add(slot2);

    when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));
    when(reservationSlotRepository.findByRoomAndReservedDateBetween(reservation.getRoom(),
        reservation.getCheckInDate(), reservation.getCheckOutDate().minusDays(1))).thenReturn(
        slots);

    // when
    reservationService.cancelReservation(user.getId(), reservation.getId());

    // then
    verify(reservationRepository, times(1)).findById(reservation.getId());
    verify(reservationSlotRepository, times(1)).findByRoomAndReservedDateBetween(
        reservation.getRoom(), reservation.getCheckInDate(),
        reservation.getCheckOutDate().minusDays(1));
    assertEquals(ReservationStatus.CANCELED, reservation.getStatus());

    String reservationConfirmedContent = String.format(
        RESERVATION_CANCELED_SUCCESS_CONTENT, reservation.getAccommodationName());

    String reservationRegisteredContent = String.format(
        RESERVATION_CANCELED_CONTENT, reservation.getUser().getNickname());

    verify(notificationService).sendReservationNotification(
        reservation.getId(),
        user,
        room.getAccommodation().getHost(),
        reservationConfirmedContent,
        reservationRegisteredContent,
        NotificationType.RESERVATION_CANCELED
    );
  }

  @Test
  @DisplayName("예약 정보가 없을 경우, RESERVATION_NOT_FOUND 예외가 발생해야 한다.")
  void cancelReservation_reservation_not_found() {
    // given
    User user = User.builder().id(1L).build();
    Room room = Room.builder().id(1L).build();

    Reservation reservation = Reservation.builder().id(1L).status(ReservationStatus.RESERVED)
        .user(user).room(room).checkInDate(LocalDate.of(2025, 1, 1))
        .checkOutDate(LocalDate.of(2025, 1, 3)).peopleCount(2).petCount(1).totalPrice(30000L)
        .createdAt(LocalDateTime.now()).build();

    when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.empty());

    // when
    MeongnyangerangException e = assertThrows(MeongnyangerangException.class,
        () -> reservationService.cancelReservation(user.getId(), reservation.getId()));

    // then
    assertEquals(ErrorCode.RESERVATION_NOT_FOUND, e.getErrorCode());
  }

  @Test
  @DisplayName("예약 유저 ID와 로그인한 유저 ID가 같지 않은 경우, INVALID_AUTHORIZED 예외가 발생해야 한다.")
  void cancelReservation_invalid_authorized() {
    // given
    User user = User.builder().id(1L).build();
    Room room = Room.builder().id(1L).build();

    Reservation reservation = Reservation.builder().id(1L).status(ReservationStatus.RESERVED)
        .user(user).room(room).checkInDate(LocalDate.of(2025, 1, 1))
        .checkOutDate(LocalDate.of(2025, 1, 3)).peopleCount(2).petCount(1).totalPrice(30000L)
        .createdAt(LocalDateTime.now()).build();

    when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));

    // when
    MeongnyangerangException e = assertThrows(MeongnyangerangException.class,
        () -> reservationService.cancelReservation(100L, reservation.getId()));

    // then
    assertEquals(ErrorCode.INVALID_AUTHORIZED, e.getErrorCode());
  }

  @Test
  @DisplayName("이미 취소된 예약인 경우, RESERVATION_ALREADY_CANCELED 예외가 발생해야 한다.")
  void cancelReservation_reservation_already_canceled() {
    // given
    User user = User.builder().id(1L).build();
    Room room = Room.builder().id(1L).build();

    Reservation reservation = Reservation.builder().id(1L).status(ReservationStatus.CANCELED)
        .user(user).room(room).checkInDate(LocalDate.of(2025, 1, 1))
        .checkOutDate(LocalDate.of(2025, 1, 3)).peopleCount(2).petCount(1).totalPrice(30000L)
        .createdAt(LocalDateTime.now()).build();

    when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));

    // when
    MeongnyangerangException e = assertThrows(MeongnyangerangException.class,
        () -> reservationService.cancelReservation(user.getId(), reservation.getId()));

    // then
    assertEquals(ErrorCode.RESERVATION_ALREADY_CANCELED, e.getErrorCode());
  }

  @Test
  @DisplayName("로그인한 호스트가 자신이 등록한 숙소의 예약 내역을 상태별로 조회할 수 있다.")
  void getHostReservation_success() {
    Long hostId = 1L;
    int page = 0;
    int size = 20;
    Pageable pageable = PageRequest.of(page, size);
    ReservationStatus status = ReservationStatus.RESERVED;

    User user = User.builder().id(1L).build();
    User user2 = User.builder().id(2L).build();
    Host host = Host.builder().id(1L).build();
    Accommodation accommodation = Accommodation.builder().id(1L).host(host).build();
    Room room1 = Room.builder().id(101L).accommodation(accommodation).name("room")
        .checkInTime(LocalTime.parse("11:00")).checkOutTime(LocalTime.parse("15:00")).build();
    Room room2 = Room.builder().id(102L).accommodation(accommodation).name("room")
        .checkInTime(LocalTime.parse("11:00")).checkOutTime(LocalTime.parse("15:00")).build();
    Room room3 = Room.builder().id(103L).accommodation(accommodation).name("room")
        .checkInTime(LocalTime.parse("11:00")).checkOutTime(LocalTime.parse("15:00")).build();

    List<Reservation> list = List.of(
        Reservation.builder().id(1L).status(ReservationStatus.RESERVED).user(user).room(room1)
            .checkInDate(LocalDate.of(2025, 1, 1)).checkOutDate(LocalDate.of(2025, 1, 3))
            .peopleCount(2).petCount(1).totalPrice(30000L).createdAt(LocalDateTime.now())
            .hasVehicle(false).build(),

        Reservation.builder().id(2L).status(ReservationStatus.RESERVED).user(user).room(room2)
            .checkInDate(LocalDate.of(2025, 1, 1)).checkOutDate(LocalDate.of(2025, 1, 3))
            .peopleCount(2).petCount(1).totalPrice(30000L).createdAt(LocalDateTime.now())
            .hasVehicle(true).build(),

        Reservation.builder().id(3L).status(ReservationStatus.COMPLETED).user(user).room(room3)
            .checkInDate(LocalDate.of(2025, 1, 1)).checkOutDate(LocalDate.of(2025, 1, 3))
            .peopleCount(2).petCount(1).totalPrice(30000L).createdAt(LocalDateTime.now())
            .hasVehicle(true).build(),

        Reservation.builder().id(4L).status(ReservationStatus.RESERVED).user(user2).room(room3)
            .checkInDate(LocalDate.of(2025, 1, 1)).checkOutDate(LocalDate.of(2025, 1, 3))
            .peopleCount(2).petCount(1).totalPrice(30000L).createdAt(LocalDateTime.now())
            .hasVehicle(false).build());

    List<Reservation> filteredList = list.stream().filter(
        reservation -> reservation.getStatus() == status && reservation.getRoom().getAccommodation()
            .getHost().getId().equals(hostId)).toList();

    Page<Reservation> reservationPage = new PageImpl<>(filteredList, pageable, filteredList.size());

    when(reservationRepository.findByHostIdAndStatus(hostId, status, pageable)).thenReturn(
        reservationPage);

    PageResponse<HostReservationResponse> response = reservationService.getHostReservation(hostId,
        pageable, status);

    assertEquals(3, response.content().size());
    assertEquals(false, response.content().get(0).isHasVehicle());
    assertEquals(true, response.content().get(1).isHasVehicle());
    assertEquals(false, response.content().get(2).isHasVehicle());
  }
}