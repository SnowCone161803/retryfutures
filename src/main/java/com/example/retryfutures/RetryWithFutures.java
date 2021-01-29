package com.example.retryfutures;

import java.util.concurrent.*;

public class RetryWithFutures {

    private final boolean CANCEL_WILL_NOT_INTERRUPT = false;
    private final boolean CANCEL_WILL_INTERRUPT = true;

    private final ScheduledExecutorService scheduler =
        Executors.newScheduledThreadPool(1);

    public boolean successful = true;
    public boolean shouldTimeout = false;

    public CompletableFuture<String> retryAnAction() {
        final var result = new CompletableFuture<String>();
        final var initialDelay = 1;
        final var interval = 1;

        final var state = new State();
        final var cancel = scheduler.scheduleAtFixedRate(
            () -> performActionWithRetry(result, state),
            initialDelay, interval, TimeUnit.SECONDS);
        state.cancel = cancel;

        final var timeout = 4;
        final boolean canInterruptWhileRunning = true;
        scheduler.schedule(
            () -> {
                cancel.cancel(canInterruptWhileRunning);
                result.completeExceptionally(new FailedInSomeWay("timed out"));
            },
            timeout,
            TimeUnit.SECONDS);
        return result;
    }

    private void performActionWithRetry(
        CompletableFuture<String> futureResult,
        State state) {
        System.out.println("Attempts left: " + state.attemptsLeft);
        if (state.attemptsLeft == 2 && !this.shouldTimeout) {
            if (this.successful) {
                futureResult.complete("completed successfully");
            } else {
                futureResult.completeExceptionally(new FailedInSomeWay("Did not complete successfully"));
            }
            state.cancel.cancel(false);
        }
        --state.attemptsLeft;
    }

    private class State  {
        public int attemptsLeft = 3;
        public Future cancel;
    }
}
