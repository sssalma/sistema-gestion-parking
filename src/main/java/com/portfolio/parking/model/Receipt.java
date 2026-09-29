package com.portfolio.parking.model;

import java.time.Duration;
import java.time.Instant;

/** Given to the driver on exit. */
public record Receipt(Ticket ticket, Instant exitTime, Money amount) {

    public Duration parkedTime() {
        return Duration.between(ticket.entryTime(), exitTime);
    }
}
