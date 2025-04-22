package com.meongnyangerang.meongnyangerang.dev;

import static com.meongnyangerang.meongnyangerang.dev.DataConstant.AREAS;
import static com.meongnyangerang.meongnyangerang.dev.DataConstant.REPORT_REASON;
import static com.meongnyangerang.meongnyangerang.dev.DataConstant.REQUIRE_COUNT;
import static com.meongnyangerang.meongnyangerang.dev.DataConstant.TOWNS_BY_AREA;
import static com.meongnyangerang.meongnyangerang.dev.DataGeneratorUtils.generateRealisticAccommodationDescription;
import static com.meongnyangerang.meongnyangerang.dev.DataGeneratorUtils.generateRealisticAccommodationName;
import static com.meongnyangerang.meongnyangerang.dev.DataGeneratorUtils.generateRealisticRoomDesc;
import static com.meongnyangerang.meongnyangerang.dev.DataGeneratorUtils.generateRealisticRoomName;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import com.meongnyangerang.meongnyangerang.domain.AccommodationRoomDocument;
import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import com.meongnyangerang.meongnyangerang.domain.accommodation.AccommodationDocument;
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
import com.meongnyangerang.meongnyangerang.domain.review.ReportStatus;
import com.meongnyangerang.meongnyangerang.domain.review.ReporterType;
import com.meongnyangerang.meongnyangerang.domain.review.Review;
import com.meongnyangerang.meongnyangerang.domain.review.ReviewImage;
import com.meongnyangerang.meongnyangerang.domain.review.ReviewReport;
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
import com.meongnyangerang.meongnyangerang.repository.ReviewReportRepository;
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
import com.meongnyangerang.meongnyangerang.service.image.S3FileService;
import java.io.IOException;
import java.util.function.Function;
import java.util.stream.Collectors;
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
  private final ReviewReportRepository reviewReportRepository;

  private final S3FileService s3FileService;
  private final ElasticsearchClient elasticsearchClient;

  private List<String> imageUrls;

  @Transactional
  public Map<String, Object> generateData(DummyDataGenerateRequest request) {
    imageUrls = s3FileService.getAllImageUrls();
    Faker faker = new Faker(new Locale(DataConstant.LOCALE));
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
      List<User> users = createUsers(
          request.getUserCount(), faker, random, savedUserEmails, usedNicknames);
      userRepository.saveAll(users);
      log.debug("Created {} users", users.size());

      // 2. 호스트 생성 (숙소 개수만큼)
      List<Host> hosts = createHosts(
          request.getAccommodationCount(), faker, random, savedHostEmails, usedNicknames);
      hostRepository.saveAll(hosts);
      log.debug("Created {} hosts", hosts.size());

      // 3. 숙소 생성 (호스트당 1개)
      List<Accommodation> accommodations = createAccommodations(request, faker, random, hosts);
      accommodationRepository.saveAll(accommodations);
      log.debug("Created {} accommodations", accommodations.size());

      // 4. 숙소 이미지 생성 (숙소당 정확히 4개)
      List<AccommodationImage> accommodationImages = createAccommodationImages(
          accommodations, random);
      accommodationImageRepository.saveAll(accommodationImages);
      log.debug("Created {} accommodationImages", accommodationImages.size());

      // 5. 숙소 애완동물 유형 설정
      List<AllowPet> allowPets = createAllowPets(accommodations, random);
      allowPetRepository.saveAll(allowPets);
      log.debug("Created {} allowPets", allowPets.size());

      // 6. 숙소 편의시설 설정
      List<AccommodationFacility> accommodationFacilities = createAccommodationFacilities(
          accommodations, random);
      accommodationFacilityRepository.saveAll(accommodationFacilities);
      log.debug("Created {} accommodationFacilities", accommodationFacilities.size());

      // 7. 숙소 애완동물 편의시설 설정
      List<AccommodationPetFacility> accommodationPetFacilities = createAccommodationPetFacilities(
          accommodations, random);
      accommodationPetFacilityRepository.saveAll(accommodationPetFacilities);
      log.debug("Created {} accommodationPetFacilities", accommodationPetFacilities.size());

      // 8. 객실 생성
      List<Room> rooms = createRooms(request, accommodations, random);
      roomRepository.saveAll(rooms);
      log.debug("Created {} rooms", rooms.size());

      // 9. 객실 해시태그 설정
      List<Hashtag> roomHashtags = createRoomHashtags(rooms, random);
      hashtagRepository.saveAll(roomHashtags);
      log.debug("Created {} roomHashtags", roomHashtags.size());

      // 10. 객실 편의시설 설정
      List<RoomFacility> roomFacilities = createRoomFacilities(rooms, random);
      roomFacilityRepository.saveAll(roomFacilities);
      log.debug("Created {} roomFacilities", roomFacilities.size());

      // 11. 객실 애완동물 시설 설정
      List<RoomPetFacility> roomPetFacilities = createRoomPetFacilities(rooms, random);
      roomPetFacilityRepository.saveAll(roomPetFacilities);
      log.debug("Created {} roomPetFacilities", roomPetFacilities.size());

      // 12. 예약 생성
      List<Reservation> reservations = createReservations(
          request.getReservationCount(), rooms, random, users, faker);
      reservationRepository.saveAll(reservations);
      log.debug("Created {} roomPetFacilities", roomPetFacilities.size());

      // 13. 리뷰 생성
      List<Review> reviews = createAndSaveReviews(
          request.getReviewCount(), reservations, random, accommodations);

      // 14. 리뷰 신고 생성
      List<ReviewReport> reports = createReports(reviews, users, random);
      reviewReportRepository.saveAll(reports);
      log.debug("Created {} roomPetFacilities", roomPetFacilities.size());

      // 각 엔티티 간 관계 매핑을 위한 맵 생성
      Map<Long, List<AllowPet>> allowPetsByAccId = allowPets.stream()
          .collect(Collectors.groupingBy(pet -> pet.getAccommodation().getId()));

      Map<Long, List<AccommodationPetFacility>> accPetFacilitiesByAccId = accommodationPetFacilities.stream()
          .collect(Collectors.groupingBy(f -> f.getAccommodation().getId()));

      Map<Long, List<AccommodationFacility>> accFacilitiesByAccId = accommodationFacilities.stream()
          .collect(Collectors.groupingBy(f -> f.getAccommodation().getId()));

      Map<Long, List<RoomPetFacility>> roomPetFacilitiesByRoomId = roomPetFacilities.stream()
          .collect(Collectors.groupingBy(f -> f.getRoom().getId()));

      Map<Long, List<RoomFacility>> roomFacilitiesByRoomId = roomFacilities.stream()
          .collect(Collectors.groupingBy(f -> f.getRoom().getId()));

      Map<Long, List<Hashtag>> hashtagsByRoomId = roomHashtags.stream()
          .collect(Collectors.groupingBy(h -> h.getRoom().getId()));

      // 최적화된 벌크 인덱싱
      bulkIndexToElasticsearch(
          accommodations, rooms,
          allowPetsByAccId, accPetFacilitiesByAccId, accFacilitiesByAccId,
          roomPetFacilitiesByRoomId, roomFacilitiesByRoomId, hashtagsByRoomId
      );
      log.debug("Success save indexes");

      // 결과 집계
      result.put("success", true);
      result.put("userCount", users.size());
      result.put("hostCount", hosts.size());
      result.put("accommodationCount", accommodations.size());
      result.put("roomCount", rooms.size());
      result.put("reservationCount", reservations.size());
      result.put("reviewCount", reviews.size());
      result.put("reportCount", reports.size());
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
      int userCount,
      Faker faker,
      Random random,
      List<String> savedUserEmails,
      Set<String> usedNicknames
  ) {
    List<User> users = new ArrayList<>();
    Set<String> usedUserEmails = new HashSet<>(savedUserEmails); // 중복 닉네임 방지를 위한 Set

    for (int i = 0; i < userCount; i++) {
      String email;
      String nickname;

      do { // 중복되지 않는 이메일 생성
        email = DataGeneratorUtils.generateEnglishEmail(random); // 무작위 이메일 생성
      } while (usedUserEmails.contains(email));
      usedUserEmails.add(email);

      do { // 중복되지 않는 닉네임 생성
        nickname = faker.name().firstName() + random.nextInt(100000); // 무작이 이름 생성
      } while (usedNicknames.contains(nickname));
      usedNicknames.add(nickname);

      User user = User.builder()
          .email(email)
          .nickname(nickname)
          .password(passwordEncoder.encode(DataConstant.USER_PASSWORD))
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
      int accommodationCount,
      Faker faker,
      Random random,
      List<String> savedHostEmails,
      Set<String> usedNicknames
  ) {
    List<Host> hosts = new ArrayList<>();
    Set<String> usedEmails = new HashSet<>(savedHostEmails);

    for (int i = 0; i < accommodationCount; i++) {
      String email;
      String nickname;

      do { // 중복되지 않는 이메일 생성
        email = DataGeneratorUtils.generateEnglishEmail(random);
      } while (usedEmails.contains(email));
      usedEmails.add(email);

      do { // 중복되지 않는 닉네임 생성
        nickname = faker.name().firstName() + random.nextInt(100000);
      } while (usedNicknames.contains(nickname));
      usedNicknames.add(nickname);

      Host host = Host.builder()
          .email(email)
          .name(faker.name().fullName().replaceAll("\\s+", ""))
          .nickname(nickname)
          .password(passwordEncoder.encode(DataConstant.HOST_PASSWORD))
          .profileImageUrl(
              random.nextBoolean() ? imageUrls.get(random.nextInt(imageUrls.size())) : null)
          .businessLicenseImageUrl(imageUrls.get(random.nextInt(imageUrls.size())))
          .submitDocumentImageUrl(imageUrls.get(random.nextInt(imageUrls.size())))
          .phoneNumber(createRandomPhoneNumber(faker))
          .status(HostStatus.ACTIVE)
          .role(Role.ROLE_HOST)
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

  private List<Review> createReviews(
      int reviewCount,
      List<Reservation> completedReservations,
      Random random
  ) {
    List<Review> reviews = new ArrayList<>();
    // 리뷰 생성 비율 적용 (기본값은 0.7, 즉 70%의 완료된 예약에 리뷰가 있음)
    // 완료된 예약 중 리뷰가 있는 비율
    double REVIEW_RATIO = 0.7;
    int maxReviewCount = (int) (completedReservations.size() * REVIEW_RATIO);
    int totalReviewCount = Math.min(reviewCount, maxReviewCount);

    // 리뷰를 작성할 예약을 무작위로 선택
    List<Reservation> reservationsCopy = new ArrayList<>(completedReservations);
    Collections.shuffle(reservationsCopy, random);

    // 배열 범위 체크 - totalReviewCount가 reservationsCopy 크기보다 클 수 없음
    totalReviewCount = Math.min(totalReviewCount, reservationsCopy.size());
    List<Reservation> reservationsForReview = reservationsCopy.subList(0, totalReviewCount);

    for (Reservation reservation : reservationsForReview) {
      Accommodation accommodation = reservation.getRoom().getAccommodation();

      double userRating = 0.5 + random.nextInt(10) * 0.5;
      double petFriendlyRating = 0.5 + random.nextInt(10) * 0.5;
      double avgRating = (userRating + petFriendlyRating) / 2.0;

      // 리뷰 작성 시간 - 체크아웃 후 0~6일 사이
      LocalDateTime checkOutDateTime = reservation.getCheckOutDate()
          .atTime(random.nextInt(13), random.nextInt(59));
      LocalDateTime randomReviewCreatedAt = checkOutDateTime
          .plusDays(random.nextInt(7)); // 체크아웃 당일 ~ 6일 후

      int stayDuration = (int) ChronoUnit.DAYS.between(
          reservation.getCheckInDate(), reservation.getCheckOutDate());

      // 현실적인 리뷰 내용 생성
      String content = DataGeneratorUtils.generateRealisticReviewContent(
          accommodation.getName(), avgRating, stayDuration, random);

      Review review = Review.builder()
          .user(reservation.getUser())
          .accommodation(reservation.getRoom().getAccommodation())
          .reservation(reservation)
          .userRating(userRating)
          .petFriendlyRating(petFriendlyRating)
          .content(content)
          .reportCount(0)
          .createdAt(randomReviewCreatedAt)
          .updatedAt(randomReviewCreatedAt)
          .build();

      reviews.add(review);
    }
    return reviews;
  }

  private List<Reservation> createReservations(
      int reservationCount,
      List<Room> rooms,
      Random random,
      List<User> users,
      Faker faker
  ) {
    List<Reservation> reservations = new ArrayList<>();
    LocalDate now = LocalDate.now();

    // 과거/미래 예약 수 계산
    // 과거 예약 비율
    double PAST_RESERVATION_RATIO = 0.6;
    int pastReservations = (int) (reservationCount * PAST_RESERVATION_RATIO);
    int futureReservations = reservationCount - pastReservations;

    // 각 객실별로 예약 날짜를 관리하기 위한 Map
    Map<Long, Set<LocalDate>> roomBookedDatesMap = new HashMap<>();
    for (Room room : rooms) {
      roomBookedDatesMap.put(room.getId(), new HashSet<>());
    }

    // 과거 예약 생성
    int pastSuccess = 0;
    int pastAttempts = 0;
    int maxPastAttempts = pastReservations * 3; // 최대 시도 횟수 제한

    while (pastSuccess < pastReservations && pastAttempts < maxPastAttempts) {
      pastAttempts++;

      // 객실 선택
      Room room = rooms.get(random.nextInt(rooms.size()));
      User user = users.get(random.nextInt(users.size()));

      // 현재 날짜 기준으로 예약 가능한 기간 설정
      LocalDate minDate = now.minusDays(120); // 과거 120일부터
      LocalDate maxDate = now.plusDays(1); // 어제까지

      // 이 객실의 이미 예약된 날짜들
      Set<LocalDate> bookedDates = roomBookedDatesMap.get(room.getId());

      // 예약 기간 선택 (체크인 날짜, 숙박 일수)
      LocalDate checkInDate = findAvailableDate(minDate, maxDate, bookedDates, random);
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

        // 예약 생성 날짜 계산 (체크인 이전의 랜덤한 날짜)
        LocalDateTime createdAt = checkInDate
            .atTime(random.nextInt(24), random.nextInt(60))
            .minusDays(random.nextInt(30) + 1); // 체크인 최소 1일 전, 최대 30일 전
        LocalDateTime canceledAt = null;
        LocalDateTime updatedAt = createdAt;

        ReservationStatus status = ReservationStatus.COMPLETED;

        // 체크아웃 날짜가 현재보다 이전인 예약 중 10%는 취소 상태로 설정
        if (checkOutDate.isBefore(now) && random.nextInt(10) < 1) {
          status = ReservationStatus.CANCELED;
          canceledAt = createdAt.plusDays(random.nextInt(5) + 1);
          updatedAt = canceledAt;
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
            .reserverName(faker.name().fullName().replaceAll("\\s+", ""))
            .reserverPhoneNumber(createRandomPhoneNumber(faker))
            .hasVehicle(random.nextBoolean())
            .totalPrice(totalPrice)
            .status(status)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .canceledAt(canceledAt)
            .build();

        reservations.add(reservation);
        pastSuccess++;
      }
    }

    // 미래 예약 생성
    int futureSuccess = 0;
    int futureAttempts = 0;
    int maxFutureAttempts = futureReservations * 3;

    while (futureSuccess < futureReservations && futureAttempts < maxFutureAttempts) {
      futureAttempts++;

      // 객실 선택
      Room room = rooms.get(random.nextInt(rooms.size()));
      User user = users.get(random.nextInt(users.size()));

      // 미래 날짜 범위 설정
      LocalDate maxDate = now.plusDays(90); // 향후 90일까지

      // 이 객실의 이미 예약된 날짜들
      Set<LocalDate> bookedDates = roomBookedDatesMap.get(room.getId());

      // 예약 기간 선택
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
          bookedDates.add(date); // 예약된 날짜 맵에 추가
        }
        reservationSlotRepository.saveAll(reservationSlots);

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

        // 예약 생성 날짜 계산 (현재 시점 이전 랜덤한 날짜)
        LocalDateTime createdAt = LocalDateTime.now()
            .minusDays(random.nextInt(30)); // 최대 30일 전에 예약

        // 예약 상태 설정 (미래 예약은 RESERVED 또는 CANCELED)
        ReservationStatus status;
        LocalDateTime canceledAt = null;
        LocalDateTime updatedAt = createdAt;

        if (random.nextInt(10) < 8) {
          status = ReservationStatus.RESERVED; // 80% 확률로 RESERVED
        } else {
          status = ReservationStatus.CANCELED; // 20% 확률로 CANCELED
          canceledAt = createdAt.plusDays(random.nextInt(5) + 1);
          updatedAt = canceledAt;
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
            .reserverName(faker.name().fullName().replaceAll("\\s+", ""))
            .reserverPhoneNumber(createRandomPhoneNumber(faker))
            .hasVehicle(random.nextBoolean())
            .totalPrice(totalPrice)
            .status(status)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .canceledAt(canceledAt)
            .build();

        reservations.add(reservation);
        futureSuccess++;
      }
    }
    return reservations;
  }

  private LocalDate findAvailableDate(
      LocalDate minDate,
      LocalDate maxDate,
      Set<LocalDate> bookedDates,
      Random random
  ) {
    for (int attempt = 0; attempt < DataConstant.MAX_RESERVATION_FIND_DATE_ATTEMPT_COUNT;
        attempt++) {
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
      Random random
  ) {
    List<Room> rooms = new ArrayList<>();

    for (Accommodation accommodation : accommodations) {
      int roomCount = 1 + random.nextInt(request.getRoomCount());

      for (int i = 0; i < roomCount; i++) {
        String name = generateRealisticRoomName(random);
        String description = generateRealisticRoomDesc(name, random);

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
            .imageUrl(roomImageUrl)
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
      String name = generateRealisticAccommodationName(random);
      String area = AREAS.get(random.nextInt(AREAS.size()));
      List<String> towns = TOWNS_BY_AREA.getOrDefault(area, List.of("서구"));
      String town = towns.get(random.nextInt(towns.size()));
      String detailedAddress = faker.address().streetAddress(); // 무작위 도로명 주소
      String description = generateRealisticAccommodationDescription(name, area, random);

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
      int reviewCount,
      List<Reservation> reservations,
      Random random,
      List<Accommodation> accommodations
  ) {
    List<Review> reviews = new ArrayList<>();

    // 예약 중 COMPLETED 상태인 것만 필터링
    List<Reservation> completedReservations = reservations.stream()
        .filter(r -> r.getStatus() == ReservationStatus.COMPLETED)
        .toList();

    if (!completedReservations.isEmpty()) {
      reviews = createReviews(reviewCount, completedReservations, random);
      reviewRepository.saveAll(reviews);
      log.info("Created {} reviews", reviews.size());

      // 리뷰 이미지 생성
      List<ReviewImage> reviewImages = createReviewImages(reviews, random);
      reviewImageRepository.saveAll(reviewImages);
      log.info("Created {} reviewImages", reviewImages.size());

      // 숙소 평점 업데이트
      updateAccommodationRating(accommodations, reviews);
      accommodationRepository.saveAll(accommodations);
    }
    return reviews;
  }

  public List<ReviewReport> createReports(List<Review> reviews, List<User> users, Random random) {
    List<ReviewReport> reports = new ArrayList<>();
    Map<Review, Set<Long>> reviewToReportedUserIds = new HashMap<>();

    // 신고가 많이 달린 리뷰와 적게 달린 리뷰를 구분
    // reportedReviewRatio 비율의 리뷰는 많은 신고(15-20개)를 받음
    // 높은 신고를 받는 리뷰 비율
    double REPORTED_REVIEW_RATIO = 0.1;
    int highlyReportedCount = (int) (reviews.size() * REPORTED_REVIEW_RATIO);

    // 리뷰 목록을 무작위로 섞어서 일부는 많은 신고, 일부는 적은 신고를 받도록 함
    List<Review> reviewsCopy = new ArrayList<>(reviews);
    Collections.shuffle(reviewsCopy, random);

    // 높은 신고를 받을 리뷰들에 대한 처리
    for (int i = 0; i < highlyReportedCount && i < reviewsCopy.size(); i++) {
      Review review = reviews.get(i % reviewsCopy.size());
      Set<Long> usedUserIds = new HashSet<>();

      // 신고 개수는 15-20개 사이에서 랜덤하게 결정
      int highlyReportRandomCount = 15 + random.nextInt(6);
      // 자기 자신의 리뷰는 신고 못하도록
      int actualReportCount = Math.min(highlyReportRandomCount, users.size() - 1);

      // 실제 신고할 사용자 선택 (리뷰 작성자 자신은 제외)
      Long reviewAuthorId = review.getUser().getId();
      List<User> potentialReporters = new ArrayList<>(users.stream()
          .filter(user -> !user.getId().equals(reviewAuthorId))
          .toList());

      // 중복 없이 사용자 선택해서 신고 생성
      Collections.shuffle(potentialReporters, random);
      for (int j = 0; j < actualReportCount && j < potentialReporters.size(); j++) {
        User reporter = potentialReporters.get(j);

        if (usedUserIds.add(reporter.getId())) {
          ReviewReport report = createSingleReviewReport(review, reporter, random);
          reports.add(report);
        }
      }
      // 신고 수 업데이트
      review.setReportCount(usedUserIds.size());
      reviewToReportedUserIds.put(review, usedUserIds);
    }

    // 나머지 리뷰에 대해서는 적은 수의 신고 생성
    for (int i = highlyReportedCount; i < reviewsCopy.size(); i++) {
      Review review = reviewsCopy.get(i);

      // 이미 많이 신고된 리뷰는 건너뜀
      if (reviewToReportedUserIds.containsKey(review)) {
        continue;
      }

      int maxReport = random.nextInt(4); // 0-3개 정도의 적은 신고
      if (maxReport == 0) {
        continue; // 신고가 없는 리뷰도 많이 있음
      }

      Set<Long> usedUserIds = new HashSet<>();
      Long reviewAuthorId = review.getUser().getId();

      // 신고자 선택 (리뷰 작성자 자신은 제외)
      List<User> potentialReporters = new ArrayList<>(users.stream()
          .filter(user -> !user.getId().equals(reviewAuthorId))
          .toList());

      Collections.shuffle(potentialReporters, random);
      for (int j = 0; j < maxReport && j < potentialReporters.size(); j++) {
        User reporter = potentialReporters.get(j);

        if (usedUserIds.add(reporter.getId())) {
          ReviewReport report = createSingleReviewReport(review, reporter, random);
          reports.add(report);
        }
      }

      if (!usedUserIds.isEmpty()) { // 신고가 있는 경우에만 리뷰 신고 수 업데이트
        review.setReportCount(usedUserIds.size());
        reviewToReportedUserIds.put(review, usedUserIds);
      }
    }
    return reports;
  }

  private void bulkIndexToElasticsearch(
      List<Accommodation> accommodations,
      List<Room> rooms,
      Map<Long, List<AllowPet>> allowPetsByAccId,
      Map<Long, List<AccommodationPetFacility>> accPetFacilitiesByAccId,
      Map<Long, List<AccommodationFacility>> accFacilitiesByAccId,
      Map<Long, List<RoomPetFacility>> roomPetFacilitiesByRoomId,
      Map<Long, List<RoomFacility>> roomFacilitiesByRoomId,
      Map<Long, List<Hashtag>> hashtagsByRoomId
  ) {
    log.info("벌크 인덱싱 시작: 숙소 {}개, 객실 {}개", accommodations.size(), rooms.size());

    // 숙소별 객실 매핑
    Map<Long, List<Room>> roomsByAccommodationId = rooms.stream()
        .collect(Collectors.groupingBy(room -> room.getAccommodation().getId()));

    // ES에 색인할 모든 문서를 모으는 부분
    List<BulkOperation> operations = new ArrayList<>();

    // 1. 모든 객실 정보를 모은 후 accommodations 인덱스 문서 생성
    for (Accommodation accommodation : accommodations) {
      Long accommodationId = accommodation.getId();
      List<Room> accRooms = roomsByAccommodationId.getOrDefault(
          accommodationId, Collections.emptyList());

      if (accRooms.isEmpty()) {
        continue;
      }

      // 모든 객실 중 최저가 찾기
      long minPrice = accRooms.stream()
          .mapToLong(Room::getPrice)
          .min()
          .orElse(0);

      // 모든 객실의 반려동물 편의시설 수집
      Set<String> allRoomPetFacilities = accRooms.stream()
          .flatMap(room -> roomPetFacilitiesByRoomId
              .getOrDefault(room.getId(), Collections.emptyList())
              .stream())
          .map(facility -> facility.getType().name())
          .collect(Collectors.toSet());

      // 숙소 관련 정보 수집
      Set<String> allowedPetTypes = getAllowedPetTypesFromMap(allowPetsByAccId, accommodationId);
      Set<String> accommodationPetFacilities = getAccommodationPetFacilitiesFromMap(
          accPetFacilitiesByAccId, accommodationId);

      // AccommodationDocument 생성 시 추가 필드 포함
      AccommodationDocument accommodationDoc = AccommodationDocument.builder()
          .id(accommodation.getId())
          .name(accommodation.getName())
          .thumbnailUrl(accommodation.getThumbnailUrl())
          .price(minPrice) // 최저가 사용
          .totalRating(accommodation.getTotalRating())
          .accommodationPetFacilities(accommodationPetFacilities)
          .allowedPetTypes(allowedPetTypes)
          .roomPetFacilities(allRoomPetFacilities) // 모든 객실의 반려동물 편의시설
          .build();

      // accommodations 인덱스에 추가
      operations.add(new BulkOperation.Builder()
          .index(idx -> idx
              .index("accommodations")
              .id(accommodationId.toString())
              .document(accommodationDoc)
          ).build());

      // 2. 각 객실별 accommodation_room 인덱스 문서 생성
      for (Room room : accRooms) {
        Long roomId = room.getId();

        // 객실별 데이터 준비
        List<AccommodationFacility> accFacilities = accFacilitiesByAccId.getOrDefault(
            accommodationId, Collections.emptyList());
        List<AccommodationPetFacility> accPetFacilities = accPetFacilitiesByAccId.getOrDefault(
            accommodationId, Collections.emptyList());
        List<AllowPet> allowPets = allowPetsByAccId.getOrDefault(accommodationId,
            Collections.emptyList());

        List<RoomFacility> roomFacilities = roomFacilitiesByRoomId.getOrDefault(roomId,
            Collections.emptyList());
        List<RoomPetFacility> roomPetFacilities = roomPetFacilitiesByRoomId.getOrDefault(roomId,
            Collections.emptyList());
        List<Hashtag> hashtags = hashtagsByRoomId.getOrDefault(roomId, Collections.emptyList());

        // 여기도 매퍼를 직접 호출하기 어려울 수 있어 객체를 새로 생성합니다
        AccommodationRoomDocument accRoomDoc = createAccommodationRoomDocument(
            accommodation, room, accFacilities, accPetFacilities,
            roomFacilities, roomPetFacilities, hashtags, allowPets);

        // accommodation_rooms 인덱스에 추가할 문서 작업 추가
        operations.add(new BulkOperation.Builder()
            .index(idx -> idx
                .index("accommodation_room")
                .id(accommodationId + "_" + roomId)
                .document(accRoomDoc)
            ).build());
      }
    }

    // 3. 벌크 요청 실행
    try {
      if (!operations.isEmpty()) {
        BulkRequest bulkRequest = new BulkRequest.Builder()
            .operations(operations)
            .build();

        elasticsearchClient.bulk(bulkRequest);
        log.info("총 {}개 문서 벌크 인덱싱 완료", operations.size());
      }
    } catch (IOException e) {
      log.error("Elasticsearch 벌크 인덱싱 실패", e);
      throw new RuntimeException("Elasticsearch 벌크 인덱싱 실패", e);
    }
  }

  private Set<String> getAllowedPetTypesFromMap(
      Map<Long, List<AllowPet>> map,
      Long accommodationId
  ) {
    return map.getOrDefault(accommodationId, Collections.emptyList())
        .stream().map(AllowPet::getPetType).map(Enum::name).collect(Collectors.toSet());
  }

  private Set<String> getAccommodationPetFacilitiesFromMap(
      Map<Long, List<AccommodationPetFacility>> map,
      Long accommodationId
  ) {
    return map.getOrDefault(accommodationId, Collections.emptyList())
        .stream().map(AccommodationPetFacility::getType).map(Enum::name)
        .collect(Collectors.toSet());
  }

  private Set<String> getRoomPetFacilitiesFromMap(
      Map<Long, List<RoomPetFacility>> map,
      Long roomId
  ) {
    return map.getOrDefault(roomId, Collections.emptyList())
        .stream().map(RoomPetFacility::getType).map(Enum::name).collect(Collectors.toSet());
  }

  // AccommodationRoomDocument 생성
  private AccommodationRoomDocument createAccommodationRoomDocument(
      Accommodation accommodation,
      Room room,
      List<AccommodationFacility> accFacilities,
      List<AccommodationPetFacility> accPetFacilities,
      List<RoomFacility> roomFacilities,
      List<RoomPetFacility> roomPetFacilities,
      List<Hashtag> hashtags,
      List<AllowPet> allowPets
  ) {
    return AccommodationRoomDocument.builder()
        .id(accommodation.getId() + "_" + room.getId())
        .accommodationId(accommodation.getId())
        .roomId(room.getId())
        .accommodationName(accommodation.getName())
        .roomName(room.getName())
        .address(accommodation.getAddress())
        .thumbnailUrl(accommodation.getThumbnailUrl())
        .accommodationType(accommodation.getType())
        .totalRating(accommodation.getTotalRating())
        .price(room.getPrice())
        .standardPeopleCount(room.getStandardPeopleCount())
        .maxPeopleCount(room.getMaxPeopleCount())
        .standardPetCount(room.getStandardPetCount())
        .maxPetCount(room.getMaxPetCount())
        .accommodationFacilities(extractEnumNames(accFacilities, AccommodationFacility::getType))
        .accommodationPetFacilities(
            extractEnumNames(accPetFacilities, AccommodationPetFacility::getType))
        .roomFacilities(extractEnumNames(roomFacilities, RoomFacility::getType))
        .roomPetFacilities(extractEnumNames(roomPetFacilities, RoomPetFacility::getType))
        .hashtags(extractEnumNames(hashtags, Hashtag::getType))
        .allowPets(extractEnumNames(allowPets, AllowPet::getPetType))
        .build();
  }

  private <T> List<String> extractEnumNames(List<T> list, Function<T, Enum<?>> extractor) {
    if (list == null || list.isEmpty()) {
      return Collections.emptyList();
    }
    return list.stream()
        .map(extractor)
        .map(Enum::name)
        .toList();
  }

  private ReviewReport createSingleReviewReport(Review review, User reporter, Random random) {
    // 신고 이유와 상태를 무작위로 선택
    String reason = REPORT_REASON.get(random.nextInt(REPORT_REASON.size()));
    ReportStatus status = randomEnum(ReportStatus.class, random);

    // 신고 시간은 리뷰 작성 이후로 설정
    LocalDateTime reportCreatedAt = review.getCreatedAt()
        .plusHours(random.nextInt(24))
        .plusDays(random.nextInt(30)); // 리뷰 작성 후 최대 30일 이내

    // 이미지 유무는 50% 확률로 결정
    String evidenceImageUrl = random.nextBoolean() ?
        imageUrls.get(random.nextInt(imageUrls.size())) : null;

    return ReviewReport.builder()
        .review(review)
        .reporterId(reporter.getId())
        .type(ReporterType.USER) // 사용자에 의한 신고
        .reason(reason)
        .evidenceImageUrl(evidenceImageUrl)
        .status(status)
        .createdAt(reportCreatedAt)
        .build();
  }

  public int createRandomCount(Random random, int typeSize) {
    return Math.min(REQUIRE_COUNT + random.nextInt(typeSize), typeSize);
  }

  private String createRandomPhoneNumber(Faker faker) {
    return "010-" + faker.number().digits(4) + "-" + faker.number().digits(4);
  }

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

  public long getReportCount() {
    return reviewReportRepository.count();
  }
}