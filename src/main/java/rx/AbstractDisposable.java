package rx;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractDisposable implements Disposable {
    private final AtomicBoolean disposed = new AtomicBoolean(false);

    @Override
    public final void dispose() {
        if (disposed.compareAndSet(false, true)) {
            onDispose();
        }
    }

    @Override
    public final boolean isDisposed() {
        return disposed.get();
    }

    protected void onDispose() {
        // Override in subclasses to perform cleanup
    }
}
