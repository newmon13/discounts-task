package dev.jlipka.payment;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.logging.Logger;

public class PaymentMethodFacade {

    private static final Logger LOGGER = Logger.getLogger(PaymentMethodFacade.class.getName());

    private final PaymentMethodExtractor paymentMethodExtractor;

    private final PaymentMethodService paymentMethodService;

    PaymentMethodFacade(PaymentMethodExtractor paymentMethodExtractor, PaymentMethodService paymentMethodService) {
        this.paymentMethodExtractor = paymentMethodExtractor;
        this.paymentMethodService = paymentMethodService;
    }

    public static PaymentMethodFacade createDefault() {
        PaymentMethodValidator paymentMethodValidator = new PaymentMethodValidator();
        return new PaymentMethodFacade(new PaymentMethodExtractor(paymentMethodValidator), new PaymentMethodService());
    }

    public List<PaymentMethod> loadPromotions(String path) {
        try {
            List<PaymentMethod> paymentMethods = paymentMethodExtractor.getPromotions(path);
            addPromotions(paymentMethods);
            return paymentMethods;
        } catch (FileNotFoundException e) {
            LOGGER.warning("Could not find file: " + path);
        }
        return List.of();
    }

    public void addPromotions(List<PaymentMethod> paymentMethods) {
        for (PaymentMethod paymentMethod : paymentMethods) {
            paymentMethodService.addPromotion(paymentMethod);
        }
    }
}
