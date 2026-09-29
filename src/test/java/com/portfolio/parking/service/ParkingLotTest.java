package com.portfolio.parking.service;

import com.portfolio.parking.assignment.BestFitStrategy;
import com.portfolio.parking.exception.NoSpotAvailableException;
import com.portfolio.parking.exception.TicketNotFoundException;
import com.portfolio.parking.exception.VehicleAlreadyParkedException;
import com.portfolio.parking.model.Car;
import com.portfolio.parking.model.ElectricCar;
import com.portfolio.parking.model.Money;
import com.portfolio.parking.model.Motorcycle;
import com.portfolio.parking.model.ParkingEvent;
import com.portfolio.parking.model.ParkingSpot;
import com.portfolio.parking.model.Receipt;
import com.portfolio.parking.model.Size;
import com.portfolio.parking.model.Ticket;
import com.portfolio.parking.model.Van;
import com.portfolio.parking.notification.RevenueTracker;
import com.portfolio.parking.pricing.HourlyPricing;
import com.portfolio.parking.support.SimulatedClock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParkingLotTest {

    private final SimulatedClock clock = new SimulatedClock(Instant.parse("2025-06-01T08:00:00Z"), ZoneOffset.UTC);
    private final List<ParkingEvent> events = new CopyOnWriteArrayList<>();
    private final RevenueTracker revenue = new RevenueTracker();

    private final HourlyPricing pricing = new HourlyPricing(Map.of(
            Size.SMALL, Money.of("1", "EUR"),
            Size.MEDIUM, Money.of("2.50", "EUR"),
            Size.LARGE, Money.of("4", "EUR")));

    private ParkingLot parkingWith(ParkingSpot... spots) {
        return new ParkingLot(List.of(spots), new BestFitStrategy(), pricing, List.of(events::add, revenue), clock);
    }

    @Test
    void enterAndExitChargesTheStay() {
        var parking = parkingWith(ParkingSpot.regular("A-01", 0, Size.MEDIUM));

        Ticket ticket = parking.enter(new Car("1234BCD"));
        clock.advance(Duration.ofMinutes(125));   // 3 started hours
        Receipt receipt = parking.exit(ticket.id());

        assertThat(receipt.amount()).isEqualTo(Money.of("7.50", "EUR"));
        assertThat(receipt.parkedTime()).isEqualTo(Duration.ofMinutes(125));
        assertThat(parking.occupiedCount()).isZero();
    }

    @Test
    void theSameVehicleCannotEnterTwice() {
        var parking = parkingWith(ParkingSpot.regular("A-01", 0, Size.MEDIUM), ParkingSpot.regular("A-02", 0, Size.MEDIUM));
        parking.enter(new Car("1234BCD"));

        assertThatThrownBy(() -> parking.enter(new Car("1234-bcd")))
                .isInstanceOf(VehicleAlreadyParkedException.class);
    }

    @Test
    void rejectsVehiclesThatDoNotFitAnyFreeSpot() {
        var parking = parkingWith(ParkingSpot.regular("A-01", 0, Size.MEDIUM));

        assertThatThrownBy(() -> parking.enter(new Van("3456MNP")))
                .isInstanceOf(NoSpotAvailableException.class);
        assertThat(events).singleElement().isInstanceOf(ParkingEvent.EntryRejected.class);
    }

    @Test
    void spotIsFreedOnExitAndCanBeReused() {
        var parking = parkingWith(ParkingSpot.regular("A-01", 0, Size.MEDIUM));
        Ticket first = parking.enter(new Car("1111AAA"));
        assertThatThrownBy(() -> parking.enter(new Car("2222BBB"))).isInstanceOf(NoSpotAvailableException.class);

        parking.exit(first.id());

        assertThat(parking.enter(new Car("2222BBB")).spot().id()).isEqualTo("A-01");
    }

    @Test
    void aTicketCanOnlyBeUsedOnce() {
        var parking = parkingWith(ParkingSpot.regular("A-01", 0, Size.MEDIUM));
        Ticket ticket = parking.enter(new Car("1234BCD"));
        parking.exit(ticket.id());

        assertThatThrownBy(() -> parking.exit(ticket.id())).isInstanceOf(TicketNotFoundException.class);
        assertThatThrownBy(() -> parking.exit("T-99999")).isInstanceOf(TicketNotFoundException.class);
    }

    @Test
    void electricCarGetsTheChargerAndRegularCarDoesNot() {
        var parking = parkingWith(ParkingSpot.charging("C-01", 0), ParkingSpot.regular("A-01", 0, Size.MEDIUM));

        Ticket car = parking.enter(new Car("1111AAA"));
        Ticket electric = parking.enter(new ElectricCar("2222BBB"));

        assertThat(car.spot().id()).isEqualTo("A-01");
        assertThat(electric.spot().id()).isEqualTo("C-01");
    }

    @Test
    void listenersReceiveEveryEventAndRevenueIsAddedUp() {
        var parking = parkingWith(ParkingSpot.regular("A-01", 0, Size.SMALL), ParkingSpot.regular("A-02", 0, Size.MEDIUM));
        Ticket moto = parking.enter(new Motorcycle("1111AAA"));
        Ticket car = parking.enter(new Car("2222BBB"));
        clock.advance(Duration.ofHours(2));
        parking.exit(moto.id());
        parking.exit(car.id());

        assertThat(events).extracting(e -> e.getClass().getSimpleName())
                .containsExactly("VehicleEntered", "VehicleEntered", "VehicleExited", "VehicleExited");
        assertThat(revenue.revenueByType()).containsExactlyEntriesOf(Map.of(
                "Car", Money.of("5.00", "EUR"),
                "Motorcycle", Money.of("2.00", "EUR")));
    }

    @Test
    void reportsFreeSpotsBySize() {
        var parking = parkingWith(
                ParkingSpot.regular("A-01", 0, Size.SMALL),
                ParkingSpot.regular("A-02", 0, Size.MEDIUM),
                ParkingSpot.regular("A-03", 0, Size.MEDIUM));
        parking.enter(new Car("1111AAA"));

        assertThat(parking.freeSpotsBySize())
                .containsEntry(Size.SMALL, 1L)
                .containsEntry(Size.MEDIUM, 1L)
                .containsEntry(Size.LARGE, 0L);
    }

    /**
     * 100 cars arrive at the same instant for 20 spots. Without proper locking two cars could
     * be given the same spot. Exactly 20 must get in, each in a different spot.
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void concurrentEntriesNeverShareASpot() throws InterruptedException {
        List<ParkingSpot> spots = IntStream.rangeClosed(1, 20)
                .mapToObj(i -> ParkingSpot.regular("S-%02d".formatted(i), i / 10, Size.MEDIUM))
                .toList();
        var parking = new ParkingLot(spots, new BestFitStrategy(), pricing, List.of(), clock);

        Set<String> assignedSpots = ConcurrentHashMap.newKeySet();
        List<Ticket> tickets = new CopyOnWriteArrayList<>();
        CountDownLatch startSignal = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(16)) {
            for (int i = 0; i < 100; i++) {
                String plate = "CAR%04d".formatted(i);
                executor.submit(() -> {
                    startSignal.await();   // release every thread at the same moment
                    try {
                        Ticket ticket = parking.enter(new Car(plate));
                        tickets.add(ticket);
                        assignedSpots.add(ticket.spot().id());
                    } catch (NoSpotAvailableException expected) {
                        // 80 cars must be turned away
                    }
                    return null;
                });
            }
            startSignal.countDown();
        }

        assertThat(tickets).hasSize(20);
        assertThat(assignedSpots).hasSize(20);
        assertThat(parking.occupiedCount()).isEqualTo(20);
    }

    @Test
    void rejectsDuplicateSpotIds() {
        List<ParkingSpot> spots = new ArrayList<>(List.of(
                ParkingSpot.regular("A-01", 0, Size.SMALL),
                ParkingSpot.regular("A-01", 0, Size.LARGE)));

        assertThatThrownBy(() -> new ParkingLot(spots, new BestFitStrategy(), pricing, List.of(), clock))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
