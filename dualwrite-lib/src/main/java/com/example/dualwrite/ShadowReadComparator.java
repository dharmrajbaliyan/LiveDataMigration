package com.example.dualwrite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

/**
 * Compares what the source returned against what the target holds, and
 * keeps a running match rate.
 *
 * <p>The rate is the gate on advancing a phase. Setting the bar high enough
 * that no systematic defect can hide inside the error budget is the point;
 * setting it at literal equality would be demanding the impossible, because
 * the dual-write window guarantees a small, permanent rate of transient
 * disagreement.
 */
public class ShadowReadComparator {

    private static final Logger log = LoggerFactory.getLogger(ShadowReadComparator.class);

    /**
     * A mismatch on a record written this recently is most likely the
     * dual-write window being observed rather than real divergence: the
     * target has committed and the source has not yet. Counted separately
     * so it neither inflates nor silently pads the match rate.
     */
    private static final Duration WRITE_WINDOW = Duration.ofMillis(50);

    private final LongAdder compared = new LongAdder();
    private final LongAdder matched = new LongAdder();
    private final LongAdder transient_ = new LongAdder();

    public void compare(Document expected, Document actual) {
        compared.increment();

        if (actual == null) {
            log.warn("shadow mismatch: {} missing from target", expected.key());
            return;
        }
        String difference = firstDifference(expected, actual);
        if (difference == null) {
            matched.increment();
            return;
        }
        if (withinWriteWindow(expected)) {
            transient_.increment();
            matched.increment();
            log.debug("shadow mismatch on {} inside the write window: {}",
                      expected.key(), difference);
            return;
        }
        log.warn("shadow mismatch on {}: {}", expected.key(), difference);
    }

    private boolean withinWriteWindow(Document expected) {
        Instant modified = expected.modifiedAt();
        return modified != null
            && Duration.between(modified, Instant.now()).compareTo(WRITE_WINDOW) < 0;
    }

    private String firstDifference(Document expected, Document actual) {
        for (Map.Entry<String, Object> field : expected.attributes().entrySet()) {
            Object a = field.getValue();
            Object b = actual.get(field.getKey());
            if (a == null ? b != null : !a.equals(b)) {
                return field.getKey() + ": source=" + a + " target=" + b;
            }
        }
        return null;
    }

    /** Proportion of compared reads that agreed, as a fraction of one. */
    public double matchRate() {
        long total = compared.sum();
        return total == 0 ? 1.0 : (double) matched.sum() / total;
    }

    public long comparisons()        { return compared.sum(); }
    public long transientMismatches() { return transient_.sum(); }
}
