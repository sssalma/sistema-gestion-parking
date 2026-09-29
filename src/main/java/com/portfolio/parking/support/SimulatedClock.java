package com.portfolio.parking.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * A clock that only moves when told to. Used by the demo and the tests to simulate
 * hours of parking in milliseconds, which is possible because ParkingLot receives a Clock
 * instead of calling Instant.now() directly.
 */
public final class SimulatedClock extends Clock {

    private volatile Instant now;
    private final ZoneId zone;

    public SimulatedClock(Instant start, ZoneId zone) {
        this.now = start;
        this.zone = zone;
    }

    public void advance(Duration duration) {
        now = now.plus(duration);
    }

    @Override
    public Instant instant() {
        return now;
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new SimulatedClock(now, zone);
    }
}
