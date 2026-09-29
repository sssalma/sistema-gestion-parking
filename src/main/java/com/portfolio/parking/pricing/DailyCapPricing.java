package com.portfolio.parking.pricing;

import com.portfolio.parking.model.Money;
import com.portfolio.parking.model.Ticket;

import java.time.Duration;
import java.time.Instant;

/**
 * Decorator: never charges more than a maximum per started 24-hour period.
 * Decorators can be stacked, e.g. {@code new GracePeriodPricing(new DailyCapPricing(new HourlyPricing(...)))}.
 */
public final class DailyCapPricing implements PricingPolicy {

    private static final long MINUTES_PER_DAY = Duration.ofDays(1).toMinutes();

    private final PricingPolicy wrapped;
    private final Money capPerDay;

    public DailyCapPricing(PricingPolicy wrapped, Money capPerDay) {
        this.wrapped = wrapped;
        this.capPerDay = capPerDay;
    }

    @Override
    public Money price(Ticket ticket, Instant exitTime) {
        long minutes = Duration.between(ticket.entryTime(), exitTime).toMinutes();
        long startedDays = Math.max(1, (minutes + MINUTES_PER_DAY - 1) / MINUTES_PER_DAY);
        return wrapped.price(ticket, exitTime).min(capPerDay.times(startedDays));
    }
}
