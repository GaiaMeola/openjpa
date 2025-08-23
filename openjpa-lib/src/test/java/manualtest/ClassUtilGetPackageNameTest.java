package manualtest;

import org.apache.openjpa.lib.util.ClassUtil;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClassUtilGetPackageNameTest {

    private enum InputCategory {
        VALID_CLASS,
        NULL_CLASS
    }

    private static Stream<Arguments> data() {
        return Stream.of(
                // 4.2.1: Classe valida con package → restituisce il package
                Arguments.of(InputCategory.VALID_CLASS, String.class, "java.lang", null),

                // 4.2.2 (sottocaso): Classe senza package → restituisce stringa vuota
                Arguments.of(InputCategory.VALID_CLASS, int.class, "", null),

                // 4.2.3: Classe nulla → restituisce null
                Arguments.of(InputCategory.NULL_CLASS, null, null, null)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    @Timeout(5)
    void testGetPackageName(InputCategory category,
                            Class<?> input,
                            String expectedOutput,
                            Class<? extends Exception> expectedException) {

        if (expectedException != null) {
            assertThrows(expectedException, () -> ClassUtil.getPackageName(input));
        } else {
            String result = ClassUtil.getPackageName(input);
            assertEquals(expectedOutput, result,
                    () -> "Unexpected result for category " + category + " with input " + input);
        }
    }
}
