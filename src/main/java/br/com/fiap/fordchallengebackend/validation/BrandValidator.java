package br.com.fiap.fordchallengebackend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class BrandValidator implements ConstraintValidator<Brand, String> {

    private static final java.util.regex.Pattern PATTERN =
        java.util.regex.Pattern.compile("^[\\p{L}0-9\\s\\-'.]{1,50}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return PATTERN.matcher(value.strip()).matches();
    }
}
