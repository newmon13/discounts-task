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
import java.util.logging.Level;
import java.util.logging.Logger;

public class DiscountCalculator {

    private final static Logger LOGGER = Logger.getLogger(DiscountCalculator.class.getName());

    private final String ordersPath;

    private final String paymentMethodsPath;

    private final OrderFacade orderFacade;

    private final PaymentMethodFacade paymentMethodFacade;

    private final Map<String, PaymentMethod> paymentMethods;

    private final Map<String, Double> paymentMethodsUsage;

    private boolean preferPartialPointsOverCards;


    public DiscountCalculator(String ordersPath, String paymentMethodsPath) {
        this.ordersPath = ordersPath;
        this.paymentMethodsPath = paymentMethodsPath;
        this.orderFacade = OrderFacade.createDefault();
        this.paymentMethodFacade = PaymentMethodFacade.createDefault();
        this.paymentMethodsUsage = new HashMap<>();
        this.paymentMethods = new HashMap<>();
        Logger.getLogger("org.hibernate.validator")
                .setLevel(Level.WARNING);
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java -jar app.jar <orders_path> <payment_methods_path>");
            return;
        }

        DiscountCalculator discountCalculator = new DiscountCalculator(args[0], args[1]);
        Map<String, Double> bestSolution = discountCalculator.findBestSolution();

        for (Map.Entry<String, Double> entry : bestSolution.entrySet()) {
            if (entry.getValue() > 0) {
                System.out.println(entry.getKey() + " " + String.format("%.2f", entry.getValue()));
            }
        }
    }

    public Map<String, Double> findBestSolution() {

        this.preferPartialPointsOverCards = false;
        Map<String, Double> result1 = calculate();

        this.preferPartialPointsOverCards = true;
        Map<String, Double> result2 = calculate();

        Optional<Double> sum1 = result1.values()
                .stream()
                .reduce(Double::sum);

        Optional<Double> sum2 = result2.values()
                .stream()
                .reduce(Double::sum);

        if (sum1.isPresent() && sum2.isPresent()) {
            return sum1.get() < sum2.get() ? result1 : result2;
        }

        return result1;
    }

    private Map<String, Double> calculate() {
        List<Order> orders = orderFacade.loadOrders(ordersPath);
        orders.sort(Comparator.comparing(Order::getValue, Comparator.reverseOrder()));

        for (PaymentMethod paymentMethod : paymentMethodFacade.loadPromotions(paymentMethodsPath)) {
            paymentMethods.put(paymentMethod.getId(), paymentMethod);
            paymentMethodsUsage.put(paymentMethod.getId(), 0.0);
        }

        if (!paymentMethods.isEmpty()) {
            for (Order order : orders) {
                processOrderOptimally(order);
            }
        }

        return paymentMethodsUsage;
    }

    private void processOrderOptimally(Order order) {
        List<DiscountOption> discountOptions = collectDiscountOptions(order);

        if (discountOptions.isEmpty()) {
            handleNoDiscountPayment(order);
        } else {
            DiscountOption bestOption = findBestDiscountOption(discountOptions);
            applyPaymentOption(bestOption);
        }
    }

    private List<DiscountOption> collectDiscountOptions(Order order) {
        List<DiscountOption> options = new ArrayList<>(findCardPaymentMethods(order));

        getFullPointsPaymentOption(order).ifPresent(options::add);
        getPartialPointsPaymentOption(order).ifPresent(options::add);

        return options;
    }

    private DiscountOption findBestDiscountOption(List<DiscountOption> discountOptions) {
        discountOptions.sort(Comparator.comparing(DiscountOption::discountAmount)
                .reversed());

        DiscountOption bestOption = discountOptions.getFirst();
        bestOption = checkAndPreferFullPointsOption(discountOptions, bestOption);
        bestOption = applyPartialPointsStrategy(discountOptions, bestOption);

        return bestOption;
    }

    private DiscountOption checkAndPreferFullPointsOption(List<DiscountOption> options, DiscountOption currentBest) {
        if (currentBest.type() == DiscountType.FULL_CARD) {
            Optional<DiscountOption> fullPointsOption = options.stream()
                    .filter(option -> option.type() == DiscountType.FULL_POINTS)
                    .findFirst();

            if (fullPointsOption.isPresent() && fullPointsOption.get()
                    .discountAmount() == currentBest.discountAmount()) {
                return fullPointsOption.get();
            }
        }
        return currentBest;
    }

    private DiscountOption applyPartialPointsStrategy(List<DiscountOption> options, DiscountOption currentBest) {
        if (!preferPartialPointsOverCards && currentBest.type() == DiscountType.PARTIAL_POINTS) {
            Optional<DiscountOption> bestCard = options.stream()
                    .filter(option -> option.type() == DiscountType.FULL_CARD)
                    .max(Comparator.comparing(DiscountOption::discountAmount));

            if (bestCard.isPresent()) {
                return bestCard.get();
            }
        }
        return currentBest;
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
            }
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