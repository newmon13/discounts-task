package dev.jlipka.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class PaymentMethodExtractor {

    private final ObjectMapper objectMapper;

    private final PaymentMethodValidator paymentMethodValidator;

    public PaymentMethodExtractor(PaymentMethodValidator paymentMethodValidator) {
        this.paymentMethodValidator = paymentMethodValidator;
        this.objectMapper = new ObjectMapper();
    }

    public List<PaymentMethod> getPromotions(String promotionsPath) throws FileNotFoundException {
        try {
            PaymentMethod[] paymentMethods = objectMapper.readValue(new File(promotionsPath), PaymentMethod[].class);
            List<PaymentMethod> validatedPaymentMethods = new ArrayList<>();

            for (PaymentMethod paymentMethod : paymentMethods) {
                if (paymentMethodValidator.validate(paymentMethod)) {
                    validatedPaymentMethods.add(paymentMethod);
                }
            }

            return validatedPaymentMethods;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new FileNotFoundException(promotionsPath);
        }
    }
}
