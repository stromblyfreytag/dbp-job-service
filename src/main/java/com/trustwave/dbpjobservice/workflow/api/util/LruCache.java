package com.trustwave.dbpjobservice.workflow.api.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class LruCache<K, V> extends LinkedHashMap<K, V> {
    private static final long serialVersionUID = 1L;

    private int maxSize;

    public LruCache(int maxEntries) {
        super((int) (maxEntries * 1.33) + 1, 0.75f, true);
        this.maxSize = maxEntries;
    }

    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return super.size() > maxSize;
    }
}
