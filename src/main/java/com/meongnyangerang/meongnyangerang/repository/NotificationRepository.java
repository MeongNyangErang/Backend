package com.meongnyangerang.meongnyangerang.repository;

import com.meongnyangerang.meongnyangerang.domain.notification.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

  Page<Notification> findAllByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

  Page<Notification> findAllByHost_IdOrderByCreatedAtDesc(Long hostId, Pageable pageable);
}
