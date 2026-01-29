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
        TWO_PARAM_CONSTRUCTOR // Per toClass(String, ClassLoader)
    }

    private static Stream<Arguments> data() {
        ClassLoader systemLoader = ClassLoader.getSystemClassLoader();

        // T14: Creazione ClassLoader Invalido tramite Mock
        ClassLoader invalidLoader = new ClassLoader() {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                throw new ClassNotFoundException("Simulated Failure");
            }
        };

        return Stream.of(
                // T1 - T5: Casi standard
                Arguments.of(InputCategory.VALID_CLASS, "java.util.Map$Entry", false, systemLoader, java.util.Map.Entry.class, null),
                Arguments.of(InputCategory.VALID_CLASS, "java.lang.String", false, systemLoader, String.class, null),
                Arguments.of(InputCategory.VALID_CLASS, "int", false, systemLoader, int.class, null),
                Arguments.of(InputCategory.VALID_CLASS, "[I", false, systemLoader, int[].class, null),
                Arguments.of(InputCategory.VALID_CLASS, "[Ljava.lang.String;", false, systemLoader, String[].class, null),

                // T4.3: Array Multidimensionale Descrittore (JVM)
                Arguments.of(InputCategory.VALID_CLASS, "[[I", false, systemLoader, int[][].class, null),

                // T2.2: SLASH-NOTATION (Mantenuto fallito/eccezione per documentare bug robustezza)
//                Arguments.of(InputCategory.INVALID_CLASS, "java/lang/Object", false, systemLoader, null, Exception.class),

                // T11, T12, T13: Array letterali (Sintassi Java)
                Arguments.of(InputCategory.VALID_CLASS, "int[]", false, systemLoader, int[].class, null),
                Arguments.of(InputCategory.VALID_CLASS, "java.lang.String[]", false, systemLoader, String[].class, null),
                Arguments.of(InputCategory.VALID_CLASS, "double[][]", false, systemLoader, double[][].class, null),

                // T9 (ex T14): Copertura Metodo a 2 parametri (Delega interna)
                Arguments.of(InputCategory.TWO_PARAM_CONSTRUCTOR, "java.lang.Integer", false, systemLoader, Integer.class, null),

                // T6, T7, T8: Casi di robustezza (Input malformati)
                Arguments.of(InputCategory.INVALID_CLASS, "", false, systemLoader, null, Exception.class),
                Arguments.of(InputCategory.INVALID_CLASS, "java..lang.String", false, systemLoader, null, Exception.class),
                Arguments.of(InputCategory.NULL_CLASS, null, false, systemLoader, null, Exception.class),

                // T9: ClassLoader nullo (Fallback su Context Loader)
                Arguments.of(InputCategory.VALID_CLASS, "manualtest.ClassUtilToClassTest", true, null, ClassUtilToClassTest.class, null),

                // T10: ClassLoader Invalido (Mock)
                Arguments.of(InputCategory.INVALID_CLASS, "java.lang.String", false, invalidLoader, null, Exception.class)
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
                    () -> "Eccezione attesa non sollevata per: " + str);
            return;
        }

        Class<?> result;
        if (category == InputCategory.TWO_PARAM_CONSTRUCTOR) {
            // Chiamata alla firma a 2 parametri per coprire il metodo delegante
            result = ClassUtil.toClass(str, loader);
        } else {
            // Chiamata alla firma completa a 3 parametri
            result = ClassUtil.toClass(str, resolve, loader);
        }

        assertEquals(expectedClass, result, "Risultato inatteso per input: " + str);
    }
}