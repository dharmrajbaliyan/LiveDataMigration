package com.example.dualwrite;

import java.util.Objects;

/** Partition key plus optional sort key. */
public record DocumentKey(String partition, String sort) {

    public DocumentKey {
        Objects.requireNonNull(partition, "partition key is required");
    }

    public static DocumentKey of(String partition, String sort) {
        return new DocumentKey(partition, sort);
    }

    @Override public String toString() {
        return sort == null ? partition : partition + "/" + sort;
    }
}
