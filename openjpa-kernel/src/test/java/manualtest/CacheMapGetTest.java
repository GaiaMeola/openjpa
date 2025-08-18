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
class CacheMapGetTest {

    private enum KeyCategory {
        IN_SOFT,
        IN_CACHE,
        IN_PINNED,
        NOT_PRESENT,
        INVALID,
        NULL
    }

    private static Stream<Arguments> data() {
        return Stream.of(
                // --- t1: chiave presente in softMap; test passato
                Arguments.of(KeyCategory.IN_SOFT, "softValue", null),
//              // --- t2: chiave presente in cacheMap; test passato
                Arguments.of(KeyCategory.IN_CACHE, "valueCache", null),
//              // --- t3: chiave presente in pinnedMap; test passato
                Arguments.of(KeyCategory.IN_PINNED, "pinnedValue", null),
//              // --- t4: chiave non presente in nessuna mappa; test passato
                Arguments.of(KeyCategory.NOT_PRESENT, null, null)
//////          // --- t5: chiave invalida; test fallito
//              Arguments.of(KeyCategory.INVALID, null, Exception.class)
////             // --- t6: chiave nulla; test fallito
//                Arguments.of(KeyCategory.NULL, null, Exception.class)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    @Timeout(5)
    void testGet(KeyCategory keyCategory,
                 Object expectedOutput,
                 Class<? extends Exception> expectedException) throws Exception {

        CacheMap cache;
        Object keyUnderTest;

        switch (keyCategory) {
            case IN_SOFT:
                cache = new CacheMap(true);
                keyUnderTest = VALID_KEY_IN_SOFT;
                putInSoftMap(cache, keyUnderTest, "softValue"); // reflection, metodo già pronto
                break;

            case IN_CACHE:
                cache = validCacheMapWithKeyInCache();
                keyUnderTest = validKey(); // quello che validCacheMapWithKeyInCache() inserisce
                break;

            case IN_PINNED:
                cache = validCacheMapAlwaysPinned();
                keyUnderTest = VALID_KEY_IN_PINNED_NON_NULL;
                cache.put(keyUnderTest, "pinnedValue");
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
            Executable exec = () -> finalCache.get(finalKey);
            Exception ex = assertThrows(expectedException, exec);
            System.out.println("Expected exception: " + ex);
        } else {
            Object result = cache.get(keyUnderTest);
            assertEquals(expectedOutput, result, "Unexpected get() result");

            if (keyCategory == KeyCategory.IN_SOFT) {
                // Verifica che la chiave sia stata spostata in cacheMap
                Field cacheMapField = CacheMap.class.getDeclaredField("cacheMap");
                cacheMapField.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<Object, Object> cacheMapInternal = (Map<Object, Object>) cacheMapField.get(cache);

                assertTrue(cacheMapInternal.containsKey(keyUnderTest),
                        "Key from softMap should be moved to cacheMap after get()");
            }
        }
    }
}