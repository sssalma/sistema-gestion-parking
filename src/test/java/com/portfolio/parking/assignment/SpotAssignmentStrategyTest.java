package com.portfolio.parking.assignment;

import com.portfolio.parking.model.Car;
import com.portfolio.parking.model.ElectricCar;
import com.portfolio.parking.model.Motorcycle;
import com.portfolio.parking.model.ParkingSpot;
import com.portfolio.parking.model.Size;
import com.portfolio.parking.model.Van;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpotAssignmentStrategyTest {

    private static final List<ParkingSpot> SPOTS = List.of(
            ParkingSpot.regular("A-01", 0, Size.LARGE),
            ParkingSpot.regular("A-02", 0, Size.MEDIUM),
            ParkingSpot.charging("A-03", 0),
            ParkingSpot.regular("B-01", 1, Size.SMALL));

    @Test
    void nearestGivesTheFirstUsableSpotOnTheLowestLevel() {
        var strategy = new NearestSpotStrategy();

        assertThat(strategy.choose(new Motorcycle("1111AAA"), SPOTS)).map(ParkingSpot::id).contains("A-01");
    }

    @Test
    void bestFitKeepsBigSpotsForBigVehicles() {
        var strategy = new BestFitStrategy();

        assertThat(strategy.choose(new Motorcycle("1111AAA"), SPOTS)).map(ParkingSpot::id).contains("B-01");
        assertThat(strategy.choose(new Car("2222BBB"), SPOTS)).map(ParkingSpot::id).contains("A-02");
        assertThat(strategy.choose(new Van("3333CCC"), SPOTS)).map(ParkingSpot::id).contains("A-01");
    }

    @Test
    void bestFitSendsElectricCarsToAChargerAndOthersNever() {
        var strategy = new BestFitStrategy();

        assertThat(strategy.choose(new ElectricCar("4444DDD"), SPOTS)).map(ParkingSpot::id).contains("A-03");
        assertThat(strategy.choose(new Car("2222BBB"), List.of(ParkingSpot.charging("A-03", 0)))).isEmpty();
    }

    @Test
    void returnsEmptyWhenNothingFits() {
        var onlySmall = List.of(ParkingSpot.regular("B-01", 1, Size.SMALL));

        assertThat(new BestFitStrategy().choose(new Van("3333CCC"), onlySmall)).isEmpty();
        assertThat(new NearestSpotStrategy().choose(new Van("3333CCC"), onlySmall)).isEmpty();
    }
}
