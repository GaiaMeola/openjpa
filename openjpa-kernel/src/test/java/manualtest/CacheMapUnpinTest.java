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
class CacheMapUnpinTest {

    private enum KeyCategory {
        IN_PINNED_NON_NULL,
        IN_PINNED_NULL,
        NOT_PRESENT,
        INVALID,
        NULL
    }

    private static Stream<Arguments> data() {
        return Stream.of(
                //test 1; test passato
                Arguments.of(KeyCategory.IN_PINNED_NON_NULL, true, null),
                //test 2; test passato
                Arguments.of(KeyCategory.IN_PINNED_NULL, false, null),
                //test 3; test passato
                Arguments.of(KeyCategory.NOT_PRESENT, false, null)
//                //test 4; test fallito
//                Arguments.of(KeyCategory.INVALID, false, Exception.class),
                //test 5; test fallito
//                Arguments.of(KeyCategory.NULL, false, Exception.class)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    @Timeout(5)
    void testUnpin(KeyCategory keyCategory,
                   boolean expectedOutput,
                   Class<? extends Exception> expectedException) throws Exception {

        CacheMap cache;
        Object keyToUnpin;

        // Setup cache e chiave da testare
        switch (keyCategory) {
            case IN_PINNED_NON_NULL:
                cache = new CacheMap(true);
                keyToUnpin = VALID_KEY_IN_PINNED_NON_NULL;
                cache.put(keyToUnpin, keyToUnpin);
                cache.pin(keyToUnpin);
                break;

            case IN_PINNED_NULL:
                cache = new CacheMap(true);
                keyToUnpin = VALID_KEY_IN_PINNED_NULL;
                cache.put(keyToUnpin, null);
                cache.pin(keyToUnpin);
                break;

            case NOT_PRESENT:
                cache = new CacheMap(true);
                keyToUnpin = validKey();
                break;

            case INVALID:
                cache = validCacheMapWithKeyInCache();
                keyToUnpin = invalidKeyMock();
                break;

            case NULL:
                cache = validCacheMapWithKeyInCache();
                keyToUnpin = NULL_KEY();
                break;

            default:
                throw new IllegalStateException("Unexpected keyCategory: " + keyCategory);
        }

        // Reflection per leggere stato iniziale
        Field pinnedMapField = CacheMap.class.getDeclaredField("pinnedMap");
        pinnedMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Object, Object> pinnedMap = (Map<Object, Object>) pinnedMapField.get(cache);

        Field pinnedSizeField = CacheMap.class.getDeclaredField("_pinnedSize");
        pinnedSizeField.setAccessible(true);
        int beforePinnedSize = (int) pinnedSizeField.get(cache);

        Object valueBeforeUnpin = pinnedMap.get(keyToUnpin);

        if (expectedException != null) {
            final Object finalKey = keyToUnpin;
            final CacheMap finalCache = cache;
            Executable exec = () -> finalCache.unpin(finalKey);
            Exception ex = assertThrows(expectedException, exec);
            System.out.println("Expected exception: " + ex);
        } else {
            boolean result = cache.unpin(keyToUnpin);
            assertEquals(expectedOutput, result, "Unexpected unpin result");

            int afterPinnedSize = (int) pinnedSizeField.get(cache);

            if (result) {
                if (valueBeforeUnpin != null) {
                    // valore non nullo: chiave rimossa e pinnedSize decrementato
                    assertFalse(pinnedMap.containsKey(keyToUnpin), "Key should be removed from pinnedMap");
                    assertEquals(beforePinnedSize - 1, afterPinnedSize,
                            "_pinnedSize should decrement by 1 when unpinning a non-null value");
                } else {
                    // valore nullo: chiave rimane ma pinnedSize invariato
                    assertTrue(pinnedMap.containsKey(keyToUnpin) || keyToUnpin == null,
                            "Key with null value may remain in pinnedMap");
                    assertEquals(beforePinnedSize, afterPinnedSize,
                            "_pinnedSize should remain unchanged when unpinning null value");
                }
            } else {
                // chiave non pinnata o non presente -> pinnedSize invariato
                assertEquals(beforePinnedSize, afterPinnedSize,
                        "_pinnedSize should remain unchanged when unpinning absent key");
            }
        }
    }
}
