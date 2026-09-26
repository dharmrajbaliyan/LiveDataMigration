package com.example.dualwrite;

import java.time.Instant;
import java.util.Optional;

/**
 * The key-value or document side.
 *
 * <p>The framework needs very little from this store: atomic single-record
 * writes, and a conditional form of them so that a compensation can check
 * what it is about to undo. Both are properties of virtually every document
 * and key-value engine.
 */
public interface TargetStore {

    Optional<Document> get(DocumentKey key);

    void put(Document document);

    /**
     * Undo a write, conditionally.
     *
     * <p>If {@code prior} is null the write was an insert, so undoing it
     * means deleting the record. Otherwise it was an update, and undoing it
     * means restoring the prior image -- deleting would not restore the old
     * value but destroy a record that legitimately existed.
     *
     * <p>Either way the operation applies only if the record still holds
     * what this transaction wrote. A concurrent writer that legitimately
     * advanced the record in the meantime must survive.
     */
    void undo(DocumentKey key, Document prior, Instant writtenAt);
}
