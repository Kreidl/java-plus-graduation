package ru.practicum.model.user;

import java.time.Instant;

import jakarta.persistence.*;

import ru.practicum.model.user.enums.ActionType;

import lombok.experimental.FieldDefaults;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "user_actions")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserAction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    Long userId;

    @Column(name = "event_id", nullable = false)
    Long eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    ActionType actionType;

    @Column(name = "timestamp", nullable = false)
    Instant timestamp;
}
