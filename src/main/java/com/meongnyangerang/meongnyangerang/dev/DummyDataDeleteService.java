package com.meongnyangerang.meongnyangerang.dev;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DummyDataDeleteService {

  private final JdbcTemplate jdbcTemplate;
  private final ElasticsearchClient elasticsearchClient;

  /**
   * 관계 순서를 고려하여 역순으로 삭제 admin, notice는 삭제하지 않습니다. (필요하다면 추가)
   */
  @Transactional
  public Map<String, Object> clearData() {
    Map<String, Object> result = new HashMap<>();

    try {
      jdbcTemplate.execute("DELETE FROM notification");
      jdbcTemplate.execute("DELETE FROM chat_read_status");
      jdbcTemplate.execute("DELETE FROM chat_message");
      jdbcTemplate.execute("DELETE FROM chat_room");

      jdbcTemplate.execute("DELETE FROM review_report");
      jdbcTemplate.execute("DELETE FROM review_image");
      jdbcTemplate.execute("DELETE FROM review");

      jdbcTemplate.execute("DELETE FROM reservation_slot");
      jdbcTemplate.execute("DELETE FROM reservation");

      jdbcTemplate.execute("DELETE FROM room_pet_facility");
      jdbcTemplate.execute("DELETE FROM room_facility");
      jdbcTemplate.execute("DELETE FROM hashtag");
      jdbcTemplate.execute("DELETE FROM room");

      jdbcTemplate.execute("DELETE FROM accommodation_pet_facility");
      jdbcTemplate.execute("DELETE FROM accommodation_facility");
      jdbcTemplate.execute("DELETE FROM allow_pet");
      jdbcTemplate.execute("DELETE FROM accommodation_image");
      jdbcTemplate.execute("DELETE FROM accommodation");

      jdbcTemplate.execute("DELETE FROM wishlist");
      jdbcTemplate.execute("DELETE FROM authentication_code");
      jdbcTemplate.execute("DELETE FROM user");
      jdbcTemplate.execute("DELETE FROM host");

      clearAllElasticsearchIndices(); // ES 인덱스 삭제

      result.put("success", true);
      result.put("message", "모든 더미 데이터가 삭제되었습니다.");

      log.info("모든 데이터 삭제 작업 완료");
    } catch (Exception e) {
      result.put("success", false);
      result.put("error", e.getMessage());
      result.put("message", "더미 데이터 삭제 중 오류가 발생했습니다.");
      throw new RuntimeException("더미 데이터 삭제 실패", e);
    }
    return result;
  }

  private void clearAllElasticsearchIndices() {
    try {
      List<String> indicesToClear = Arrays.asList("accommodations", "accommodation_rooms");

      for (String index : indicesToClear) {
        try {
          // 각 인덱스 삭제
          DeleteIndexRequest request = DeleteIndexRequest.of(r -> r.index(index));
          elasticsearchClient.indices().delete(request);
          log.info("인덱스 삭제 완료: {}", index);
        } catch (Exception e) {
          log.warn("인덱스 {} 삭제 중 오류 발생: {}", index, e.getMessage());
        }
      }
      log.info("모든 인덱스 삭제 작업 완료");
    } catch (Exception e) {
      log.error("Elasticsearch 인덱스 삭제 중 오류 발생", e);
      throw new RuntimeException("Elasticsearch 인덱스 삭제 실패", e);
    }
  }
}
