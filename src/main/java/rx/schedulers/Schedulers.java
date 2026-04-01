package rx.schedulers;

import rx.Scheduler;

public final class Schedulers {
    private static final Scheduler IO = new IOThreadScheduler();
    private static final Scheduler COMPUTATION = new ComputationScheduler();
    private static final Scheduler SINGLE = new SingleThreadScheduler();

    private Schedulers() {
        // Prevent instantiation
    }

    public static Scheduler io() {
        return IO;
    }

    public static Scheduler computation() {
        return COMPUTATION;
    }

    public static Scheduler single() {
        return SINGLE;
    }
}
