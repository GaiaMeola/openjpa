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
class CacheMapRemoveTest {

    private enum KeyCategory {
        IN_SOFT,
        IN_CACHE,
        IN_PINNED_NON_NULL,
        IN_PINNED_NULL,
        NOT_PRESENT,
        INVALID,
        NULL
    }

    private static Stream<Arguments> data() {
        return Stream.of(
                //test t1; test passato
                Arguments.of(KeyCategory.IN_SOFT, "softValue", null),
                //test t2; test passato
                Arguments.of(KeyCategory.IN_CACHE, "valueCache", null),
                //test t3; test passato
                Arguments.of(KeyCategory.IN_PINNED_NON_NULL, "pinnedValue", null),
                //test t4; test passato
                Arguments.of(KeyCategory.IN_PINNED_NULL, null, null),
                //test t5; test passato
                Arguments.of(KeyCategory.NOT_PRESENT, null, null)
                //test t6; test fallito
//                Arguments.of(KeyCategory.INVALID, null, Exception.class)
                //test t7; test fallito
//                Arguments.of(KeyCategory.NULL, null, Exception.class)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    @Timeout(5)
    void testRemove(KeyCategory keyCategory,
                    Object expectedOutput,
                    Class<? extends Exception> expectedException) throws Exception {

        CacheMap cache;
        Object keyUnderTest;

        switch (keyCategory) {
            case IN_SOFT:
                cache = new CacheMap(true);
                keyUnderTest = VALID_KEY_IN_SOFT;
                putInSoftMap(cache, keyUnderTest, "softValue");
                break;

            case IN_CACHE:
                cache = validCacheMapWithKeyInCache();
                keyUnderTest = validKey(); // già presente con "valueCache"
                break;

            case IN_PINNED_NON_NULL:
                cache = validCacheMapAlwaysPinned();
                keyUnderTest = VALID_KEY_IN_PINNED_NON_NULL;
                cache.put(keyUnderTest, "pinnedValue");
                break;

            case IN_PINNED_NULL:
                cache = validCacheMapAlwaysPinned();
                keyUnderTest = VALID_KEY_IN_PINNED_NULL;
                cache.put(keyUnderTest, null);
                break;

            case NOT_PRESENT:
                cache = emptyValidCacheMap();
                keyUnderTest = validKey();
                break;

            case INVALID:
                cache = validCacheMapWithKeyInCache();
                keyUnderTest = invalidKeyMock();
                break;

            case NULL:
                cache = validCacheMapWithKeyInCache();
                keyUnderTest = NULL_KEY();
                break;

            default:
                throw new IllegalStateException("Unexpected keyCategory: " + keyCategory);
        }

        if (expectedException != null) {
            final Object finalKey = keyUnderTest;
            final CacheMap finalCache = cache;
            Executable exec = () -> finalCache.remove(finalKey);
            Exception ex = assertThrows(expectedException, exec);
            System.out.println("Expected exception: " + ex);
        } else {
            // Salva pinnedSize prima della rimozione
            Field pinnedSizeField = CacheMap.class.getDeclaredField("_pinnedSize");
            pinnedSizeField.setAccessible(true);
            int beforePinnedSize = (int) pinnedSizeField.get(cache);

            Object result = cache.remove(keyUnderTest);
            assertEquals(expectedOutput, result, "Unexpected remove() result");

            // Controlli specifici
            if (keyCategory == KeyCategory.IN_PINNED_NON_NULL || keyCategory == KeyCategory.IN_PINNED_NULL) {
                verifyPinnedSize(cache, keyUnderTest, beforePinnedSize, keyCategory == KeyCategory.IN_PINNED_NON_NULL);
            }

            if (keyCategory == KeyCategory.IN_SOFT) {
                Field softMapField = CacheMap.class.getDeclaredField("softMap");
                softMapField.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<Object, Object> softMapInternal = (Map<Object, Object>) softMapField.get(cache);

                assertFalse(softMapInternal.containsKey(keyUnderTest),
                        "Key should be removed from softMap");
            }

            if (keyCategory == KeyCategory.IN_CACHE) {
                Field cacheMapField = CacheMap.class.getDeclaredField("cacheMap");
                cacheMapField.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<Object, Object> cacheMapInternal = (Map<Object, Object>) cacheMapField.get(cache);

                assertFalse(cacheMapInternal.containsKey(keyUnderTest),
                        "Key should be removed from cacheMap");
            }
        }
    }

    // Helper per verificare pinnedSize e pinnedMap
    private void verifyPinnedSize(CacheMap cache, Object key, int beforeSize, boolean shouldDecrement) throws Exception {
        Field pinnedMapField = CacheMap.class.getDeclaredField("pinnedMap");
        pinnedMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Object, Object> pinnedMapInternal = (Map<Object, Object>) pinnedMapField.get(cache);

        assertTrue(pinnedMapInternal.containsKey(key),
                "Pinned key should remain in pinnedMap after remove()");

        if (shouldDecrement) {
            // Caso IN_PINNED_NON_NULL → il valore precedente era non null
            assertNull(pinnedMapInternal.get(key),
                    "Pinned key value should become null after remove()");
        } else {
            // Caso IN_PINNED_NULL → il valore era già null
            assertNull(pinnedMapInternal.get(key),
                    "Pinned key should stay null after remove()");
        }

        Field pinnedSizeField = CacheMap.class.getDeclaredField("_pinnedSize");
        pinnedSizeField.setAccessible(true);
        int afterSize = (int) pinnedSizeField.get(cache);

        if (shouldDecrement) {
            assertEquals(beforeSize - 1, afterSize,
                    "_pinnedSize must decrement when removing pinned non-null value");
        } else {
            assertEquals(beforeSize, afterSize,
                    "_pinnedSize must not decrement when removing pinned null value");
        }
    }
}