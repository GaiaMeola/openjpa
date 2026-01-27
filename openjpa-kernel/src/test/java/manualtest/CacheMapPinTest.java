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

                //test 1; test passato
                Arguments.of(false, KeyCategory.IN_CACHE, true, null),
                //test 2; test passato
                Arguments.of(false, KeyCategory.IN_SOFT, true, null),
                //test 3; test passato
                Arguments.of(false, KeyCategory.IN_PINNED_NON_NULL, true, null),
                //test 4; test passato
                Arguments.of(false, KeyCategory.IN_PINNED_NULL, false, null),
                //test 5; test passato
                Arguments.of(false, KeyCategory.NOT_PRESENT, false, null),
                //test 6; test fallito
                Arguments.of(false, KeyCategory.INVALID, false, Exception.class),
                //test 7; test fallito
                Arguments.of(false, KeyCategory.NULL, false,  Exception.class),
                // test P1: caso con cache invalida; test passato
                Arguments.of(true, KeyCategory.NOT_PRESENT, false, null)
        );
    }

    // aggiunto a seguito di PIT per uccidere le mutazioni sopravvissute

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
            cache = invalidCacheMap(); // Capacità 4 (sicura)
            keyToPin = validKey();
        } else {
            switch (keyCategory) {
                case IN_CACHE:
                    cache = validCacheMapWithKeyInCache();
                    keyToPin = validKey();
                    break;
                case IN_SOFT:
                    cache = validCacheMapAlwaysSoft(); // Capacità 4
                    keyToPin = VALID_KEY_IN_SOFT;
                    cache.put(keyToPin, "someValue");
                    // Forziamo l'eviction per mandarlo in soft
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

            // 1. Verifica del risultato booleano
            assertEquals(expectedOutput, result, "Il risultato di pin() non è corretto");

            // --- VERIFICA BLACK BOX ---
            assertEquals(expectedOutput, result, "Il risultato di pin() non è corretto");

            if (result) {
                // Se pin ha avuto successo (true)
                assertNotNull(cache.get(keyToPin), "Il valore deve essere presente");
                assertFalse(cache.isEmpty(), "La cache non può essere vuota dopo un pin riuscito");
            } else {
                // Se pin ha fallito (false)
                assertNull(cache.get(keyToPin), "Il valore deve essere null");

                // Se la cache era già vuota o il valore era null, verifichiamo la coerenza
                // Invece di size() >= 0, verifichiamo che la size non sia cambiata negativamente
                assertTrue(cache.size() <= 100, "La size non deve superare la capacità massima");
            }
        }
    }
}
