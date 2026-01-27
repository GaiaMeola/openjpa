package manualtest;

import org.apache.openjpa.util.CacheMap;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class CacheMapConstructorTest {

    private static Stream<Arguments> data() {
        return Stream.of(
                // 3.1 & 3.2: Casi validi standard (lru true/false)
                Arguments.of(true, 100, 100, 0.75f, 1, null, 100),
                Arguments.of(false, 100, 100, 0.75f, 1, null, 100),

                // T3: max = -1 -> Autocorrezione a Integer.MAX_VALUE (-1 in getCacheSize)
                Arguments.of(true, -1, 100, 0.75f, 1, null, -1),

                // T4: max = 0 -> Capacità nulla ammessa, storage disabilitato
                Arguments.of(true, 0, 100, 0.75f, 1, null, 0),

                // 3.5 & 3.6: max positivo (Valori di frontiera)
                Arguments.of(true, 1, 100, 0.75f, 1, null, 1),
                Arguments.of(true, 500, 100, 0.75f, 1, null, 500),

                // T7: size = -1 -> Autocorrezione al valore predefinito 500
                Arguments.of(true, 100, -1, 0.75f, 1, null, 100),

                // 3.8: size = 0 -> Dimensione iniziale non valida (Lancia eccezione dalla mappa interna)
                Arguments.of(true, 100, 0, 0.75f, 1, IllegalArgumentException.class, 0),

                // 3.10 & 3.11: load <= 0 -> Eccezione prevista (Tabella 44)
                Arguments.of(true, 100, 100, -1f, 1, IllegalArgumentException.class, 0),
                Arguments.of(true, 100, 100, 0f, 1, IllegalArgumentException.class, 0),

                // 3.12 & 3.13: concurrencyLevel <= 0 -> Solitamente autocorretto o accettato
                Arguments.of(true, 100, 100, 0.75f, -1, null, 100),
                Arguments.of(true, 100, 100, 0.75f, 0, null, 100)
        );
    }

    @ParameterizedTest(name = "Test {index}: max={1}, size={2}, load={3} -> Atteso: {5}")
    @MethodSource("data")
    @Timeout(5)
    void construct(boolean lru, int max, int size, float load, int concurrencyLevel,
                   Class<? extends Exception> expectedException, int expectedSizeResult) {

        if (expectedException != null) {
            // Verifica che vengano lanciate le eccezioni per parametri critici (come load o size=0)
            assertThrows(expectedException,
                    () -> new CacheMap(lru, max, size, load, concurrencyLevel));
        } else {
            // Esecuzione del costruttore con autocorrezione (Black Box)
            CacheMap cm = new CacheMap(lru, max, size, load, concurrencyLevel);

            assertNotNull(cm, "L'istanza di CacheMap dovrebbe essere valida");

            // Verifica dell'autocorrezione tramite API pubblica
            // getCacheSize() è il metodo pubblico per verificare la capacità massima impostata
            assertEquals(expectedSizeResult, cm.getCacheSize(),
                    "Il valore di max non corrisponde all'aspettativa (considerando l'autocorrezione)");

            // Verifica funzionale minima: la cache deve essere operativa
            if (max != 0) {
                cm.put("key", "value");
                assertEquals("value", cm.get("key"), "La cache dovrebbe permettere put/get se max > 0");
            }
        }
    }
}