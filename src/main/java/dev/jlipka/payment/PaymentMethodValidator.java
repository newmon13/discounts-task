package dev.jlipka.payment;


import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

import java.util.Set;

class PaymentMethodValidator {

    private final Validator validator;

    public PaymentMethodValidator() {
        ValidatorFactory factory = Validation.byDefaultProvider()
                .configure()
                .messageInterpolator(new ParameterMessageInterpolator())
                .buildValidatorFactory();
        this.validator = factory.getValidator();
    }

    public boolean validate(PaymentMethod paymentMethod) {
        Set<ConstraintViolation<PaymentMethod>> constraintViolations = validator.validate(paymentMethod);
        if (constraintViolations.isEmpty()) {
            return true;
        } else {
            throw new ConstraintViolationException("Promotion is not valid", constraintViolations);
        }
    }
}
