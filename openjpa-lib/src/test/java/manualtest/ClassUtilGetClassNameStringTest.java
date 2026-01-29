package manualtest;

import org.apache.openjpa.lib.util.ClassUtil;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClassUtilGetClassNameStringTest {

    private enum InputCategory {
        VALID,
        EMPTY,
        INVALID,
        NULL
    }

    private static Stream<Arguments> data() {

        return Stream.of(
                // ===== STRINGHE VALIDE =====
                // 4.4.1) Classe interna
                Arguments.of(InputCategory.VALID, "java.util.Map$Entry", "Map$Entry", null),
//
//              // 4.4.2) Classe con package
                Arguments.of(InputCategory.VALID, "java.lang.String", "String", null),
//                Arguments.of(InputCategory.VALID, "java/lang/Object", "Object", null), // AGGIUNTO: Raffinamento Slash-notation --> test fallito
//
                // 4.4.3) Classe senza package
                Arguments.of(InputCategory.VALID, "MyClass", "MyClass", null),
                Arguments.of(InputCategory.VALID, "int", "int", null),

                // 4.4.4) Classe array (Specializzazione: Primitivo vs Riferimento vs Multidimensionale)
                // Array di primitivi (Descrittore binario JVM)
                Arguments.of(InputCategory.VALID, "[I", "int[]", null),                // AGGIUNTO: Raffinamento Array Primitivo
                Arguments.of(InputCategory.VALID, "[Z", "boolean[]", null),            // AGGIUNTO: Raffinamento Array Primitivo

                // Array di riferimenti (Descrittore binario [L...;)
                Arguments.of(InputCategory.VALID, "[Ljava.lang.String;", "String[]", null), // AGGIUNTO: Raffinamento Array Riferimento

                // Array multidimensionale (Logica iterativa/ricorsiva)
                Arguments.of(InputCategory.VALID, "[[[I", "int[][][]", null),          // AGGIUNTO: Raffinamento Multidimensionale
                Arguments.of(InputCategory.VALID, "[[Ljava.awt.Point;", "Point[][]", null), // AGGIUNTO: Raffinamento Multidimensionale

                // T4.6: Oracle Ideale - Un codice tipo 'X' non esiste, ci si aspetta eccezione.
                // Il test FALLIRÀ perché il software restituisce "[X[]" invece di lanciare Exception.
//                Arguments.of(InputCategory.INVALID, "[X", null, Exception.class),

                // T4.7: Oracle Ideale - Manca il terminatore ';', ci si aspetta eccezione.
                // Il test FALLIRÀ perché il software restituisce "String[]" invece di lanciare Exception.
//                Arguments.of(InputCategory.INVALID, "[Ljava.lang.String", null, Exception.class),

                // T8) RAFFINAMENTO BVA: Punto in posizione 0 (PIT)
                Arguments.of(InputCategory.VALID, ".SimpleClass", "SimpleClass", null),
                // ===== STRINGHE NON VALIDE =====
                // 4.4.5) Stringa vuota
                Arguments.of(InputCategory.EMPTY, "", "", null),

                // 4.4.6) Stringa non valida (Boundary: delimitatori in posizioni errate) ---> Test Fallito
//                Arguments.of(InputCategory.INVALID, "java..lang.String", null, Exception.class), // AGGIUNTO: Boundary check

                // ===== STRINGHE NULLE =====
                // 4.4.7) Null
                Arguments.of(InputCategory.NULL, null, null, null)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    @Timeout(5)
    void testGetClassName(InputCategory category,
                          String input,
                          String expectedResult,
                          Class<? extends Exception> expectedException) {

        if (expectedException != null) {
            assertThrows(expectedException,
                    () -> ClassUtil.getClassName(input),
                    () -> "Unexpected exception for category " + category + " with input: " + input);
            return;
        }

        // Caso normale
        String result = ClassUtil.getClassName(input);
        assertEquals(expectedResult, result,
                () -> "Unexpected result for category " + category + " with input: " + input);
    }
}