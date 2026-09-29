package com.ridelink.driver.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SriLankanDistrictValidator implements ConstraintValidator<SriLankanDistrict, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        // null/blank is reported by @NotBlank, so only one message is shown
        return value == null || value.isBlank() || SriLankaDistricts.isValid(value);
    }
}