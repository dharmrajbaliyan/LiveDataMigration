package com.example.dualwrite;

import java.util.Optional;

/**
 * The relational side. Implemented over whatever persistence API the
 * application already uses -- here, hand-written JPA repository methods.
 *
 * @param <E> the entity type
 * @param <K> its identifier type
 */
public interface SourceStore<E, K> {

    Optional<E> findById(K id);

    E save(E entity);

    /**
     * The committed state of a record, read without the persistence
     * context's in-flight changes.
     *
     * <p>This is what makes a correct compensation possible. Undoing an
     * update means restoring the value the record held before the
     * transaction started, so that value has to be captured before the
     * target is written -- and it has to be the committed one, not the
     * mutated instance the caller is holding.
     */
    Optional<E> findCommittedById(K id);
}
