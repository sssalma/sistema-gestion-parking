package com.portfolio.parking.pricing;

import com.portfolio.parking.model.Money;
import com.portfolio.parking.model.Ticket;

import java.time.Instant;

/** Same price regardless of time or vehicle, e.g. for a concert or a football match. */
public final class FlatRatePricing implements PricingPolicy {

    private final Money price;

    public FlatRatePricing(Money price) {
        this.price = price;
    }

    @Override
    public Money price(Ticket ticket, Instant exitTime) {
        return price;
    }
}
