package com.portfolio.parking.pricing;

import com.portfolio.parking.model.Money;
import com.portfolio.parking.model.Ticket;

import java.time.Duration;
import java.time.Instant;

/**
 * Decorator: short stays are free, anything longer is priced by the wrapped policy.
 * It adds behaviour to any PricingPolicy without modifying it.
 */
public final class GracePeriodPricing implements PricingPolicy {

    private final PricingPolicy wrapped;
    private final Duration gracePeriod;

    public GracePeriodPricing(PricingPolicy wrapped, Duration gracePeriod) {
        this.wrapped = wrapped;
        this.gracePeriod = gracePeriod;
    }

    @Override
    public Money price(Ticket ticket, Instant exitTime) {
        Duration stay = Duration.between(ticket.entryTime(), exitTime);
        if (stay.compareTo(gracePeriod) <= 0) {
            return Money.zero(wrapped.price(ticket, exitTime).currency());
        }
        return wrapped.price(ticket, exitTime);
    }
}
