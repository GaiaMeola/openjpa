package manualtest;

import costumutils.Utils;
import org.apache.openjpa.lib.util.ClassUtil;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

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
                // 4.4.1) Classe interna; test passato
                Arguments.of(InputCategory.VALID, Utils.validInnerClass(), "JSpinner$DefaultEditor", null),
                // 4.4.2) Classe con package; test passato
                Arguments.of(InputCategory.VALID, Utils.validNormalClassWithPackage(), "String", null),
                // 4.4.3) Classe senza package (primitiva); test passato
                Arguments.of(InputCategory.VALID, Utils.validPrimitiveClass(), "int", null),
                // 4.4.4) Classe array; test passato
                Arguments.of(InputCategory.VALID, Utils.validArrayClassObject(), "Point[][]", null),

                // ===== STRINGHE NON VALIDE =====
                // 4.4.5) Stringa vuota; test passato
                Arguments.of(InputCategory.EMPTY, Utils.emptyString(), "", null),
                // 4.4.6) Stringa non valida; test fallito
//                Arguments.of(InputCategory.INVALID, Utils.invalidBinaryName(), null, Exception.class),

                // ===== STRINGHE NULLE =====
                // 4.4.7) Null; test passato
                Arguments.of(InputCategory.NULL, Utils.nullString(), null, null)
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