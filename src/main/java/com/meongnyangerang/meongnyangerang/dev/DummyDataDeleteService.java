package com.meongnyangerang.meongnyangerang.dev;

import com.meongnyangerang.meongnyangerang.repository.AuthenticationCodeRepository;
import com.meongnyangerang.meongnyangerang.repository.HostRepository;
import com.meongnyangerang.meongnyangerang.repository.NotificationRepository;
import com.meongnyangerang.meongnyangerang.repository.ReservationRepository;
import com.meongnyangerang.meongnyangerang.repository.ReservationSlotRepository;
import com.meongnyangerang.meongnyangerang.repository.ReviewImageRepository;
import com.meongnyangerang.meongnyangerang.repository.ReviewRepository;
import com.meongnyangerang.meongnyangerang.repository.UserRepository;
import com.meongnyangerang.meongnyangerang.repository.WishlistRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationFacilityRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationImageRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationPetFacilityRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AllowPetRepository;
import com.meongnyangerang.meongnyangerang.repository.chat.ChatMessageRepository;
import com.meongnyangerang.meongnyangerang.repository.chat.ChatReadStatusRepository;
import com.meongnyangerang.meongnyangerang.repository.chat.ChatRoomRepository;
import com.meongnyangerang.meongnyangerang.repository.room.HashtagRepository;
import com.meongnyangerang.meongnyangerang.repository.room.RoomFacilityRepository;
import com.meongnyangerang.meongnyangerang.repository.room.RoomPetFacilityRepository;
import com.meongnyangerang.meongnyangerang.repository.room.RoomRepository;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DummyDataDeleteService {

  private final HostRepository hostRepository;
  private final UserRepository userRepository;
  private final AuthenticationCodeRepository authenticationCodeRepository;
  private final WishlistRepository wishlistRepository;

  private final AccommodationRepository accommodationRepository;
  private final AccommodationImageRepository accommodationImageRepository;
  private final AllowPetRepository allowPetRepository;
  private final AccommodationFacilityRepository accommodationFacilityRepository;
  private final AccommodationPetFacilityRepository accommodationPetFacilityRepository;

  private final RoomRepository roomRepository;
  private final HashtagRepository hashtagRepository;
  private final RoomFacilityRepository roomFacilityRepository;
  private final RoomPetFacilityRepository roomPetFacilityRepository;

  private final ReservationRepository reservationRepository;
  private final ReservationSlotRepository reservationSlotRepository;

  private final ReviewRepository reviewRepository;
  private final ReviewImageRepository reviewImageRepository;

  private final ChatRoomRepository chatRoomRepository;
  private final ChatMessageRepository chatMessageRepository;
  private final ChatReadStatusRepository chatReadStatusRepository;

  private final NotificationRepository notificationRepository;

  /**
   * 관계 순서를 고려하여 역순으로 삭제 admin, notice는 삭제하지 않습니다. (필요하다면 추가)
   */
  @Transactional
  public Map<String, Object> clearData() {
    Map<String, Object> result = new HashMap<>();

    try {
      notificationRepository.deleteAll();

      chatReadStatusRepository.deleteAll();
      chatMessageRepository.deleteAll();
      chatRoomRepository.deleteAll();

      // 신고 기능 추가 시, 리뷰 신고 제거도 필요
      reviewImageRepository.deleteAll();
      reviewRepository.deleteAll();

      reservationSlotRepository.deleteAll();
      reservationRepository.deleteAll();

      roomPetFacilityRepository.deleteAll();
      roomFacilityRepository.deleteAll();
      hashtagRepository.deleteAll();
      roomRepository.deleteAll();

      accommodationPetFacilityRepository.deleteAll();
      accommodationFacilityRepository.deleteAll();
      allowPetRepository.deleteAll();
      accommodationImageRepository.deleteAll();
      accommodationRepository.deleteAll();

      wishlistRepository.deleteAll();
      authenticationCodeRepository.deleteAll();
      userRepository.deleteAll();
      hostRepository.deleteAll();

      result.put("success", true);
      result.put("message", "모든 더미 데이터가 삭제되었습니다.");
    } catch (Exception e) {
      result.put("success", false);
      result.put("error", e.getMessage());
      result.put("message", "더미 데이터 삭제 중 오류가 발생했습니다.");
      throw new RuntimeException("더미 데이터 삭제 실패", e);
    }
    return result;
  }
}
