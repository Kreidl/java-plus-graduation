package ru.practicum.model.event;

import java.time.Instant;

import jakarta.persistence.*;

import lombok.experimental.FieldDefaults;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "event_similarity")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventSimilarity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_event", nullable = false)
    Long eventA;

    @Column(name = "second_event", nullable = false)
    Long eventB;

    @Column(name = "score", nullable = false)
    Double score;

    @Column(name = "timestamp", nullable = false)
    Instant timestamp;
}
