package br.com.fiap.fordchallengebackend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Set;

public class ValidVehicleTypeValidator implements ConstraintValidator<ValidVehicleType, String> {

    private static final Set<String> ALLOWED = Set.of("cars", "trucks", "motorcycles");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return ALLOWED.contains(value.strip().toLowerCase());
    }
}
