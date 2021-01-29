package com.example.retryfutures;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.ExecutionException;

@RunWith(MockitoJUnitRunner.class)
public class RetryWithFuturesTests {

    @InjectMocks
    private RetryWithFutures retryWithFutures;

    @Test
    public void testCompletesSuccessfully() throws Exception {
        this.retryWithFutures.successful = true;
        final var resultFuture = this.retryWithFutures.retryAnAction();
        final var result = resultFuture.get();

        System.out.println("the result was: " + result);

        Thread.sleep(10_000L);
    }

    @Test
    public void testCompletesUnsuccessfully() throws Exception {
        this.retryWithFutures.successful = false;
        final var resultFuture = this.retryWithFutures.retryAnAction();

        try {
            resultFuture.get();
        } catch (ExecutionException ex) {
            System.out.println("future completed with exception: " + ex.getCause());
        }

        Thread.sleep(10_000L);
    }

    @Test
    public void testCompletesWithTimeout() throws Exception {
        this.retryWithFutures.shouldTimeout = true;
        final var resultFuture = this.retryWithFutures.retryAnAction();

        try {
            resultFuture.get();
        } catch (Exception ex) {
            System.out.println("future completed with exception: " + ex.getCause());
        }

        Thread.sleep(10_000L);
    }
}
