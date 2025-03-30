package com.meongnyangerang.meongnyangerang.domain.accommodation;

import com.meongnyangerang.meongnyangerang.domain.host.Host;
import com.meongnyangerang.meongnyangerang.dto.accommodation.AccommodationCreateRequest;
import com.meongnyangerang.meongnyangerang.dto.accommodation.AccommodationUpdateRequest;
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
import java.time.LocalDateTime;
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
public class Accommodation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "host_id", nullable = false)
  private Host host;

  @Column(nullable = false, length = 100)
  private String name;

  private String description;

  @Column(nullable = false)
  private String address;

  private String detailedAddress;

  @Column(nullable = false)
  private Double latitude;

  @Column(nullable = false)
  private Double longitude;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AccommodationType type;

  @Column(nullable = false)
  private String thumbnailUrl;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(nullable = false)
  private LocalDateTime updatedAt;

  public void updateAccommodation(AccommodationUpdateRequest request, String thumbnailUrl) {
    this.name = request.name();
    this.description = request.description();
    this.address = request.address();
    this.detailedAddress = request.detailedAddress();
    this.latitude = request.latitude();
    this.longitude = request.longitude();
    this.type = request.type();
    this.thumbnailUrl = thumbnailUrl;
  }
}
