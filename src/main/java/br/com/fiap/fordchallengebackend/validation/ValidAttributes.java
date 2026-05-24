package br.com.fiap.fordchallengebackend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidAttributesValidator.class)
public @interface ValidAttributes {
    String message() default "Atributos invalidos: max 20 items, cada um com 1 a 60 caracteres alfanumericos";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
