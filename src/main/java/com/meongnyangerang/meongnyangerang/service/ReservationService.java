package com.meongnyangerang.meongnyangerang.service;

import com.meongnyangerang.meongnyangerang.domain.reservation.Reservation;
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
import jakarta.persistence.OptimisticLockException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 예약 관련 비즈니스 로직을 처리하는 서비스
 */
@Service
@RequiredArgsConstructor
public class ReservationService {

  private final ReservationRepository reservationRepository;
  private final ReservationSlotRepository reservationSlotRepository;
  private final UserRepository userRepository;
  private final RoomRepository roomRepository;

  /**
   * 사용자와 객실 정보를 바탕으로 예약을 생성하는 메소드.
   * 예약 가능한지 확인하고, 예약을 처리한 후 예약 정보를 DB에 저장합니다.
   *
   * @param userId            사용자 ID
   * @param reservationRequest 예약 요청 정보
   */
  @Transactional
  public void createReservation(Long userId, ReservationRequest reservationRequest) {
    // 사용자 검증
    User user = validateUser(userId);

    // 객실 검증
    Room room = validateRoom(reservationRequest.getRoomId());

    // 객실 예약 가능 여부 확인
    checkRoomAvailability(room, reservationRequest.getCheckInDate(),
        reservationRequest.getCheckOutDate());

    // 예약 날짜에 대해 객실 예약 처리
    List<ReservationSlot> reservations = bookRoomForDates(room,
        reservationRequest.getCheckInDate(), reservationRequest.getCheckOutDate());

    // 예약 정보 생성 후 DB에 저장
    saveReservation(user, room, reservationRequest);
  }

  /**
   * 사용자 ID에 해당하는 사용자가 존재하는지 확인합니다.
   * 존재하지 않으면 예외를 발생시킵니다.
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
   * 객실 ID에 해당하는 객실이 존재하는지 확인합니다.
   * 존재하지 않으면 예외를 발생시킵니다.
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
   * 주어진 체크인 날짜와 체크아웃 날짜 범위 내에 해당 객실이 이미 예약된 날짜가 있는지 확인합니다.
   * 예약된 날짜가 있으면 예외를 발생시킵니다.
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
   * 체크인부터 체크아웃 전날까지 예약 슬롯을 확인하고 예약되지 않은 슬롯에 대해 예약을 진행합니다.
   * 예약된 슬롯은 예외를 발생시키고, 예약되지 않은 슬롯은 예약 처리합니다.
   *
   * @param room         예약하려는 객실
   * @param checkInDate  체크인 날짜
   * @param checkOutDate 체크아웃 날짜
   * @return 예약된 슬롯 목록
   * @throws MeongnyangerangException 이미 예약된 슬롯이 있을 경우 예외 발생
   */
  private List<ReservationSlot> bookRoomForDates(Room room, LocalDate checkInDate,
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

    return reservations;
  }

  /**
   * 사용자와 객실 정보 및 예약 요청 정보를 바탕으로 예약 정보를 DB에 저장합니다.
   *
   * @param user               예약한 사용자
   * @param room               예약된 객실
   * @param reservationRequest 예약 요청 정보
   */
  private void saveReservation(User user, Room room, ReservationRequest reservationRequest) {
    Reservation reservation = reservationRequest.toEntity(user, room);
    reservationRepository.save(reservation);
  }

}
