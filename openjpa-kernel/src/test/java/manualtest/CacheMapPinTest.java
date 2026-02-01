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
                Arguments.of(false, KeyCategory.IN_CACHE, true, null), //p2
                Arguments.of(false, KeyCategory.IN_SOFT, true, null), //p3
                Arguments.of(false, KeyCategory.IN_PINNED_NON_NULL, true, null), //p4
                Arguments.of(false, KeyCategory.IN_PINNED_NULL, false, null), //p5
                Arguments.of(false, KeyCategory.NOT_PRESENT, false, null), //p6

                // Esempio corretto per il caso 6
                Arguments.of(false, KeyCategory.INVALID, false, RuntimeException.class), //p7

                // Modifica 2: Caso NULL già allineato alla tua osservazione
                Arguments.of(false, KeyCategory.NULL, false, null), //p8

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
        Object keyToPin = new Object();

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
                    cache = emptyValidCacheMap();
                    cache.pin(keyToPin); // Adesso la chiave è nella pinnedMap (valore null)
                    cache.put(keyToPin, "pinnedValue");
                    break;

                case IN_PINNED_NULL:
                    cache = emptyValidCacheMap();
                    cache.pin(keyToPin);
                    // Non chiamare put. La chiave è in pinnedMap con valore NULL.
                    // Il test chiamerà pin(keyToPin) -> riga 16 -> ritorna FALSE.
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
            int initialTotalSize = cache.size();
            int initialPinnedKeys = cache.getPinnedKeys().size();

            boolean result = cache.pin(keyToPin);

            // 1. Verifica dell'output
            assertEquals(expectedOutput, result, "Il risultato di pin() non è corretto per: " + keyCategory);

            // 2. Verifica dello stato interno
            if (result) {
                // Se pin ha successo, l'elemento è stato SPOSTATO da cache/softMap a pinnedMap.
                // Di conseguenza, la dimensione totale (size()) deve rimanere invariata.
                assertEquals(initialTotalSize, cache.size(),
                        "La size() totale non riflette correttamente l'incremento di _pinnedSize");

                // Verifichiamo anche che sia effettivamente nei pinnedKeys
                assertTrue(cache.getPinnedKeys().contains(keyToPin), "La chiave deve essere nei pinnedKeys");
            } else {
                // Se result è false (es. NOT_PRESENT), la chiave viene aggiunta ai pinnedKeys
                // ma con valore null, quindi _pinnedSize NON deve incrementare.
                // La size totale deve aumentare di 0 (perché il nullo non conta in _pinnedSize)
                // ma i pinnedKeys aumentano di 1.
                if (keyCategory == KeyCategory.NOT_PRESENT) {
                    assertEquals(initialPinnedKeys + 1, cache.getPinnedKeys().size());
                    assertEquals(initialTotalSize, cache.size(), "Il pin di un null non deve incrementare la size()");
                }
            }
        }
    }
}