package com.example.shipment.repo;

import com.example.dualwrite.SourceStore;
import com.example.shipment.domain.ShipmentEntity;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Adapts JPA to the library's SourceStore. */
@Component
public class ShipmentSourceStore implements SourceStore<ShipmentEntity, String> {

    private final ShipmentJpaRepository jpa;
    private final EntityManager em;

    public ShipmentSourceStore(ShipmentJpaRepository jpa, EntityManager em) {
        this.jpa = jpa;
        this.em = em;
    }

    @Override
    public Optional<ShipmentEntity> findById(String id) {
        return jpa.findById(id);
    }

    @Override
    public ShipmentEntity save(ShipmentEntity entity) {
        return jpa.save(entity);
    }

    /**
     * The committed row, not the one the caller is holding.
     *
     * <p>This is subtler than it looks. If the entity is already managed and
     * has been mutated, a plain find hands back that same mutated instance
     * from the persistence context -- the new value, not the old one. Using
     * it as the prior image would mean "undoing" an update by restoring the
     * value we are trying to roll back.
     *
     * <p>So: evict whatever the context is holding, then read again. A find
     * by id does not trigger an auto-flush, so the second read sees what is
     * actually committed. The result is detached, which also keeps it clear
     * of the save that follows.
     */
    @Override
    public Optional<ShipmentEntity> findCommittedById(String id) {
        ShipmentEntity inContext = em.find(ShipmentEntity.class, id);
        if (inContext != null) {
            em.detach(inContext);
        }
        ShipmentEntity committed = em.find(ShipmentEntity.class, id);
        if (committed != null) {
            em.detach(committed);
        }
        return Optional.ofNullable(committed);
    }
}
