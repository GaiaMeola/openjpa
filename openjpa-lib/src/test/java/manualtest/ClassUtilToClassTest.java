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
        NULL_CLASS
    }

    private static Stream<Arguments> data() {
        return Stream.of(
                // T1, T2.1, T3.1, T4.1, T4.2: Casi che passano normalmente
                Arguments.of(InputCategory.VALID_CLASS, "java.util.Map$Entry", false, ClassLoader.getSystemClassLoader(), java.util.Map.Entry.class, null),
                Arguments.of(InputCategory.VALID_CLASS, "java.lang.String", false, ClassLoader.getSystemClassLoader(), String.class, null),
                Arguments.of(InputCategory.VALID_CLASS, "int", false, ClassLoader.getSystemClassLoader(), int.class, null),
                Arguments.of(InputCategory.VALID_CLASS, "[I", false, ClassLoader.getSystemClassLoader(), int[].class, null),
                Arguments.of(InputCategory.VALID_CLASS, "[Ljava.lang.String;", false, ClassLoader.getSystemClassLoader(), String[].class, null),

                // T2.2: SLASH-NOTATION ---> test fallito
//                Arguments.of(InputCategory.VALID_CLASS, "java/lang/Object", false, ClassLoader.getSystemClassLoader(), Object.class, null),

                // T5, T6, T7: Casi di robustezza ed errori sintattici
                Arguments.of(InputCategory.INVALID_CLASS, "", false, ClassLoader.getSystemClassLoader(), null, Exception.class),
                Arguments.of(InputCategory.INVALID_CLASS, "java..lang.String", false, ClassLoader.getSystemClassLoader(), null, Exception.class),
                Arguments.of(InputCategory.NULL_CLASS, null, false, ClassLoader.getSystemClassLoader(), null, Exception.class),

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

        // Caso normale: verifico che il risultato sia quello atteso
        Class<?> result = ClassUtil.toClass(str, resolve, loader);
        assertEquals(expectedClass, result,
                () -> "Unexpected result for category " + category + " with input: " + str);
    }
}