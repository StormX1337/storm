package xyz.stormclient.licenceserver;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A token bucket per caller.
 *
 * <p>Activation takes a key someone could otherwise guess at, so the public
 * endpoints get a ceiling on how fast one address may try. Buckets refill over
 * time and idle ones are dropped, so the map cannot grow without bound.
 */
public final class RateLimit {

    private static final long IDLE_TIMEOUT = 10 * 60 * 1000L;

    private final int burst;
    private final double perSecond;
    private final Map<String, Bucket> buckets = new HashMap<String, Bucket>();

    private long lastSweep = System.currentTimeMillis();

    public RateLimit(int burst, double perSecond) {
        this.burst = burst;
        this.perSecond = perSecond;
    }

    public synchronized boolean allow(String caller) {
        long now = System.currentTimeMillis();
        sweep(now);

        Bucket bucket = buckets.get(caller);
        if (bucket == null) {
            bucket = new Bucket();
            bucket.tokens = burst;
            bucket.updated = now;
            buckets.put(caller, bucket);
        }

        bucket.tokens = Math.min(burst, bucket.tokens + (now - bucket.updated) / 1000.0 * perSecond);
        bucket.updated = now;
        if (bucket.tokens < 1) return false;
        bucket.tokens -= 1;
        return true;
    }

    private void sweep(long now) {
        if (now - lastSweep < IDLE_TIMEOUT) return;
        lastSweep = now;
        Iterator<Map.Entry<String, Bucket>> it = buckets.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue().updated > IDLE_TIMEOUT) it.remove();
        }
    }

    private static final class Bucket {
        double tokens;
        long updated;
    }
}
