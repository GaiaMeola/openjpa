package manualtest;

import org.apache.openjpa.util.CacheMap;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import customutils.Utils;

import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class CacheMapUnpinTest {

    private enum KeyCategory {
        IN_PINNED_NON_NULL, IN_PINNED_NULL, NOT_PRESENT, INVALID, NULL
    }

    private static Stream<Arguments> data() {
        return Stream.of(
                // test 1: Presente e non nullo -> unpin successo
                Arguments.of(KeyCategory.IN_PINNED_NON_NULL, true, null),
                // test 2: Presente ma nullo -> unpin ritorna false (secondo logica OpenJPA)
                Arguments.of(KeyCategory.IN_PINNED_NULL, false, null),
                // test 3: Non presente -> unpin ritorna false
                Arguments.of(KeyCategory.NOT_PRESENT, false, null),
                // test 4: Invalido -> RuntimeException (dal nostro Utils)
                Arguments.of(KeyCategory.INVALID, false, RuntimeException.class),
                // test 5: Chiave null -> Accettata (ritorna false perché non può essere pinnata validamente)
                Arguments.of(KeyCategory.NULL, false, null)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    @Timeout(5)
    void testUnpin(KeyCategory keyCategory,
                   boolean expectedOutput,
                   Class<? extends Exception> expectedException) {

        CacheMap cache = new CacheMap(true, 10); // Capacità standard sicura
        Object keyToUnpin;
        Object value = "someValue";

        // --- SETUP ---
        switch (keyCategory) {
            case IN_PINNED_NON_NULL:
                keyToUnpin = "pinnedKey";
                cache.put(keyToUnpin, value);
                cache.pin(keyToUnpin); // Lo blocchiamo
                break;
            case IN_PINNED_NULL:
                keyToUnpin = "nullPinnedKey";
                cache.put(keyToUnpin, null);
                cache.pin(keyToUnpin);
                break;
            case NOT_PRESENT:
                keyToUnpin = "absentKey";
                break;
            case INVALID:
                keyToUnpin = Utils.invalidKeyMock();
                break;
            case NULL:
                keyToUnpin = null;
                break;
            default:
                throw new IllegalStateException("Unexpected category");
        }

        // --- ESECUZIONE E VERIFICA ---
        if (expectedException != null) {
            assertThrows(expectedException, () -> cache.unpin(keyToUnpin));
        } else {
            boolean result = cache.unpin(keyToUnpin);
            assertEquals(expectedOutput, result, "Il risultato di unpin() non è corretto");

            // Verifica funzionale post-unpin:
            // 1. Se l'unpin ha avuto successo o la chiave esisteva, il dato deve essere ancora leggibile.
            // Unpin rimuove il blocco, non l'oggetto dalla cache!
            if (keyCategory == KeyCategory.IN_PINNED_NON_NULL) {
                assertEquals(value, cache.get(keyToUnpin), "L'oggetto deve essere ancora presente dopo l'unpin");
            }
        }
    }
}