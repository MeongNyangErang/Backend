package com.meongnyangerang.meongnyangerang.service;

import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import com.meongnyangerang.meongnyangerang.domain.reservation.Reservation;
import com.meongnyangerang.meongnyangerang.domain.reservation.ReservationSlot;
import com.meongnyangerang.meongnyangerang.domain.reservation.ReservationStatus;
import com.meongnyangerang.meongnyangerang.domain.room.Room;
import com.meongnyangerang.meongnyangerang.domain.user.User;
import com.meongnyangerang.meongnyangerang.dto.CustomReservationResponse;
import com.meongnyangerang.meongnyangerang.dto.HostReservationResponse;
import com.meongnyangerang.meongnyangerang.dto.ReservationRequest;
import com.meongnyangerang.meongnyangerang.dto.ReservationResponse;
import com.meongnyangerang.meongnyangerang.dto.UserReservationResponse;
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

  /**
   * 사용자와 객실 정보를 바탕으로 예약을 생성하는 메소드. 예약 가능한지 확인하고, 예약을 처리한 후 예약 정보를 DB에 저장합니다.
   *
   * @param userId  사용자 ID
   * @param request 예약 요청 정보
   */
  @Transactional
  public ReservationResponse createReservation(Long userId, ReservationRequest request) {
    // 사용자 검증
    User user = validateUser(userId);

    // 객실 검증
    Room room = validateRoom(request.getRoomId());

    // 객실 예약 가능 여부 확인
    checkRoomAvailability(room, request.getCheckInDate(),
        request.getCheckOutDate());

    // 예약 날짜에 대해 객실 예약 처리
    bookRoomForDates(room, request.getCheckInDate(), request.getCheckOutDate());

    // 예약 정보 생성 후 DB에 저장
    Reservation savedReservation = saveReservation(user, room, request);

    // 예약 알림 저장 및 전송 (사용자와 호스트에게 전송)
    notificationService.sendReservationNotification(
        savedReservation.getId(),
        savedReservation.getAccommodationName(),
        user,
        room.getAccommodation().getHost()
    );

    return new ReservationResponse(UUID.randomUUID().toString());
  }

  /**
   * 사용자가 예약 상태(RESERVED, COMPLETED, CANCELED)에 따라 예약 목록을 조회합니다.
   *
   * @param userId
   * @param cursorId
   * @param size
   * @param status
   */
  public CustomReservationResponse<UserReservationResponse> getUserReservations(Long userId,
      Long cursorId, int size,
      ReservationStatus status) {
    // 해당 유저의 예약 내역만 조회
    List<Reservation> reservationList = reservationRepository.findByUserIdAndStatus(userId,
        cursorId, size + 1, status.name());

    // 예약 정보 -> DTO 변환
    List<UserReservationResponse> content = reservationList.stream()
        .limit(size)
        .map(this::mapToUserReservationResponse)
        .toList();

    // 다음 데이터 존재 여부 확인
    boolean hasNext = reservationList.size() > size;
    Long cursor = hasNext ? reservationList.get(size).getId() : null;

    return new CustomReservationResponse<>(content, cursor, hasNext);
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
  }

  public CustomReservationResponse<HostReservationResponse> getHostReservation(Long hostId,
      Long cursorId, int size,
      ReservationStatus status) {
    // 자신의 숙소 예약 내역만 조회
    List<Reservation> reservationList = reservationRepository.findByHostIdAndStatus(hostId,
        cursorId, size + 1, status.name());

    // 예약 정보 -> DTO 변환
    List<HostReservationResponse> content = reservationList.stream()
        .limit(size)
        .map(this::mapToHostReservationResponse)
        .toList();

    // 다음 데이터 존재 여부 확인
    boolean hasNext = reservationList.size() > size;
    Long cursor = hasNext ? reservationList.get(size).getId() : null;

    return new CustomReservationResponse<>(content, cursor, hasNext);
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

  private UserReservationResponse mapToUserReservationResponse(Reservation reservation) {
    boolean reviewWritten = reviewRepository.existsByReservationId(reservation.getId());
    Room room = reservation.getRoom();
    Accommodation accommodation = room.getAccommodation();

    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    return UserReservationResponse.builder()
        .reservationId(reservation.getId())
        .reservationDate(reservation.getCreatedAt().format(dateFormatter))
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
