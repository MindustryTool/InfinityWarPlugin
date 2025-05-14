package infinitywar;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SimpleCache {
    private static final long TTL_MS = 20_000; 
    private static final int CLEANUP_INTERVAL = 1000; 

    private final Map<Integer, Long> cache = new ConcurrentHashMap<>();
    private final AtomicInteger actionCount = new AtomicInteger(0);

    public void put(int id) {
        cache.put(id, System.currentTimeMillis());
        maybeCleanup();
    }

    public boolean isContain(int id) {
        maybeCleanup();
        Long time = cache.get(id);
        if (time == null) return false;
        if (System.currentTimeMillis() - time > TTL_MS) {
            cache.remove(id);
            return false;
        }
        return true;
    }

    public void remove(int id) {
        cache.remove(id);
    }

    private void maybeCleanup() {
        if (actionCount.incrementAndGet() >= CLEANUP_INTERVAL) {
            cleanup();
            actionCount.set(0);
        }
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        for (var entry : cache.entrySet()) {
            if (now - entry.getValue() > TTL_MS) {
                cache.remove(entry.getKey());
            }
        }
    }
}
