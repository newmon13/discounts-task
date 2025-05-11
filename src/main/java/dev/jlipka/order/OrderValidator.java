package dev.jlipka.order;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

import java.util.Set;
import java.util.logging.Logger;

class OrderValidator {

    private final Validator validator;

    public OrderValidator() {
        Logger.getLogger("org.hibernate.validator")
                .setLevel(java.util.logging.Level.WARNING);
        ValidatorFactory factory = Validation.byDefaultProvider()
                .configure()
                .messageInterpolator(new ParameterMessageInterpolator())
                .buildValidatorFactory();
        this.validator = factory.getValidator();
    }

    public boolean validate(Order order) {
        Set<ConstraintViolation<Order>> constraintViolations = validator.validate(order);
        if (constraintViolations.isEmpty()) {
            return true;
        } else {
            throw new ConstraintViolationException("Order is not valid", constraintViolations);
        }
    }
}
