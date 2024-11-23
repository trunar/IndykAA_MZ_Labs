package org.example.proxy;

import java.util.concurrent.ConcurrentHashMap;

public class CacheManager {
    private static final ConcurrentHashMap<String, byte[]> cache = new ConcurrentHashMap<>();

    public static void cacheResponse(String url, byte[] response) {
        cache.put(url, response);
    }

    public static boolean hasCached(String url) {
        return cache.containsKey(url);
    }

    public static byte[] getCachedResponse(String url) {
        return cache.get(url);
    }

    public static void clearCache() {
        cache.clear();
    }
}