package com.example.shipment.repo;

import com.example.dualwrite.DualStore;
import com.example.shipment.domain.ShipmentEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * The repository the application calls. Both methods are hand-written and
 * route through the dual-write library rather than going straight to JPA.
 *
 * <p>Keeping this layer explicit is a deliberate choice. The same effect
 * can be had transparently, by hooking the persistence framework's own
 * load and flush events, and that is attractive when a large codebase
 * cannot be touched. It also hides the protocol: a reader has to understand
 * the framework's event model before they can see what happens to a write.
 * Two methods of pass-through is a small price for keeping it legible.
 */
@Repository
public class ShipmentRepository {

    private final DualStore<ShipmentEntity, String> dualStore;

    public ShipmentRepository(DualStore<ShipmentEntity, String> dualStore) {
        this.dualStore = dualStore;
    }

    public Optional<ShipmentEntity> get(String shipmentId) {
        return dualStore.findById(shipmentId);
    }

    public ShipmentEntity save(ShipmentEntity shipment) {
        return dualStore.save(shipment);
    }
}
