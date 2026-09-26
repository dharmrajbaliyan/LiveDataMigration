package com.example.shipment.repo;

import com.example.shipment.domain.ShipmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/** Plain Spring Data JPA. The dual-write logic sits above this, not in it. */
public interface ShipmentJpaRepository extends JpaRepository<ShipmentEntity, String> {
}
