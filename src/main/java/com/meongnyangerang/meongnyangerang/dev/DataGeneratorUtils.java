package com.meongnyangerang.meongnyangerang.dev;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import net.datafaker.Faker;

/**
 * 현실적인 더미 데이터 생성을 위한 유틸리티 클래스
 */
public class DataGeneratorUtils {

  /**
   * 현실적인 숙소 이름 생성
   */
  public static String generateRealisticAccommodationName(Random random) {
    String prefix = DataConstant.ACCOMMODATION_NAME_PREFIXES.get(
        random.nextInt(DataConstant.ACCOMMODATION_NAME_PREFIXES.size()));
    String suffix = DataConstant.ACCOMMODATION_NAME_SUFFIXES.get(
        random.nextInt(DataConstant.ACCOMMODATION_NAME_SUFFIXES.size()));

    // 50%의 확률로 지역명을 앞에 추가
    if (random.nextBoolean()) {
      String area = DataConstant.AREAS.get(
          random.nextInt(DataConstant.AREAS.size()));
      return area + " " + prefix + " " + suffix;
    }

    return prefix + " " + suffix;
  }

  /**
   * 현실적인 숙소 설명 생성
   */
  public static String generateRealisticAccommodationDescription(String name, String area,
      Random random) {
    String template = DataConstant.ACCOMMODATION_DESC_TEMPLATES.get(
        random.nextInt(DataConstant.ACCOMMODATION_DESC_TEMPLATES.size()));
    String feature = DataConstant.ACCOMMODATION_FEATURES.get(
        random.nextInt(DataConstant.ACCOMMODATION_FEATURES.size()));

    return template
        .replace("{name}", name)
        .replace("{area}", area)
        .replace("{feature}", feature);
  }

  /**
   * 현실적인 객실 이름 생성
   */
  public static String generateRealisticRoomName(Random random) {
    String prefix = DataConstant.ROOM_NAME_PREFIXES.get(
        random.nextInt(DataConstant.ROOM_NAME_PREFIXES.size()));
    String suffix = DataConstant.ROOM_NAME_SUFFIXES.get(
        random.nextInt(DataConstant.ROOM_NAME_SUFFIXES.size()));

    return prefix + " " + suffix;
  }

  /**
   * 현실적인 객실 설명 생성
   */
  public static String generateRealisticRoomDesc(String name, Random random) {
    String template = DataConstant.ROOM_DESC_TEMPLATES.get(
        random.nextInt(DataConstant.ROOM_DESC_TEMPLATES.size()));
    String feature = DataConstant.ROOM_FEATURES.get(
        random.nextInt(DataConstant.ROOM_FEATURES.size()));

    return template
        .replace("{name}", name)
        .replace("{feature}", feature);
  }

  /**
   * 영어로 이메일 생성
   */
  public static String generateEnglishEmail(Random random) {
    // 영어 Locale으로 임시 Faker 생성
    Faker enFaker = new Faker(new Locale("en"));

    // 영어로 이메일 생성
    String email = enFaker.internet().emailAddress();

    // 중복 방지를 위해 선택적으로 이메일에 랜덤 숫자 추가
    if (random.nextBoolean()) {
      email = email.replace("@", random.nextInt(10000) + "@");
    }

    return email;
  }

  public static String generateRealisticReviewContent(
      String accommodationName,
      double rating, // 평균 평점 (1.0~5.0)
      int stayDuration, // 숙박 일수
      Random random
  ) {
    // 평점에 따라 템플릿 유형 선택
    List<String> templates;
    List<String> conclusions;
    List<String> petComments;
    boolean includeStaffComment = random.nextBoolean();
    String staffComment = "";

    if (rating >= 4.0) { // 높은 평점
      templates = DataConstant.REVIEW_TEMPLATES.subList(0, 5); // 긍정적 템플릿
      conclusions = DataConstant.POSITIVE_CONCLUSIONS;
      petComments = DataConstant.PET_POSITIVE_COMMENTS;
      if (includeStaffComment) {
        staffComment = DataConstant.STAFF_COMMENTS.get(
            random.nextInt(DataConstant.STAFF_COMMENTS.size()));
      }
    } else if (rating >= 2.5) { // 중간 평점
      templates = DataConstant.REVIEW_TEMPLATES.subList(5, 10); // 중립적 템플릿
      conclusions = DataConstant.NEUTRAL_CONCLUSIONS;
      petComments = DataConstant.PET_GENERAL_COMMENTS;
    } else { // 낮은 평점
      templates = DataConstant.REVIEW_TEMPLATES.subList(10, 15); // 부정적 템플릿
      conclusions = DataConstant.NEGATIVE_CONCLUSIONS;
      petComments = DataConstant.PET_NEGATIVE_COMMENTS;
      if (includeStaffComment) {
        staffComment = DataConstant.STAFF_NEGATIVE_COMMENTS.get(
            random.nextInt(DataConstant.STAFF_NEGATIVE_COMMENTS.size()));
      }
    }

    // 템플릿과 다양한 구성 요소 선택
    String template = templates.get(random.nextInt(templates.size()));
    String petType = DataConstant.PET_TYPES.get(
        random.nextInt(DataConstant.PET_TYPES.size()));
    String stayDurationStr = stayDuration > 0 && stayDuration <= 5 ?
        String.valueOf(stayDuration) :
        DataConstant.STAY_DURATIONS.get(
            random.nextInt(DataConstant.STAY_DURATIONS.size()));
    String positivePoint = DataConstant.POSITIVE_POINTS.get(
        random.nextInt(DataConstant.POSITIVE_POINTS.size()));
    String negativePoint = DataConstant.NEGATIVE_POINTS.get(
        random.nextInt(DataConstant.NEGATIVE_POINTS.size()));
    String petPositive = DataConstant.PET_POSITIVE_COMMENTS.get(
        random.nextInt(DataConstant.PET_POSITIVE_COMMENTS.size()));
    String petNegative = DataConstant.PET_NEGATIVE_COMMENTS.get(
        random.nextInt(DataConstant.PET_NEGATIVE_COMMENTS.size()));
    String petComment = petComments.get(random.nextInt(petComments.size()));
    String conclusion = conclusions.get(random.nextInt(conclusions.size()));
    String neutralConclusion = DataConstant.NEUTRAL_CONCLUSIONS.get(
        random.nextInt(DataConstant.NEUTRAL_CONCLUSIONS.size()));
    String negativeConclusion = DataConstant.NEGATIVE_CONCLUSIONS.get(
        random.nextInt(DataConstant.NEGATIVE_CONCLUSIONS.size()));

    // 템플릿에 값 채우기
    String result = template
        .replace("{accommodationName}", accommodationName)
        .replace("{petType}", petType)
        .replace("{stayDuration}", stayDurationStr)
        .replace("{positivePoint}", positivePoint)
        .replace("{negativePoint}", negativePoint)
        .replace("{petPositive}", petPositive)
        .replace("{petNegative}", petNegative)
        .replace("{petComment}", petComment)
        .replace("{staffComment}", staffComment)
        .replace("{conclusion}", conclusion)
        .replace("{neutralConclusion}", neutralConclusion)
        .replace("{negativeConclusion}", negativeConclusion);

    // 만약 템플릿에 중괄호({})가 있다면 빈 문자열로 대체
    result = result.replaceAll("\\{[^}]*\\}", "");

    return result;
  }

  // 인스턴스화 방지
  private DataGeneratorUtils() {
    throw new UnsupportedOperationException("유틸리티 클래스는 인스턴스화할 수 없습니다.");
  }
}
