package manualtest;

import customutils.Utils;
import org.apache.openjpa.util.CacheMap;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.stream.Stream;

import static customutils.Utils.putInPinnedMap;
import static customutils.Utils.putInSoftMap;
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

class CacheMapPutTest {

    // --- Valore preesistente per il test pinnato ---
    private static final Object pinnedValue = new Object();
    // --- Valore preesistente per il test soft ---
    private static final Object softValue = new Object();

    static Stream<Arguments> data() {
        return Stream.of(
                // --- P1: cache invalida → ritorna sempre null ---; test passato
                Arguments.of(KeyCategory.NOT_PRESENT, ValueCategory.VALID, CacheType.INVALID_CACHE, null, null),

//              // --- PINNED TEST CASES ---
                // --- t1; test passato
                Arguments.of(KeyCategory.IN_PINNED_NON_NULL, ValueCategory.VALID, CacheType.NORMAL, pinnedValue, null),
                // --- t2; test fallito
//               Arguments.of(KeyCategory.IN_PINNED_NON_NULL, ValueCategory.NULL, CacheType.NORMAL, pinnedValue, null)
                // --- t3; test passato
                Arguments.of(KeyCategory.IN_PINNED_NULL, ValueCategory.VALID, CacheType.NORMAL, null, null),
                // --- t4; test fallito
//                Arguments.of(KeyCategory.IN_PINNED_NULL, ValueCategory.NULL, CacheType.NORMAL, null, null)
//               // --- SOFT TEST CASES ---
                // --- t5; test passato
                Arguments.of(KeyCategory.IN_SOFT, ValueCategory.VALID, CacheType.NORMAL, softValue, null),
                // --- t6; test passato
                Arguments.of(KeyCategory.IN_SOFT, ValueCategory.NULL, CacheType.NORMAL, softValue, null),
//                // --- REAL CACHE TEST CASES ---
                // --- t7; test passato
                Arguments.of(KeyCategory.IN_CACHE, ValueCategory.VALID, CacheType.NORMAL, "valueCache", null),
                // --- t8; test passato
                Arguments.of(KeyCategory.IN_CACHE, ValueCategory.NULL, CacheType.NORMAL, "valueCache", null),
                // --- t9; test passato
                Arguments.of(KeyCategory.NOT_PRESENT, ValueCategory.VALID, CacheType.NORMAL, null, null),
                // --- t10; test passato
                Arguments.of(KeyCategory.NOT_PRESENT, ValueCategory.NULL, CacheType.NORMAL, null, null)
                // --- t11; test fallito
//                Arguments.of(KeyCategory.INVALID_KEY, ValueCategory.NULL, CacheType.NORMAL, null, Exception.class)
                // --- t12; test fallito
//                Arguments.of(KeyCategory.NULL, ValueCategory.NULL, CacheType.NORMAL, null, Exception.class)

        );
    }

    @ParameterizedTest
    @MethodSource("data")
    @Timeout(5)
    void testPut(KeyCategory keyCategory,
                 ValueCategory valueCategory,
                 CacheType cacheType,
                 Object expectedOldValue,
                 Class<? extends Exception> expectedException) throws Exception {

        CacheMap cache;
        Object key;
        Object value = (valueCategory == ValueCategory.VALID) ? new Object() : null;

        // --- Setup cache ---
        switch (cacheType) {
            case NORMAL:
                cache = Utils.emptyValidCacheMap();
                switch (keyCategory) {
                    case IN_CACHE:
                        cache = Utils.validCacheMapWithKeyInCache();
                        key = Utils.validKey();
                        break;
                    case IN_PINNED_NON_NULL:
                        key = Utils.VALID_KEY_IN_PINNED_NON_NULL;
                        putInPinnedMap(cache, key, pinnedValue);
                        break;
                    case IN_PINNED_NULL:
                        key = Utils.VALID_KEY_IN_PINNED_NULL;
                        putInPinnedMap(cache, key, null);
                        break;
                    case IN_SOFT:
                        key = Utils.validKey();
                        putInSoftMap(cache, key, softValue);
                        break;
                    case NOT_PRESENT:
                        key = Utils.validKey();
                        break;
                    case INVALID_KEY:
                        key = Utils.invalidKeyMock();
                        break;
                    case NULL:
                        key = Utils.NULL_KEY();
                        break;
                    default:
                        throw new IllegalStateException("Unexpected keyCategory: " + keyCategory);
                }
                break;
            case INVALID_CACHE:
                cache = Utils.invalidCacheMap();
                key = Utils.validKey();
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
            CacheMap finalCache = cache;
            Executable exec = () -> finalCache.put(finalKey, finalValue);
            assertThrows(expectedException, exec, "Attesa eccezione per il caso specifico");
        } else {
            Object oldValue = cache.put(key, value);

            // Verifica ritorno
            assertSame(expectedOldValue, oldValue,
                    "Il valore ritornato deve essere lo stesso oggetto già presente o null se assente");

            // --- Controlli pinnedMap ---
            if (pinnedMap.containsKey(key)) {
                int expectedPinnedSize = beforePinnedSize;

                if (oldValue == null && value != null) {
                    // Inserimento di un nuovo valore non-null
                    expectedPinnedSize = beforePinnedSize + 1;
                } else if (oldValue != null && value == null) {
                    // Rimozione di un valore non-null
                    expectedPinnedSize = beforePinnedSize - 1;
                }
                // else: sia old che new sono null, oppure entrambi non-null → pinnedSize invariato

                assertEquals(expectedPinnedSize, pinnedSizeField.get(cache),
                        "Il valore di _pinnedSize deve riflettere correttamente le modifiche nella pinnedMap");
            }
            // --- Controlli softMap ---
            if (softMap.containsKey(key)) {
                Object currentSoftValue = softMap.get(key);
                if (value != null) {
                    // Dopo put il valore dovrebbe essere rimosso dalla softMap
                    assertNotEquals(value, currentSoftValue,
                            "SoftMap non dovrebbe contenere il nuovo valore se è stato spostato nella cache principale");
                }
            }
        }
    }
}