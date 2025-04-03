package com.meongnyangerang.meongnyangerang.repository;

import com.meongnyangerang.meongnyangerang.domain.user.Wishlist;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
  boolean existsByUserIdAndAccommodationId(Long userId, Long accommodationId);

  Optional<Wishlist> findByUserIdAndAccommodationId(Long userId, Long accommodationId);

  @Query(value = """
      SELECT * FROM wishlist w
      WHERE w.user_id = :userId
        AND (:cursorId = 0 OR w.id <= :cursorId)
      ORDER BY w.created_at DESC
      LIMIT :size
  """, nativeQuery = true)
  List<Wishlist> findByUserId(
      @Param("userId") Long userId,
      @Param("cursorId") Long cursorId,
      @Param("size") int size);
}
