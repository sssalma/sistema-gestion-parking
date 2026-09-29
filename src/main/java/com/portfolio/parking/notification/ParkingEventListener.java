package com.portfolio.parking.notification;

import com.portfolio.parking.model.ParkingEvent;

/**
 * Observer notified after everything that happens at the barrier. A single-method interface
 * on purpose (Interface Segregation): each listener only has to care about one thing.
 */
@FunctionalInterface
public interface ParkingEventListener {

    void onEvent(ParkingEvent event);
}
