package manualtest;

import costumutils.InitCheckClass;
import costumutils.Utils;
import org.apache.openjpa.lib.util.ClassUtil;
import org.junit.jupiter.api.BeforeEach;
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

    @BeforeEach
    void resetInitCheck() {
        // Resetta il flag prima di ogni test
        InitCheckClass.initialized = false;
    }

    private static Stream<Arguments> data() {
        return Stream.of(
                // ===== CLASSI VALIDE =====
                Arguments.of(InputCategory.VALID_CLASS, Utils.validInnerClass(), Utils.resolveFalse(), Utils.validClassLoader(), javax.swing.JSpinner.DefaultEditor.class, null),
                Arguments.of(InputCategory.VALID_CLASS, Utils.validNormalClassWithPackage(), Utils.resolveFalse(), Utils.validClassLoader(), String.class, null),
                Arguments.of(InputCategory.VALID_CLASS, Utils.validPrimitiveClass(), Utils.resolveFalse(), Utils.validClassLoader(), int.class, null),
                Arguments.of(InputCategory.VALID_CLASS, Utils.validArrayClassObject(), Utils.resolveFalse(), Utils.validClassLoader(), java.awt.Point[][].class, null),

                // ===== STRINGHE NON VALIDE =====
                Arguments.of(InputCategory.INVALID_CLASS, Utils.emptyString(), Utils.resolveFalse(), Utils.validClassLoader(), null, Exception.class),
                Arguments.of(InputCategory.INVALID_CLASS, Utils.invalidBinaryName(), Utils.resolveFalse(), Utils.validClassLoader(), null, Exception.class),
                Arguments.of(InputCategory.NULL_CLASS, Utils.nullString(), Utils.resolveFalse(), Utils.validClassLoader(), null, Exception.class),

                // ===== CLASSE DI TEST InitCheckClass =====
                Arguments.of(InputCategory.VALID_CLASS, "costumutils.InitCheckClass", true, null, InitCheckClass.class, null)
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

        // Caso speciale InitCheckClass
        Class<?> result = ClassUtil.toClass(str, resolve, effectiveLoader);
        if ("costumutils.InitCheckClass".equals(str)) {
            // Verifica il flag
            if (Boolean.TRUE.equals(resolve)) {
                assertTrue(InitCheckClass.initialized, "Class should have been initialized when resolve=true");
            } else {
                assertFalse(InitCheckClass.initialized, "Class should not be initialized when resolve=false");
            }
            assertEquals("costumutils.InitCheckClass", result.getName(), "Unexpected result class");
        } else {
            // Test normale per tutte le altre classi
            assertEquals(expectedClass, result,
                    () -> "Unexpected result for category " + category + " with input: " + str);
        }
    }
}