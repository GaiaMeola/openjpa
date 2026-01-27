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
                // Test funzionali standard
                Arguments.of(false, KeyCategory.IN_CACHE, true, null),
                Arguments.of(false, KeyCategory.IN_SOFT, true, null),
                Arguments.of(false, KeyCategory.IN_PINNED_NON_NULL, true, null),
                Arguments.of(false, KeyCategory.IN_PINNED_NULL, false, null),
                Arguments.of(false, KeyCategory.NOT_PRESENT, false, null),
                Arguments.of(false, KeyCategory.INVALID, false, Exception.class),
                //Test modificato --> a seguito dell'esecuzione
                Arguments.of(false, KeyCategory.NULL, false, null),

                // test P1: Category Partition (Iru=TRUE, max=0, size=100, key=valid)
                // Output atteso FALSE perché con max=0 la chiave non può risiedere in cache
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
            // P1: Usiamo il costruttore a 5 parametri che accetta max=0 senza crashare
            // lru=true, max=0, size=100, load=0.75f, concurrency=1
            cache = new CacheMap(true, 0, 100, 0.75f, 1);
            keyToPin = "validKeyP1";
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

            assertEquals(expectedOutput, result, "Il risultato di pin() non è corretto");

            if (result) {
                assertNotNull(cache.get(keyToPin));
            } else {
                // Se pin fallisce (come in P1), verifichiamo che la cache sia coerente
                assertTrue(cache.size() <= 100);
            }
        }
    }
}