package com.meongnyangerang.meongnyangerang.repository;

import com.meongnyangerang.meongnyangerang.domain.image.ImageDeletionQueue;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImageDeletionQueueRepository extends JpaRepository<ImageDeletionQueue, Long> {

  List<ImageDeletionQueue> findAllByOrderByRegisteredAtAsc(PageRequest of);
}
