package com.example.dualwrite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Writes to a relational source and a key-value target inside one
 * application-level operation, and reads back from whichever of them the
 * current migration phase says is authoritative.
 *
 * <p>There is no two-phase commit between the two stores, so this is not an
 * atomic write. What it is instead is an ordering chosen so that the window
 * in which the two can disagree is as small as possible, plus a compensation
 * that closes the window when the source commit fails.
 *
 * @param <E> the relational entity type
 * @param <K> its identifier type
 */
public class DualStore<E, K> {

    private static final Logger log = LoggerFactory.getLogger(DualStore.class);

    private final SourceStore<E, K> source;
    private final TargetStore target;
    private final SchemaConverter<E, K> converter;
    private final Supplier<MigrationPhase> phase;
    private final Clock clock;
    private final ShadowReadComparator comparator;

    public DualStore(SourceStore<E, K> source,
                     TargetStore target,
                     SchemaConverter<E, K> converter,
                     Supplier<MigrationPhase> phase,
                     Clock clock,
                     ShadowReadComparator comparator) {
        this.source = source;
        this.target = target;
        this.converter = converter;
        this.phase = phase;
        this.clock = clock;
        this.comparator = comparator;
    }

    /**
     * The dual-write path. Must be called inside a source transaction.
     *
     * <pre>
     *   1. transaction is already open
     *   2. write the source          -- uncommitted, invisible to readers
     *   3. arm the compensation, then write the target -- commits on ACK
     *   4. return; the source transaction commits
     * </pre>
     *
     * <p>Between 3 and 4 the target holds a committed write while the source
     * does not. That window is where every failure mode of this protocol
     * lives, and it is deliberately kept to a single network round trip.
     * If the commit in step 4 fails, the compensation armed in step 3 runs.
     * If the process dies before either, the write is left dangling in the
     * target and only the anti-entropy sweep will find it.
     */
    public E save(E entity) {
        MigrationPhase current = phase.get();
        K id = converter.idOf(entity);

        // One timestamp, minted once, written to both stores. Comparing the
        // two copies later compares two copies of one clock reading rather
        // than two clocks, so no skew between the stores can arise.
        // Truncated to milliseconds because the target cannot represent more,
        // and a value that survives the round trip unchanged is worth more
        // than the extra precision.
        Instant now = clock.instant().truncatedTo(ChronoUnit.MILLIS);
        stamp(entity, now);

        // The committed image, captured before anything is written. An
        // update can only be undone by restoring this.
        Document prior = current.writesTarget()
            ? source.findCommittedById(id).map(converter::toDocument).orElse(null)
            : null;

        E saved = current.writesSource() ? source.save(entity) : entity;

        if (current.writesTarget()) {
            Document document = converter.toDocument(saved);
            DocumentKey key = document.key();

            // Armed before the write, so it is already in place if the
            // commit that follows fails.
            if (current.writesSource()) {
                Compensations.onRollback(() -> {
                    log.warn("source rolled back, undoing target write for {}", key);
                    target.undo(key, prior, now);
                });
            }
            target.put(document);
        }
        return saved;
    }

    /**
     * Reads from whichever store the current phase trusts, and -- during the
     * shadow phase -- issues the same read against the other one and
     * compares.
     *
     * <p>The comparison is one-directional by construction. It starts from
     * the source, so a record that exists only in the target is invisible to
     * it. Finding those is the anti-entropy sweep's job, not this one's.
     */
    public Optional<E> findById(K id) {
        MigrationPhase current = phase.get();

        if (current.readsTarget()) {
            return target.get(converter.keyForId(id)).map(converter::toEntity);
        }

        Optional<E> fromSource = source.findById(id);
        if (current.shadowReads() && fromSource.isPresent()) {
            shadowCompare(fromSource.get());
        }
        return fromSource;
    }

    private void shadowCompare(E sourceEntity) {
        try {
            Document expected = converter.toDocument(sourceEntity);
            Document actual = target.get(expected.key()).orElse(null);
            comparator.compare(expected, actual);
        } catch (RuntimeException e) {
            // A shadow read is an observation, never part of the answer.
            // It must not be able to fail the request it is observing.
            log.warn("shadow read failed", e);
        }
    }

    /** Set modifiedAt (and createdAt on first write) from the single reading. */
    private void stamp(E entity, Instant now) {
        if (entity instanceof Timestamped t) {
            if (t.getCreatedAt() == null) t.setCreatedAt(now);
            t.setModifiedAt(now);
        }
    }

    /** Implemented by entities that carry the stamps the protocol relies on. */
    public interface Timestamped {
        Instant getCreatedAt();
        void setCreatedAt(Instant at);
        void setModifiedAt(Instant at);
    }
}
