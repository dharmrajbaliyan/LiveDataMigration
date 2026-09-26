package com.example.shipment.api;

import com.example.dualwrite.MigrationPhase;
import com.example.dualwrite.ShadowReadComparator;
import com.example.shipment.ShipmentService;
import com.example.shipment.config.MigrationProperties;
import com.example.shipment.domain.ShipmentEntity;
import com.example.shipment.domain.ShipmentStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/shipments")
public class ShipmentController {

    private final ShipmentService shipments;
    private final MigrationProperties migration;
    private final ShadowReadComparator comparator;

    public ShipmentController(ShipmentService shipments,
                              MigrationProperties migration,
                              ShadowReadComparator comparator) {
        this.shipments = shipments;
        this.migration = migration;
        this.comparator = comparator;
    }

    /** Read the status of a shipment. */
    @GetMapping("/{id}/status")
    public Map<String, Object> readStatus(@PathVariable String id) {
        ShipmentEntity s = shipments.get(id);
        return Map.of(
            "shipmentId", s.getShipmentId(),
            "orderId", s.getOrderId(),
            "status", s.getStatus(),
            "itemCount", s.getItemCount(),
            "modifiedAt", String.valueOf(s.getModifiedAt()),
            "servedFrom", migration.phase().readsTarget() ? "target" : "source");
    }

    /** Advance the status of a shipment. */
    @PutMapping("/{id}/status")
    public Map<String, Object> updateStatus(
            @PathVariable String id,
            @RequestParam ShipmentStatus to,
            @RequestParam(defaultValue = "false") boolean failAfterTargetWrite) {
        ShipmentEntity s = shipments.updateStatus(id, to, failAfterTargetWrite);
        return Map.of(
            "shipmentId", s.getShipmentId(),
            "status", s.getStatus(),
            "modifiedAt", String.valueOf(s.getModifiedAt()));
    }

    /** Current phase and the match rate that gates advancing past it. */
    @GetMapping("/migration")
    public Map<String, Object> migrationStatus() {
        return Map.of(
            "phase", migration.phase(),
            "reversible", migration.phase().isReversible(),
            "shadowComparisons", comparator.comparisons(),
            "matchRate", comparator.matchRate(),
            "transientMismatches", comparator.transientMismatches());
    }

    @PutMapping("/migration")
    public Map<String, Object> advancePhase(@RequestParam MigrationPhase to) {
        migration.advanceTo(to);
        return migrationStatus();
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> notFound(NoSuchElementException e) {
        return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(ShipmentService.SimulatedCommitFailure.class)
    public ResponseEntity<Map<String, String>> rolledBack(
            ShipmentService.SimulatedCommitFailure e) {
        return ResponseEntity.status(500).body(Map.of(
            "error", e.getMessage(),
            "expect", "compensation restores the target to its prior image"));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> conflict(IllegalStateException e) {
        return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
    }
}
