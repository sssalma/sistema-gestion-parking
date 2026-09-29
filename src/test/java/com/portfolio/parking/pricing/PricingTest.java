package com.portfolio.parking.pricing;

import com.portfolio.parking.model.Car;
import com.portfolio.parking.model.Money;
import com.portfolio.parking.model.Motorcycle;
import com.portfolio.parking.model.ParkingSpot;
import com.portfolio.parking.model.Size;
import com.portfolio.parking.model.Ticket;
import com.portfolio.parking.model.Van;
import com.portfolio.parking.model.Vehicle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PricingTest {

    private static final Instant ENTRY = Instant.parse("2025-06-01T08:00:00Z");

    private static final HourlyPricing HOURLY = new HourlyPricing(Map.of(
            Size.SMALL, eur("1.00"),
            Size.MEDIUM, eur("2.50"),
            Size.LARGE, eur("4.00")));

    private static Money eur(String amount) {
        return Money.of(amount, "EUR");
    }

    private static Ticket ticket(Vehicle vehicle) {
        return new Ticket("T-1", vehicle, ParkingSpot.regular("A-01", 0, Size.LARGE), ENTRY);
    }

    private static Instant after(Duration duration) {
        return ENTRY.plus(duration);
    }

    @ParameterizedTest
    @CsvSource({
            "0,  0",
            "1,  1",
            "60, 1",
            "61, 2",
            "180, 3"
    })
    void countsEveryStartedHour(long minutes, long expectedHours) {
        assertThat(HourlyPricing.startedHours(Duration.ofMinutes(minutes))).isEqualTo(expectedHours);
    }

    @Test
    void hourlyRateDependsOnVehicleSize() {
        Instant exit = after(Duration.ofMinutes(90));   // 2 started hours

        assertThat(HOURLY.price(ticket(new Motorcycle("1111AAA")), exit)).isEqualTo(eur("2.00"));
        assertThat(HOURLY.price(ticket(new Car("2222BBB")), exit)).isEqualTo(eur("5.00"));
        assertThat(HOURLY.price(ticket(new Van("3333CCC")), exit)).isEqualTo(eur("8.00"));
    }

    @Test
    void gracePeriodMakesShortStaysFree() {
        var pricing = new GracePeriodPricing(HOURLY, Duration.ofMinutes(15));
        Ticket car = ticket(new Car("2222BBB"));

        assertThat(pricing.price(car, after(Duration.ofMinutes(15)))).isEqualTo(eur("0.00"));
        assertThat(pricing.price(car, after(Duration.ofMinutes(16)))).isEqualTo(eur("2.50"));
    }

    @Test
    void dailyCapLimitsThePricePerStartedDay() {
        var pricing = new DailyCapPricing(HOURLY, eur("20.00"));
        Ticket car = ticket(new Car("2222BBB"));

        assertThat(pricing.price(car, after(Duration.ofHours(4)))).isEqualTo(eur("10.00"));    // below the cap
        assertThat(pricing.price(car, after(Duration.ofHours(12)))).isEqualTo(eur("20.00"));   // 30.00 capped
        assertThat(pricing.price(car, after(Duration.ofHours(30)))).isEqualTo(eur("40.00"));   // 2 days x 20
    }

    /** Decorators stack: each one adds a rule without knowing what it wraps. */
    @Test
    void decoratorsCanBeCombined() {
        PricingPolicy pricing = new GracePeriodPricing(new DailyCapPricing(HOURLY, eur("20.00")), Duration.ofMinutes(15));
        Ticket car = ticket(new Car("2222BBB"));

        assertThat(pricing.price(car, after(Duration.ofMinutes(10)))).isEqualTo(eur("0.00"));
        assertThat(pricing.price(car, after(Duration.ofHours(2)))).isEqualTo(eur("5.00"));
        assertThat(pricing.price(car, after(Duration.ofHours(20)))).isEqualTo(eur("20.00"));
    }

    @Test
    void anyLambdaCanBeAPricingPolicy() {
        // PricingPolicy has a single method: a one-off promotion needs no new class
        PricingPolicy freeParking = (ticket, exit) -> eur("0");

        assertThat(freeParking.price(ticket(new Car("2222BBB")), after(Duration.ofHours(5)))).isEqualTo(eur("0.00"));
    }
}
