package ru.practicum.event.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.experimental.FieldDefaults;
import lombok.*;

@Getter
@Setter
@Builder
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Location {
    @Column(name = "location_lat", nullable = false, precision = 9, scale = 6)
    BigDecimal lat;

    @Column(name = "location_lon", nullable = false, precision = 9, scale = 6)
    BigDecimal lon;
}
