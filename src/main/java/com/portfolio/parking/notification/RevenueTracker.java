package com.portfolio.parking.notification;

import com.portfolio.parking.model.Money;
import com.portfolio.parking.model.ParkingEvent;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Adds up the money collected, per vehicle type. Only reacts to exits, ignores every other event. */
public class RevenueTracker implements ParkingEventListener {

    private final ConcurrentMap<String, Money> revenueByType = new ConcurrentHashMap<>();

    @Override
    public void onEvent(ParkingEvent event) {
        if (event instanceof ParkingEvent.VehicleExited(var receipt)) {
            revenueByType.merge(receipt.ticket().vehicle().type(), receipt.amount(), Money::plus);
        }
    }

    /** Sorted by vehicle type for stable output. */
    public Map<String, Money> revenueByType() {
        return new TreeMap<>(revenueByType);
    }
}
