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
        final var initialDelay = 1;
        final var interval = 1;

        final var state = new State();
        state.cancelIntervalSchedule = scheduler.scheduleAtFixedRate(
            () -> performActionWithRetry(state),
            initialDelay, interval, TimeUnit.SECONDS);

        final var timeout = 4;
        scheduler.schedule(
            () -> {
                state.cancelIntervalSchedule.cancel(CANCEL_WILL_INTERRUPT);
                state.resultFuture.completeExceptionally(new FailedInSomeWay("timed out"));
            },
            timeout,
            TimeUnit.SECONDS);
        return state.resultFuture;
    }

    private void performActionWithRetry(
        State state) {
        System.out.println("Attempts left: " + state.attemptsLeft);
        if (state.attemptsLeft == 2 && !this.shouldTimeout) {
            if (this.successful) {
                state.resultFuture.complete("completed successfully");
            } else {
                state.resultFuture.completeExceptionally(new FailedInSomeWay("Did not complete successfully"));
            }
            state.cancelIntervalSchedule.cancel(CANCEL_WILL_NOT_INTERRUPT);
        }
        --state.attemptsLeft;
    }

    private class State  {
        public CompletableFuture<String> resultFuture = new CompletableFuture<>();
        public int attemptsLeft = 3;
        public Future cancelIntervalSchedule;
    }
}
