package com.example.shipment.convert;

import com.example.dualwrite.Document;
import com.example.dualwrite.DocumentKey;
import com.example.dualwrite.SchemaConverter;
import com.example.shipment.domain.ShipmentEntity;
import com.example.shipment.domain.ShipmentStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps the relational shipment row onto its document form.
 *
 * <p>One row becomes one document here, so the mapping is close to
 * mechanical. It would not be if several tables collapsed into one
 * document: shipmentId alone addresses the record, and orderId is the sort
 * key so that shipments can also be reached from the order side.
 */
@Component
public class ShipmentSchemaConverter implements SchemaConverter<ShipmentEntity, String> {

    @Override
    public String idOf(ShipmentEntity entity) {
        return entity.getShipmentId();
    }

    @Override
    public DocumentKey keyOf(ShipmentEntity entity) {
        return DocumentKey.of(entity.getShipmentId(), entity.getOrderId());
    }

    /**
     * Addressing by id alone needs the sort key too, so a read that has only
     * the shipment id has to recover the order id. Here it comes back from
     * the target's own record; a schema whose sort key were not derivable
     * this way would need a secondary index instead.
     */
    @Override
    public DocumentKey keyForId(String shipmentId) {
        return DocumentKey.of(shipmentId, null);
    }

    @Override
    public Document toDocument(ShipmentEntity e) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("shipmentId", e.getShipmentId());
        attributes.put("orderId", e.getOrderId());
        attributes.put("status", e.getStatus().name());
        attributes.put("itemCount", e.getItemCount());
        attributes.put("estimatedDeliveryDate", text(e.getEstimatedDeliveryDate()));
        attributes.put("executedDeliveryDate", text(e.getExecutedDeliveryDate()));
        attributes.put("createdAt", e.getCreatedAt());
        attributes.put("modifiedAt", e.getModifiedAt());
        return new Document(keyOf(e), attributes);
    }

    @Override
    public ShipmentEntity toEntity(Document d) {
        ShipmentEntity e = new ShipmentEntity(
            (String) d.get("shipmentId"),
            (String) d.get("orderId"),
            ShipmentStatus.valueOf((String) d.get("status")),
            ((Number) d.get("itemCount")).intValue(),
            date(d.get("estimatedDeliveryDate")));
        e.setExecutedDeliveryDate(date(d.get("executedDeliveryDate")));
        e.setCreatedAt((Instant) d.get("createdAt"));
        e.setModifiedAt((Instant) d.get("modifiedAt"));
        return e;
    }

    private static String text(LocalDate d) { return d == null ? null : d.toString(); }

    private static LocalDate date(Object v) {
        return v == null ? null : LocalDate.parse((String) v);
    }
}
