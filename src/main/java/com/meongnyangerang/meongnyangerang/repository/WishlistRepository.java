package com.meongnyangerang.meongnyangerang.repository;

import com.meongnyangerang.meongnyangerang.domain.user.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
  boolean existsByUserIdAndAccommodationId(Long userId, Long accommodationId);

}
