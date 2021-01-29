package com.example.retryfutures;

import java.util.concurrent.*;
import java.util.function.Supplier;

public class RetryWithFutures<T> {

    public static final Object FAILED_ATTEMPT = null;

    private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;
    private static final int GLOBAL_TIMEOUT = 4;
    private static final int INTERVAL = 1;
    private static final int INITIAL_DELAY = 0;

    private final boolean CANCEL_WILL_NOT_INTERRUPT = false;
    private final boolean CANCEL_WILL_INTERRUPT = true;

    private final ScheduledExecutorService scheduler =
        Executors.newScheduledThreadPool(1);

    // to show multiple attempts before success or failure
    // used to make test output more clear
    public boolean performActionOnSecondAttempt = false;

    /**
     * Retry an action specified as a callback.
     *
     * <li>When the callback returns a value, complete the future with the value</li>
     * <li>When the callback returns 'null', then retry the action</li>
     * <li>When the callback throws, complete the future exceptionally</li>
     * <li>if the global timeout is reached, complete the future exceptionally</li>
     * @param callback action to retry
     * @return future that completes when on the first successful retry
     */
    public CompletableFuture<T> retryAnAction(Supplier<T> callback) {
        final var state = new State(callback);

        // setup retry interval
        state.cancelIntervalSchedule = scheduler.scheduleAtFixedRate(
            () -> performActionWithRetry(state),
            INITIAL_DELAY, INTERVAL, TIME_UNIT);

        // handle global timeout timeout
        final var timeout = 4;
        scheduler.schedule(
            () -> state.failure(new FailedInSomeWay("timed out")),
            GLOBAL_TIMEOUT, TIME_UNIT);
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

        // method to retry
        final Supplier<T> callback;

        // future returned to be completed later
        CompletableFuture<T> resultFuture = new CompletableFuture<>();
        int attemptNumber = 0;

        // used to cancel the interval from running any more
        Future cancelIntervalSchedule;

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
