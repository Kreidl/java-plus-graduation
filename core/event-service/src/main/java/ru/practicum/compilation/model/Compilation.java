package ru.practicum.compilation.model;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.*;

import ru.practicum.event.model.Event;

import lombok.experimental.FieldDefaults;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "compilation")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Compilation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    Long id;

    @Column(name = "title", nullable = false)
    String title;

    @Column(name = "pinned")
    Boolean pinned;

    @ManyToMany
    @JoinTable(
            name = "compilation_events",
            joinColumns = @JoinColumn(name = "compilation_id"),
            inverseJoinColumns = @JoinColumn(name = "event_id"))
    Set<Event> events = new LinkedHashSet<>();
}
