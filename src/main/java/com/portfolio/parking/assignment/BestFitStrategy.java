package com.portfolio.parking.assignment;

import com.portfolio.parking.model.ParkingSpot;
import com.portfolio.parking.model.Vehicle;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

/**
 * Keeps big spots for big vehicles: picks the smallest spot the vehicle fits in.
 * Electric vehicles are sent to a charger when one is free.
 */
public final class BestFitStrategy implements SpotAssignmentStrategy {

    @Override
    public Optional<ParkingSpot> choose(Vehicle vehicle, Collection<ParkingSpot> freeSpots) {
        Comparator<ParkingSpot> preference = Comparator
                .comparing((ParkingSpot spot) -> !spot.hasCharger())   // false (= has charger) sorts first
                .thenComparing(ParkingSpot::size)
                .thenComparingInt(ParkingSpot::level)
                .thenComparing(ParkingSpot::id);

        return freeSpots.stream()
                .filter(vehicle::canUse)
                .min(preference);
    }
}
