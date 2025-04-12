package com.meongnyangerang.meongnyangerang.controller;

import com.meongnyangerang.meongnyangerang.domain.chat.SenderType;
import com.meongnyangerang.meongnyangerang.domain.user.Role;
import com.meongnyangerang.meongnyangerang.dto.chat.PageResponse;
import com.meongnyangerang.meongnyangerang.dto.notification.MessageNotificationRequest;
import com.meongnyangerang.meongnyangerang.dto.notification.NotificationResponse;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.security.UserDetailsImpl;
import com.meongnyangerang.meongnyangerang.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    Role role = userDetail.getRole();

    if (role == Role.ROLE_USER) {
      notificationService.sendMessageNotification(request, userDetail.getId(), SenderType.USER);
    } else if (role == Role.ROLE_HOST) {
      notificationService.sendMessageNotification(request, userDetail.getId(), SenderType.HOST);
    } else {
      throw new MeongnyangerangException(ErrorCode.INVALID_AUTHORIZED);
    }
    return ResponseEntity.ok().build();
  }

  /**
   * 알림 목록 조회
   */
  @GetMapping
  public ResponseEntity<PageResponse<NotificationResponse>> getNotifications(
      @AuthenticationPrincipal UserDetailsImpl userDetails,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
      Pageable pageable
  ) {
    Role viewerRole = userDetails.getRole();

    if (viewerRole == Role.ROLE_USER) {
      return ResponseEntity.ok(
          notificationService.getNotificationsAsUser(userDetails.getId(), pageable));
    } else if (viewerRole == Role.ROLE_HOST) {
      return ResponseEntity.ok(
          notificationService.getNotificationsAsHost(userDetails.getId(), pageable));
    } else {
      throw new MeongnyangerangException(ErrorCode.INVALID_AUTHORIZED);
    }
  }

  /**
   * 알림 삭제
   */
  @DeleteMapping("/{notificationId}")
  public ResponseEntity<Void> deleteNotification(
      @PathVariable Long notificationId,
      @AuthenticationPrincipal UserDetailsImpl userDetails
  ) {
    Role viewerRole = userDetails.getRole();

    if (viewerRole == Role.ROLE_USER) {
      notificationService.deleteNotificationAsUser(notificationId, userDetails.getId());
    } else if (viewerRole == Role.ROLE_HOST) {
      notificationService.deleteNotificationAsHost(notificationId, userDetails.getId());
    } else {
      throw new MeongnyangerangException(ErrorCode.INVALID_AUTHORIZED);
    }
    return ResponseEntity.ok().build();
  }
}
