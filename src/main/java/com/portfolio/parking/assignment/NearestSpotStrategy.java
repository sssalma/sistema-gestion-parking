package com.portfolio.parking.assignment;

import com.portfolio.parking.model.ParkingSpot;
import com.portfolio.parking.model.Vehicle;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

/** Gives the closest usable spot to the entrance: lowest level first, then spot id. */
public final class NearestSpotStrategy implements SpotAssignmentStrategy {

    private static final Comparator<ParkingSpot> BY_DISTANCE =
            Comparator.comparingInt(ParkingSpot::level).thenComparing(ParkingSpot::id);

    @Override
    public Optional<ParkingSpot> choose(Vehicle vehicle, Collection<ParkingSpot> freeSpots) {
        return freeSpots.stream()
                .filter(vehicle::canUse)
                .min(BY_DISTANCE);
    }
}
