package com.portfolio.parking.assignment;

import com.portfolio.parking.model.ParkingSpot;
import com.portfolio.parking.model.Vehicle;

import java.util.Collection;
import java.util.Optional;

/** Strategy that picks which free spot a vehicle gets. */
@FunctionalInterface
public interface SpotAssignmentStrategy {

    /**
     * @param freeSpots spots that are currently empty (not necessarily usable by this vehicle)
     */
    Optional<ParkingSpot> choose(Vehicle vehicle, Collection<ParkingSpot> freeSpots);
}
