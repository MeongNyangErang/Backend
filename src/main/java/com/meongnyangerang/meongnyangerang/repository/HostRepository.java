package com.meongnyangerang.meongnyangerang.repository;

import com.meongnyangerang.meongnyangerang.domain.host.Host;
import com.meongnyangerang.meongnyangerang.domain.host.HostStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface HostRepository extends JpaRepository<Host, Long> {

  boolean existsByEmail(String email);

  boolean existsByNickname(String nickname);

  Optional<Host> findByEmail(String email);

  List<Host> findAllByStatusAndDeletedAtBefore(HostStatus status, LocalDateTime cutoff);

  @Query(value = "SELECT * FROM Host h " +
      "WHERE h.status = :status " +
      "AND (:cursorId = 0 OR h.id >= :cursorId) " +
      "ORDER BY h.created_at ASC LIMIT :size",
      nativeQuery = true)
  List<Host> findAllByStatus(@Param("cursorId") Long cursorId, @Param("size") int size,
      @Param("status") String status);

  Optional<Host> findByIdAndStatus(Long id, HostStatus status);

  boolean existByPhoneNumberAndIdNot(String phoneNumber, Long hostId);
}
