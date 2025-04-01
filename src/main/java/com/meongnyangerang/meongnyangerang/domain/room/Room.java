package com.meongnyangerang.meongnyangerang.domain.room;

import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import com.meongnyangerang.meongnyangerang.dto.room.RoomUpdateRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Room {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "accommodation_id", nullable = false)
  private Accommodation accommodation;

  @Column(nullable = false, length = 100)
  private String name;

  private String description;

  @Column(nullable = false)
  private Integer standardPeopleCount;

  @Column(nullable = false)
  private Integer maxPeopleCount;

  @Column(nullable = false)
  private Integer standardPetCount;

  @Column(nullable = false)
  private Integer maxPetCount;

  @Column(nullable = false)
  private String imageUrl;

  @Column(nullable = false)
  private Long price;

  private Long extraPeopleFee;

  private Long extraPetFee;

  @Column(nullable = false)
  private Long extraFee;

  @Column(nullable = false)
  private LocalTime checkInTime;

  @Column(nullable = false)
  private LocalTime checkOutTime;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(nullable = false)
  private LocalDateTime updatedAt;

  public Room updateRoom(RoomUpdateRequest request, String imageUrl){
    this.name = request.name();
    this.description = request.description();
    this.standardPeopleCount = request.standardPeopleCount();
    this.maxPeopleCount = request.maxPeopleCount();
    this.standardPetCount = request.standardPetCount();
    this.maxPetCount = request.maxPetCount();
    this.imageUrl = imageUrl;
    this.price = request.price();
    this.extraPeopleFee = request.extraPeopleFee();
    this.extraPetFee = request.extraPetFee();
    this.extraFee = request.extraFee();
    this.checkInTime = request.checkInTime();
    this.checkOutTime = request.checkOutTime();

    return this;
  }
}
