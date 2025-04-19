package com.meongnyangerang.meongnyangerang.repository;

import com.meongnyangerang.meongnyangerang.domain.user.Wishlist;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

  boolean existsByUserIdAndAccommodationId(Long userId, Long accommodationId);

  Optional<Wishlist> findByUserIdAndAccommodationId(Long userId, Long accommodationId);

  Page<Wishlist> findByUserId(Long userId, Pageable pageable);
}
