package com.meongnyangerang.meongnyangerang.domain.reservation;

import com.meongnyangerang.meongnyangerang.domain.accommodation.Room;
import com.meongnyangerang.meongnyangerang.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Reservation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "room_id", nullable = false)
  private Room room;

  @Column(nullable = false)
  private LocalDate checkInDate;

  @Column(nullable = false)
  private LocalDate checkOutDate;

  @Column(nullable = false)
  private Integer peopleCount;

  @Column(nullable = false)
  private Integer petCount;

  @Column(nullable = false, length = 50)
  private String reserverName;

  @Column(nullable = false, length = 20)
  private String reserverPhoneNumber;

  @Column(nullable = false)
  private Boolean hasVehicle;

  @Column(nullable = false)
  private Long totalPrice;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ReservationStatus status;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(nullable = false)
  private LocalDateTime updatedAt;

  private LocalDateTime canceledAt;

  enum ReservationStatus {
    RESERVED, COMPLETED, CANCELLED;
  }
}


