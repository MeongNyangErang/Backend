package com.meongnyangerang.meongnyangerang.service;

import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.*;

import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
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
import jakarta.persistence.OptimisticLockException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 예약 관련 비즈니스 로직을 처리하는 서비스
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReservationService {

  private final ReservationRepository reservationRepository;
  private final ReservationSlotRepository reservationSlotRepository;
  private final UserRepository userRepository;
  private final RoomRepository roomRepository;
  private final ReviewRepository reviewRepository;
  private final NotificationService notificationService;
  private final PortOneService portOneService;

  private static final String RESERVATION_CONFIRMED_CONTENT = "%s 숙소 예약이 확정되었습니다.";
  private static final String RESERVATION_REGISTERED_CONTENT = "%s 님이 예약하였습니다.";
  private static final String RESERVATION_CANCELED_SUCCESS_CONTENT =
      "%s 숙소 예약이 성공적으로 취소되었습니다.";
  private static final String RESERVATION_CANCELED_CONTENT = "%s님이 예약을 취소하였습니다.";

  /**
   * 사용자, 객실, 예약 슬롯 상태를 검증하고 예약 슬롯을 임시 선점(hold = true, expiredAt = +5분)합니다.
   */
  @Transactional
  public void validateAndHoldSlots(Long userId, ReservationRequest request) {
    validateUser(userId);
    Room room = validateRoom(request.getRoomId());
    checkRoomAvailability(room, request.getCheckInDate(), request.getCheckOutDate()); // 확정된 예약 확인
    checkRoomHoldStatus(room, request.getCheckInDate(), request.getCheckOutDate()); // 다른 사용자 결제 중 hold 확인

    // 예약 슬롯을 임시 선점 (hold = true, expiredAt = now + 5분)
    holdReservationSlots(room, request.getCheckInDate(), request.getCheckOutDate());
  }

  @Transactional
  public ReservationResponse createReservationAfterPayment(Long userId, PaymentReservationRequest request) {
    // 결제 검증
    ReservationRequest reservationRequest = request.getReservationRequest();
    portOneService.verifyPayment(request.getImpUid(), reservationRequest.getTotalPrice());

    // 예약 처리
    User user = validateUser(userId);
    Room room = validateRoom(reservationRequest.getRoomId());

    // hold 상태인 슬롯들을 조회하고 유효성 검증
    List<ReservationSlot> slots = findAndValidateHoldSlots(room,
        reservationRequest.getCheckInDate(), reservationRequest.getCheckOutDate());
    // 유효성 검증을 마친 예약 슬롯 예약 확정 처리
    confirmReservationSlots(slots);

    Reservation savedReservation = saveReservation(user, room, reservationRequest,
        request.getImpUid(), request.getMerchantUid());
    sendNotificationWhenReservationRegistered(savedReservation);

    return new ReservationResponse(savedReservation.getMerchantUid());
  }

  // 사용자가 예약 상태(RESERVED, COMPLETED, CANCELED)에 따라 예약 목록을 조회합니다.
  public PageResponse<UserReservationResponse> getUserReservations(Long userId, Pageable pageable,
      ReservationStatus status) {
    // 해당 유저의 예약 내역만 조회
    Page<Reservation> reservationList = reservationRepository.findByUserIdAndStatus(userId, status,
        pageable);

    // 예약 정보 -> DTO 변환
    Page<UserReservationResponse> responsePage = reservationList.map(
        this::mapToUserReservationResponse);

    return PageResponse.from(responsePage);
  }

  @Transactional
  public void cancelReservation(Long userId, Long reservationId) {
    // 예약 정보 가져오기
    Reservation reservation = reservationRepository.findById(reservationId)
        .orElseThrow(() -> new MeongnyangerangException(RESERVATION_NOT_FOUND));

    // 사용자가 예약한 내역인지 확인
    if (!reservation.getUser().getId().equals(userId)) {
      throw new MeongnyangerangException(INVALID_AUTHORIZED);
    }

    // 이미 취소된 예약인지 확인
    if (reservation.getStatus() == ReservationStatus.CANCELED) {
      throw new MeongnyangerangException(RESERVATION_ALREADY_CANCELED);
    }

    updateReservationSlot(reservation);

    // 예약 상태 변경
    reservation.setStatus(ReservationStatus.CANCELED);
    sendNotificationWhenReservationCanceled(reservation); // 사용자와 호스트에게 알림 발송
  }

  public PageResponse<HostReservationResponse> getHostReservation(Long hostId, Pageable pageable,
      ReservationStatus status) {
    // 자신의 숙소 예약 내역만 조회
    Page<Reservation> reservationList = reservationRepository.findByHostIdAndStatus(hostId, status,
        pageable);

    // 예약 정보 -> DTO 변환
    Page<HostReservationResponse> responsePage = reservationList.map(
        this::mapToHostReservationResponse);

    return PageResponse.from(responsePage);
  }

  /**
   * 사용자 ID에 해당하는 사용자가 존재하는지 확인합니다. 존재하지 않으면 예외를 발생시킵니다.
   *
   * @param userId 사용자 ID
   * @return 검증된 사용자 객체
   * @throws MeongnyangerangException 사용자 정보가 없으면 예외 발생
   */
  private User validateUser(Long userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new MeongnyangerangException(USER_NOT_FOUND));
  }

  /**
   * 객실 ID에 해당하는 객실이 존재하는지 확인합니다. 존재하지 않으면 예외를 발생시킵니다.
   *
   * @param roomId 객실 ID
   * @return 검증된 객실 객체
   * @throws MeongnyangerangException 객실 정보가 없으면 예외 발생
   */
  private Room validateRoom(Long roomId) {
    return roomRepository.findById(roomId)
        .orElseThrow(() -> new MeongnyangerangException(ROOM_NOT_FOUND));
  }

  /**
   * 주어진 체크인 날짜와 체크아웃 날짜 범위 내에 해당 객실이 이미 예약된 날짜가 있는지 확인합니다. 예약된 날짜가 있으면 예외를 발생시킵니다.
   *
   * @param room         예약하려는 객실
   * @param checkInDate  체크인 날짜
   * @param checkOutDate 체크아웃 날짜
   * @throws MeongnyangerangException 객실이 이미 예약된 경우 예외 발생
   */
  private void checkRoomAvailability(Room room, LocalDate checkInDate, LocalDate checkOutDate) {
    boolean isRoomBooked = reservationSlotRepository
        .existsByRoomIdAndReservedDateBetweenAndIsReserved(
            room.getId(), checkInDate, checkOutDate.minusDays(1), true);

    if (isRoomBooked) {
      throw new MeongnyangerangException(ROOM_ALREADY_RESERVED);
    }
  }

  private void checkRoomHoldStatus(Room room, LocalDate checkInDate, LocalDate checkOutDate) {
    boolean isHold = reservationSlotRepository.existsByRoomIdAndReservedDateBetweenAndHoldTrue(
        room.getId(), checkInDate, checkOutDate.minusDays(1));
    if (isHold) {
      throw new MeongnyangerangException(ROOM_TEMPORARILY_HELD);
    }
  }

  /**
   * 예약 슬롯을 임시 선점합니다 (hold = true, expiredAt = now + 5분)
   */
  private void holdReservationSlots(Room room, LocalDate checkIn, LocalDate checkOut) {
    List<ReservationSlot> slots = new ArrayList<>();

    for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
      LocalDate finalDate = date;

      ReservationSlot slot = reservationSlotRepository
          .findByRoomIdAndReservedDate(room.getId(), date)
          .orElseGet(() -> new ReservationSlot(room, finalDate, false));

      slot.setHold(true);
      slot.setExpiredAt(LocalDateTime.now().plusMinutes(5));
      slots.add(slot);
    }
    try {
      reservationSlotRepository.saveAll(slots);
    } catch (OptimisticLockException e) {
      throw new MeongnyangerangException(ErrorCode.ROOM_ALREADY_RESERVED);
    }
  }

  /**
   * hold 상태인 슬롯들을 조회하고 유효성 검증
   */
  private List<ReservationSlot> findAndValidateHoldSlots(Room room, LocalDate checkIn, LocalDate checkOut) {
    LocalDateTime now = LocalDateTime.now();
    List<ReservationSlot> slots = new ArrayList<>();
    for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
      ReservationSlot slot = reservationSlotRepository
          .findByRoomIdAndReservedDate(room.getId(), date)
          .orElseThrow(() -> new MeongnyangerangException(RESERVATION_NOT_FOUND));

      if (!Boolean.TRUE.equals(slot.getHold()) || slot.getExpiredAt() == null || slot.getExpiredAt().isBefore(now)) {
        throw new MeongnyangerangException(RESERVATION_SLOT_EXPIRED);
      }

      slots.add(slot);
    }
    return slots;
  }

  /**
   * 예약 슬롯 확정 처리: hold → reserved (IsReserved = true, Hold = false, ExpiredAt = null)
   */
  private void confirmReservationSlots(List<ReservationSlot> slots) {
    for (ReservationSlot slot : slots) {
      slot.setIsReserved(true);
      slot.setHold(false);
      slot.setExpiredAt(null);
    }

    reservationSlotRepository.saveAll(slots);
  }

  /**
   * 사용자와 객실 정보 및 예약 요청 정보를 바탕으로 예약 정보를 DB에 저장합니다.
   *
   * @param user    예약한 사용자
   * @param room    예약된 객실
   * @param request 예약 요청 정보
   */
  private Reservation saveReservation(User user, Room room, ReservationRequest request,
      String impUid, String merchantUid) {
    Reservation reservation = request.toEntity(user, room);
    reservation.setImpUid(impUid);
    reservation.setMerchantUid(merchantUid);
    return reservationRepository.save(reservation);
  }

  private void updateReservationSlot(Reservation reservation) {
    List<ReservationSlot> slots = reservationSlotRepository.findByRoomAndReservedDateBetween(
        reservation.getRoom(), reservation.getCheckInDate(),
        reservation.getCheckOutDate().minusDays(1));

    for (ReservationSlot slot : slots) {
      slot.setIsReserved(false);
    }
  }

  private void sendNotificationWhenReservationRegistered(Reservation reservation) {
    String reservationConfirmedContent = String.format(
        RESERVATION_CONFIRMED_CONTENT, reservation.getAccommodationName());

    String reservationRegisteredContent = String.format(
        RESERVATION_REGISTERED_CONTENT, reservation.getUser().getNickname());

    notificationService.sendReservationNotification(
        reservation.getId(),
        reservation.getUser(),
        reservation.getRoom().getAccommodation().getHost(),
        reservationConfirmedContent,
        reservationRegisteredContent,
        NotificationType.RESERVATION_CONFIRMED
    );
  }

  private void sendNotificationWhenReservationCanceled(Reservation reservation) {
    String reservationConfirmedContent = String.format(
        RESERVATION_CANCELED_SUCCESS_CONTENT, reservation.getAccommodationName());

    String reservationRegisteredContent = String.format(
        RESERVATION_CANCELED_CONTENT, reservation.getUser().getNickname());

    notificationService.sendReservationNotification(
        reservation.getId(),
        reservation.getUser(),
        reservation.getRoom().getAccommodation().getHost(),
        reservationConfirmedContent,
        reservationRegisteredContent,
        NotificationType.RESERVATION_CANCELED
    );
  }

  private UserReservationResponse mapToUserReservationResponse(Reservation reservation) {
    boolean reviewWritten = reviewRepository.existsByReservationId(reservation.getId());
    Room room = reservation.getRoom();
    Accommodation accommodation = room.getAccommodation();

    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    return UserReservationResponse.builder()
        .reservationId(reservation.getId())
        .reservationDate(reservation.getCreatedAt().format(dateFormatter))
        .accommodationId(accommodation.getId())
        .accommodationName(accommodation.getName())
        .roomName(room.getName())
        .checkInDate(reservation.getCheckInDate().format(dateFormatter))
        .checkOutDate(reservation.getCheckOutDate().format(dateFormatter))
        .checkInTime(room.getCheckInTime().format(timeFormatter))
        .checkOutTime(room.getCheckOutTime().format(timeFormatter))
        .peopleCount(reservation.getPeopleCount())
        .petCount(reservation.getPetCount())
        .totalPrice(reservation.getTotalPrice())
        .reviewWritten(reviewWritten)
        .build();
  }

  private HostReservationResponse mapToHostReservationResponse(Reservation reservation) {
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    return HostReservationResponse.builder()
        .reservationId(reservation.getId())
        .reservationDate(reservation.getCreatedAt().format(dateFormatter))
        .reserverName(reservation.getReserverName())
        .reserverPhoneNumber(reservation.getReserverPhoneNumber())
        .hasVehicle(reservation.getHasVehicle())
        .checkInDate(reservation.getCheckInDate().format(dateFormatter))
        .checkOutDate(reservation.getCheckOutDate().format(dateFormatter))
        .peopleCount(reservation.getPeopleCount())
        .petCount(reservation.getPetCount())
        .totalPrice(reservation.getTotalPrice())
        .build();
  }
}
