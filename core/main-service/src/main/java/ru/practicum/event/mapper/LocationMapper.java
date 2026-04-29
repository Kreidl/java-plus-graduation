package ru.practicum.event.mapper;

import ru.practicum.event.dto.LocationDto;
import ru.practicum.event.model.Location;

import lombok.experimental.UtilityClass;

@UtilityClass
public class LocationMapper {

    public Location mapToEntity(LocationDto locationDto) {
        return new Location(locationDto.lat(), locationDto.lon());
    }

    public LocationDto mapToDto(Location location) {
        return new LocationDto(location.getLat(), location.getLon());
    }
}
