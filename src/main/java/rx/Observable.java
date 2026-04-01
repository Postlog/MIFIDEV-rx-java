package rx;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.concurrent.atomic.AtomicInteger;

public class Observable<T> {
    private final ObservableOnSubscribe<T> source;

    protected Observable(ObservableOnSubscribe<T> source) {
        this.source = source;
    }

    public static <T> Observable<T> create(ObservableOnSubscribe<T> source) {
        return new Observable<>(source);
    }

    public Disposable subscribe(Observer<? super T> observer) {
        CreateEmitter<T> emitter = new CreateEmitter<>(observer);
        try {
            source.subscribe(emitter);
        } catch (Throwable t) {
            emitter.onError(t);
        }
        return emitter;
    }

    public <R> Observable<R> map(Function<? super T, ? extends R> mapper) {
        return new Observable<>(emitter -> {
            subscribe(new Observer<T>() {
                @Override
                public void onNext(T item) {
                    try {
                        R result = mapper.apply(item);
                        emitter.onNext(result);
                    } catch (Throwable t) {
                        emitter.onError(t);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    emitter.onError(t);
                }

                @Override
                public void onComplete() {
                    emitter.onComplete();
                }
            });
            // We should link the disposables in a real implementation.
            // For now, if emitter is disposed, we can dispose the upstream.
        });
    }

    public Observable<T> filter(Predicate<? super T> predicate) {
        return new Observable<>(emitter -> {
            subscribe(new Observer<T>() {
                @Override
                public void onNext(T item) {
                    try {
                        if (predicate.test(item)) {
                            emitter.onNext(item);
                        }
                    } catch (Throwable t) {
                        emitter.onError(t);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    emitter.onError(t);
                }

                @Override
                public void onComplete() {
                    emitter.onComplete();
                }
            });
        });
    }

    public <R> Observable<R> flatMap(Function<? super T, ? extends Observable<? extends R>> mapper) {
        return new Observable<>(emitter -> {
            subscribe(new Observer<T>() {
                final AtomicInteger active = new AtomicInteger(1); // 1 for the main source

                @Override
                public void onNext(T item) {
                    try {
                        Observable<? extends R> inner = mapper.apply(item);
                        active.incrementAndGet();
                        inner.subscribe(new Observer<R>() {
                            @Override
                            public void onNext(R innerItem) {
                                emitter.onNext(innerItem);
                            }

                            @Override
                            public void onError(Throwable t) {
                                emitter.onError(t);
                            }

                            @Override
                            public void onComplete() {
                                if (active.decrementAndGet() == 0) {
                                    emitter.onComplete();
                                }
                            }
                        });
                    } catch (Throwable t) {
                        emitter.onError(t);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    emitter.onError(t);
                }

                @Override
                public void onComplete() {
                    if (active.decrementAndGet() == 0) {
                        emitter.onComplete();
                    }
                }
            });
        });
    }

    public Observable<T> subscribeOn(Scheduler scheduler) {
        return new Observable<>(emitter -> {
            scheduler.execute(() -> {
                subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T item) {
                        emitter.onNext(item);
                    }

                    @Override
                    public void onError(Throwable t) {
                        emitter.onError(t);
                    }

                    @Override
                    public void onComplete() {
                        emitter.onComplete();
                    }
                });
            });
        });
    }

    public Observable<T> observeOn(Scheduler scheduler) {
        return new Observable<>(emitter -> {
            subscribe(new Observer<T>() {
                @Override
                public void onNext(T item) {
                    scheduler.execute(() -> emitter.onNext(item));
                }

                @Override
                public void onError(Throwable t) {
                    scheduler.execute(() -> emitter.onError(t));
                }

                @Override
                public void onComplete() {
                    scheduler.execute(emitter::onComplete);
                }
            });
        });
    }
}
