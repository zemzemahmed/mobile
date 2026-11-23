package com.example.medireminder.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * One single background thread shared by all repositories.
 * Room forbids database work on the main (UI) thread, so we use this everywhere.
 */
public final class AppExecutor {

    private static final ExecutorService DATABASE = Executors.newSingleThreadExecutor();

    private AppExecutor() {
        // Nobody needs to create an object of this class.
    }

    public static ExecutorService database() {
        return DATABASE;
    }
}
