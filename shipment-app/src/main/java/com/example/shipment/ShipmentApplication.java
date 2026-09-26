package com.example.shipment;

import com.example.shipment.domain.ShipmentEntity;
import com.example.shipment.domain.ShipmentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDate;

@SpringBootApplication
public class ShipmentApplication {

    private static final Logger log = LoggerFactory.getLogger(ShipmentApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(ShipmentApplication.class, args);
    }

    /** One shipment to exercise the two endpoints against. */
    @Bean
    CommandLineRunner seed(ShipmentService shipments) {
        return args -> {
            shipments.create(new ShipmentEntity(
                "SHIP-1", "ORDER-1", ShipmentStatus.INIT, 3,
                LocalDate.now().plusDays(2)));
            shipments.create(new ShipmentEntity(
                "SHIP-2", "ORDER-2", ShipmentStatus.PICK, 1,
                LocalDate.now().plusDays(5)));
            log.info("seeded SHIP-1, SHIP-2");
            log.info("  GET  http://localhost:8080/shipments/SHIP-1/status");
            log.info("  PUT  http://localhost:8080/shipments/SHIP-1/status?to=PICK");
            log.info("  GET  http://localhost:8080/shipments/migration");
        };
    }
}
