package br.com.fiap.fordchallengebackend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = VersionNameValidator.class)
public @interface VersionName {
    String message() default "Versao invalida: deve conter apenas letras, numeros, espacos, hifen ou apice (max 100 caracteres)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
