package com.example.shipment.domain;

import com.example.dualwrite.DualStore;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "shipment")
public class ShipmentEntity implements DualStore.Timestamped {

    @Id
    @Column(name = "shipment_id")
    private String shipmentId;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentStatus status;

    @Column(name = "item_count")
    private int itemCount;

    @Column(name = "estimated_delivery_date")
    private LocalDate estimatedDeliveryDate;

    @Column(name = "executed_delivery_date")
    private LocalDate executedDeliveryDate;

    // Stamped by the library, not by the database, so that one reading can
    // be written to both stores.
    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "modified_at")
    private Instant modifiedAt;

    protected ShipmentEntity() {}

    public ShipmentEntity(String shipmentId, String orderId, ShipmentStatus status,
                          int itemCount, LocalDate estimatedDeliveryDate) {
        this.shipmentId = shipmentId;
        this.orderId = orderId;
        this.status = status;
        this.itemCount = itemCount;
        this.estimatedDeliveryDate = estimatedDeliveryDate;
    }

    public String getShipmentId() { return shipmentId; }
    public String getOrderId()    { return orderId; }
    public ShipmentStatus getStatus() { return status; }
    public int getItemCount()     { return itemCount; }
    public LocalDate getEstimatedDeliveryDate() { return estimatedDeliveryDate; }
    public LocalDate getExecutedDeliveryDate()  { return executedDeliveryDate; }

    public void setOrderId(String orderId) { this.orderId = orderId; }
    public void setStatus(ShipmentStatus status) { this.status = status; }
    public void setItemCount(int itemCount) { this.itemCount = itemCount; }
    public void setEstimatedDeliveryDate(LocalDate d) { this.estimatedDeliveryDate = d; }
    public void setExecutedDeliveryDate(LocalDate d)  { this.executedDeliveryDate = d; }

    @Override public Instant getCreatedAt() { return createdAt; }
    @Override public void setCreatedAt(Instant at) { this.createdAt = at; }
    public Instant getModifiedAt() { return modifiedAt; }
    @Override public void setModifiedAt(Instant at) { this.modifiedAt = at; }
}
