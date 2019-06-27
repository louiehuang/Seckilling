package com.seckilling.service;

/**
 * Local cache
 */
public interface CacheService {
    void setLocalCommonCache(String key, Object value);

    Object getFromLocalCommonCache(String key);
}
