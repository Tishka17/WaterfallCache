package com.github.simonpercic.waterfallcache;

import com.github.simonpercic.waterfallcache.cache.Cache;
import com.github.simonpercic.waterfallcache.callback.WaterfallCallback;
import com.github.simonpercic.waterfallcache.callback.WaterfallGetCallback;
import com.github.simonpercic.waterfallcache.model.SimpleObject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author Simon Percic <a href="https://github.com/simonpercic">https://github.com/simonpercic</a>
 */
public class WaterfallCacheAsyncTest {

    @Mock Cache cache;

    WaterfallCache waterfallCache;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        waterfallCache = WaterfallCache.builder()
                .addCache(cache)
                .withObserveOnScheduler(Schedulers.immediate())
                .build();
    }

    @Test
    public void testGetAsync() throws Exception {
        String key = "TEST_KEY";
        String value = "TEST_VALUE";

        when(cache.get(eq(key), eq(SimpleObject.class))).thenReturn(Observable.just(new SimpleObject(value)));

        CountDownLatch countDownLatch = new CountDownLatch(1);

        waterfallCache.<SimpleObject>getAsync(key, SimpleObject.class,
                new TestWaterfallGetCallback<>(simpleObject -> {
                    assertEquals(value, simpleObject.getValue());
                    countDownLatch.countDown();
                }));

        assertCountDownLatchCalled(countDownLatch);
    }

    @Test
    public void testPutAsync() throws Exception {
        final String key = "TEST_KEY";
        final String value = "TEST_VALUE";

        SimpleObject simpleObject = new SimpleObject(value);

        when(cache.put(eq(key), eq(simpleObject))).thenReturn(Observable.just(true));

        CountDownLatch countDownLatch = new CountDownLatch(1);

        waterfallCache.<Boolean>putAsync(key, simpleObject, new TestWaterfallCallback() {
            @Override public void onSuccess() {
                countDownLatch.countDown();
            }
        });

        assertCountDownLatchCalled(countDownLatch);
    }

    @Test
    public void testContainsAsync() throws Exception {
        String key = "TEST_KEY";

        when(cache.contains(eq(key))).thenReturn(Observable.just(true));

        CountDownLatch countDownLatch = new CountDownLatch(1);

        waterfallCache.<Boolean>containsAsync(key, new TestWaterfallGetCallback<>(contains -> {
            assertTrue(contains);
            countDownLatch.countDown();
        }));

        assertCountDownLatchCalled(countDownLatch);
    }

    @Test
    public void testNotContainsAsync() throws Exception {
        String key = "TEST_KEY";

        when(cache.contains(eq(key))).thenReturn(Observable.just(false));

        CountDownLatch countDownLatch = new CountDownLatch(1);

        waterfallCache.<Boolean>containsAsync(key, new TestWaterfallGetCallback<>(contains -> {
            assertFalse(contains);
            countDownLatch.countDown();
        }));

        assertCountDownLatchCalled(countDownLatch);
    }

    @Test
    public void testRemoveAsync() throws Exception {
        final String key = "TEST_KEY";

        when(cache.remove(eq(key))).thenReturn(Observable.just(true));

        CountDownLatch countDownLatch = new CountDownLatch(1);

        waterfallCache.<Boolean>removeAsync(key, new TestWaterfallCallback() {
            @Override public void onSuccess() {
                countDownLatch.countDown();
            }
        });

        assertCountDownLatchCalled(countDownLatch);
    }

    @Test
    public void testClearAsync() throws Exception {
        when(cache.clear()).thenReturn(Observable.just(true));

        CountDownLatch countDownLatch = new CountDownLatch(1);

        waterfallCache.<Boolean>clearAsync(new TestWaterfallCallback() {
            @Override public void onSuccess() {
                countDownLatch.countDown();
            }
        });

        assertCountDownLatchCalled(countDownLatch);
    }

    private void assertCountDownLatchCalled(CountDownLatch countDownLatch) throws InterruptedException {
        if (!countDownLatch.await(100, TimeUnit.MILLISECONDS)) {
            fail();
        }
    }

    private static class TestWaterfallGetCallback<T> implements WaterfallGetCallback<T> {

        private final Action1<T> assertAction;

        private TestWaterfallGetCallback(Action1<T> assertAction) {
            this.assertAction = assertAction;
        }

        @Override public void onSuccess(T t) {
            assertNotNull(t);
            assertAction.call(t);
        }

        @Override public void onFailure(Throwable throwable) {
            fail(throwable.getMessage());
        }
    }

    private static abstract class TestWaterfallCallback implements WaterfallCallback {

        @Override public void onFailure(Throwable throwable) {
            fail(throwable.getMessage());
        }
    }
}
