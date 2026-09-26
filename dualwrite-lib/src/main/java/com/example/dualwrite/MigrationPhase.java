package com.example.dualwrite;

/**
 * The phased cutover. Each phase changes where reads are served from and
 * whether the source still receives writes. Phases are advanced one at a
 * time, and only once the shadow-read match rate has held at its threshold.
 *
 * <p>Note the asymmetry at the end: every phase up to READ_TARGET can be
 * reversed for free, because the source still holds a complete copy.
 * TARGET_ONLY cannot -- once the source stops receiving writes it begins to
 * rot, and there is no way back that does not lose data.
 */
public enum MigrationPhase {

    /** Write both, read source. Target is populated but not trusted. */
    DUAL_WRITE_READ_SOURCE(true, true, false, false),

    /** Write both, read source, and compare every read against the target. */
    SHADOW_COMPARE(true, true, false, true),

    /** Write both, read target. Source is still a complete fallback. */
    READ_TARGET(true, true, true, false),

    /** Write target only, read target. No way back from here. */
    TARGET_ONLY(false, true, true, false);

    private final boolean writeSource;
    private final boolean writeTarget;
    private final boolean readTarget;
    private final boolean shadowRead;

    MigrationPhase(boolean writeSource, boolean writeTarget,
                   boolean readTarget, boolean shadowRead) {
        this.writeSource = writeSource;
        this.writeTarget = writeTarget;
        this.readTarget = readTarget;
        this.shadowRead = shadowRead;
    }

    public boolean writesSource() { return writeSource; }
    public boolean writesTarget() { return writeTarget; }
    public boolean readsTarget()  { return readTarget; }
    public boolean shadowReads()  { return shadowRead; }

    /** True while the source is authoritative and can still be reverted to. */
    public boolean isReversible() { return writeSource; }
}
