package com.ashik.askaboutme.utils;

import java.util.concurrent.ExecutionException;

public class CompletableFutureBlock {

    public static <T> T block(java.util.concurrent.CompletableFuture<T> future) {
        try {
            return future.get();
        }catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Thread was interrupted while blocking on CompletableFuture", e);
        }catch (ExecutionException e) {
            throw new IllegalStateException("Error while blocking on CompletableFuture", e);
        }
        catch (Exception e) {
            throw new RuntimeException("Error while blocking on CompletableFuture", e);
        }
    }
}
