package com.meongnyangerang.meongnyangerang.dev;

import java.util.List;
import java.util.Map;

/**
 * 더미 데이터 생성에 사용되는 상수값들을 모아놓은 클래스
 */
public class DataConstant {

  // 이미지 URL 목록 - 실제 서비스에서는 S3에 있는 이미지 URL로 대체
  public static final List<String> IMAGE_URLS = List.of(
      "https://s3.ap-northeast-2.amazonaws.com/jjae-3.3-storage/image/72015682-b942-44da-825d-89d3b0004b99.png.",
      "https://s3.ap-northeast-2.amazonaws.com/jjae-3.3-storage/image/ef298fbb-d4f6-4510-9861-0465fc4ac4b6.png",
      "https://s3.ap-northeast-2.amazonaws.com/jjae-3.3-storage/image/199836da-d9fa-4053-a080-62600c350404.png",
      "https://s3.ap-northeast-2.amazonaws.com/jjae-3.3-storage/image/199836da-d9fa-4053-a080-62600c350404.png",
      "https://s3.ap-northeast-2.amazonaws.com/jjae-3.3-storage/image/f6706acc-97be-4be5-9f84-9331cc604ebd.png"
  );

  // 지역 정보
  public static final List<String> AREAS = List.of(
      "서울", "부산", "제주", "인천", "대구", "광주", "대전", "울산", "경기", "강원");

  public static final Map<String, List<String>> TOWNS_BY_AREA = Map.of(
      "서울", List.of("강남구", "서초구", "송파구", "마포구", "종로구"),
      "부산", List.of("해운대구", "수영구", "남구", "부산진구", "동래구"),
      "제주", List.of("제주시", "서귀포시", "애월읍", "조천읍", "한림읍"),
      "인천", List.of("중구", "연수구", "남동구", "서구", "계양구"),
      "대구", List.of("중구", "수성구", "달서구", "동구", "북구")
  );

  // 숙소 이름 관련 상수
  public static final List<String> ACCOMMODATION_NAME_PREFIXES = List.of(
      "그랜드", "로얄", "호텔", "리조트", "스위트", "팰리스", "풀빌라", "펜션", "시티", "하이엔드",
      "프리미엄", "럭셔리", "포레스트", "파크", "비치", "마운틴", "레이크", "시티뷰", "파노라마", "가든"
  );

  public static final List<String> ACCOMMODATION_NAME_SUFFIXES = List.of(
      "호텔", "리조트", "스테이", "레지던스", "스위트", "빌라", "펜션", "인", "하우스", "코티지",
      "로프트", "타워", "캐슬", "맨션", "팰리스", "히든 하우스", "비치 하우스", "시티 호텔", "힐 리조트"
  );

  // 객실 이름 관련 상수
  public static final List<String> ROOM_NAME_PREFIXES = List.of(
      "디럭스", "스탠다드", "슈페리어", "익스클루시브", "이그제큐티브", "프리미엄", "스위트", "패밀리", "럭셔리",
      "코너", "프레지덴셜", "로얄", "허니문", "더블", "트윈", "싱글", "쿼드", "스튜디오", "파노라마", "그랜드"
  );

  public static final List<String> ROOM_NAME_SUFFIXES = List.of(
      "룸", "스위트", "더블룸", "싱글룸", "트윈룸", "패밀리룸", "디럭스룸", "스튜디오", "테라스룸", "오션뷰",
      "마운틴뷰", "시티뷰", "가든뷰", "풀억세스룸", "프라이빗룸", "킹룸", "퀸룸", "스페셜룸", "펫프렌들리룸"
  );

  // 숙소 설명 관련 상수
  public static final List<String> ACCOMMODATION_DESC_TEMPLATES = List.of(
      "{area}의 중심부에 위치한 {name}은(는) 최고의 서비스와 시설을 갖추고 있습니다. 모든 객실에서 {feature}을(를) 즐기실 수 있으며, 반려동물과 함께 특별한 추억을 만들어보세요.",
      "{name}에서 특별한 휴가를 즐겨보세요. {area}의 아름다운 전경과 함께 편안한 휴식을 제공합니다. 반려동물 친화적인 환경에서 {feature}을(를) 경험해보세요.",
      "반려동물과 함께하는 여행에 최적화된 {name}입니다. {area}의 {feature}과(와) 가까우며, 모던한 인테리어와 청결한 환경을 자랑합니다.",
      "{area} 최고의 위치에 자리한 {name}에서 특별한 시간을 보내세요. {feature}을(를) 갖춘 객실과 반려동물을 위한 다양한 편의시설이 준비되어 있습니다.",
      "자연과 도시가 어우러진 {area}에 위치한 {name}은(는) 반려동물 동반 고객을 위한 최적의 선택입니다. {feature}을(를) 포함한 다양한 서비스를 경험해보세요."
  );

  public static final List<String> ACCOMMODATION_FEATURES = List.of(
      "아름다운 전망", "럭셔리한 인테리어", "최신식 시설", "넓은 공간", "프라이빗 정원",
      "야외 테라스", "수영장", "스파 시설", "레스토랑", "카페", "바비큐 시설", "피트니스 센터",
      "반려동물 놀이터", "반려동물 케어 서비스", "반려동물 동반 산책로", "무료 와이파이", "주차장",
      "무료 조식", "컨시어지 서비스", "셔틀 서비스"
  );

  // 객실 설명 관련 상수
  public static final List<String> ROOM_DESC_TEMPLATES = List.of(
      "넓고 쾌적한 {name} 객실에서 편안한 휴식을 즐기세요. {feature}을(를) 갖추고 있으며, 반려동물과 함께 머물기에 최적화되어 있습니다.",
      "모던한 디자인의 {name}에서 특별한 밤을 보내세요. {feature}이(가) 제공되며, 반려동물을 위한 편의용품도 준비되어 있습니다.",
      "{name}은(는) 반려동물과 함께하는 여행객을 위해 특별히 디자인되었습니다. {feature}을(를) 즐기며 소중한 추억을 만들어보세요.",
      "세련된 인테리어의 {name}에서 품격 있는 휴식을 경험하세요. {feature}을(를) 갖추고 있어 반려동물과 함께 편안한 시간을 보낼 수 있습니다.",
      "반려동물 친화적인 {name} 객실은 {feature}을(를) 제공합니다. 넓은 공간에서 반려동물과 함께 즐거운 시간을 보내세요."
  );

  public static final List<String> ROOM_FEATURES = List.of(
      "킹사이즈 침대", "트윈 침대", "퀸사이즈 침대", "테라스", "발코니", "욕조", "레인샤워",
      "미니바", "커피머신", "55인치 스마트 TV", "블루투스 스피커", "반려동물 베드", "반려동물 식기",
      "반려동물 배변패드", "반려동물 장난감", "반려동물 목욕 시설", "반려동물 산책 용품",
      "무료 와이파이", "냉장고", "전자레인지", "에어컨", "난방 시설", "슬리퍼", "목욕 가운"
  );

  // 리뷰 템플릿
  public static final List<String> REVIEW_TEMPLATES = List.of(
      // 긍정적인 리뷰 템플릿 (높은 평점)
      "{accommodationName}에서 {petType}과(와) 함께 {stayDuration}박 머물렀어요. {positivePoint}이(가) 정말 좋았습니다. {petPositive} {conclusion}",
      "{petType}을(를) 데리고 간 첫 여행이었는데 {accommodationName}에서 좋은 경험을 했습니다. {positivePoint}. {petPositive} {conclusion}",
      "{accommodationName}은(는) {petType} 동반 여행객에게 완벽한 선택이었습니다. {positivePoint}. {staffComment} {conclusion}",
      "정말 만족스러운 숙박이었습니다. {positivePoint}이(가) 인상적이었고, {petType}도 편안하게 지냈어요. {petPositive} {conclusion}",
      "{petType}과(와) 함께하는 여행에 최적화된 숙소에요. {positivePoint}. {staffComment} {petPositive} {conclusion}",

      // 중립적인 리뷰 템플릿 (중간 평점)
      "{accommodationName}에서 {stayDuration}박 묵었습니다. {positivePoint}은(는) 좋았지만, {negativePoint}은(는) 아쉬웠어요. {petComment}",
      "전체적으로는 괜찮은 숙소였습니다. {positivePoint}. 하지만 {negativePoint}. {petComment} {neutralConclusion}",
      "{petType}과(와) 여행하기에 나쁘지 않은 곳이에요. {positivePoint}. 다만 {negativePoint}이 아쉬웠습니다. {neutralConclusion}",
      "위치가 좋고 {positivePoint}이(가) 좋았습니다. 그러나 {negativePoint}. {petComment} {neutralConclusion}",
      "{stayDuration}박 동안 묵었는데 평범했어요. {positivePoint}은(는) 좋았지만, {negativePoint}. {petComment}",

      // 부정적인 리뷰 템플릿 (낮은 평점)
      "아쉬운 점이 많았습니다. {negativePoint}. {petNegative} {negativeConclusion}",
      "{petType}과(와) 함께 숙박하기에는 불편한 점이 많았어요. {negativePoint}. {petNegative} {negativeConclusion}",
      "기대했던 것보다 실망스러웠습니다. {negativePoint}. {staffNegative} {negativeConclusion}",
      "{accommodationName}의 {negativePoint}이(가) 매우 불편했습니다. {petNegative} {staffNegative} {negativeConclusion}",
      "반려동물 동반 숙소라고 하기에는 부족한 점이 많아요. {negativePoint}. {petNegative} {negativeConclusion}"
  );

  // 반려동물 종류
  public static final List<String> PET_TYPES = List.of(
      "강아지", "고양이", "소형견", "중형견", "대형견", "반려견", "반려묘", "애완동물", "애견", "반려동물"
  );

  // 체류 기간
  public static final List<String> STAY_DURATIONS = List.of(
      "1", "2", "3", "4", "5", "주말", "연휴"
  );

  // 긍정적인 측면
  public static final List<String> POSITIVE_POINTS = List.of(
      "객실이 넓고 쾌적한 점", "청결도", "조용한 환경", "친절한 직원들", "아름다운 전망", "반려동물 어메니티",
      "반려동물 놀이 공간", "위치", "주변 산책로", "조식", "객실 시설", "안락한 침대", "수건과 침구의 청결함",
      "반려동물 동반 식당", "주차 편의성", "안전한 환경", "프라이빗한 공간", "객실 인테리어", "주변 관광지와의 접근성",
      "반려동물 친화적인 분위기", "넓은 발코니", "주변 환경", "가성비", "반려동물 케어 서비스"
  );

  // 부정적인 측면
  public static final List<String> NEGATIVE_POINTS = List.of(
      "객실이 생각보다 좁은 점", "청소 상태가 미흡한 점", "소음이 심한 점", "직원들의 응대가 불친절한 점", "주차가 불편한 점",
      "반려동물 시설이 부족한 점", "가격 대비 시설이 부족한 점", "체크인/체크아웃 과정이 복잡한 점", "냄새가 나는 점",
      "에어컨/난방 시설이 제대로 작동하지 않는 점", "와이파이가 느린 점", "반려동물 추가 요금이 비싼 점",
      "주변 산책로가 부족한 점", "객실 내 시설 고장", "욕실 시설이 노후된 점", "방음이 잘 안 되는 점",
      "반려동물을 위한 공간이 협소한 점", "침구류의 청결도가 떨어지는 점", "주변 편의시설 부족", "조식 품질이 낮은 점"
  );

  // 반려동물 관련 긍정적 코멘트
  public static final List<String> PET_POSITIVE_COMMENTS = List.of(
      "반려동물 용품(간식, 배변패드 등)도 준비되어 있어 좋았어요.",
      "반려동물과 함께 이용할 수 있는 공간이 잘 마련되어 있었습니다.",
      "반려동물도 편안하게 지낼 수 있는 환경이었습니다.",
      "반려동물 동반 손님에 대한 배려가 느껴졌습니다.",
      "반려동물 장난감과 침대가 준비되어 있어 세심한 배려가 느껴졌어요.",
      "반려동물 동반 고객을 위한 별도의 서비스가 인상적이었습니다.",
      "반려동물과 함께 산책하기 좋은 코스가 가까이 있어 좋았습니다.",
      "반려동물 전용 수건과 식기가 준비되어 있어 편리했어요.",
      "반려동물도 스트레스 없이 즐겁게 지냈습니다.",
      "반려동물을 진심으로 환영하는 분위기가 좋았습니다."
  );

  // 반려동물 관련 부정적 코멘트
  public static final List<String> PET_NEGATIVE_COMMENTS = List.of(
      "반려동물을 위한 공간이 생각보다 좁았습니다.",
      "반려동물 어메니티가 홍보와 달리 거의 없었어요.",
      "반려동물 동반 고객에 대한 추가 요금이 과하게 비쌌습니다.",
      "직원들이 반려동물에 대해 불편한 기색을 보였어요.",
      "반려동물과 산책할 공간이 매우 제한적이었습니다.",
      "반려동물 친화적이라고 하기에는 시설이 많이 부족했어요.",
      "다른 투숙객들이 반려동물에 대해 불편해하는 분위기였습니다.",
      "반려동물 배변 처리를 위한 시설이 없어 불편했어요.",
      "반려동물 소음에 대한 항의가 있어 스트레스 받았습니다.",
      "반려동물과 함께 이용할 수 있는 공간에 제한이 많았어요."
  );

  // 일반 반려동물 코멘트
  public static final List<String> PET_GENERAL_COMMENTS = List.of(
      "반려동물도 편안하게 지냈습니다.",
      "반려동물과 함께 여행하기에 괜찮은 곳이었어요.",
      "반려동물에 대한 추가 요금은 합리적인 수준이었습니다.",
      "반려동물 동반 투숙객을 위한 기본적인 시설은 갖추고 있었어요.",
      "반려동물과 함께 묵을 수 있는 객실 상태는 양호했습니다.",
      "반려동물 동반 여행객들에게 적당한 환경이었어요.",
      "반려동물을 위한 기본적인 서비스는 제공되었습니다.",
      "반려동물과 함께 지내기에 크게 불편함은 없었어요.",
      "반려동물에 대한 직원들의 태도는 보통 수준이었습니다.",
      "반려동물 동반 여행에 필요한 기본 사항은 충족되었어요."
  );

  // 직원 관련 코멘트
  public static final List<String> STAFF_COMMENTS = List.of(
      "직원분들이 친절하고 반려동물에게도 따뜻하게 대해주셨어요.",
      "체크인 시 반려동물 관련 시설에 대해 상세히 안내해주셔서 좋았습니다.",
      "직원들이 반려동물의 이름을 기억하고 반겨주는 모습이 인상적이었어요.",
      "무엇이든 필요한 것이 있으면 빠르게 도와주셨습니다.",
      "프론트 데스크에서 주변 반려동물 동반 가능한 장소들을 추천해주셨어요."
  );

  // 직원 관련 부정적 코멘트
  public static final List<String> STAFF_NEGATIVE_COMMENTS = List.of(
      "직원들이 반려동물에 대해 불편한 기색을 보였습니다.",
      "체크인 과정에서 반려동물 관련 안내가 부족했어요.",
      "요청사항에 대한 응대가 느리고 불친절했습니다.",
      "직원들이 반려동물 관련 지식이나 서비스 마인드가 부족해 보였어요.",
      "문의사항에 대한 답변이 불명확하고 도움이 되지 않았습니다."
  );

  // 긍정적인 결론
  public static final List<String> POSITIVE_CONCLUSIONS = List.of(
      "다음에도 꼭 재방문하고 싶은 숙소입니다.",
      "반려동물과 함께하는 여행에 강력 추천합니다.",
      "가격 대비 만족도가 매우 높았던 숙소에요.",
      "다른 반려인들에게도 자신 있게 추천할 수 있습니다.",
      "또 방문하고 싶은 좋은 숙소였습니다.",
      "전반적으로 매우 만족스러운 경험이었습니다.",
      "반려동물 동반 여행객에게 최적의 선택이 될 것 같아요.",
      "기대 이상으로 좋은 숙소였습니다.",
      "편안하고 즐거운 시간을 보낼 수 있었어요.",
      "반려동물과의 특별한 추억을 만들 수 있는 곳입니다."
  );

  // 중립적인 결론
  public static final List<String> NEUTRAL_CONCLUSIONS = List.of(
      "개선점이 있지만 나쁘지 않은 숙소였습니다.",
      "가격 대비 적당한 수준의 서비스였어요.",
      "몇 가지 불편함이 있었지만 전반적으로는 괜찮았습니다.",
      "특별히 인상적이진 않았지만 무난한 숙박이었어요.",
      "반려동물과 함께 묵기에 적절한 수준의 숙소입니다.",
      "기대 대비 평범한 경험이었어요.",
      "장단점이 있는 숙소입니다.",
      "다음에는 다른 숙소도 알아볼 것 같아요.",
      "반려동물과 함께 단기 숙박으로는 괜찮습니다.",
      "크게 불만은 없지만 특별히 추천할 정도는 아니에요."
  );

  // 부정적인 결론
  public static final List<String> NEGATIVE_CONCLUSIONS = List.of(
      "다시 방문하지는 않을 것 같습니다.",
      "반려동물과 함께하는 여행에는 추천하지 않아요.",
      "가격 대비 만족도가 매우 낮았습니다.",
      "다른 반려인들에게 추천하기 어려운 숙소에요.",
      "많은 개선이 필요한 숙소입니다.",
      "기대와 달리 실망스러운 경험이었어요.",
      "반려동물 동반 숙소라고 하기에는 부족한 점이 많습니다.",
      "더 나은 대안을 찾는 것이 좋을 것 같아요.",
      "가격에 비해 제공되는 서비스가 너무 부족합니다.",
      "반려동물과 함께 묵기에 적합하지 않은 곳이에요."
  );

  // 비밀번호 상수
  public static final String USER_PASSWORD = "!User123";
  public static final String HOST_PASSWORD = "!Host123";
  public static final String LOCALE = "ko";
  public static final int REQUIRE_COUNT = 1;
  public static final int MAX_RESERVATION_FIND_DATE_ATTEMPT_COUNT = 20;

  private DataConstant() {
    throw new UnsupportedOperationException("유틸리티 클래스는 인스턴스화할 수 없습니다.");
  }
}
