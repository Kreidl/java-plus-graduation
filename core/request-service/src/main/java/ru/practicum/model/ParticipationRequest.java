package ru.practicum.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import ru.practicum.request.enums.EventRequestStatus;

import lombok.experimental.FieldDefaults;
import lombok.*;

@Entity
@Table(name = "participation_request")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ParticipationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "event_id", nullable = false)
    Long eventId;

    @Column(name = "requester_id", nullable = false)
    Long requesterId;

    @Column(nullable = false)
    LocalDateTime created;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    EventRequestStatus status;
}
