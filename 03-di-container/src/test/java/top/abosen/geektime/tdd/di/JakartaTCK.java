package top.abosen.geektime.tdd.di;

import jakarta.inject.Named;
import junit.framework.Test;
import org.atinject.tck.Tck;
import org.atinject.tck.auto.Car;
import org.atinject.tck.auto.*;
import org.atinject.tck.auto.accessories.*;

/**
 * @author qiubaisen
 * @date 2022/11/14
 */
public class JakartaTCK {
    public static Test suite() {
        ContextConfig config = new ContextConfig();
        config.from(new Config() {
            @Export(Car.class)
            Convertible car;

            @Drivers
            @Export(Seat.class)
            DriversSeat drivers;

            Seat seat;

            Tire tire;

            @Export(Engine.class)
            V8Engine engine;

            @Named("spare")
            @Export(Tire.class)
            SpareTire spare;

            FuelTank fuelTank;
            SpareTire spareTire;
            Cupholder cupholder;
        });

        Car car = config.getContext().get(ComponentRef.of(Car.class));
        return Tck.testsFor(car, false, true);
    }
}
