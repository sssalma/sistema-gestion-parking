package com.portfolio.parking.pricing;

import com.portfolio.parking.model.Money;
import com.portfolio.parking.model.Ticket;

import java.time.Instant;

/**
 * Strategy that decides how much a stay costs.
 * New tariffs are new implementations; the ParkingLot never changes (Open/Closed).
 */
public interface PricingPolicy {

    Money price(Ticket ticket, Instant exitTime);
}
