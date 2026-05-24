package br.com.fiap.fordchallengebackend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidVehicleTypeValidator.class)
public @interface ValidVehicleType {
    String message() default "Tipo de veiculo invalido: deve ser 'cars', 'trucks' ou 'motorcycles'";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
