package rx;

import org.junit.jupiter.api.Test;
import rx.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SchedulerTest {

    @Test
    public void testSubscribeOn() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<String> threadNames = Collections.synchronizedList(new ArrayList<>());

        Observable.<Integer>create(emitter -> {
            threadNames.add(Thread.currentThread().getName());
            emitter.onNext(1);
            emitter.onComplete();
        })
        .subscribeOn(Schedulers.io())
        .subscribe(new Observer<Integer>() {
            @Override
            public void onNext(Integer item) {
                threadNames.add(Thread.currentThread().getName());
            }

            @Override
            public void onError(Throwable t) {
                latch.countDown();
            }

            @Override
            public void onComplete() {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(2, threadNames.size());
        assertTrue(threadNames.get(0).contains("pool")); // typical name for CachedThreadPool threads
        assertEquals(threadNames.get(0), threadNames.get(1)); // Since observeOn isn't defined, onNext runs in same thread
    }

    @Test
    public void testObserveOn() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<String> threadNames = Collections.synchronizedList(new ArrayList<>());

        Observable.<Integer>create(emitter -> {
            threadNames.add(Thread.currentThread().getName());
            emitter.onNext(1);
            emitter.onComplete();
        })
        .observeOn(Schedulers.computation())
        .subscribe(new Observer<Integer>() {
            @Override
            public void onNext(Integer item) {
                threadNames.add(Thread.currentThread().getName());
            }

            @Override
            public void onError(Throwable t) {
                latch.countDown();
            }

            @Override
            public void onComplete() {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(2, threadNames.size());
        assertEquals(Thread.currentThread().getName(), threadNames.get(0)); // creation thread
        assertTrue(threadNames.get(1).contains("pool")); // observation thread
    }
}
