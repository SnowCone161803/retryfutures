package com.example.retryfutures;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.ExecutionException;

@RunWith(MockitoJUnitRunner.class)
public class RetryWithFuturesTests {

    @InjectMocks
    private RetryWithFutures<String> retryWithFutures;

    @Test
    public void testCompletesSuccessfully() throws Exception {
        this.retryWithFutures.performActionOnSecondAttempt = true;
        final var resultFuture = this.retryWithFutures.retryAnAction(() -> "success");

        // blocks waiting for a result
        final var result = resultFuture.get();

        System.out.println("the result was: " + result);
    }

    @Test
    public void testCompletesUnsuccessfully() throws Exception {
        this.retryWithFutures.performActionOnSecondAttempt = true;
        final var resultFuture =
            this.retryWithFutures.retryAnAction(() -> {
                throw new RuntimeException("UNITTEST: expected exception");
            });

        try {
            // blocks waiting for a result
            resultFuture.get();
        } catch (ExecutionException ex) {
            System.out.println("future completed with exception: " + ex.getCause());
        }
    }

    @Test
    public void testCompletesWithTimeout() throws Exception {
        // never succeeds
        final String failedAttempt = null;
        final var resultFuture = this.retryWithFutures.retryAnAction(() -> failedAttempt);

        try {
            resultFuture.get();
        } catch (Exception ex) {
            System.out.println("future completed with exception: " + ex.getCause());
        }
    }
}
