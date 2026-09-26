package com.example.dualwrite;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A record in the target store, as a plain map of attributes.
 *
 * <p>Keeping this neutral is what lets the library stay independent of the
 * target engine: nothing here knows about DynamoDB. A TargetStore
 * implementation translates to and from whatever its engine expects.
 */
public final class Document {

    private final DocumentKey key;
    private final Map<String, Object> attributes;

    public Document(DocumentKey key, Map<String, Object> attributes) {
        this.key = key;
        this.attributes = new LinkedHashMap<>(attributes);
    }

    public DocumentKey key() { return key; }

    public Map<String, Object> attributes() { return attributes; }

    public Object get(String name) { return attributes.get(name); }

    /**
     * The last-modified stamp, used both for conflict resolution during
     * backfill and as the condition guarding a compensation.
     */
    public Instant modifiedAt() { return (Instant) attributes.get("modifiedAt"); }

    @Override public String toString() { return key + " " + attributes; }
}
