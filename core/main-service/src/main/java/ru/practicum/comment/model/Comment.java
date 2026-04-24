package ru.practicum.comment.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import ru.practicum.event.model.Event;
import ru.practicum.user.model.User;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    Event event;

    @Column(nullable = false)
    LocalDateTime created;

    @Column(nullable = false)
    boolean edited;
}
