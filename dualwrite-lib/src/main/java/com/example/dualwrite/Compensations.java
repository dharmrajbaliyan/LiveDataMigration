package com.example.dualwrite;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Registers an action to run if the surrounding source transaction rolls
 * back.
 *
 * <p>The library asks only two things of its host environment, and this is
 * the second of them: a callback that fires once the transaction has
 * finished and reports which way it went. Most transaction managers offer
 * one; this implementation uses Spring's.
 */
public final class Compensations {

    private Compensations() {}

    public static void onRollback(Runnable compensation) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException(
                "dual write must run inside a transaction: without one there is "
              + "no rollback signal, and a failed commit would leave the target "
              + "holding a write the source never kept");
        }
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) compensation.run();
                }
            });
    }
}
