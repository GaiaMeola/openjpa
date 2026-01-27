package manualtest;

import customutils.Utils;
import org.apache.openjpa.util.CacheMap;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CacheMapGetTest {

    private enum KeyCategory {
        IN_SOFT, IN_CACHE, IN_PINNED, NOT_PRESENT, INVALID, NULL
    }

    private static Stream<Arguments> data() {
        return Stream.of(
                // t1: Recupero da memoria secondaria (Soft)
                Arguments.of(KeyCategory.IN_SOFT, "softValue", null),
                // t2: Recupero da cache standard
                Arguments.of(KeyCategory.IN_CACHE, "valueCache", null),
                // t3: Recupero da elementi bloccati (Pinned)
                Arguments.of(KeyCategory.IN_PINNED, "pinnedValue", null),
                // t4: Chiave mancante -> deve tornare null
                Arguments.of(KeyCategory.NOT_PRESENT, null, null),
                // t5: Chiave invalida -> RuntimeException (gestita da Utils.invalidKeyException)
                Arguments.of(KeyCategory.INVALID, null, Exception.class),
                // t6: Chiave null -> OpenJPA la accetta e ritorna null (visto sperimentalmente)
                Arguments.of(KeyCategory.NULL, null, null)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    @Timeout(5)
    void testGet(KeyCategory keyCategory,
                 Object expectedOutput,
                 Class<? extends Exception> expectedException) {

        CacheMap cache;
        Object keyUnderTest;

        // --- SETUP BLACK BOX ---
        switch (keyCategory) {
            case IN_SOFT:
                cache = Utils.validCacheMapAlwaysSoft(); // Capacità 4
                keyUnderTest = "softKey";
                cache.put(keyUnderTest, expectedOutput);
                // Forziamo l'eviction: inseriamo altri 5 elementi per mandare 'softKey' in softMap
                for(int i=0; i<5; i++) cache.put("extra" + i, "val");
                break;

            case IN_CACHE:
                cache = Utils.validCacheMapWithKeyInCache();
                keyUnderTest = Utils.validKey();
                break;

            case IN_PINNED:
                cache = Utils.validCacheMapAlwaysPinned();
                keyUnderTest = "pinnedKey";
                cache.put(keyUnderTest, expectedOutput);
                cache.pin(keyUnderTest);
                break;

            case NOT_PRESENT:
                cache = Utils.emptyValidCacheMap();
                keyUnderTest = "missingKey";
                break;

            case INVALID:
                cache = Utils.emptyValidCacheMap();
                keyUnderTest = Utils.invalidKeyMock();
                break;

            case NULL:
                cache = Utils.emptyValidCacheMap();
                keyUnderTest = null;
                break;

            default:
                throw new IllegalStateException("Unexpected category");
        }

        // --- ESECUZIONE E VERIFICA ---
        if (expectedException != null) {
            final Object finalKey = keyUnderTest;
            final CacheMap finalCache = cache;
            assertThrows(expectedException, () -> finalCache.get(finalKey));
        } else {
            Object result = cache.get(keyUnderTest);

            // 1. Verifica del valore restituito
            assertEquals(expectedOutput, result, "Il valore ottenuto dalla get() non è corretto");

            // 2. Verifica della presenza tramite API pubblica
            if (expectedOutput != null) {
                assertTrue(cache.containsKey(keyUnderTest), "La cache deve confermare la presenza della chiave");
            }
        }
    }
}