package com.example.shipment.domain;

/**
 * Shipment lifecycle. The order matters: a shipment only ever moves
 * forward, which is what lets the anti-entropy sweep spot a target record
 * that has run ahead of the source by comparing ordinals rather than
 * comparing every field.
 */
public enum ShipmentStatus {
    INIT, PICK, PACK, OUTBOUND;

    public boolean canAdvanceTo(ShipmentStatus next) {
        return next.ordinal() > this.ordinal();
    }
}
