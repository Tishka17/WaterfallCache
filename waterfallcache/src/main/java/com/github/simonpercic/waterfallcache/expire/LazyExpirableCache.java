package com.github.simonpercic.waterfallcache.expire;

import com.github.simonpercic.waterfallcache.cache.Cache;
import com.github.simonpercic.waterfallcache.cache.RxCache;
import com.github.simonpercic.waterfallcache.callback.WaterfallCallback;
import com.github.simonpercic.waterfallcache.callback.WaterfallGetCallback;
import com.github.simonpercic.waterfallcache.utils.AsyncUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.TimeUnit;

import rx.Observable;

/**
 * Lazily expirable cache.
 * Cache items expire after a set time.
 * Being lazy, items only expire when getting them from cache.
 *
 * @author Simon Percic <a href="https://github.com/simonpercic">https://github.com/simonpercic</a>
 */
public final class LazyExpirableCache implements Cache {

    // the underlying cache that holds the values
    private final RxCache underlyingCache;

    // expire after milliseconds
    private final long expireMillis;

    // time observable
    private final Observable<Long> timeObservable;

    private LazyExpirableCache(RxCache underlyingCache, long expireMillis, Observable<Long> timeObservable) {
        this.underlyingCache = underlyingCache;
        this.expireMillis = expireMillis;
        this.timeObservable = timeObservable;
    }

    /**
     * Creates an lazy expirable cache from an actual Cache.
     *
     * @param cache the underlying cache that will hold the values
     * @param expireAfter expire after value
     * @param expireAfterUnit expire after time unit
     * @return lazy expirable cache instance
     */
    public static LazyExpirableCache fromCache(RxCache cache, long expireAfter, TimeUnit expireAfterUnit) {
        return fromCache(cache, expireAfter, expireAfterUnit, new AndroidSystemTimeProvider());
    }

    /**
     * Creates an lazy expirable cache from an actual Cache.
     *
     * @param cache the underlying cache that will hold the values
     * @param expireAfter expire after value
     * @param expireAfterUnit expire after time unit
     * @param simpleTimeProvider instance of SimpleTimeProvider
     * @return lazy expirable cache instance
     */
    public static LazyExpirableCache fromCache(RxCache cache, long expireAfter, TimeUnit expireAfterUnit,
            SimpleTimeProvider simpleTimeProvider) {

        Observable<Long> timeObservable = Observable.defer(() -> Observable.just(simpleTimeProvider.currentTime()));

        return fromCache(cache, expireAfter, expireAfterUnit, timeObservable);
    }

    /**
     * Creates an lazy expirable cache from an actual Cache.
     *
     * @param cache the underlying cache that will hold the values
     * @param expireAfter expire after value
     * @param expireAfterUnit expire after time unit
     * @param timeObservable instance of a time observable
     * @return lazy expirable cache instance
     */
    public static LazyExpirableCache fromCache(RxCache cache, long expireAfter, TimeUnit expireAfterUnit,
            Observable<Long> timeObservable) {

        long millis = expireAfterUnit.toMillis(expireAfter);
        return new LazyExpirableCache(cache, millis, timeObservable);
    }

    // region Reactive methods

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Observable<T> get(String key, Type type) {
        TimedValueType timedValueType = new TimedValueType(type);

        return timeObservable.flatMap(currentTime ->
                underlyingCache.<TimedValue<T>>get(key, timedValueType).flatMap(timedValue -> {
                    if (timedValue == null) {
                        return Observable.just(null);
                    }

                    if (timedValue.addedOn + expireMillis < currentTime) {
                        return underlyingCache.remove(key).map(success -> null);
                    } else {
                        return Observable.just(timedValue.value);
                    }
                }));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Observable<Boolean> put(String key, Object object) {
        return timeObservable.flatMap(currentTime -> {
            TimedValue timedValue = new TimedValue<>(object, currentTime);
            return underlyingCache.put(key, timedValue);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Observable<Boolean> contains(String key) {
        return get(key, Object.class).flatMap(o -> Observable.just(o != null));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Observable<Boolean> remove(String key) {
        return underlyingCache.remove(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Observable<Boolean> clear() {
        return underlyingCache.clear();
    }

    // endregion Reactive methods

    // region asynchronous methods

    @Override public <T> void getAsync(String key, Type typeOfT, WaterfallGetCallback<T> callback) {
        AsyncUtils.doAsync(get(key, typeOfT), callback);
    }

    @Override public void putAsync(String key, Object object, WaterfallCallback callback) {
        AsyncUtils.doAsync(put(key, object), callback);
    }

    @Override public void containsAsync(String key, WaterfallGetCallback<Boolean> callback) {
        AsyncUtils.doAsync(contains(key), callback);
    }

    @Override public void removeAsync(String key, WaterfallCallback callback) {
        AsyncUtils.doAsync(remove(key), callback);
    }

    @Override public void clearAsync(WaterfallCallback callback) {
        AsyncUtils.doAsync(clear(), callback);
    }

    // endregion asynchronous methods

    static class TimedValue<T> {
        T value;
        long addedOn;

        TimedValue(T value, long time) {
            this.value = value;
            this.addedOn = time;
        }
    }

    static class TimedValueType implements ParameterizedType {
        Type type;

        TimedValueType(Type type) {
            this.type = type;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return new Type[]{type};
        }

        @Override
        public Type getRawType() {
            return TimedValue.class;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }
    }
}
