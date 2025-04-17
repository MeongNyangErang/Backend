package com.meongnyangerang.meongnyangerang.dev;

import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import com.meongnyangerang.meongnyangerang.domain.accommodation.AccommodationImage;
import com.meongnyangerang.meongnyangerang.domain.accommodation.AccommodationType;
import com.meongnyangerang.meongnyangerang.domain.accommodation.AllowPet;
import com.meongnyangerang.meongnyangerang.domain.accommodation.PetType;
import com.meongnyangerang.meongnyangerang.domain.accommodation.facility.AccommodationFacility;
import com.meongnyangerang.meongnyangerang.domain.accommodation.facility.AccommodationFacilityType;
import com.meongnyangerang.meongnyangerang.domain.accommodation.facility.AccommodationPetFacility;
import com.meongnyangerang.meongnyangerang.domain.accommodation.facility.AccommodationPetFacilityType;
import com.meongnyangerang.meongnyangerang.domain.host.Host;
import com.meongnyangerang.meongnyangerang.domain.host.HostStatus;
import com.meongnyangerang.meongnyangerang.domain.reservation.Reservation;
import com.meongnyangerang.meongnyangerang.domain.reservation.ReservationSlot;
import com.meongnyangerang.meongnyangerang.domain.reservation.ReservationStatus;
import com.meongnyangerang.meongnyangerang.domain.review.Review;
import com.meongnyangerang.meongnyangerang.domain.review.ReviewImage;
import com.meongnyangerang.meongnyangerang.domain.room.Room;
import com.meongnyangerang.meongnyangerang.domain.room.facility.Hashtag;
import com.meongnyangerang.meongnyangerang.domain.room.facility.HashtagType;
import com.meongnyangerang.meongnyangerang.domain.room.facility.RoomFacility;
import com.meongnyangerang.meongnyangerang.domain.room.facility.RoomFacilityType;
import com.meongnyangerang.meongnyangerang.domain.room.facility.RoomPetFacility;
import com.meongnyangerang.meongnyangerang.domain.room.facility.RoomPetFacilityType;
import com.meongnyangerang.meongnyangerang.domain.user.Role;
import com.meongnyangerang.meongnyangerang.domain.user.User;
import com.meongnyangerang.meongnyangerang.domain.user.UserStatus;
import com.meongnyangerang.meongnyangerang.repository.HostRepository;
import com.meongnyangerang.meongnyangerang.repository.ReservationRepository;
import com.meongnyangerang.meongnyangerang.repository.ReservationSlotRepository;
import com.meongnyangerang.meongnyangerang.repository.ReviewImageRepository;
import com.meongnyangerang.meongnyangerang.repository.ReviewRepository;
import com.meongnyangerang.meongnyangerang.repository.UserRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationFacilityRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationImageRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationPetFacilityRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AllowPetRepository;
import com.meongnyangerang.meongnyangerang.repository.room.HashtagRepository;
import com.meongnyangerang.meongnyangerang.repository.room.RoomFacilityRepository;
import com.meongnyangerang.meongnyangerang.repository.room.RoomPetFacilityRepository;
import com.meongnyangerang.meongnyangerang.repository.room.RoomRepository;
import com.meongnyangerang.meongnyangerang.service.AccommodationRoomSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DummyDataCreateService {

  private final HostRepository hostRepository;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

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

  private final AccommodationRoomSearchService searchService;

  private static final String USER_PASSWORD = "password123";
  private static final String HOST_PASSWORD = "host123";
  private static final String LOCALE = "ko";
  private static final int REQUIRE_COUNT = 1;
  private static final int MAX_RESERVATION_FIND_DATE_ATTEMPT_COUNT = 20;

  // 이미지 URL 목록 - 실제 서비스에서는 S3에 있는 이미지 URL로 대체
  private final List<String> imageUrls = List.of(
      "https://picsum.photos/800/600",
      "https://picsum.photos/800/601",
      "https://picsum.photos/800/602",
      "https://picsum.photos/800/603",
      "https://picsum.photos/800/604"
  );

  // 지역 정보
  private final List<String> areas = List.of(
      "서울", "부산", "제주", "인천", "대구", "광주", "대전", "울산", "경기", "강원");
  private final Map<String, List<String>> townsByArea = Map.of(
      "서울", List.of("강남구", "서초구", "송파구", "마포구", "종로구"),
      "부산", List.of("해운대구", "수영구", "남구", "부산진구", "동래구"),
      "제주", List.of("제주시", "서귀포시", "애월읍", "조천읍", "한림읍"),
      "인천", List.of("중구", "연수구", "남동구", "서구", "계양구"),
      "대구", List.of("중구", "수성구", "달서구", "동구", "북구")
  );


  @Transactional
  public Map<String, Object> generateData(DummyDataGenerateRequest request) {
    Faker faker = new Faker(new Locale(LOCALE));
    Random random = new Random();
    Map<String, Object> result = new HashMap<>();

    try {
      List<User> savedUsers = userRepository.findAll();
      List<Host> savedHosts = hostRepository.findAll();
      Set<String> usedNicknames = settingSavedNicknames(savedUsers, savedHosts);

      List<String> savedUserEmails = savedUsers
          .stream()
          .map(User::getEmail)
          .toList();

      List<String> savedHostEmails = savedHosts
          .stream()
          .map(Host::getEmail)
          .toList();

      // 1. 사용자 생성 (예약 및 리뷰 작성을 위해)
      List<User> users = createUsers(faker, random, savedUserEmails, usedNicknames);
      userRepository.saveAll(users);

      // 2. 호스트 생성 (숙소 개수만큼)
      List<Host> hosts = createHosts(request, faker, random, savedHostEmails, usedNicknames);
      hostRepository.saveAll(hosts);

      // 3. 숙소 생성 (호스트당 1개)
      List<Accommodation> accommodations = createAccommodations(request, faker, random, hosts);
      accommodationRepository.saveAll(accommodations);

      // 4. 숙소 이미지 생성 (숙소당 정확히 4개)
      List<AccommodationImage> accommodationImages = createAccommodationImages(
          accommodations, random);
      accommodationImageRepository.saveAll(accommodationImages);

      // 5. 숙소 애완동물 유형 설정
      List<AllowPet> allowPets = createAllowPets(accommodations, random);
      allowPetRepository.saveAll(allowPets);

      // 6. 숙소 시설 설정
      List<AccommodationFacility> accommodationFacilities = createAccommodationFacilities(
          accommodations, random);
      accommodationFacilityRepository.saveAll(accommodationFacilities);

      // 7. 숙소 애완동물 시설 설정
      List<AccommodationPetFacility> accommodationPetFacilities = createAccommodationPetFacilities(
          accommodations, random);
      accommodationPetFacilityRepository.saveAll(accommodationPetFacilities);

      // 8. 객실 생성
      List<Room> rooms = createRooms(request, accommodations, random, faker);
      roomRepository.saveAll(rooms);

      // 9. 객실 해시태그 설정
      List<Hashtag> roomHashtags = createRoomHashtags(rooms, random);
      hashtagRepository.saveAll(roomHashtags);

      // 10. 객실 시설 설정
      List<RoomFacility> roomFacilities = createRoomFacilities(rooms, random);
      roomFacilityRepository.saveAll(roomFacilities);

      // 11. 객실 애완동물 시설 설정
      List<RoomPetFacility> roomPetFacilities = createRoomPetFacilities(rooms, random);
      roomPetFacilityRepository.saveAll(roomPetFacilities);

      // 12. 예약 생성
      List<Reservation> reservations = createReservations(request, rooms, random, users, faker);
      reservationRepository.saveAll(reservations);

      // 13. 리뷰 생성
      List<Review> reviews = createAndSaveReviews(
          request, reservations, random, faker, accommodations);

      // Elasticsearch 색인 저장
      saveIndex(accommodations, rooms);

      // 결과 집계
      result.put("success", true);
      result.put("hostCount", hosts.size());
      result.put("accommodationCount", accommodations.size());
      result.put("roomCount", rooms.size());
      result.put("userCount", users.size());
      result.put("reservationCount", reservations.size());
      result.put("reviewCount", reviews.size());
      result.put("message", "더미 데이터가 성공적으로 생성되었습니다.");

    } catch (Exception e) {
      result.put("success", false);
      result.put("error", e.getMessage());
      result.put("message", "더미 데이터 생성 중 오류가 발생했습니다.");
      throw new RuntimeException("더미 데이터 생성 실패", e);
    }
    return result;
  }

  private Set<String> settingSavedNicknames(List<User> savedUsers, List<Host> savedHosts) {
    Set<String> usedNicknames = new HashSet<>();

    usedNicknames.addAll(savedUsers
        .stream()
        .map(User::getNickname)
        .toList());

    usedNicknames.addAll(savedHosts
        .stream()
        .map(Host::getNickname)
        .toList());

    return usedNicknames;
  }

  private List<User> createUsers(
      Faker faker,
      Random random,
      List<String> savedUserEmails,
      Set<String> usedNicknames
  ) {
    List<User> users = new ArrayList<>();
    Set<String> usedUserEmails = new HashSet<>(savedUserEmails); // 중복 닉네임 방지를 위한 Set

    for (int i = 0; i < 100; i++) {
      String email;
      String nickname;

      do { // 중복되지 않는 이메일 생성
        email = faker.internet().emailAddress(); // 무작위 이메일 생성
      } while (usedUserEmails.contains(email));
      usedUserEmails.add(email);

      do { // 중복되지 않는 닉네임 생성
        nickname = faker.name().firstName() + random.nextInt(100000); // 무작이 이름 생성
      } while (usedNicknames.contains(nickname));
      usedNicknames.add(nickname);

      User user = User.builder()
          .email(email)
          .nickname(nickname)
          .password(passwordEncoder.encode(USER_PASSWORD))
          .profileImage( // 50% 확률로 이미지 있거나, null
              random.nextBoolean() ? imageUrls.get(random.nextInt(imageUrls.size())) : null)
          .status(UserStatus.ACTIVE)
          .role(Role.ROLE_USER)
          .createdAt(LocalDateTime.now())
          .updatedAt(LocalDateTime.now())
          .build();

      users.add(user);
    }
    return users;
  }

  private List<Host> createHosts(
      DummyDataGenerateRequest request,
      Faker faker,
      Random random,
      List<String> savedHostEmails,
      Set<String> usedNicknames
  ) {
    List<Host> hosts = new ArrayList<>();
    Set<String> usedEmails = new HashSet<>(savedHostEmails);

    for (int i = 0; i < request.getAccommodationCount(); i++) {
      String email;
      String nickname;

      do { // 중복되지 않는 이메일 생성
        email = faker.internet().emailAddress();
      } while (usedEmails.contains(email));
      usedEmails.add(email);

      do { // 중복되지 않는 닉네임 생성
        nickname = faker.name().firstName() + random.nextInt(100000);
      } while (usedNicknames.contains(nickname));
      usedNicknames.add(nickname);

      Host host = Host.builder()
          .email(email)
          .name(faker.name().fullName())
          .nickname(nickname)
          .password(passwordEncoder.encode(HOST_PASSWORD))
          .profileImageUrl(
              random.nextBoolean() ? imageUrls.get(random.nextInt(imageUrls.size())) : null)
          .businessLicenseImageUrl(imageUrls.get(random.nextInt(imageUrls.size())))
          .submitDocumentImageUrl(imageUrls.get(random.nextInt(imageUrls.size())))
          .phoneNumber(createRandomPhoneNumber(faker))
          .status(HostStatus.ACTIVE)
          .role(Role.ROLE_USER)
          .createdAt(LocalDateTime.now())
          .updatedAt(LocalDateTime.now())
          .build();

      hosts.add(host);
    }
    return hosts;
  }

  private void updateAccommodationRating(
      List<Accommodation> accommodations,
      List<Review> reviews
  ) {
    for (Accommodation accommodation : accommodations) {
      List<Review> accommodationReviews = reviews.stream()
          .filter(r -> r.getAccommodation().getId().equals(accommodation.getId()))
          .toList();

      if (!accommodationReviews.isEmpty()) {
        double avgRating = accommodationReviews.stream()
            .mapToDouble(r -> (r.getUserRating() + r.getPetFriendlyRating()) / 2)
            .average()
            .orElse(4.0);

        accommodation.setTotalRating(Math.round(avgRating * 10) / 10.0);
      }
    }
  }

  private List<ReviewImage> createReviewImages(List<Review> reviews, Random random) {
    List<ReviewImage> reviewImages = new ArrayList<>();

    for (Review review : reviews) {
      if (random.nextInt(10) < 5) { // 50% 확률로 리뷰 이미지 1~3개 추가
        int imageCount = 1 + random.nextInt(3);

        for (int i = 0; i < imageCount; i++) {
          ReviewImage reviewImage = ReviewImage.builder()
              .review(review)
              .imageUrl(imageUrls.get(random.nextInt(imageUrls.size())))
              .createdAt(review.getCreatedAt())
              .build();

          reviewImages.add(reviewImage);
        }
      }
    }
    return reviewImages;
  }

  private List<Review> createReviews(DummyDataGenerateRequest request,
      List<Reservation> completedReservations,
      Random random,
      Faker faker
  ) {
    List<Review> reviews = new ArrayList<>();
    int totalReviewCount = Math.min(request.getReviewCount(), completedReservations.size());

    for (int i = 0; i < totalReviewCount; i++) {
      Reservation reservation = completedReservations.get(i % completedReservations.size());

      double userRating = 0.5 + random.nextInt(10) * 0.5;
      double petFriendlyRating = 0.5 + random.nextInt(10) * 0.5;
      LocalDateTime randomReviewCreatedAt = reservation.getCheckOutDate()
          .atTime(12, 0).plusDays(random.nextInt(5));

      Review review = Review.builder()
          .user(reservation.getUser())
          .accommodation(reservation.getRoom().getAccommodation())
          .reservation(reservation)
          .userRating(userRating)
          .petFriendlyRating(petFriendlyRating)
          .content(faker.lorem().paragraph(1 + random.nextInt(3)))
          .reportCount(0)
          .createdAt(randomReviewCreatedAt)
          .updatedAt(randomReviewCreatedAt)
          .build();

      reviews.add(review);
    }
    return reviews;
  }

  private List<Reservation> createReservations(
      DummyDataGenerateRequest request,
      List<Room> rooms,
      Random random,
      List<User> users,
      Faker faker
  ) {
    List<Reservation> reservations = new ArrayList<>();

    // 각 객실별로 예약 날짜를 관리하기 위한 Map
    Map<Long, Set<LocalDate>> roomBookedDatesMap = new HashMap<>();
    for (Room room : rooms) {
      roomBookedDatesMap.put(room.getId(), new HashSet<>());
    }

    int totalReservations = request.getReservationCount();
    int successCount = 0;
    int attemptCount = 0;
    int maxAttempts = totalReservations * 3; // 최대 시도 횟수 제한

    while (successCount < totalReservations && attemptCount < maxAttempts) {
      attemptCount++;

      // 객실 선택
      Room room = rooms.get(random.nextInt(rooms.size()));
      User user = users.get(random.nextInt(users.size()));

      // 현재 날짜 기준으로 예약 가능한 기간 설정
      LocalDate now = LocalDate.now();
      LocalDate maxDate = now.plusDays(90); // 향후 90일 이내

      // 이 객실의 이미 예약된 날짜들
      Set<LocalDate> bookedDates = roomBookedDatesMap.get(room.getId());

      // 예약 기간 선택 (체크인 날짜, 숙박 일수)
      LocalDate checkInDate = findAvailableDate(now, maxDate, bookedDates, random);
      int stayDays = 1 + random.nextInt(5); // 1~5일 숙박
      LocalDate checkOutDate = checkInDate.plusDays(stayDays);

      // 선택한 날짜가 모두 예약 가능한지 확인
      boolean allDatesAvailable = true;
      Set<LocalDate> reservingDates = new HashSet<>();

      for (LocalDate date = checkInDate; date.isBefore(checkOutDate); date = date.plusDays(1)) {
        if (bookedDates.contains(date)) {
          allDatesAvailable = false;
          break;
        }
        reservingDates.add(date);
      }

      // 날짜가 모두 예약 가능하면 예약 생성 진행
      if (allDatesAvailable) {
        // 예약 슬롯 생성
        List<ReservationSlot> reservationSlots = new ArrayList<>();
        for (LocalDate date : reservingDates) {
          ReservationSlot slot = new ReservationSlot();
          slot.setRoom(room);
          slot.setReservedDate(date);
          slot.setIsReserved(true);

          reservationSlots.add(slot);
          bookedDates.add(date); // 예약된 날짜 맵에 추가 (향후 중복 방지)
        }
        reservationSlotRepository.saveAll(reservationSlots); // 예약 슬롯 저장

        // 인원 및 반려동물 수
        int peopleCount = 1 + random.nextInt(room.getMaxPeopleCount());
        int petCount = 1 + random.nextInt(room.getMaxPetCount());

        // 가격 계산
        long basePrice = room.getPrice() * stayDays;
        long extraPeoplePrice = 0;

        if (peopleCount > room.getStandardPeopleCount()) {
          extraPeoplePrice =
              (room.getExtraPeopleFee() * (peopleCount - room.getStandardPeopleCount())) * stayDays;
        }

        long extraPetPrice = 0;
        if (petCount > room.getStandardPetCount()) {
          extraPetPrice =
              (room.getExtraPetFee() * (petCount - room.getStandardPetCount())) * stayDays;
        }

        long totalPrice = basePrice + extraPeoplePrice + extraPetPrice + room.getExtraFee();

        // 예약 상태 설정
        ReservationStatus status;
        LocalDateTime createdAt = LocalDateTime.now().minusDays(random.nextInt(100));
        LocalDateTime updatedAt = createdAt;
        LocalDateTime canceledAt = null;

        if (checkInDate.isBefore(now)) {
          status = ReservationStatus.COMPLETED;
        } else {
          if (random.nextInt(10) < 8) {
            status = ReservationStatus.RESERVED; // 80% 확률로 RESERVED
          } else {
            status = ReservationStatus.CANCELED; // 20% 확률로 CANCELED
            canceledAt = createdAt.plusDays(random.nextInt(5) + 1);
            updatedAt = canceledAt;
          }
        }

        // 예약 객체 생성
        Reservation reservation = Reservation.builder()
            .user(user)
            .room(room)
            .accommodationName(room.getAccommodation().getName())
            .checkInDate(checkInDate)
            .checkOutDate(checkOutDate)
            .peopleCount(peopleCount)
            .petCount(petCount)
            .reserverName(faker.name().fullName())
            .reserverPhoneNumber(createRandomPhoneNumber(faker))
            .hasVehicle(random.nextBoolean())
            .totalPrice(totalPrice)
            .status(status)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .canceledAt(canceledAt)
            .build();

        reservations.add(reservation);
        successCount++;
      }
    }

    if (successCount < totalReservations) {
      log.warn("생성된 예약 수({})가 요청된 수({})보다 적습니다. 가능한 날짜가 부족합니다.",
          successCount, totalReservations);
    }

    return reservations;
  }

  private LocalDate findAvailableDate(
      LocalDate minDate,
      LocalDate maxDate,
      Set<LocalDate> bookedDates,
      Random random
  ) {
    for (int attempt = 0; attempt < MAX_RESERVATION_FIND_DATE_ATTEMPT_COUNT; attempt++) {
      // 최소 날짜(minDate)와 최대 날짜(maxDate) 사이의 일수를 계산
      long daysBetween = ChronoUnit.DAYS.between(minDate, maxDate);

      if (daysBetween <= 0) {
        return minDate; // 범위가 없으면 최소 날짜 반환
      }

      // 최소 날짜와 최대 날짜 사이에서 무작위로 하루를 선택
      LocalDate candidateDate = minDate.plusDays(random.nextInt((int) daysBetween));

      // 선택한 날짜가 이미 예약되었는지 확인
      if (!bookedDates.contains(candidateDate)) {
        return candidateDate;
      }
    }
    // 적절한 날짜를 찾지 못한 경우 현재로부터 먼 미래 날짜 반환
    return maxDate.minusDays(5); // 최대 날짜에서 5일 전 (숙박 기간 고려)
  }

  private List<RoomPetFacility> createRoomPetFacilities(List<Room> rooms, Random random) {
    List<RoomPetFacility> roomPetFacilities = new ArrayList<>();

    for (Room room : rooms) {
      List<RoomPetFacilityType> petFacilityTypes = Arrays.asList(RoomPetFacilityType.values());
      Collections.shuffle(petFacilityTypes, random);

      int petFacilityCount = createRandomCount(random, petFacilityTypes.size());

      for (int i = 0; i < petFacilityCount; i++) {
        RoomPetFacilityType petFacilityType = petFacilityTypes.get(i);
        RoomPetFacility petFacility = RoomPetFacility.builder()
            .room(room)
            .type(petFacilityType)
            .createdAt(room.getCreatedAt())
            .build();

        roomPetFacilities.add(petFacility);
      }
    }
    return roomPetFacilities;
  }

  private List<RoomFacility> createRoomFacilities(List<Room> rooms, Random random) {
    List<RoomFacility> roomFacilities = new ArrayList<>();

    for (Room room : rooms) {
      List<RoomFacilityType> facilityTypes = Arrays.asList(RoomFacilityType.values());
      Collections.shuffle(facilityTypes, random);

      int facilityCount = createRandomCount(random, facilityTypes.size());

      for (int i = 0; i < facilityCount; i++) {
        RoomFacilityType facilityType = facilityTypes.get(i);
        RoomFacility facility = RoomFacility.builder()
            .room(room)
            .type(facilityType)
            .createdAt(room.getCreatedAt())
            .build();

        roomFacilities.add(facility);
      }
    }
    return roomFacilities;
  }

  private List<Hashtag> createRoomHashtags(List<Room> rooms, Random random) {
    List<Hashtag> roomHashtags = new ArrayList<>();

    for (Room room : rooms) {
      List<HashtagType> hashtagTypes = Arrays.asList(HashtagType.values());
      Collections.shuffle(hashtagTypes, random);

      int hashtagCount = createRandomCount(random, hashtagTypes.size());

      for (int i = 0; i < hashtagCount; i++) {
        HashtagType hashtagType = hashtagTypes.get(i);
        Hashtag hashtag = Hashtag.builder()
            .room(room)
            .type(hashtagType)
            .createdAt(room.getCreatedAt())
            .build();

        roomHashtags.add(hashtag);
      }
    }
    return roomHashtags;
  }

  private List<Room> createRooms(
      DummyDataGenerateRequest request,
      List<Accommodation> accommodations,
      Random random,
      Faker faker
  ) {
    List<Room> rooms = new ArrayList<>();

    for (Accommodation accommodation : accommodations) {
      int roomCount = 1 + random.nextInt(request.getRoomCount());

      for (int i = 0; i < roomCount; i++) {
        String name = faker.lorem().word() + " " + faker.lorem().word() + " 룸";
        String description = faker.lorem().paragraph(1);

        // 인원 및 반려동물 수 설정
        int standardPeopleCount = 1 + random.nextInt(3);
        int maxPeopleCount = standardPeopleCount + random.nextInt(3);

        int standardPetCount = 1 + random.nextInt(2);
        int maxPetCount = standardPetCount + random.nextInt(2);

        // 이미지 및 가격 설정
        String roomImageUrl = imageUrls.get(random.nextInt(imageUrls.size()));
        Long price = 50000L + (random.nextInt(20) * 10000L); // 5~25만원

        // 추가 요금 설정
        Long extraPeopleFee = 10000L + (random.nextInt(5) * 5000L); // 1~3만원
        Long extraPetFee = 10000L + (random.nextInt(3) * 5000L); // 1~2만원
        Long extraFee = 5000L + (random.nextInt(5) * 1000L); // 5천~1만원

        // 체크인/체크아웃 시간
        LocalTime checkInTime = LocalTime.of(
            14 + random.nextInt(3), 0); // 14:00~16:00
        LocalTime checkOutTime = LocalTime.of(
            10 + random.nextInt(3), 0); // 10:00~12:00

        Room room = Room.builder()
            .accommodation(accommodation)
            .name(name)
            .description(description)
            .standardPeopleCount(standardPeopleCount)
            .maxPeopleCount(maxPeopleCount)
            .standardPetCount(standardPetCount)
            .maxPetCount(maxPetCount)
            .imageUrl(roomImageUrl) // 객실 이미지는 Room 엔티티에 직접 저장
            .price(price)
            .extraPeopleFee(extraPeopleFee)
            .extraPetFee(extraPetFee)
            .extraFee(extraFee)
            .checkInTime(checkInTime)
            .checkOutTime(checkOutTime)
            .createdAt(accommodation.getCreatedAt())
            .updatedAt(accommodation.getUpdatedAt())
            .build();

        rooms.add(room);
      }
    }
    return rooms;
  }

  private List<AccommodationPetFacility> createAccommodationPetFacilities(
      List<Accommodation> accommodations,
      Random random
  ) {
    List<AccommodationPetFacility> accommodationPetFacilities = new ArrayList<>();

    for (Accommodation accommodation : accommodations) {
      List<AccommodationPetFacilityType> petFacilityTypes = Arrays.asList(
          AccommodationPetFacilityType.values());
      Collections.shuffle(petFacilityTypes, random);

      int petFacilityCount = createRandomCount(random, petFacilityTypes.size());

      for (int i = 0; i < petFacilityCount; i++) {
        AccommodationPetFacilityType petFacilityType = petFacilityTypes.get(i);
        AccommodationPetFacility petFacility = AccommodationPetFacility.builder()
            .accommodation(accommodation)
            .type(petFacilityType)
            .createdAt(accommodation.getCreatedAt())
            .build();

        accommodationPetFacilities.add(petFacility);
      }
    }
    return accommodationPetFacilities;
  }

  private List<AccommodationFacility> createAccommodationFacilities(
      List<Accommodation> accommodations,
      Random random
  ) {
    List<AccommodationFacility> accommodationFacilities = new ArrayList<>();

    for (Accommodation accommodation : accommodations) {
      List<AccommodationFacilityType> facilityTypes = Arrays.asList(
          AccommodationFacilityType.values());
      Collections.shuffle(facilityTypes, random);

      int facilityCount = createRandomCount(random, facilityTypes.size());

      for (int i = 0; i < facilityCount; i++) {
        AccommodationFacilityType facilityType = facilityTypes.get(i);
        AccommodationFacility facility = AccommodationFacility.builder()
            .accommodation(accommodation)
            .type(facilityType)
            .createdAt(accommodation.getCreatedAt())
            .build();

        accommodationFacilities.add(facility);
      }
    }
    return accommodationFacilities;
  }

  private List<AllowPet> createAllowPets(List<Accommodation> accommodations, Random random) {
    List<AllowPet> allowPets = new ArrayList<>();

    for (Accommodation accommodation : accommodations) {
      List<PetType> petTypes = Arrays.asList(PetType.values());
      Collections.shuffle(petTypes, random);

      int petTypeCount = createRandomCount(random, petTypes.size());

      for (int i = 0; i < petTypeCount; i++) {
        PetType petType = petTypes.get(i);
        AllowPet allowPet = AllowPet.builder()
            .accommodation(accommodation)
            .petType(petType)
            .createdAt(accommodation.getCreatedAt())
            .build();

        allowPets.add(allowPet);
      }
    }
    return allowPets;
  }

  private List<AccommodationImage> createAccommodationImages(
      List<Accommodation> accommodations,
      Random random
  ) {
    List<AccommodationImage> accommodationImages = new ArrayList<>();

    for (Accommodation accommodation : accommodations) {
      int imageCount = random.nextInt(4); // 0~3개 중 랜덤 선택

      for (int i = 0; i < imageCount; i++) {
        AccommodationImage image = AccommodationImage.builder()
            .accommodation(accommodation)
            .imageUrl(imageUrls.get(random.nextInt(imageUrls.size())))
            .createdAt(accommodation.getCreatedAt())
            .build();

        accommodationImages.add(image);
      }
    }
    return accommodationImages;
  }

  private List<Accommodation> createAccommodations(
      DummyDataGenerateRequest request,
      Faker faker,
      Random random,
      List<Host> hosts
  ) {
    List<Accommodation> accommodations = new ArrayList<>();

    for (int i = 0; i < request.getAccommodationCount(); i++) {
      String name = faker.company().name() + " " + // 무작위 회사 이름
          faker.lorem().word() + " " + // 무작위 단어
          faker.lorem().word();
      String description = faker.lorem().paragraph(1); // 무작위 한 개의 문단

      // 주소 설정
      String area = areas.get(random.nextInt(areas.size()));
      List<String> towns = townsByArea.getOrDefault(area, List.of("서구"));
      String town = towns.get(random.nextInt(towns.size()));
      String detailedAddress = faker.address().streetAddress(); // 무작위 도로명 주소

      // 위치 정보 (위도, 경도)
      Double latitude = 33.0 + random.nextDouble() * 10.0; // 33~43 범위의 위도
      Double longitude = 125.0 + random.nextDouble() * 10.0; // 125~135 범위의 경도

      AccommodationType type = randomEnum(AccommodationType.class, random); // 무작위 숙소 유형
      String thumbnailUrl = imageUrls.get(random.nextInt(imageUrls.size())); // 썸네일 이미지

      // 평점 및 조회수
      Double totalRating =
          Math.round(1.0 + random.nextDouble() * 4.0 * 10) / 10.0; // 1.0~5.0 범위의 평점
      Long viewCount = random.nextLong(10000);

      Accommodation accommodation = Accommodation.builder()
          .host(hosts.get(i))
          .name(name)
          .description(description)
          .address(area + " " + town)
          .detailedAddress(detailedAddress)
          .latitude(latitude)
          .longitude(longitude)
          .type(type)
          .thumbnailUrl(thumbnailUrl)
          .totalRating(totalRating)
          .viewCount(viewCount)
          .createdAt(LocalDateTime.now())
          .updatedAt(LocalDateTime.now())
          .build();

      accommodations.add(accommodation);
    }
    return accommodations;
  }

  private List<Review> createAndSaveReviews(
      DummyDataGenerateRequest request,
      List<Reservation> reservations,
      Random random,
      Faker faker,
      List<Accommodation> accommodations
  ) {
    List<Review> reviews = new ArrayList<>();

    // 예약 중 COMPLETED 상태인 것만 필터링
    List<Reservation> completedReservations = reservations.stream()
        .filter(r -> r.getStatus() == ReservationStatus.COMPLETED)
        .toList();

    if (!completedReservations.isEmpty()) {
      reviews = createReviews(request, completedReservations, random, faker);
      reviewRepository.saveAll(reviews);

      // 리뷰 이미지 생성
      List<ReviewImage> reviewImages = createReviewImages(reviews, random);
      reviewImageRepository.saveAll(reviewImages);

      // 숙소 평점 업데이트
      updateAccommodationRating(accommodations, reviews);
      accommodationRepository.saveAll(accommodations);
    }
    return reviews;
  }

  private void saveIndex(List<Accommodation> accommodations, List<Room> rooms) {
    for (Accommodation accommodation : accommodations) {
      List<Room> relatedRooms = rooms.stream()
          .filter(room -> room.getAccommodation().getId().equals(accommodation.getId()))
          .toList();

      Room firstRoom = relatedRooms.get(0);
      searchService.indexAccommodationDocument(accommodation, firstRoom);

      for (Room room : relatedRooms) {
        searchService.save(accommodation, room);
      }

      if (relatedRooms.size() > 1) {
        searchService.updateAccommodationDocument(accommodation);
      }
    }
  }

  public int createRandomCount(Random random, int typeSize) {
    return Math.min(REQUIRE_COUNT + random.nextInt(typeSize), typeSize);
  }

  private String createRandomPhoneNumber(Faker faker) {
    return "010-" + faker.number().digits(4) + "-" + faker.number().digits(4);
  }

  // Enum 클래스들의 값을 가져오기 위한 헬퍼 메서드
  private <T extends Enum<?>> T randomEnum(Class<T> enumClass, Random random) {
    T[] values = enumClass.getEnumConstants();
    return values[random.nextInt(values.length)];
  }

  // 상태 조회 메서드들
  public long getHostCount() {
    return hostRepository.count();
  }

  public long getAccommodationCount() {
    return accommodationRepository.count();
  }

  public long getRoomCount() {
    return roomRepository.count();
  }

  public long getUserCount() {
    return userRepository.count();
  }

  public long getReservationCount() {
    return reservationRepository.count();
  }

  public long getReviewCount() {
    return reviewRepository.count();
  }
}