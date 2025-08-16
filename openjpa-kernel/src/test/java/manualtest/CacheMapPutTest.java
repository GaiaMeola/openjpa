package manualtest;

import customutils.Utils;
import org.apache.openjpa.util.CacheMap;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.function.Executable;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CacheMapPutTest {

    private enum KeyCategory {
        IN_CACHE,
        IN_SOFT,
        IN_PINNED_NON_NULL,
        IN_PINNED_NULL,
        NOT_PRESENT,
        INVALID_KEY,
        NULL
    }

    private enum ValueCategory {
        VALID,
        NULL
    }

    private enum CacheType {
        NORMAL,
        INVALID_CACHE
    }

    private static Stream<Arguments> data() {

        Object pinnedNonNullValue = new Object();
        Object pinnedNullValue = null;
        Object softValue = new Object();

        return Stream.of(
                // Test cache invalida
                Arguments.of(KeyCategory.NOT_PRESENT, ValueCategory.VALID, CacheType.INVALID_CACHE, null, null),
                // Altri test possono essere aggiunti qui, senza usare new Object() direttamente

                // Test pinnedMap
                Arguments.of(KeyCategory.IN_PINNED_NON_NULL, ValueCategory.VALID, CacheType.NORMAL, pinnedNonNullValue, null), // T1
                Arguments.of(KeyCategory.IN_PINNED_NON_NULL, ValueCategory.NULL, CacheType.NORMAL, pinnedNonNullValue, null),  // T2
                Arguments.of(KeyCategory.IN_PINNED_NULL, ValueCategory.VALID, CacheType.NORMAL, pinnedNullValue, null),        // T3
                Arguments.of(KeyCategory.IN_PINNED_NULL, ValueCategory.NULL, CacheType.NORMAL, pinnedNullValue, null),         // T4

                // Test softMap
                Arguments.of(KeyCategory.IN_SOFT, ValueCategory.VALID, CacheType.NORMAL, softValue, null)                     // T5
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    @Timeout(5)
    void testPut(KeyCategory keyCategory,
                 ValueCategory valueCategory,
                 CacheType cacheType,
                 Class<? extends Exception> expectedException) throws Exception {

        CacheMap cache;
        Object key;
        Object value = (valueCategory == ValueCategory.VALID) ? new Object() : null;
        Object preExistingValue;

        // --- Setup cache ---
        Object expectedOldValue;
        switch (cacheType) {
            case NORMAL:
                switch (keyCategory) {
                    case IN_CACHE:
                        cache = Utils.validCacheMapWithKeyInCache();
                        key = Utils.validKey();
                        preExistingValue = cache.get(key); // prendiamo il valore già presente
                        expectedOldValue = preExistingValue;
                        break;
                    case IN_SOFT:
                        cache = Utils.validCacheMapAlwaysSoft();
                        key = Utils.VALID_KEY_IN_SOFT;
                        preExistingValue = new Object();
                        cache.put(key, preExistingValue);
                        expectedOldValue = preExistingValue;
                        break;
                    case IN_PINNED_NON_NULL:
                        cache = Utils.validCacheMapAlwaysPinned();
                        key = Utils.VALID_KEY_IN_PINNED_NON_NULL;
                        preExistingValue = new Object();
                        cache.put(key, preExistingValue);
                        expectedOldValue = preExistingValue;
                        break;
                    case IN_PINNED_NULL:
                        cache = Utils.validCacheMapAlwaysPinned();
                        key = Utils.VALID_KEY_IN_PINNED_NULL;
                        cache.put(key, null);
                        expectedOldValue = null;
                        break;
                    case NOT_PRESENT:
                        cache = Utils.emptyValidCacheMap();
                        key = Utils.validKey();
                        expectedOldValue = null;
                        break;
                    case INVALID_KEY:
                        cache = Utils.emptyValidCacheMap();
                        key = Utils.invalidKeyMock();
                        expectedOldValue = null;
                        break;
                    case NULL:
                        cache = Utils.emptyValidCacheMap();
                        key = Utils.NULL_KEY();
                        expectedOldValue = null;
                        break;
                    default:
                        throw new IllegalStateException("Unexpected keyCategory: " + keyCategory);
                }
                break;
            case INVALID_CACHE:
                cache = Utils.invalidCacheMap();
                key = Utils.validKey();
                expectedOldValue = null;
                break;
            default:
                throw new IllegalStateException("Unexpected cacheType: " + cacheType);
        }

        // --- Reflection per leggere stato interno ---
        Field pinnedMapField = CacheMap.class.getDeclaredField("pinnedMap");
        pinnedMapField.setAccessible(true);
        Map<?, ?> pinnedMap = (Map<?, ?>) pinnedMapField.get(cache);

        Field pinnedSizeField = CacheMap.class.getDeclaredField("_pinnedSize");
        pinnedSizeField.setAccessible(true);
        int beforePinnedSize = (int) pinnedSizeField.get(cache);

        Field softMapField = CacheMap.class.getDeclaredField("softMap");
        softMapField.setAccessible(true);
        Map<?, ?> softMap = (Map<?, ?>) softMapField.get(cache);

        // --- Esecuzione del test ---
        if (expectedException != null) {
            final Object finalKey = key;
            final Object finalValue = value;
            Executable exec = () -> cache.put(finalKey, finalValue);
            assertThrows(expectedException, exec, "Attesa eccezione per il caso specifico");
        } else {
            Object oldValue = cache.put(key, value);

            // Verifica valore ritornato
            assertSame(expectedOldValue, oldValue, "Old value deve essere lo stesso oggetto già presente");

            // Controlli pinnedMap
            if (pinnedMap.containsKey(key)) {
                if (value != null && oldValue == null) {
                    assertEquals(beforePinnedSize + 1, pinnedSizeField.get(cache));
                } else {
                    assertEquals(beforePinnedSize, pinnedSizeField.get(cache));
                }
            }

            // Controlli softMap
            if (softMap.containsKey(key) && oldValue == null) {
                assertNotEquals(value, softMap.get(key), "SoftMap non dovrebbe contenere il nuovo valore se è stato spostato nella pinnedMap");
            }
        }
    }
}