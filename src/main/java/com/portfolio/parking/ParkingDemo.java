package com.portfolio.parking;

import com.portfolio.parking.assignment.BestFitStrategy;
import com.portfolio.parking.exception.ParkingException;
import com.portfolio.parking.model.Car;
import com.portfolio.parking.model.ElectricCar;
import com.portfolio.parking.model.Money;
import com.portfolio.parking.model.Motorcycle;
import com.portfolio.parking.model.ParkingSpot;
import com.portfolio.parking.model.Size;
import com.portfolio.parking.model.Ticket;
import com.portfolio.parking.model.Van;
import com.portfolio.parking.notification.EventLog;
import com.portfolio.parking.notification.RevenueTracker;
import com.portfolio.parking.pricing.DailyCapPricing;
import com.portfolio.parking.pricing.GracePeriodPricing;
import com.portfolio.parking.pricing.HourlyPricing;
import com.portfolio.parking.pricing.PricingPolicy;
import com.portfolio.parking.service.ParkingLot;
import com.portfolio.parking.support.SimulatedClock;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/** Simulates a busy day in a small two-level car park and prints what happened. */
public class ParkingDemo {

    public static void main(String[] args) {
        ZoneId zone = ZoneId.systemDefault();
        var clock = new SimulatedClock(LocalDateTime.now().withHour(8).withMinute(0).atZone(zone).toInstant(), zone);

        // Composition root: the only place where concrete classes are chosen and wired together.
        PricingPolicy pricing = new GracePeriodPricing(
                new DailyCapPricing(
                        new HourlyPricing(Map.of(
                                Size.SMALL, Money.of("1.00", "EUR"),
                                Size.MEDIUM, Money.of("2.50", "EUR"),
                                Size.LARGE, Money.of("4.00", "EUR"))),
                        Money.of("20.00", "EUR")),
                Duration.ofMinutes(15));

        var eventLog = new EventLog(zone);
        var revenue = new RevenueTracker();
        var parking = new ParkingLot(
                List.of(
                        ParkingSpot.regular("A-01", 0, Size.SMALL),
                        ParkingSpot.regular("A-02", 0, Size.MEDIUM),
                        ParkingSpot.charging("A-03", 0),
                        ParkingSpot.regular("B-01", 1, Size.MEDIUM),
                        ParkingSpot.regular("B-02", 1, Size.LARGE)),
                new BestFitStrategy(),
                pricing,
                List.of(eventLog, revenue),
                clock);

        Ticket moto = parking.enter(new Motorcycle("1234-BCD"));
        Ticket tesla = parking.enter(new ElectricCar("5678 FGH"));
        Ticket car = parking.enter(new Car("9012JKL"));
        clock.advance(Duration.ofMinutes(10));
        Ticket van = parking.enter(new Van("3456MNP"));

        clock.advance(Duration.ofMinutes(2));
        parking.exit(moto.id());                                   // 12 min: inside the grace period
        attempt(() -> parking.enter(new Car("9012JKL")));          // same plate is still inside
        Ticket car2 = parking.enter(new Car("7777XYZ"));
        attempt(() -> parking.enter(new Van("8888WWW")));          // the only large spot is taken

        clock.advance(Duration.ofMinutes(170));
        parking.exit(car.id());                                    // 3 h 2 min -> 4 started hours
        parking.exit(van.id());

        clock.advance(Duration.ofHours(10));
        parking.exit(tesla.id());                                  // 13 h: hits the daily cap
        attempt(() -> parking.exit(tesla.id()));                   // ticket already used
        parking.exit(car2.id());

        System.out.println("Event log");
        System.out.println("-".repeat(78));
        eventLog.lines().forEach(System.out::println);
        System.out.println();
        System.out.println("Revenue by vehicle type: " + revenue.revenueByType());
        System.out.println("Free spots now: " + parking.freeSpotsBySize());
    }

    private static void attempt(Runnable operation) {
        try {
            operation.run();
        } catch (ParkingException e) {
            System.out.println("Rejected: " + e.getMessage());
        }
    }
}
