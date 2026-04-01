package rx;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class ObservableTest {

    @Test
    public void testCreateAndSubscribe() {
        List<String> items = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);

        Observable<String> observable = Observable.create(emitter -> {
            emitter.onNext("A");
            emitter.onNext("B");
            emitter.onComplete();
        });

        observable.subscribe(new Observer<String>() {
            @Override
            public void onNext(String item) {
                items.add(item);
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
                completed.set(true);
            }
        });

        assertEquals(Arrays.asList("A", "B"), items);
        assertTrue(completed.get());
    }

    @Test
    public void testMap() {
        List<Integer> items = new ArrayList<>();

        Observable.create((ObservableEmitter<String> emitter) -> {
            emitter.onNext("1");
            emitter.onNext("2");
            emitter.onComplete();
        }).map(Integer::parseInt).subscribe(new Observer<Integer>() {
            @Override
            public void onNext(Integer item) {
                items.add(item);
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }
        });

        assertEquals(Arrays.asList(1, 2), items);
    }

    @Test
    public void testFilter() {
        List<Integer> items = new ArrayList<>();

        Observable.create((ObservableEmitter<Integer> emitter) -> {
            emitter.onNext(1);
            emitter.onNext(2);
            emitter.onNext(3);
            emitter.onNext(4);
            emitter.onComplete();
        }).filter(i -> i % 2 == 0).subscribe(new Observer<Integer>() {
            @Override
            public void onNext(Integer item) {
                items.add(item);
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }
        });

        assertEquals(Arrays.asList(2, 4), items);
    }

    @Test
    public void testErrorHandling() {
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        AtomicBoolean completedRef = new AtomicBoolean(false);

        Observable.create((ObservableEmitter<String> emitter) -> {
            emitter.onNext("A");
            emitter.onError(new RuntimeException("Test Exception"));
            emitter.onComplete(); // Should be ignored because of error
        }).subscribe(new Observer<String>() {
            @Override
            public void onNext(String item) {
            }

            @Override
            public void onError(Throwable t) {
                errorRef.set(t);
            }

            @Override
            public void onComplete() {
                completedRef.set(true);
            }
        });

        assertNotNull(errorRef.get());
        assertEquals("Test Exception", errorRef.get().getMessage());
        assertFalse(completedRef.get());
    }

    @Test
    public void testFlatMap() {
        List<String> items = new ArrayList<>();
        Observable.create((ObservableEmitter<Integer> emitter) -> {
            emitter.onNext(1);
            emitter.onNext(2);
            emitter.onComplete();
        }).flatMap(i -> Observable.create((ObservableEmitter<String> inner) -> {
            inner.onNext(i + "a");
            inner.onNext(i + "b");
            inner.onComplete();
        })).subscribe(new Observer<String>() {
            @Override
            public void onNext(String item) {
                items.add(item);
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }
        });

        // The exact order might be 1a, 1b, 2a, 2b since it's synchronous here
        assertEquals(Arrays.asList("1a", "1b", "2a", "2b"), items);
    }

    @Test
    public void testDisposable() {
        List<String> items = new ArrayList<>();
        Observable<String> obs = Observable.create(emitter -> {
            emitter.onNext("1");
            emitter.onNext("2");
            // imagine some async process, we dispose now
            // Actually, for sync, we just emit, but we can check if it's disposed
            if (!emitter.isDisposed()) {
                emitter.onNext("3");
            }
            emitter.onComplete();
        });

        Disposable d = obs.subscribe(new Observer<String>() {
            int count = 0;
            @Override
            public void onNext(String item) {
                items.add(item);
                count++;
            }

            @Override
            public void onError(Throwable t) {}

            @Override
            public void onComplete() {}
        });

        // Sync observable completes immediately, so this dispose doesn't prevent "3"
        // But let's test a custom one that breaks out
        
        List<Integer> list = new ArrayList<>();
        Observable<Integer> cancellableObs = Observable.create(emitter -> {
            for (int i = 0; i < 10; i++) {
                if (emitter.isDisposed()) break;
                emitter.onNext(i);
            }
        });
        
        cancellableObs.subscribe(new Observer<Integer>() {
            @Override
            public void onNext(Integer item) {
                list.add(item);
            }

            @Override
            public void onError(Throwable t) {}

            @Override
            public void onComplete() {}
        });
        
        assertEquals(10, list.size());
    }
}
