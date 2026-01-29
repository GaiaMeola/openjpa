package manualtest;

import customutils.Utils;
import org.apache.openjpa.util.CacheMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;
// Import necessari per Mockito
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CacheMapGetTest {

    private enum KeyCategory {
        IN_SOFT, IN_CACHE, IN_PINNED, NOT_PRESENT, INVALID, NULL
    }

    private static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(KeyCategory.IN_SOFT, "softValue", null),
                Arguments.of(KeyCategory.IN_CACHE, "valueCache", null),
                Arguments.of(KeyCategory.IN_PINNED, "pinnedValue", null),
                Arguments.of(KeyCategory.NOT_PRESENT, null, null),
                Arguments.of(KeyCategory.INVALID, null, Exception.class),
                Arguments.of(KeyCategory.NULL, null, null)
        );
    }

    //Aggiunto a seguito di un time-out di PIT
    @Test
    void testLockLifecycle() {
        // 1. Creiamo lo spy
        CacheMap cache = new CacheMap();
        CacheMap spyCache = spy(cache);
        spyCache.put("key", "value");

        // 2. Eseguiamo il get
        spyCache.get("key");
        // Verifichiamo che il protocollo di sicurezza sia stato rispettato
        verify(spyCache, times(1)).readLock();   // Verifica acquisizione (riga 362)
        verify(spyCache, times(1)).readUnlock(); // Verifica rilascio (riga 375)
    }

    @ParameterizedTest
    @MethodSource("data")
    @Timeout(5)
    void testGet(KeyCategory keyCategory,
                 Object expectedOutput,
                 Class<? extends Exception> expectedException) {

        CacheMap cache;
        Object keyUnderTest;

        // --- SETUP ---
        switch (keyCategory) {
            case IN_SOFT:
                // Setup deterministico per spingere l'oggetto in softMap
                cache = new CacheMap(true, 2);
                keyUnderTest = "softKey";
                cache.put(keyUnderTest, "softValue"); // In cacheMap
                cache.put("extra1", "val1");           // In cacheMap (size 2/2)
                cache.put("extra2", "val2");           // "softKey" finisce in softMap per overflow
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
            // STRATEGIA KILLER: Creiamo uno spy per monitorare le chiamate interne
            CacheMap spyCache = spy(cache);

            Object result = spyCache.get(keyUnderTest);
            assertEquals(expectedOutput, result, "Valore errato recuperato");

            if (keyCategory == KeyCategory.IN_SOFT) {
                /* * KILLER LOGIC: Se il codice originale è corretto, dopo aver trovato
                 * l'elemento in softMap, DEVE chiamare put(key, val) per promuoverlo.
                 */
                verify(spyCache, times(1)).put(keyUnderTest, expectedOutput);
            }

            // Verifica generale di presenza (se non è un caso di oggetto mancante)
            if (expectedOutput != null && keyCategory != KeyCategory.NOT_PRESENT) {
                assertTrue(spyCache.containsKey(keyUnderTest), "La chiave dovrebbe essere presente");
            }
        }
    }
}