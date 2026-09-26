package com.example.shipment.config;

import com.example.dualwrite.MigrationPhase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Holds the current phase. Mutable on purpose: advancing a phase is an
 * operational act, taken once the match rate has held, not a redeploy.
 */
@Component
public class MigrationProperties {

    private volatile MigrationPhase phase;

    public MigrationProperties(@Value("${migration.phase:SHADOW_COMPARE}") MigrationPhase phase) {
        this.phase = phase;
    }

    public MigrationPhase phase() { return phase; }

    public void advanceTo(MigrationPhase next) { this.phase = next; }
}
