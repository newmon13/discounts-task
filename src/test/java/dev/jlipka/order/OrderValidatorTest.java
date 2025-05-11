package dev.jlipka.order;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class OrderValidatorTest {
    private final OrderValidator validator = new OrderValidator();

    @Test
    void shouldContainThreeConstraintValidationLogs() {
        // given
        Order invalidOrder = new Order();
        invalidOrder.setId("");
        invalidOrder.setValue(-10);
        invalidOrder.setPromotions(List.of(""));

        try {
            // when
            validator.validate(invalidOrder);
            fail();
        } catch (ConstraintViolationException e) {
            // then
            assertEquals("Order is not valid", e.getMessage());
            assertEquals(3, e.getConstraintViolations().size());

            assertTrue(e.getConstraintViolations().stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("id")));
            assertTrue(e.getConstraintViolations().stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("value")));
        }
    }
}
