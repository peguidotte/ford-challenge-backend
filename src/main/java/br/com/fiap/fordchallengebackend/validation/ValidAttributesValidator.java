package br.com.fiap.fordchallengebackend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;

public class ValidAttributesValidator implements ConstraintValidator<ValidAttributes, List<String>> {

    private static final java.util.regex.Pattern ITEM_PATTERN =
        java.util.regex.Pattern.compile("^[\\p{L}0-9\\s\\-'.]{1,60}$");

    @Override
    public boolean isValid(List<String> value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        if (value.size() > 20) {
            return false;
        }
        for (var item : value) {
            if (item == null || item.isBlank()) {
                return false;
            }
            if (!ITEM_PATTERN.matcher(item.strip()).matches()) {
                return false;
            }
        }
        return true;
    }
}
