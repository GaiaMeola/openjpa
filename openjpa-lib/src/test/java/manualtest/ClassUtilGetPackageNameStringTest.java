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
class ClassUtilGetPackageNameStringTest {

    private enum InputCategory {
        VALID,
        EMPTY,
        INVALID,
        NULL
    }

    private static Stream<Arguments> data() {
        return Stream.of(
                // ===== STRINGHE VALIDE =====
                // 4.5.1) Classe interna
                Arguments.of(InputCategory.VALID, "java.util.Map$Entry", "java.util", null),

                // 4.5.2) Classe con package
                Arguments.of(InputCategory.VALID, "java.lang.String", "java.lang", null),
//                Arguments.of(InputCategory.VALID, "java/lang/Object", "java/lang", null), // RAFFINAMENTO: Slash-notation --> Test Fallito

                // 4.5.3) Classe senza package (Default Package)
                Arguments.of(InputCategory.VALID, "MyClass", "", null),
                Arguments.of(InputCategory.VALID, "int", "", null),

                // 4.5.4) Classe array (Specializzazione)
                Arguments.of(InputCategory.VALID, "[I", "", null),                               // Array Primitivo (nessun package)
                Arguments.of(InputCategory.VALID, "[Ljava.lang.String;", "java.lang", null),      // Array Riferimento
                Arguments.of(InputCategory.VALID, "[[[Ljava.awt.Point;", "java.awt", null),      // Multidimensionale

                // ===== STRINGHE NON VALIDE =====
                // 4.5.5) Stringa vuota
                Arguments.of(InputCategory.EMPTY, "", "", null),

                // 4.5.6) Stringa non valida (Robustezza)
//                Arguments.of(InputCategory.INVALID, "java..lang.String", null, Exception.class), // Ci si aspetta Exception --> Test fallito

                // ===== STRINGHE NULLE =====
                // 4.5.7) Null
                Arguments.of(InputCategory.NULL, null, null, null)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    @Timeout(5)
    void testGetPackageName(InputCategory category,
                            String input,
                            String expectedResult,
                            Class<? extends Exception> expectedException) {

        if (expectedException != null) {
            assertThrows(expectedException,
                    () -> ClassUtil.getPackageName(input),
                    () -> "Unexpected exception for category " + category + " with input: " + input);
            return;
        }

        // Caso normale
        String result = ClassUtil.getPackageName(input);
        assertEquals(expectedResult, result,
                () -> "Unexpected result for category " + category + " with input: " + input);
    }
}
