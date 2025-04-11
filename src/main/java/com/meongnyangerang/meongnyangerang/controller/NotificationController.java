package com.meongnyangerang.meongnyangerang.controller;

import com.meongnyangerang.meongnyangerang.dto.notification.MessageNotificationRequest;
import com.meongnyangerang.meongnyangerang.security.UserDetailsImpl;
import com.meongnyangerang.meongnyangerang.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {

  private final NotificationService notificationService;

  /**
   * 메시지 알림 전송
   */
  @PostMapping("/messages")
  public ResponseEntity<Void> sendMessageNotification(
      @RequestBody MessageNotificationRequest request,
      @AuthenticationPrincipal UserDetailsImpl userDetail
  ) {
    notificationService.sendNotification(request, userDetail.getId());
    return ResponseEntity.ok().build();
  }
}
