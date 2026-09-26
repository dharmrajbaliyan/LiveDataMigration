package com.example.dualwrite;

/**
 * Maps one relational entity onto one target-store document.
 *
 * <p>This is the only part of a migration that is genuinely
 * application-specific. Everything else in this library -- the write
 * ordering, the compensation, the phase routing -- is the same whatever the
 * schema looks like.
 *
 * @param <E> the relational entity type
 * @param <K> its identifier type
 */
public interface SchemaConverter<E, K> {

    /** The entity's identifier, used to look up its prior state. */
    K idOf(E entity);

    /** Where this entity lives in the target store. */
    DocumentKey keyOf(E entity);

    /** Same, addressed by identifier alone, for reads. */
    DocumentKey keyForId(K id);

    /** Relational to document. */
    Document toDocument(E entity);

    /** Document back to relational, for reads served from the target. */
    E toEntity(Document document);
}
