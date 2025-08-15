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
//                Arguments.of(false, KeyCategory.IN_CACHE, true, null),
//                Arguments.of(false, KeyCategory.IN_SOFT, true, null),
//                Arguments.of(false, KeyCategory.IN_PINNED_NON_NULL, true, null),
//                Arguments.of(false, KeyCategory.IN_PINNED_NULL, false, null),
//                Arguments.of(false, KeyCategory.NOT_PRESENT, false, null),
//                Arguments.of(false, KeyCategory.INVALID, false, RuntimeException.class),
//                Arguments.of(false, KeyCategory.NULL, false, NullPointerException.class),

//                Arguments.of(true, KeyCategory.IN_CACHE, false, RuntimeException.class),

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
                    cache = validCacheMapWithKeyInSoft();
                    keyToPin = validKey();
                    break;
                case IN_PINNED_NON_NULL:
                    cache = validCacheMapWithKeyInPinnedNonNull();
                    keyToPin = validKey();
                    break;
                case IN_PINNED_NULL:
                    cache = validCacheMapWithKeyInPinnedNull();
                    keyToPin = validKey();
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

        if (expectedException != null) {
            final Object finalKey = keyToPin;
            final CacheMap finalCache = cache;
            Executable exec = () -> finalCache.pin(finalKey);
            Exception ex = assertThrows(expectedException, exec);
            System.out.println("Expected exception: " + ex);
        } else {
            boolean result = cache.pin(keyToPin);
            assertEquals(expectedOutput, result, "Unexpected pin result");

            // --- Reflection per controllare lo stato interno ---
            Field pinnedMapField = CacheMap.class.getDeclaredField("pinnedMap");
            pinnedMapField.setAccessible(true);
            Map<?, ?> pinnedMap = (Map<?, ?>) pinnedMapField.get(cache);

            Field pinnedSizeField = CacheMap.class.getDeclaredField("_pinnedSize");
            pinnedSizeField.setAccessible(true);
            int pinnedSize = (int) pinnedSizeField.get(cache);

            if (expectedOutput) {
                // true => valore non null pinnato
                assertTrue(pinnedMap.containsKey(keyToPin),
                        "Key should be in pinnedMap after pin() returns true");
                assertNotNull(pinnedMap.get(keyToPin),
                        "Pinned value should not be null when pin() returns true");
                assertTrue(pinnedSize > 0,
                        "_pinnedSize should be incremented when pinning a non-null value");
            } else {
                // false => valore null pinnato
                assertTrue(pinnedMap.containsKey(keyToPin),
                        "Key should still be in pinnedMap even when pin() returns false");
                assertNull(pinnedMap.get(keyToPin),
                        "Pinned value should be null when pin() returns false");
                assertEquals(0, pinnedSize,
                        "_pinnedSize should remain 0 when pinning a null value");
            }
        }
    }
}
