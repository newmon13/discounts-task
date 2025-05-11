package dev.jlipka.payment;

import java.util.HashMap;
import java.util.Map;

class PaymentMethodService {
    private final Map<String, PaymentMethod> promotions;

    public PaymentMethodService() {
        promotions = new HashMap<>();
    }

    public void addPromotion(PaymentMethod paymentMethod) {
        promotions.put(paymentMethod.getId(), paymentMethod);
    }
}
