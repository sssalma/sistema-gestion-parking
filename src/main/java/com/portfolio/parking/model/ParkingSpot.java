package com.portfolio.parking.model;

import java.util.Objects;

/** A physical place in the car park. Immutable: whether it is occupied is tracked by the ParkingLot. */
public record ParkingSpot(String id, int level, Size size, boolean hasCharger) {

    public ParkingSpot {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(size, "size");
        if (hasCharger && size == Size.SMALL) {
            throw new IllegalArgumentException("Charging spots must fit a car");
        }
    }

    public static ParkingSpot regular(String id, int level, Size size) {
        return new ParkingSpot(id, level, size, false);
    }

    public static ParkingSpot charging(String id, int level) {
        return new ParkingSpot(id, level, Size.MEDIUM, true);
    }
}
