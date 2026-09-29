package com.portfolio.parking.notification;

import com.portfolio.parking.model.Car;
import com.portfolio.parking.model.Money;
import com.portfolio.parking.model.ParkingEvent;
import com.portfolio.parking.model.ParkingSpot;
import com.portfolio.parking.model.Receipt;
import com.portfolio.parking.model.Size;
import com.portfolio.parking.model.Ticket;
import com.portfolio.parking.model.Van;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class EventLogTest {

    private static final Instant AT = Instant.parse("2025-06-01T08:00:00Z");

    @Test
    void describesEachEventType() {
        Ticket ticket = new Ticket("T-00001", new Car("1234BCD"), ParkingSpot.regular("A-02", 0, Size.MEDIUM), AT);
        var log = new EventLog(ZoneOffset.UTC);

        log.onEvent(new ParkingEvent.VehicleEntered(ticket));
        log.onEvent(new ParkingEvent.VehicleExited(new Receipt(ticket, AT.plusSeconds(3600), Money.of("2.50", "EUR"))));
        log.onEvent(new ParkingEvent.EntryRejected(new Van("3456MNP"), "No free spot", AT));

        assertThat(log.lines()).satisfiesExactly(
                line -> assertThat(line).startsWith("08:00  IN").contains("spot A-02", "T-00001"),
                line -> assertThat(line).startsWith("09:00  OUT").contains("after 60 min", "2.50 EUR"),
                line -> assertThat(line).startsWith("08:00  DENY").contains("Van 3456MNP", "No free spot"));
    }
}
