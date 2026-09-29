package com.portfolio.parking.model;

import java.time.Instant;

/** Given to the driver on entry. */
public record Ticket(String id, Vehicle vehicle, ParkingSpot spot, Instant entryTime) {
}
