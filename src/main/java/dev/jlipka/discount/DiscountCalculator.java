package dev.jlipka.discount;

import dev.jlipka.order.Order;
import dev.jlipka.order.OrderFacade;
import dev.jlipka.payment.PaymentMethod;
import dev.jlipka.payment.PaymentMethodFacade;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DiscountCalculator {

    private final static Logger LOGGER = Logger.getLogger(DiscountCalculator.class.getName());

    private final String ordersPath;

    private final String paymentMethodsPath;

    private final OrderFacade orderFacade;

    private final PaymentMethodFacade paymentMethodFacade;

    private Map<String, PaymentMethod> paymentMethods;

    private Map<String, Double> paymentMethodsUsage;

    public DiscountCalculator(String ordersPath, String paymentMethodsPath) {
        this.ordersPath = ordersPath;
        this.paymentMethodsPath = paymentMethodsPath;
        this.orderFacade = OrderFacade.createDefault();
        this.paymentMethodFacade = PaymentMethodFacade.createDefault();
        Logger.getLogger("org.hibernate.validator")
                .setLevel(Level.WARNING);
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java -jar app.jar <orders_path> <payment_methods_path>");
            return;
        }

        DiscountCalculator discountCalculator = new DiscountCalculator(args[0], args[1]);
        Map<String, Double> result1 = discountCalculator.calculate(true);
        Map<String, Double> result2 = discountCalculator.calculate(false);

        Optional<Double> reduce1 = result1.values()
                .stream()
                .reduce(Double::sum);

        Optional<Double> reduce2 = result2.values()
                .stream()
                .reduce(Double::sum);

        Set<Map.Entry<String, Double>> betterSolution;
        if (reduce1.get() < reduce2.get()) {
            betterSolution = result1.entrySet();
        } else {
            betterSolution = result2.entrySet();
        }

        for (Map.Entry<String, Double> entry : betterSolution) {
            if (entry.getValue() > 0) {
                System.out.println(entry.getKey() + " " + String.format("%.2f", entry.getValue()));
            }
        }
    }

    public Map<String, Double> calculate(boolean preferCardsOverPartialPoints) {
        paymentMethodsUsage = new HashMap<>();
        List<Order> orders = orderFacade.loadOrders(ordersPath);
        orders.sort(Comparator.comparing(Order::getValue, Comparator.reverseOrder()));
        this.paymentMethods = new HashMap<>();
        for (PaymentMethod paymentMethod : paymentMethodFacade.loadPromotions(paymentMethodsPath)) {
            paymentMethods.put(paymentMethod.getId(), paymentMethod);
            paymentMethodsUsage.put(paymentMethod.getId(), 0.0);
        }

        for (Order order : orders) {
            processOrderOptimally(order, preferCardsOverPartialPoints);
        }

        return paymentMethodsUsage;
    }

    private void processOrderOptimally(Order order, boolean preferCardsOverPartialPoints) {
        List<DiscountOption> cardPaymentMethods = findCardPaymentMethods(order);
        Optional<DiscountOption> fullPointsPaymentOption = getFullPointsPaymentOption(order);
        Optional<DiscountOption> partialPointsPaymentOption = getPartialPointsPaymentOption(order);

        List<DiscountOption> discountOptions = new ArrayList<>(cardPaymentMethods);
        fullPointsPaymentOption.ifPresent(discountOptions::add);
        partialPointsPaymentOption.ifPresent(discountOptions::add);

        if (discountOptions.isEmpty()) {
            handleNoDiscountPayment(order);
        } else {
            discountOptions.sort(Comparator.comparing(DiscountOption::discountAmount)
                    .reversed());
            DiscountOption bestOption = discountOptions.getFirst();
            if (bestOption.type() == DiscountType.FULL_CARD) {
                Optional<DiscountOption> fullPointsOption = discountOptions.stream()
                        .filter(option -> option.type() == DiscountType.FULL_POINTS)
                        .findFirst();

                if (fullPointsOption.isPresent() && fullPointsOption.get()
                        .discountAmount() == bestOption.discountAmount()) {
                    bestOption = fullPointsOption.get();
                }
            }

            if (preferCardsOverPartialPoints && bestOption.type() == DiscountType.PARTIAL_POINTS) {
                Optional<DiscountOption> bestCard = discountOptions.stream()
                        .filter(discountOption -> discountOption.type() == DiscountType.FULL_CARD)
                        .max(Comparator.comparing(DiscountOption::discountAmount));
                if (bestCard.isPresent()) {
                    bestOption = bestCard.get();
                }
            }

            applyPaymentOption(bestOption);
        }
    }


    private List<DiscountOption> findCardPaymentMethods(Order order) {
        List<DiscountOption> cardDiscountOptions = new ArrayList<>();
        if (order.getPromotions() != null && !order.getPromotions()
                .isEmpty()) {
            for (String methodId : order.getPromotions()) {
                PaymentMethod method = paymentMethods.get(methodId);
                if (hasEnoughLimit(method, order.getValue())) {
                    double discountAmount = order.getValue() * method.getDiscount() * 0.01;
                    double amountAfterDiscount = order.getValue() - discountAmount;
                    cardDiscountOptions.add(new DiscountOption(DiscountType.FULL_CARD, methodId, amountAfterDiscount, discountAmount, 0.0, null));
                }
            }
        }
        return cardDiscountOptions;
    }

    private Optional<DiscountOption> getFullPointsPaymentOption(Order order) {
        PaymentMethod pointsMethod = paymentMethods.get("PUNKTY");
        if (hasEnoughLimit(pointsMethod, order.getValue())) {
            double discountAmount = order.getValue() * pointsMethod.getDiscount() * 0.01;
            double amountAfterDiscount = order.getValue() - discountAmount;
            return Optional.of(new DiscountOption(DiscountType.FULL_POINTS, "PUNKTY", amountAfterDiscount, discountAmount, amountAfterDiscount, null));
        }
        return Optional.empty();
    }

    private Optional<DiscountOption> getPartialPointsPaymentOption(Order order) {
        PaymentMethod pointsMethod = paymentMethods.get("PUNKTY");
        double minimumPointsNeeded = order.getValue() * 0.1;
        if (pointsMethod.getLimit() >= minimumPointsNeeded) {
            double pointsToUse = Math.min(pointsMethod.getLimit(), order.getValue() * 0.9);
            double discountAmount = order.getValue() * 0.1;
            double totalAmountAfterDiscount = order.getValue() - discountAmount;
            double remainingForCard = totalAmountAfterDiscount - pointsToUse;

            for (PaymentMethod cardMethod : paymentMethods.values()) {
                if (!cardMethod.getId()
                        .equals("PUNKTY") && hasEnoughLimit(cardMethod, remainingForCard)) {
                    return Optional.of(new DiscountOption(DiscountType.PARTIAL_POINTS, cardMethod.getId(), totalAmountAfterDiscount, discountAmount, pointsToUse, cardMethod));
                }
            }
        }
        return Optional.empty();
    }

    private void applyPaymentOption(DiscountOption option) {
        switch (option.type()) {
            case FULL_CARD -> updatePaymentMethodLimit(option.primaryMethodId(), option.amountAfterDiscount());
            case FULL_POINTS -> updatePaymentMethodLimit("PUNKTY", option.amountAfterDiscount());
            case PARTIAL_POINTS -> {
                updatePaymentMethodLimit("PUNKTY", option.pointsAmount());
                updatePaymentMethodLimit(option.primaryMethodId(), option.amountAfterDiscount() - option.pointsAmount());
            }
        }
    }

    private void handleNoDiscountPayment(Order order) {
        double orderValue = order.getValue();

        for (PaymentMethod method : paymentMethods.values()) {
            if (!method.getId()
                    .equals("PUNKTY") && hasEnoughLimit(method, orderValue)) {
                updatePaymentMethodLimit(method.getId(), orderValue);
                return;
            }
        }

        for (PaymentMethod method : paymentMethods.values()) {
            if (!method.getId()
                    .equals("PUNKTY") && method.getLimit() > 0) {
                double amountToPay = Math.min(method.getLimit(), orderValue);
                updatePaymentMethodLimit(method.getId(), amountToPay);
                orderValue -= amountToPay;

                if (orderValue <= 0) {
                    break;
                }
            }
        }

        if (orderValue > 0) {
            LOGGER.warning("Nie można opłacić zamówienia: " + order.getId() + ", brakuje: " + orderValue);
        }
    }

    private boolean hasEnoughLimit(PaymentMethod method, double amount) {
        return method.getLimit() >= amount;
    }

    private void updatePaymentMethodLimit(String methodId, double amount) {
        PaymentMethod paymentMethod = paymentMethods.get(methodId);
        boolean updateResult = paymentMethod.updateLimit(amount);

        if (updateResult) {
            paymentMethodsUsage.put(methodId, paymentMethodsUsage.get(methodId) + amount);
        } else {
            LOGGER.warning("Tried to update limit by too much for method: " + methodId);
        }
    }
}