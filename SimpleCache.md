```
private final ConcurrentHashMap<K, CacheEntry<V>> cache = new ConcurrentHashMap<>();
```
For better performance, it needs an initial size and load factor. This need to be calculated based on the number of entries the cache is storing. Initial size and load factor help to avoid frequent map resize, which leads to redistribution of map entries into buckets.

--------------------------------------------------------
```
private final long ttlMs = 60000; // 1 minute
```
This can be a constant if the TTL applies for all entries. Better make it configurable in the constructor because depends on the purpose, each cache instance needs a different TTL.

--------------------------------------------------------------
```
public CacheEntry(V value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }
```
The timestamp can be auto generated whenever a CacheEntry is initialized, it can be removed from the constructor. Minor issue: The timestamp when the entry get created will be a little bit different from the actual time it get added to the cache. Not a major issue and can be tolerated.

---------------------------------------------------------------- 
```
public void put(K key, V value) {
        cache.put(key, new CacheEntry<>(value, System.currentTimeMillis()));
    }
``` 
*Problem 1*: There is no limit check, that means we can run into OOM. We need to implement the algorithm to remove the least recently used cache entry.

*Problem 2*: Given 2 threads put the same key but different values, we need to ensure to always keep the latest version so that we don't put stale version in the cache. Suggestion: add version number as an attribute in the value and check it on put.

*Problem 3*: Many threads can put at the same time, and any calculation/logic check can be expensive. We should reuse calculateIfAbsent so that any expensive logic will be executed when the key doesn't exist.

*Problem 4*: No null check for the key, and when the value is null, it doesn't need to be added to the cache, which take the space for no reason.

-----------------------------------------------------------------
```
public V get(K key) {
        CacheEntry<V> entry = cache.get(key);
        if (entry != null) {
            if (System.currentTimeMillis() - entry.getTimestamp() < ttlMs) {
                return entry.getValue();
            }
        }
        return null;
    }
```
Here we know when an entry expired, however we don't evict it. This will leave the entry in the map forever and eventually cause OOM.
We can put the logic inside computeIfPresent and return null so that the map will remove the value automatically.

--------------------------------------------------------------------
```
public int size() {
        return cache.size();
    }
```
It's a little bit ambiguous since we didn't evict the stale entries, it will return the size of the map. Do we need to return only active entry count?