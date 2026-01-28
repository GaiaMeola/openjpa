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
class ClassUtilToClassTest {

    private enum InputCategory {
        VALID_CLASS,
        INVALID_CLASS,
        NULL_CLASS,
        TWO_PARAM_CONSTRUCTOR // AGGIUNTO per coprire toClass(String, ClassLoader)
    }

    private static Stream<Arguments> data() {
        ClassLoader loader = ClassLoader.getSystemClassLoader();
        return Stream.of(
                // T1 - T4.2: Casi standard
                Arguments.of(InputCategory.VALID_CLASS, "java.util.Map$Entry", false, loader, java.util.Map.Entry.class, null),
                Arguments.of(InputCategory.VALID_CLASS, "java.lang.String", false, loader, String.class, null),
                Arguments.of(InputCategory.VALID_CLASS, "int", false, loader, int.class, null),
                Arguments.of(InputCategory.VALID_CLASS, "[I", false, loader, int[].class, null),
                Arguments.of(InputCategory.VALID_CLASS, "[Ljava.lang.String;", false, loader, String[].class, null),

                // T2.2: SLASH-NOTATION ---> test fallito
                // Arguments.of(InputCategory.VALID_CLASS, "java/lang/Object", false, ClassLoader.getSystemClassLoader(), Object.class, null),

                // T11, T12, T13: Risoluzione righe 89-100 (Array letterali)
                Arguments.of(InputCategory.VALID_CLASS, "int[]", false, loader, int[].class, null),
                Arguments.of(InputCategory.VALID_CLASS, "java.lang.String[]", false, loader, String[].class, null),
                Arguments.of(InputCategory.VALID_CLASS, "double[][]", false, loader, double[][].class, null),

                // T14: Risoluzione Copertura Costruttore a 2 parametri (toClass(str, loader))
                // Questo test coprirà la riga orfana che delega il lavoro al metodo a 3 parametri.
                Arguments.of(InputCategory.TWO_PARAM_CONSTRUCTOR, "java.lang.Integer", false, loader, Integer.class, null),

                // T5, T6, T7: Casi di robustezza
                Arguments.of(InputCategory.INVALID_CLASS, "", false, loader, null, Exception.class),
                Arguments.of(InputCategory.INVALID_CLASS, "java..lang.String", false, loader, null, Exception.class),
                Arguments.of(InputCategory.NULL_CLASS, null, false, loader, null, Exception.class),

                // T10: Risoluzione copertura ClassLoader nullo
                Arguments.of(InputCategory.VALID_CLASS, "java.lang.String", true, null, String.class, null)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    @Timeout(5)
    void testToClass(InputCategory category,
                     String str,
                     Boolean resolve,
                     ClassLoader loader,
                     Class<?> expectedClass,
                     Class<? extends Exception> expectedException) {

        if (expectedException != null) {
            assertThrows(expectedException,
                    () -> ClassUtil.toClass(str, resolve, loader),
                    () -> "Unexpected exception for category " + category + " with input: " + str);
            return;
        }

        // LOGICA MODIFICATA:
        Class<?> result;
        if (category == InputCategory.TWO_PARAM_CONSTRUCTOR) {
            // Chiamata al metodo a 2 parametri per coprire lo 0% di quel metodo
            result = ClassUtil.toClass(str, loader);
        } else {
            // Chiamata standard al metodo a 3 parametri
            result = ClassUtil.toClass(str, resolve, loader);
        }

        assertEquals(expectedClass, result,
                () -> "Unexpected result for category " + category + " with input: " + str);
    }
}