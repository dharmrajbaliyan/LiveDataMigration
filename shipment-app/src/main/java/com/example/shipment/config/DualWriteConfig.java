package com.example.shipment.config;

import com.example.dualwrite.*;
import com.example.shipment.domain.ShipmentEntity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class DualWriteConfig {

    @Bean
    public ShadowReadComparator shadowReadComparator() {
        return new ShadowReadComparator();
    }

    /**
     * The clock is injected rather than taken from Instant.now() so that the
     * stamp written to both stores comes from one identifiable source.
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public DualStore<ShipmentEntity, String> shipmentDualStore(
            SourceStore<ShipmentEntity, String> source,
            TargetStore target,
            SchemaConverter<ShipmentEntity, String> converter,
            MigrationProperties migration,
            ShadowReadComparator comparator,
            Clock clock) {
        return new DualStore<>(source, target, converter, migration::phase, clock, comparator);
    }
}
