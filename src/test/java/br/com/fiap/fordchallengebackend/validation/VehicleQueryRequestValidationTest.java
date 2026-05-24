package br.com.fiap.fordchallengebackend.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class VehicleQueryRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    record TestRecord(
        @Brand String brand,
        @ModelName String model,
        @VersionName String version,
        @ValidVehicleType String vehicleType,
        @ValidAttributes List<String> attributes
    ) {}

    @ParameterizedTest
    @ValueSource(strings = {
        "Ford", "Ford-Motor", "F-150", "BMW X5",
        "Mercedes-Benz C180", "Volkswagen", "Chevrolet Onix Plus"
    })
    void shouldAcceptValidBrands(String brand) {
        assertTrue(validator.validateValue(TestRecord.class, "brand", brand).isEmpty());
    }

    static Stream<String> invalidBrands() {
        return Stream.of(
            "<script>alert(1)</script>",
            "; DROP TABLE users; --",
            "' OR 1=1 --",
            "<img src=x onerror=alert(1)>",
            "a".repeat(51),
            "",
            "  ",
            null
        );
    }

    @ParameterizedTest
    @MethodSource("invalidBrands")
    void shouldRejectInvalidBrands(String brand) {
        assertFalse(validator.validateValue(TestRecord.class, "brand", brand).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Ranger", "F-250", "Civic G10", "Corolla Altis Premium", "Onix Plus 1.0 Turbo"})
    void shouldAcceptValidModels(String model) {
        assertTrue(validator.validateValue(TestRecord.class, "model", model).isEmpty());
    }

    static Stream<String> invalidModels() {
        return Stream.of("<img src=x>", "a".repeat(81), "", "  ", null, "' OR 1=1 --");
    }

    @ParameterizedTest
    @MethodSource("invalidModels")
    void shouldRejectInvalidModels(String model) {
        assertFalse(validator.validateValue(TestRecord.class, "model", model).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Raptor", "Raptor 2.0", "XLT 4x4 Diesel", "Sport 2.0 Turbo AT", "Platinum V6"})
    void shouldAcceptValidVersions(String version) {
        assertTrue(validator.validateValue(TestRecord.class, "version", version).isEmpty());
    }

    static Stream<String> invalidVersions() {
        return Stream.of("a".repeat(101), "", "  ", null, "<script>");
    }

    @ParameterizedTest
    @MethodSource("invalidVersions")
    void shouldRejectInvalidVersions(String version) {
        assertFalse(validator.validateValue(TestRecord.class, "version", version).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"cars", "trucks", "motorcycles", "", "  ", "CARS", "TRUCKS"})
    void shouldAcceptValidVehicleTypes(String type) {
        assertTrue(validator.validateValue(TestRecord.class, "vehicleType", type).isEmpty());
    }

    @Test
    void shouldAcceptNullVehicleType() {
        assertTrue(validator.validateValue(TestRecord.class, "vehicleType", null).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"bicycle", "suv", "van", "invalid", "123"})
    void shouldRejectInvalidVehicleTypes(String type) {
        assertFalse(validator.validateValue(TestRecord.class, "vehicleType", type).isEmpty());
    }

    @Test
    void shouldAcceptValidAttributes() {
        assertTrue(validator.validateValue(TestRecord.class, "attributes", List.of("Motor", "Potencia")).isEmpty());
    }

    @Test
    void shouldAcceptMax20Attributes() {
        assertTrue(validator.validateValue(TestRecord.class, "attributes", Stream.generate(() -> "Motor").limit(20).toList()).isEmpty());
    }

    @Test
    void shouldRejectMoreThan20Attributes() {
        assertFalse(validator.validateValue(TestRecord.class, "attributes", Stream.generate(() -> "Motor").limit(21).toList()).isEmpty());
    }

    @Test
    void shouldRejectNullAttributes() {
        assertFalse(validator.validateValue(TestRecord.class, "attributes", null).isEmpty());
    }

    @Test
    void shouldRejectEmptyAttributes() {
        assertFalse(validator.validateValue(TestRecord.class, "attributes", List.of()).isEmpty());
    }

    @Test
    void shouldRejectAttributeWithSpecialChars() {
        assertFalse(validator.validateValue(TestRecord.class, "attributes", List.of("Motor; DROP TABLE")).isEmpty());
    }

    @Test
    void shouldRejectAttributeWithXss() {
        assertFalse(validator.validateValue(TestRecord.class, "attributes", List.of("<script>alert('xss')</script>")).isEmpty());
    }

    @Test
    void shouldRejectAttributeExceedingMaxLength() {
        assertFalse(validator.validateValue(TestRecord.class, "attributes", List.of("a".repeat(61))).isEmpty());
    }
}
