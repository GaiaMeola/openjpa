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
class ClassUtilGetClassNameTest {

    private enum InputCategory {
        VALID_CLASS,
        NULL_CLASS
    }

    private static Stream<Arguments> data() {
        return Stream.of(
                // 4.1.1: Istanza valida → restituisce nome semplice della classe; test passato
                Arguments.of(InputCategory.VALID_CLASS, Object.class, "Object", null),

                // 4.1.2: Istanza nulla → restituisce null; test passato
                Arguments.of(InputCategory.NULL_CLASS, null, null, null)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    @Timeout(5)
    void testGetClassName(InputCategory category,
                          Class<?> input,
                          String expectedOutput,
                          Class<? extends Exception> expectedException) {

        if (expectedException != null) {
            assertThrows(expectedException, () -> ClassUtil.getClassName(input));
        } else {
            String result = ClassUtil.getClassName(input);
            assertEquals(expectedOutput, result,
                    () -> "Unexpected result for category " + category);
        }
    }
}
