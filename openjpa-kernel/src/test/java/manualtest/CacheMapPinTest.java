package manualtest;

import org.apache.openjpa.util.CacheMap;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Field;
import java.util.Map;
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
                //Arguments.of(false, KeyCategory.INVALID, false, Exception.class),
                //test 7; test fallito
                //Arguments.of(false, KeyCategory.NULL, false,  Exception.class),
                // test P1: caso con cache invalida; test passato
                Arguments.of(true, KeyCategory.NOT_PRESENT, false, null)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    @Timeout(5)
    void testPin(boolean useInvalidCache,
                 KeyCategory keyCategory,
                 boolean expectedOutput,
                 Class<? extends Exception> expectedException) throws Exception {

        CacheMap cache;
        Object keyToPin;

        if (useInvalidCache) {
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
                    cache.put(keyToPin, new Object()); // già in soft
                    break;
                case IN_PINNED_NON_NULL:
                    cache = validCacheMapAlwaysPinned(); // spy che forza tutti i put nella pinnedMap
                    keyToPin = VALID_KEY_IN_PINNED_NON_NULL; // chiave già pinnata
                    cache.put(keyToPin, new Object()); // inserimento direttamente nella pinnedMap
                    break;
                case IN_PINNED_NULL:
                    cache = validCacheMapAlwaysPinned(); // spy che forza tutti i put nella pinnedMap
                    keyToPin = VALID_KEY_IN_PINNED_NULL; // chiave già pinnata
                    cache.put(keyToPin, null); // la chiave già pinnata ma con valore null
                    break;
                case NOT_PRESENT:
                    cache = emptyValidCacheMap();
                    keyToPin = validKey();
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
                    throw new IllegalStateException("Unexpected keyCategory: " + keyCategory);
            }
        }

        // --- Reflection per leggere stato iniziale ---
        Field pinnedMapField = CacheMap.class.getDeclaredField("pinnedMap");
        pinnedMapField.setAccessible(true);
        Map<?, ?> pinnedMap = (Map<?, ?>) pinnedMapField.get(cache);

        Field pinnedSizeField = CacheMap.class.getDeclaredField("_pinnedSize");
        pinnedSizeField.setAccessible(true);
        int beforePinnedSize = (int) pinnedSizeField.get(cache);

        // valore prima di pin()
        Object valueBeforePin = pinnedMap.get(keyToPin);

        if (expectedException != null) {
            final Object finalKey = keyToPin;
            final CacheMap finalCache = cache;
            Executable exec = () -> finalCache.pin(finalKey);
            Exception ex = assertThrows(expectedException, exec);
            System.out.println("Expected exception: " + ex);
        } else {
            boolean result = cache.pin(keyToPin);
            assertEquals(expectedOutput, result, "Unexpected pin result");

            int afterPinnedSize = (int) pinnedSizeField.get(cache);

            if (result) {
                assertNotNull(pinnedMap.get(keyToPin), "Pinned value should not be null when pin() returns true");

                if (valueBeforePin == null) {
                    assertEquals(beforePinnedSize + 1, afterPinnedSize,
                            "_pinnedSize should increment by exactly 1 when pinning a non-null value");
                } else {
                    assertEquals(beforePinnedSize, afterPinnedSize,
                            "_pinnedSize should remain unchanged if value was already pinned non-null");
                }

            } else {
                assertTrue(pinnedMap.containsKey(keyToPin),
                        "Key should still be in pinnedMap even when pin() returns false");
                assertNull(pinnedMap.get(keyToPin), "Pinned value should be null when pin() returns false");
                assertEquals(beforePinnedSize, afterPinnedSize,
                        "_pinnedSize should remain unchanged when pinning a null value");
            }
        }
    }
}
