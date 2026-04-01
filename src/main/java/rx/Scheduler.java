package rx;

public interface Scheduler {
    void execute(Runnable task);
}
