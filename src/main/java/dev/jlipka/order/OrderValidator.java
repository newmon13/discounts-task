package dev.jlipka.order;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

class OrderValidator {

    private final static Logger LOGGER = Logger.getLogger(OrderValidator.class.getName());

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
            logViolations(constraintViolations);
            return false;
        }
    }

    private void logViolations(Set<ConstraintViolation<Order>> violations) {
        for (ConstraintViolation<Order> violation : violations) {
            Map.Entry<Path, Object> entry = Map.entry(violation.getPropertyPath(), violation.getInvalidValue());
            LOGGER.warning(entry + " " + violation.getMessage());
        }
    }
}
