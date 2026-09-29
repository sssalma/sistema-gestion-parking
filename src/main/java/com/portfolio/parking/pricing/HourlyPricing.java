package com.portfolio.parking.pricing;

import com.portfolio.parking.model.Money;
import com.portfolio.parking.model.Size;
import com.portfolio.parking.model.Ticket;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

/** Charges every started hour, with a different rate per vehicle size. */
public final class HourlyPricing implements PricingPolicy {

    private final Map<Size, Money> ratePerHour;

    public HourlyPricing(Map<Size, Money> ratePerHour) {
        for (Size size : Size.values()) {
            if (!ratePerHour.containsKey(size)) {
                throw new IllegalArgumentException("Missing hourly rate for " + size);
            }
        }
        this.ratePerHour = new EnumMap<>(ratePerHour);
    }

    @Override
    public Money price(Ticket ticket, Instant exitTime) {
        long startedHours = startedHours(Duration.between(ticket.entryTime(), exitTime));
        return ratePerHour.get(ticket.vehicle().size()).times(startedHours);
    }

    /** 0 min -> 0 h, 1 min -> 1 h, 60 min -> 1 h, 61 min -> 2 h. */
    static long startedHours(Duration duration) {
        long minutes = duration.toMinutes();
        return (minutes + 59) / 60;
    }
}
