package com.example.retryfutures;

import java.util.concurrent.*;
import java.util.function.Supplier;

public class RetryWithFutures<T> {

    public static final Object FAILED_ATTEMPT = null;

    private final boolean CANCEL_WILL_NOT_INTERRUPT = false;
    private final boolean CANCEL_WILL_INTERRUPT = true;

    private final ScheduledExecutorService scheduler =
        Executors.newScheduledThreadPool(1);

    // to show multiple attempts before success or failure
    public boolean performActionOnSecondAttempt = false;

    public CompletableFuture<T> retryAnAction(Supplier<T> callback) {
        final var initialDelay = 1;
        final var interval = 1;

        final var state = new State(callback);

        // setup retry interval
        state.cancelIntervalSchedule = scheduler.scheduleAtFixedRate(
            () -> performActionWithRetry(state),
            initialDelay, interval, TimeUnit.SECONDS);

        // handle global timeout timeout
        final var timeout = 4;
        scheduler.schedule(
            () -> state.failure(new FailedInSomeWay("timed out")),
            timeout, TimeUnit.SECONDS);
        return state.resultFuture;
    }

    private void performActionWithRetry(State state) {
        ++state.attemptNumber;
        System.out.println("Attempts number: " + state.attemptNumber);
        if (this.performActionOnSecondAttempt && state.attemptNumber != 2) {
            return;
        }
        try {
            final var result = state.callback.get();
            if (result == FAILED_ATTEMPT) {
                return;
            }
            state.success(result);
        } catch (Exception ex) {
            state.failure(ex);
        }
    }

    private class State  {

        public CompletableFuture<T> resultFuture = new CompletableFuture<>();
        public int attemptNumber = 0;

        private Future cancelIntervalSchedule;
        private final Supplier<T> callback;

        State(Supplier<T> callback) {
            this.callback = callback;
        }

        public void success(T result) {
            this.resultFuture.complete(result);
            this.cancelIntervalSchedule.cancel(CANCEL_WILL_NOT_INTERRUPT);
        }

        public void failure(Throwable t) {
            this.resultFuture.completeExceptionally(t);
            this.cancelIntervalSchedule.cancel(CANCEL_WILL_INTERRUPT);
        }
    }
}
