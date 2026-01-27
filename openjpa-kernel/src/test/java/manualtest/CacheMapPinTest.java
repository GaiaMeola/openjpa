package manualtest;

import org.apache.openjpa.util.CacheMap;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static customutils.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CacheMapPinTest {

    private enum KeyCategory {
        IN_CACHE,
        IN_SOFT,
        IN_PINNED_NON_NULL,
        IN_PINNED_NULL,
        NOT_PRESENT,
        INVALID,
        NULL
    }

    private static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(false, KeyCategory.IN_CACHE, true, null),
                Arguments.of(false, KeyCategory.IN_SOFT, true, null),
                Arguments.of(false, KeyCategory.IN_PINNED_NON_NULL, true, null),
                Arguments.of(false, KeyCategory.IN_PINNED_NULL, false, null),
                Arguments.of(false, KeyCategory.NOT_PRESENT, false, null),

                // Esempio corretto per il caso 6
                Arguments.of(false, KeyCategory.INVALID, false, RuntimeException.class),

                // Modifica 2: Caso NULL già allineato alla tua osservazione
                Arguments.of(false, KeyCategory.NULL, false, null),

                // test P1: Utilizzerà la nuova Utils.invalidCacheMap() aggiornata a max=0
                Arguments.of(true, KeyCategory.NOT_PRESENT, false, null)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    @Timeout(5)
    void testPin(boolean useInvalidCache,
                 KeyCategory keyCategory,
                 boolean expectedOutput,
                 Class<? extends Exception> expectedException) {

        CacheMap cache;
        Object keyToPin;

        // --- SETUP ---
        if (useInvalidCache) {
            // P1: Utilizza Utils.invalidCacheMap() che implementa il costruttore a 5 parametri
            // con max=0 per simulare una cache invalida senza causare crash durante l'init.
            cache = invalidCacheMap();
            keyToPin = validKey();
        } else {
            switch (keyCategory) {
                case IN_CACHE:
                    cache = validCacheMapWithKeyInCache();
                    keyToPin = validKey();
                    break;
                case IN_SOFT:
                    cache = validCacheMapAlwaysSoft();
                    keyToPin = VALID_KEY_IN_SOFT;
                    cache.put(keyToPin, "someValue");
                    // Forziamo l'eviction per spostare la chiave in softMap
                    for (int i = 0; i < 5; i++) cache.put("extra" + i, "val");
                    break;
                case IN_PINNED_NON_NULL:
                    cache = validCacheMapAlwaysPinned();
                    keyToPin = VALID_KEY_IN_PINNED_NON_NULL;
                    cache.put(keyToPin, "pinnedValue");
                    break;
                case IN_PINNED_NULL:
                    cache = validCacheMapAlwaysPinned();
                    keyToPin = VALID_KEY_IN_PINNED_NULL;
                    cache.put(keyToPin, null);
                    break;
                case NOT_PRESENT:
                    cache = emptyValidCacheMap();
                    keyToPin = "nonExistentKey";
                    break;
                case INVALID:
                    cache = emptyValidCacheMap();
                    keyToPin = invalidKeyMock();
                    break;
                case NULL:
                    cache = emptyValidCacheMap();
                    keyToPin = NULL_KEY();
                    break;
                default:
                    throw new IllegalStateException("Unexpected: " + keyCategory);
            }
        }

        // --- ESECUZIONE E VERIFICA ---
        if (expectedException != null) {
            final Object finalKey = keyToPin;
            final CacheMap finalCache = cache;
            assertThrows(expectedException, () -> finalCache.pin(finalKey));
        } else {
            boolean result = cache.pin(keyToPin);

            // Verifica dell'output atteso (FALSE per P1, NULL e INVALID)
            assertEquals(expectedOutput, result, "Il risultato di pin() non è corretto per: " + keyCategory);

            // Verifica dello stato post-operazione
            if (result) {
                assertNotNull(cache.get(keyToPin), "La chiave pinnata deve essere presente");
            } else {
                // Se pin fallisce (come in P1), la cache non deve contenere il valore
                if (useInvalidCache) {
                    assertNull(cache.get(keyToPin), "Con max=0 la cache non deve memorizzare dati");
                }
                assertTrue(cache.size() <= 100, "Invariante sulla dimensione massima violato");
            }
        }
    }
}