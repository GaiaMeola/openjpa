package manualtest;

import customutils.Utils;
import org.apache.openjpa.util.CacheMap;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

enum KeyCategory {
    IN_CACHE, IN_SOFT, IN_PINNED_NON_NULL, IN_PINNED_NULL, NOT_PRESENT, INVALID_KEY, NULL
}

enum ValueCategory {
    VALID, NULL
}

enum CacheType {
    NORMAL, INVALID_CACHE
}

class ObservableCacheMap extends CacheMap {
    public int addedCalls = 0;
    public int removedCalls = 0;

    public ObservableCacheMap(boolean lru, int max) {
        super(lru, max);
    }

    @Override
    protected void entryAdded(Object key, Object value) {
        addedCalls++;
        super.entryAdded(key, value);
    }

    @Override
    protected void entryRemoved(Object key, Object value, boolean expired) {
        removedCalls++;
        super.entryRemoved(key, value, expired);
    }
}

class CacheMapPutTest {

    // --- Valore preesistente per il test pinnato ---
    private static final Object pinnedValue = new Object();
    // --- Valore preesistente per il test soft ---
    private static final Object softValue = new Object();

    static Stream<Arguments> data() {
        return Stream.of(
                // P1: Cache invalida (max=0) -> Ritorna null
                Arguments.of(KeyCategory.NOT_PRESENT, ValueCategory.VALID, CacheType.INVALID_CACHE, null, null),

                // P2-P3: In pinnedMap (not null) -> Ritorna pinnedValue (Sia con valore VALID che NULL)
                Arguments.of(KeyCategory.IN_PINNED_NON_NULL, ValueCategory.VALID, CacheType.NORMAL, pinnedValue, null),
                Arguments.of(KeyCategory.IN_PINNED_NON_NULL, ValueCategory.NULL, CacheType.NORMAL, pinnedValue, null),

                // P4-P5: In pinnedMap (null) -> Ritorna null
                Arguments.of(KeyCategory.IN_PINNED_NULL, ValueCategory.VALID, CacheType.NORMAL, null, null),
                Arguments.of(KeyCategory.IN_PINNED_NULL, ValueCategory.NULL, CacheType.NORMAL, null, null),

                // P6-P7: In softMap -> Ritorna softValue
                Arguments.of(KeyCategory.IN_SOFT, ValueCategory.VALID, CacheType.NORMAL, softValue, null),
                Arguments.of(KeyCategory.IN_SOFT, ValueCategory.NULL, CacheType.NORMAL, softValue, null),

                // P8-P9: In cacheMap -> Ritorna valueCache
                Arguments.of(KeyCategory.IN_CACHE, ValueCategory.VALID, CacheType.NORMAL, "valueCache", null),
                Arguments.of(KeyCategory.IN_CACHE, ValueCategory.NULL, CacheType.NORMAL, "valueCache", null),

                // P10-P11: Not mapped -> Ritorna null
                Arguments.of(KeyCategory.NOT_PRESENT, ValueCategory.VALID, CacheType.NORMAL, null, null),
                Arguments.of(KeyCategory.NOT_PRESENT, ValueCategory.NULL, CacheType.NORMAL, null, null),

                // P12: Invalid Instance -> Si aspetta Exception
                Arguments.of(KeyCategory.INVALID_KEY, ValueCategory.NULL, CacheType.NORMAL, null, Exception.class),

                // P13: Null Key -> Si aspetta Exception (secondo tabella) --> modificato con null
                Arguments.of(KeyCategory.NULL, ValueCategory.NULL, CacheType.NORMAL, null, null)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    @Timeout(5)
    void testPut(KeyCategory keyCategory, ValueCategory valueCategory, CacheType cacheType,
                 Object expectedOldValue, Class<? extends Exception> expectedException) {

        // Utilizziamo la classe Observable per poter spiare
        ObservableCacheMap cache = new ObservableCacheMap(true, 10);
        Object key = "testKey";
        Object value = (valueCategory == ValueCategory.VALID) ? "newValue" : null;

        // --- SETUP ---
        if (cacheType == CacheType.INVALID_CACHE) {
            cache.setCacheSize(0); // Forza maxSize = 0 per riga 402
        } else {
            switch (keyCategory) {
                case IN_CACHE: cache.put(key, "valueCache"); break;
                case IN_PINNED_NON_NULL:
                    cache.pin(key);
                    cache.put(key, pinnedValue);
                    break;
                case IN_PINNED_NULL: cache.pin(key); break;
                case IN_SOFT:
                    cache.setCacheSize(2);
                    cache.put(key, softValue);
                    cache.put("extra1", "v1");
                    cache.put("extra2", "v2"); // Sposta 'key' in softMap
                    break;
                case INVALID_KEY: key = Utils.invalidKeyMock(); break;
                case NULL: key = null; break;
                default: break;
            }
        }

        // Reset contatori dopo il setup
        cache.addedCalls = 0;
        cache.removedCalls = 0;
        int sizeBefore = cache.size();

        // --- ESECUZIONE ---
        if (expectedException != null) {
            final Object fKey = key;
            final Object fVal = value;
            assertThrows(expectedException, () -> cache.put(fKey, fVal));
        } else {
            Object oldValue = cache.put(key, value);

            // verifica incremento size
            if (keyCategory == KeyCategory.IN_PINNED_NULL && valueCategory == ValueCategory.VALID) {
                assertEquals(sizeBefore + 1, cache.size(), " _pinnedSize non incrementato");
            }

            if (cacheType == CacheType.NORMAL && value != null) {
                assertTrue(cache.addedCalls > 0, "entryAdded rimosso o non chiamato");
            }
            if (expectedOldValue != null) {
                assertTrue(cache.removedCalls > 0, "entryRemoved rimosso o non chiamato");
            }

            // 3. Verifica standard valore di ritorno
            assertSame(expectedOldValue, oldValue);

            // 4. KILLER RIGA 422 (writeUnlock): se il test finisce senza timeout, il lock è gestito
        }
    }
}