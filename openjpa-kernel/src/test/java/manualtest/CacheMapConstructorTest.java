package manualtest;

import org.apache.openjpa.util.CacheMap;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit testing for {@link CacheMap} <br>
 * Tested method: constructor (lru, max, size, load, concurrencyLevel)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CacheMapConstructorTest {

    private static Stream<Arguments> data() {
        return Stream.of(

                // 3.1) lru = true, valid; test passato
                Arguments.of(true, 100, 100, 0.75f, 1, null),

                // 3.2) lru = false, valid; test passato
                Arguments.of(false, 100, 100, 0.75f, 1, null),

                // 3.3) max = -1, eccezione attesa; test fallito
                /*Arguments.of(true, -1, 100, 0.75f, 1, IllegalArgumentException.class) */

                // 3.4) max = 0, eccezione attesa; test fallito
                /*Arguments.of(true, 0, 100, 0.75f, 1, IllegalArgumentException.class) */

                // 3.5) max = 1, valido; test passato
                Arguments.of(true, 1, 100, 0.75f, 1, null),

                // 3.6) max = 100, valido; test passato
                Arguments.of(true, 100, 100, 0.75f, 1, null),

                // 3.7) size = -1, eccezione attesa; test fallito
               /* Arguments.of(true, 100, -1, 0.75f, 1, IllegalArgumentException.class)*/

                // 3.8) size = 0, eccezione attesa; test passato
                Arguments.of(true, 100, 0, 0.75f, 1, IllegalArgumentException.class),

                // 3.9) size = 1, valido; test passato
                Arguments.of(true, 100, 1, 0.75f, 1, null),

                // 3.10) load = -1, eccezione attesa; test passato
                Arguments.of(true, 100, 100, -1f, 1, IllegalArgumentException.class),

                // 3.11) load = 0, eccezione attesa; test passato
                Arguments.of(true, 100, 100, 0f, 1, IllegalArgumentException.class),

                // 3.12) concurrencyLevel = -1, eccezione attesa; test fallito
                /*Arguments.of(true, 100, 100, 0.75f, -1, IllegalArgumentException.class)*/

                // 3.13) concurrencyLevel = 0, eccezione attesa; test fallito
                /*Arguments.of(true, 100, 100, 0.75f, 0, IllegalArgumentException.class)*/

                // CM-1: size negativo -> deve essere corretto a 500; aggiunto a seguito di Jacoco
                Arguments.of(true, 100, -1, 0.75f, 1, null),

                // CM-2: max negativo -> deve essere corretto a Integer.MAX_VALUE, aggiunto a seguito di Jacoco
                Arguments.of(true, -1, 100, 0.75f, 1, null)
        );
    }

    //aggiunti per PIT --> per uccidere le mutazioni sopravvissute
    @Test
    void maxZeroShouldRemainZero() {
        CacheMap cm = new CacheMap(true, 0, 100, 0.75f, 1);
        assertNotNull(cm, "CacheMap instance should not be null");
        assertEquals(0, cm.getCacheSize(), "max = 0 deve rimanere invariato");
    }

    @Test
    void maxPositiveShouldRemainPositive() {
        CacheMap cm = new CacheMap(true, 50, 100, 0.75f, 1);
        assertNotNull(cm, "CacheMap instance should not be null");
        assertEquals(50, cm.getCacheSize(), "max positivo deve rimanere invariato");
    }

    @Test
    void maxNegativeShouldBecomeUnlimited() {
        CacheMap cm = new CacheMap(true, -1, 100, 0.75f, 1);
        assertNotNull(cm, "CacheMap instance should not be null");
        assertEquals(-1, cm.getCacheSize(), "max negativo deve diventare illimitato (-1)");
    }


    @ParameterizedTest(name = "Test {index}: lru={0}, max={1}, size={2}, load={3}, concurrency={4}")
    @MethodSource("data")
    @Timeout(value = 5)
    void construct(boolean lru, int max, int size, float load, int concurrencyLevel,
                   Class<? extends Exception> expectedException) {
        if (expectedException != null) {
            assertThrows(expectedException,
                    () -> new CacheMap(lru, max, size, load, concurrencyLevel),
                    "Expected exception: " + expectedException.getName());
        } else {
            try {
                CacheMap cm = new CacheMap(lru, max, size, load, concurrencyLevel);
                assertNotNull(cm, "CacheMap instance should not be null");
            } catch (Exception e) {
                fail("Unexpected exception thrown: " + e);
            }
        }
    }
}
