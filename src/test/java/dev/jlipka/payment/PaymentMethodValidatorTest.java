package dev.jlipka.payment;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentMethodValidatorTest {

    private final PaymentMethodValidator validator = new PaymentMethodValidator();

    @Test
    void shouldContainThreeConstraintValidationLogs() {
        // given
        PaymentMethod invalidPaymentMethod = new PaymentMethod();
        invalidPaymentMethod.setId("");
        invalidPaymentMethod.setDiscount(0);
        invalidPaymentMethod.setLimit(0.0);

        try {
            // when
            validator.validate(invalidPaymentMethod);
            fail();
        } catch (ConstraintViolationException e) {
            // then
            assertEquals("Promotion is not valid", e.getMessage());
            assertEquals(3, e.getConstraintViolations().size());
            
            assertTrue(e.getConstraintViolations().stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("id")));
            assertTrue(e.getConstraintViolations().stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("discount")));
            assertTrue(e.getConstraintViolations().stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("limit")));
        }
    }
}