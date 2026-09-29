package com.portfolio.parking.notification;

import com.portfolio.parking.model.ParkingEvent;
import com.portfolio.parking.model.ParkingEvent.EntryRejected;
import com.portfolio.parking.model.ParkingEvent.VehicleEntered;
import com.portfolio.parking.model.ParkingEvent.VehicleExited;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Keeps a readable, append-only log of every event. */
public class EventLog implements ParkingEventListener {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final ZoneId zone;
    private final List<String> lines = new CopyOnWriteArrayList<>();

    public EventLog(ZoneId zone) {
        this.zone = zone;
    }

    @Override
    public void onEvent(ParkingEvent event) {
        lines.add(TIME.format(event.at().atZone(zone)) + "  " + describe(event));
    }

    /** Pattern matching over the sealed hierarchy: a new event type breaks compilation here until handled. */
    static String describe(ParkingEvent event) {
        return switch (event) {
            case VehicleEntered e -> "IN   %-22s -> spot %s (ticket %s)".formatted(
                    e.ticket().vehicle(), e.ticket().spot().id(), e.ticket().id());
            case VehicleExited e -> "OUT  %-22s after %d min, paid %s".formatted(
                    e.receipt().ticket().vehicle(), e.receipt().parkedTime().toMinutes(), e.receipt().amount());
            case EntryRejected e -> "DENY %-22s %s".formatted(e.vehicle(), e.reason());
        };
    }

    public List<String> lines() {
        return List.copyOf(lines);
    }
}
