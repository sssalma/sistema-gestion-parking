package com.portfolio.parking.model;

import java.time.Instant;

/**
 * Everything that can happen at the barrier.
 * <p>
 * The interface is {@code sealed}: these three records are the only possible events,
 * so a {@code switch} over a ParkingEvent is checked for completeness by the compiler.
 */
public sealed interface ParkingEvent {

    Instant at();

    record VehicleEntered(Ticket ticket) implements ParkingEvent {
        @Override
        public Instant at() {
            return ticket.entryTime();
        }
    }

    record VehicleExited(Receipt receipt) implements ParkingEvent {
        @Override
        public Instant at() {
            return receipt.exitTime();
        }
    }

    record EntryRejected(Vehicle vehicle, String reason, Instant at) implements ParkingEvent {
    }
}
