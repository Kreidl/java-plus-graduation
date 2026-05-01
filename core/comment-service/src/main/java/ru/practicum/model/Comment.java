package ru.practicum.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.experimental.FieldDefaults;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "comment")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    String text;

    @Column(name = "author_id", nullable = false)
    Long authorId;

    @Column(name = "event_id", nullable = false)
    Long eventId;

    @Column(nullable = false)
    LocalDateTime created;

    @Column(nullable = false)
    boolean edited;
}
