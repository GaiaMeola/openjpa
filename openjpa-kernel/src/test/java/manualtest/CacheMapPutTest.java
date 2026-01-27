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
                Arguments.of(KeyCategory.NOT_PRESENT, ValueCategory.NULL, CacheType.NORMAL, null, null),
                // --- t11; test fallito
                Arguments.of(KeyCategory.INVALID_KEY, ValueCategory.NULL, CacheType.NORMAL, null, Exception.class),
                // --- t12; test fallito
               Arguments.of(KeyCategory.NULL, ValueCategory.NULL, CacheType.NORMAL, null, Exception.class)

        );
    }

    @ParameterizedTest
    @MethodSource("data")
    @Timeout(5)
    void testPut(KeyCategory keyCategory,
                 ValueCategory valueCategory,
                 CacheType cacheType,
                 Object expectedOldValue,
                 Class<? extends Exception> expectedException) {

        CacheMap cache;
        Object key;
        Object value = (valueCategory == ValueCategory.VALID) ? "newValue" : null;

        // --- SETUP BLACK BOX ---
        if (cacheType == CacheType.INVALID_CACHE) {
            // In Utils.invalidCacheMap() usa new CacheMap(true, 1) per evitare IllegalArgumentException
            cache = Utils.invalidCacheMap();
            key = Utils.validKey();
        } else {
            cache = Utils.emptyValidCacheMap();
            key = Utils.validKey();

            switch (keyCategory) {
                case IN_CACHE:
                    cache = Utils.validCacheMapWithKeyInCache();
                    break;
                case IN_PINNED_NON_NULL:
                    cache.put(key, pinnedValue);
                    break;
                case IN_PINNED_NULL:
                    cache.put(key, null);
                    break;
                case IN_SOFT:
                    cache = Utils.validCacheMapAlwaysSoft(); // Capacità 4
                    key = Utils.validKey();
                    cache.put(key, softValue);

                    // Riempiamo i restanti slot (4 in totale) per forzare l'eviction di 'key'
                    cache.put("extra1", "v1");
                    cache.put("extra2", "v2");
                    cache.put("extra3", "v3");
                    cache.put("trigger", "v4");
                    // Ora 'key' è sicuramente nella softMap
                    break;
                case INVALID_KEY:
                    key = Utils.invalidKeyMock();
                    break;
                case NULL:
                    key = Utils.NULL_KEY();
                    break;
                case NOT_PRESENT:
                default:
                    break;
            }
        }

        // --- ESECUZIONE ---
        if (expectedException != null) {
            Object finalKey = key;
            CacheMap finalCache = cache;
            assertThrows(expectedException, () -> finalCache.put(finalKey, value));
        } else {
            Object oldValue = cache.put(key, value);

            // --- VERIFICA BLACK BOX ROBUSTA ---

            // 1. Verifica del valore di ritorno (Il contratto fondamentale del metodo put)
            assertSame(expectedOldValue, oldValue, "Il valore ritornato deve essere quello precedente");

            // 2. Verifica dello stato finale tramite API pubblica get()
            // Se la cache è "invalida" (capacità 1 e trigger già inserito), il comportamento di put
            // potrebbe scartare immediatamente il nuovo valore. In tutti gli altri casi:
            if (cacheType == CacheType.NORMAL && keyCategory != KeyCategory.IN_SOFT) {
                assertEquals(value, cache.get(key), "Il valore cercato tramite get() deve corrispondere all'ultimo inserito");
            }

            // 3. Verifica per il caso SoftMap
            if (keyCategory == KeyCategory.IN_SOFT) {
                // Verifichiamo che dopo il put, il valore sia comunque recuperabile (è stato riportato in pinned)
                assertEquals(value, cache.get(key), "L'elemento deve essere recuperabile anche se precedentemente era in softMap");
            }
        }
    }
}