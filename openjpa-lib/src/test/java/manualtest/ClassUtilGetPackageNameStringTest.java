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
                // 4.5.1) Classe interna; test passato
                Arguments.of(InputCategory.VALID, Utils.validInnerClass(), "javax.swing", null),
//                // 4.5.2) Classe con package; test passato
                Arguments.of(InputCategory.VALID, Utils.validNormalClassWithPackage(), "java.lang", null),
//                // 4.5.3) Classe senza package (primitiva); test passato
                Arguments.of(InputCategory.EMPTY, Utils.validPrimitiveClass(), "", null),
//                // 4.5.4) Classe array; test passato
                Arguments.of(InputCategory.VALID, Utils.validArrayClassObject(), "java.awt", null),
//
//                // ===== STRINGHE NON VALIDE =====
//                // 4.5.5) Stringa vuota; test passato
                Arguments.of(InputCategory.EMPTY, Utils.emptyString(), "", null),
//                // 4.5.6) Stringa non valida; test fallito
//                Arguments.of(InputCategory.INVALID, Utils.invalidBinaryName(), null, Exception.class)
//
//                // ===== STRINGHE NULLE =====
//                // 4.5.7) Null; test passato
                Arguments.of(InputCategory.NULL, Utils.nullString(), null, null),

                //Aggiunti i test a seguito da JaCoCo
                Arguments.of(InputCategory.VALID, Utils.validPrimitiveArray(), "", null),         // GP-1
                Arguments.of(InputCategory.VALID, Utils.validPrimitiveMultiArray(), "", null),    // GP-2
                Arguments.of(InputCategory.VALID, "[Ljava.lang.String;", "java.lang", null)       // GP-3
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
