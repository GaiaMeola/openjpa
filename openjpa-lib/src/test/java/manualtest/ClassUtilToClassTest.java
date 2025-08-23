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
class ClassUtilToClassTest {

    private enum InputCategory {
        VALID_CLASS,
        INVALID_CLASS,
        NULL_CLASS
    }

    private static Stream<Arguments> data() {
        return Stream.of(
                // ===== CLASSI VALIDE =====
                //test 4.3.1; test passato
                Arguments.of(InputCategory.VALID_CLASS, Utils.validInnerClass(), Utils.resolveFalse(), Utils.validClassLoader(), javax.swing.JSpinner.DefaultEditor.class, null),
                //test 4.3.2; test passato
                Arguments.of(InputCategory.VALID_CLASS, Utils.validNormalClassWithPackage(), Utils.resolveFalse(), Utils.validClassLoader(), String.class, null),
                //test 4.3.3; test passato
                Arguments.of(InputCategory.VALID_CLASS, Utils.validPrimitiveClass(), Utils.resolveFalse(), Utils.validClassLoader(), int.class, null),
                //test 4.3.4; test passato
                Arguments.of(InputCategory.VALID_CLASS, Utils.validArrayClassObject(), Utils.resolveFalse(), Utils.validClassLoader(), java.awt.Point[][].class, null),

                // ===== STRINGHE NON VALIDE =====
                //test 4.3.5; test passato
                Arguments.of(InputCategory.INVALID_CLASS, Utils.emptyString(), Utils.resolveFalse(), Utils.validClassLoader(), null, Exception.class),
                //test 4.3.6; test passato
                Arguments.of(InputCategory.INVALID_CLASS, Utils.invalidBinaryName(), Utils.resolveFalse(), Utils.validClassLoader(), null, Exception.class),
                //test 4.3.7; test passato
                Arguments.of(InputCategory.NULL_CLASS, Utils.nullString(), Utils.resolveFalse(), Utils.validClassLoader(), null, Exception.class),
                //test 4.3.8; test passato
                Arguments.of(InputCategory.VALID_CLASS, Utils.validInnerClass(), Utils.resolveTrue(), Utils.validClassLoader(), javax.swing.JSpinner.DefaultEditor.class, null),
                //test 4.3.9; test passato
                Arguments.of(InputCategory.INVALID_CLASS, Utils.validInnerClass(), Utils.resolveTrue(), Utils.invalidClassLoader(), null, Exception.class),
                //test 4.3.10; test passato
                Arguments.of(InputCategory.NULL_CLASS, Utils.validInnerClass(), Utils.resolveTrue(), Utils.nullClassLoader(), javax.swing.JSpinner.DefaultEditor.class, null)
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

        ClassLoader effectiveLoader = loader != null ? loader : Thread.currentThread().getContextClassLoader();

        if (expectedException != null) {
            assertThrows(expectedException,
                    () -> ClassUtil.toClass(str, resolve, effectiveLoader),
                    () -> "Unexpected exception for category " + category + " with input: " + str);
            return;
        }

        // Caso normale: verifico che il risultato sia quello atteso
        Class<?> result = ClassUtil.toClass(str, resolve, effectiveLoader);
        assertEquals(expectedClass, result,
                () -> "Unexpected result for category " + category + " with input: " + str);
    }
}