package com.github.simonpercic.waterfallcache.expire;

import com.github.simonpercic.waterfallcache.cache.Cache;
import com.github.simonpercic.waterfallcache.util.ObserverUtil;

import java.util.concurrent.TimeUnit;

import rx.Observable;

/**
 * Lazily expirable cache.
 * Cache items expire after a set time.
 * Being lazy, items only expire when getting them from cache.
 *
 * @author Simon Percic <a href="https://github.com/simonpercic">https://github.com/simonpercic</a>
 */
public class LazyExpirableCache implements Cache {

    // the underlying cache that holds the values
    private final Cache underlyingCache;

    // expire after milliseconds
    private final long expireMillis;

    private LazyExpirableCache(Cache underlyingCache, long expireMillis) {
        this.underlyingCache = underlyingCache;
        this.expireMillis = expireMillis;
    }

    /**
     * Creates an lazy expirable cache from an actual Cache.
     *
     * @param cache the underlying cache that will hold the values
     * @param expireAfter expire after value
     * @param expireAfterUnit expire after time unit
     * @return lazy expirable cache instance
     */
    public static LazyExpirableCache fromCache(Cache cache, long expireAfter, TimeUnit expireAfterUnit) {
        long millis = expireAfterUnit.toMillis(expireAfter);
        return new LazyExpirableCache(cache, millis);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Observable<T> get(String key, Class<T> classOfT) {
        return underlyingCache.get(key, TimedValue.class).map(timedValue -> {
            if (timedValue == null) {
                return null;
            }

            if (timedValue.addedOn + expireMillis < getCurrentTime()) {
                underlyingCache.remove(key).subscribe(ObserverUtil.silentObserver());
                return null;
            } else {
                return classOfT.cast(timedValue.value);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Observable<Boolean> put(String key, Object object) {
        TimedValue timedValue = new TimedValue(object, getCurrentTime());
        return underlyingCache.put(key, timedValue);
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

    private static long getCurrentTime() {
        return SystemCacheClock.getCurrentTime();
    }

    static class TimedValue {
        Object value;
        long addedOn;

        TimedValue(Object value, long time) {
            this.value = value;
            this.addedOn = time;
        }
    }
}
