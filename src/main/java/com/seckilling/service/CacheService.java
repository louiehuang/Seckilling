package com.seckilling.service;

/**
 * Local cache
 */
public interface CacheService {
    void setCommonCache(String key, Object value);

    Object getFromCommonCache(String key);
}
