package com.meongnyangerang.meongnyangerang.service;

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

  @Transactional(readOnly = true)
  public void validateReservation(Long userId, ReservationRequest request) {
    validateUser(userId);
    Room room = validateRoom(request.getRoomId());
    checkRoomAvailability(room, request.getCheckInDate(), request.getCheckOutDate());
  }

  @Transactional
  public ReservationResponse createReservationAfterPayment(Long userId, PaymentReservationRequest request) {
    // 결제 검증
    ReservationRequest reservationRequest = request.getReservationRequest();
    portOneService.verifyPayment(request.getImpUid(), reservationRequest.getTotalPrice());

    // 예약 처리
    User user = validateUser(userId);
    Room room = validateRoom(reservationRequest.getRoomId());
    checkRoomAvailability(room, reservationRequest.getCheckInDate(), reservationRequest.getCheckOutDate());
    bookRoomForDates(room, reservationRequest.getCheckInDate(), reservationRequest.getCheckOutDate());
    Reservation savedReservation = saveReservation(user, room, reservationRequest);
    sendNotificationWhenReservationRegistered(savedReservation);

    return new ReservationResponse(UUID.randomUUID().toString());
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
        .orElseThrow(() -> new MeongnyangerangException(ErrorCode.RESERVATION_NOT_FOUND));

    // 사용자가 예약한 내역인지 확인
    if (!reservation.getUser().getId().equals(userId)) {
      throw new MeongnyangerangException(ErrorCode.INVALID_AUTHORIZED);
    }

    // 이미 취소된 예약인지 확인
    if (reservation.getStatus() == ReservationStatus.CANCELED) {
      throw new MeongnyangerangException(ErrorCode.RESERVATION_ALREADY_CANCELED);
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
        .orElseThrow(() -> new MeongnyangerangException(ErrorCode.USER_NOT_FOUND));
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
        .orElseThrow(() -> new MeongnyangerangException(ErrorCode.ROOM_NOT_FOUND));
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
      throw new MeongnyangerangException(ErrorCode.ROOM_ALREADY_RESERVED);
    }
  }

  /**
   * 체크인부터 체크아웃 전날까지 예약 슬롯을 확인하고 예약되지 않은 슬롯에 대해 예약을 진행합니다. 예약된 슬롯은 예외를 발생시키고, 예약되지 않은 슬롯은 예약 처리합니다.
   *
   * @param room         예약하려는 객실
   * @param checkInDate  체크인 날짜
   * @param checkOutDate 체크아웃 날짜
   * @return 예약된 슬롯 목록
   * @throws MeongnyangerangException 이미 예약된 슬롯이 있을 경우 예외 발생
   */
  private void bookRoomForDates(Room room, LocalDate checkInDate,
      LocalDate checkOutDate) {
    List<ReservationSlot> reservations = new ArrayList<>();

    // 체크인부터 체크아웃 전날까지 예약 슬롯을 확인하고 업데이트
    for (LocalDate date = checkInDate; date.isBefore(checkOutDate); date = date.plusDays(1)) {
      LocalDate finalDate = date;

      // 해당 날짜에 예약 슬롯을 조회하고 없으면 생성
      ReservationSlot slot = reservationSlotRepository
          .findByRoomIdAndReservedDate(room.getId(), date)
          .orElseGet(() -> new ReservationSlot(room, finalDate, false));

      // 이미 예약된 슬롯이라면 예외 발생
      if (slot.getIsReserved()) {
        throw new MeongnyangerangException(ErrorCode.ROOM_ALREADY_RESERVED);
      }

      // 슬롯 예약 처리
      slot.setIsReserved(true);
      reservations.add(slot);
    }

    try {
      reservationSlotRepository.saveAll(reservations);  // 저장 시 버전 번호 확인
    } catch (OptimisticLockException e) {
      throw new MeongnyangerangException(ErrorCode.ROOM_ALREADY_RESERVED);
    }
  }

  /**
   * 사용자와 객실 정보 및 예약 요청 정보를 바탕으로 예약 정보를 DB에 저장합니다.
   *
   * @param user    예약한 사용자
   * @param room    예약된 객실
   * @param request 예약 요청 정보
   */
  private Reservation saveReservation(User user, Room room, ReservationRequest request) {
    Reservation reservation = request.toEntity(user, room);
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
