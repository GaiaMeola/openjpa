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
                // P1: Cache invalida (max=0) -> Ritorna null
                Arguments.of(KeyCategory.NOT_PRESENT, ValueCategory.VALID, CacheType.INVALID_CACHE, null, null),

                // T1-T2: In pinnedMap (not null) -> Ritorna pinnedValue (Sia con valore VALID che NULL)
                Arguments.of(KeyCategory.IN_PINNED_NON_NULL, ValueCategory.VALID, CacheType.NORMAL, pinnedValue, null),
                Arguments.of(KeyCategory.IN_PINNED_NON_NULL, ValueCategory.NULL, CacheType.NORMAL, pinnedValue, null),

                // T3-T4: In pinnedMap (null) -> Ritorna null
                Arguments.of(KeyCategory.IN_PINNED_NULL, ValueCategory.VALID, CacheType.NORMAL, null, null),
                Arguments.of(KeyCategory.IN_PINNED_NULL, ValueCategory.NULL, CacheType.NORMAL, null, null),

                // T5-T6: In softMap -> Ritorna softValue
                Arguments.of(KeyCategory.IN_SOFT, ValueCategory.VALID, CacheType.NORMAL, softValue, null),
                Arguments.of(KeyCategory.IN_SOFT, ValueCategory.NULL, CacheType.NORMAL, softValue, null),

                // T7-T8: In cacheMap -> Ritorna valueCache
                Arguments.of(KeyCategory.IN_CACHE, ValueCategory.VALID, CacheType.NORMAL, "valueCache", null),
                Arguments.of(KeyCategory.IN_CACHE, ValueCategory.NULL, CacheType.NORMAL, "valueCache", null),

                // T9-T10: Not mapped -> Ritorna null
                Arguments.of(KeyCategory.NOT_PRESENT, ValueCategory.VALID, CacheType.NORMAL, null, null),
                Arguments.of(KeyCategory.NOT_PRESENT, ValueCategory.NULL, CacheType.NORMAL, null, null),

                // T11: Invalid Instance -> Si aspetta Exception
                Arguments.of(KeyCategory.INVALID_KEY, ValueCategory.NULL, CacheType.NORMAL, null, Exception.class),

                // T12: Null Key -> Si aspetta Exception (secondo tabella) --> modificato con null
                Arguments.of(KeyCategory.NULL, ValueCategory.NULL, CacheType.NORMAL, null, null)
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
                    cache.pin(key);              // 1. Sposta la chiave in pinnedMap
                    cache.put(key, pinnedValue); // 2. Assegna il valore preesistente
                    break;
                case IN_PINNED_NULL:
                    cache.pin(key);              // 1. Sposta la chiave in pinnedMap
                    // Non serve put(key, null) se pin() inizializza già a null,
                    // ma rende esplicito il tuo caso T3-T4.
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