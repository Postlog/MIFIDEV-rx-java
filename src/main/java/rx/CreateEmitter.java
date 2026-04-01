package rx;

import java.util.concurrent.atomic.AtomicBoolean;

public class CreateEmitter<T> extends AbstractDisposable implements ObservableEmitter<T> {
    private final Observer<? super T> observer;
    private final AtomicBoolean done = new AtomicBoolean(false);

    public CreateEmitter(Observer<? super T> observer) {
        this.observer = observer;
    }

    @Override
    public void onNext(T item) {
        if (!isDisposed() && !done.get()) {
            observer.onNext(item);
        }
    }

    @Override
    public void onError(Throwable t) {
        if (!isDisposed() && done.compareAndSet(false, true)) {
            observer.onError(t);
            dispose();
        }
    }

    @Override
    public void onComplete() {
        if (!isDisposed() && done.compareAndSet(false, true)) {
            observer.onComplete();
            dispose();
        }
    }
}
