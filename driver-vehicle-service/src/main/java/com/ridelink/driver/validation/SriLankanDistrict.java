package com.ridelink.driver.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = SriLankanDistrictValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface SriLankanDistrict {
    String message() default "Service area must be one of the districts of Sri Lanka";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}