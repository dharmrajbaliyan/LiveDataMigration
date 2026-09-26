package com.example.shipment;

import com.example.shipment.domain.ShipmentEntity;
import com.example.shipment.domain.ShipmentStatus;
import com.example.shipment.repo.ShipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.NoSuchElementException;

@Service
public class ShipmentService {

    private final ShipmentRepository shipments;

    public ShipmentService(ShipmentRepository shipments) {
        this.shipments = shipments;
    }

    /**
     * Read-modify-write, inside one transaction.
     *
     * <p>The transaction boundary is what the compensation hangs off: if the
     * commit at the end of this method fails, the library's rollback
     * callback fires and undoes the target write that already succeeded.
     */
    @Transactional
    public ShipmentEntity updateStatus(String shipmentId, ShipmentStatus next) {
        return updateStatus(shipmentId, next, false);
    }

    /**
     * The same update, with the option of failing after the target write has
     * already committed.
     *
     * <p>This exists to make the dangerous window reachable on demand. The
     * target write succeeds, then the source transaction rolls back, and the
     * compensation registered by the library has to put the target back the
     * way it was. It is the one path that is hard to observe in production
     * and the one most worth being able to watch.
     */
    @Transactional
    public ShipmentEntity updateStatus(String shipmentId, ShipmentStatus next,
                                       boolean failAfterTargetWrite) {
        ShipmentEntity shipment = shipments.get(shipmentId)
            .orElseThrow(() -> new NoSuchElementException("no shipment " + shipmentId));

        if (!shipment.getStatus().canAdvanceTo(next)) {
            throw new IllegalStateException(
                "shipment " + shipmentId + " cannot move from "
                + shipment.getStatus() + " to " + next);
        }
        shipment.setStatus(next);
        if (next == ShipmentStatus.OUTBOUND) {
            shipment.setExecutedDeliveryDate(LocalDate.now());
        }
        ShipmentEntity saved = shipments.save(shipment);

        if (failAfterTargetWrite) {
            throw new SimulatedCommitFailure(
                "target write committed, now failing the source transaction");
        }
        return saved;
    }

    /** Marker for the injected failure above. */
    public static class SimulatedCommitFailure extends RuntimeException {
        public SimulatedCommitFailure(String message) { super(message); }
    }

    @Transactional(readOnly = true)
    public ShipmentEntity get(String shipmentId) {
        return shipments.get(shipmentId)
            .orElseThrow(() -> new NoSuchElementException("no shipment " + shipmentId));
    }

    @Transactional
    public ShipmentEntity create(ShipmentEntity shipment) {
        return shipments.save(shipment);
    }
}
