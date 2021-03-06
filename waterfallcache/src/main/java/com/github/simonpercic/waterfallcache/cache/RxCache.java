package com.github.simonpercic.waterfallcache.cache;

import java.lang.reflect.Type;

import rx.Observable;

/**
 * @author Simon Percic <a href="https://github.com/simonpercic">https://github.com/simonpercic</a>
 */
public interface RxCache {

    /**
     * Get from cache.
     *
     * @param key key
     * @param typeOfT type of cache value
     * @param <T> T of cache value
     * @return Observable that emits the cache value
     */
    <T> Observable<T> get(String key, Type typeOfT);

    /**
     * Put value to cache.
     *
     * @param key key
     * @param object value
     * @return Observable that emits <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    Observable<Boolean> put(String key, Object object);

    /**
     * Cache contains key.
     *
     * @param key key
     * @return Observable that emits <tt>true</tt> if cache contains key, <tt>false</tt> otherwise
     */
    Observable<Boolean> contains(String key);

    /**
     * Remove cache value.
     *
     * @param key key
     * @return Observable that emits <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    Observable<Boolean> remove(String key);

    /**
     * Clear all cache values.
     *
     * @return Observable that emits <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    Observable<Boolean> clear();
}
