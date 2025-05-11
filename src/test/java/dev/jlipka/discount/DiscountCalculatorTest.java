package dev.jlipka.discount;

import dev.jlipka.order.Order;
import dev.jlipka.order.OrderFacade;
import dev.jlipka.payment.PaymentMethod;
import dev.jlipka.payment.PaymentMethodFacade;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class DiscountCalculatorTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @Mock
    private OrderFacade mockOrderFacade;

    @Mock
    private PaymentMethodFacade mockPaymentMethodFacade;

    private DiscountCalculator discountCalculator;

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        outContent.reset();
    }

    @Test
    void shouldReturnAllUsedPaymentMethods() {
        //given
        try (MockedStatic<OrderFacade> orderFacadeMock = mockStatic(OrderFacade.class);
             MockedStatic<PaymentMethodFacade> paymentMethodFacadeMock = mockStatic(PaymentMethodFacade.class)) {

            orderFacadeMock.when(OrderFacade::createDefault).thenReturn(mockOrderFacade);
            paymentMethodFacadeMock.when(PaymentMethodFacade::createDefault).thenReturn(mockPaymentMethodFacade);

            List<Order> orders = createTestOrders();
            List<PaymentMethod> paymentMethods = createTestPaymentMethods();

            when(mockOrderFacade.loadOrders(anyString())).thenReturn(orders);
            when(mockPaymentMethodFacade.loadPromotions(anyString())).thenReturn(paymentMethods);

            discountCalculator = new DiscountCalculator("", "");
            //when
            Map<String, Double> result = discountCalculator.findBestSolution();

            //then
            assertNotNull(result);
            assertTrue(result.containsKey("mBank"));
            assertTrue(result.containsKey("Santander"));
            assertTrue(result.containsKey("PUNKTY"));
        }
    }

    @Test
    void shouldPrintOutUsageMessageWhenPassedInvalidNumberOfArguments() {
        //given && when
        DiscountCalculator.main(new String[]{});
        //then
        assertTrue(outContent.toString().contains("Usage: java -jar app.jar <orders_path> <payment_methods_path>"));
    }

    @Test
    void shouldReturnMapWithAllValuesSetToZero() {
        //given
        try (MockedStatic<OrderFacade> orderFacadeMock = mockStatic(OrderFacade.class);
             MockedStatic<PaymentMethodFacade> paymentMethodFacadeMock = mockStatic(PaymentMethodFacade.class)) {

            orderFacadeMock.when(OrderFacade::createDefault).thenReturn(mockOrderFacade);
            paymentMethodFacadeMock.when(PaymentMethodFacade::createDefault).thenReturn(mockPaymentMethodFacade);

            List<Order> emptyOrders = new ArrayList<>();

            List<PaymentMethod> paymentMethods = createTestPaymentMethods();

            when(mockOrderFacade.loadOrders(anyString())).thenReturn(emptyOrders);
            when(mockPaymentMethodFacade.loadPromotions(anyString())).thenReturn(paymentMethods);
            discountCalculator = new DiscountCalculator("", "");

            //when
            Map<String, Double> result = discountCalculator.findBestSolution();

            //then
            assertNotNull(result);
            assertEquals(result.size(), paymentMethods.size());
            assertTrue(result.values().stream().allMatch(aDouble -> aDouble == 0.0));
        }
    }

    @Test
    void shouldReturnEmptyResultMap() {
        try (MockedStatic<OrderFacade> orderFacadeMock = mockStatic(OrderFacade.class);
             MockedStatic<PaymentMethodFacade> paymentMethodFacadeMock = mockStatic(PaymentMethodFacade.class)) {

            orderFacadeMock.when(OrderFacade::createDefault).thenReturn(mockOrderFacade);
            paymentMethodFacadeMock.when(PaymentMethodFacade::createDefault).thenReturn(mockPaymentMethodFacade);

            List<Order> orders = createTestOrders();

            List<PaymentMethod> emptyPaymentMethods = new ArrayList<>();

            when(mockOrderFacade.loadOrders(anyString())).thenReturn(orders);
            when(mockPaymentMethodFacade.loadPromotions(anyString())).thenReturn(emptyPaymentMethods);

            //when

            discountCalculator = new DiscountCalculator("", "");

            Map<String, Double> result = discountCalculator.findBestSolution();

            //then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Test
    void shouldReturnLessMoneyUsedWhenDiscountsExists() {
        //given
        try (MockedStatic<OrderFacade> orderFacadeMock = mockStatic(OrderFacade.class);
             MockedStatic<PaymentMethodFacade> paymentMethodFacadeMock = mockStatic(PaymentMethodFacade.class)) {

            orderFacadeMock.when(OrderFacade::createDefault).thenReturn(mockOrderFacade);
            paymentMethodFacadeMock.when(PaymentMethodFacade::createDefault).thenReturn(mockPaymentMethodFacade);

            List<Order> testOrders = createTestOrders();
            List<PaymentMethod> testPaymentMethods = createTestPaymentMethods();

            when(mockOrderFacade.loadOrders(anyString())).thenReturn(testOrders);
            when(mockPaymentMethodFacade.loadPromotions(anyString())).thenReturn(testPaymentMethods);

            discountCalculator = new DiscountCalculator("", "");

            Optional<Double> moneyToPayWithoutDiscounts = testOrders.stream()
                    .map(Order::getValue)
                    .reduce(Double::sum);

            //when

            Map<String, Double> bestSolution = discountCalculator.findBestSolution();
            Optional<Double> moneyToPayWithDiscounts = bestSolution.values().stream().reduce(Double::sum);

            //then

            assertTrue(moneyToPayWithDiscounts.isPresent());
            assertTrue(moneyToPayWithoutDiscounts.isPresent());
            assertTrue(moneyToPayWithDiscounts.get() < moneyToPayWithoutDiscounts.get());
        }
    }

    private List<Order> createTestOrders() {
        List<Order> orders = new ArrayList<>();

        Order order1 = new Order();
        order1.setId("ORDER1");
        order1.setValue(100.0);
        List<String> promotions1 = new ArrayList<>();
        promotions1.add("mBank");
        order1.setPromotions(promotions1);
        orders.add(order1);

        Order order2 = new Order();
        order2.setId("ORDER2");
        order2.setValue(200.0);
        List<String> promotions2 = new ArrayList<>();
        promotions2.add("Santander");
        order2.setPromotions(promotions2);
        orders.add(order2);

        return orders;
    }

    private List<PaymentMethod> createTestPaymentMethods() {
        List<PaymentMethod> paymentMethods = new ArrayList<>();

        PaymentMethod visa = new PaymentMethod();
        visa.setId("mBank");
        visa.setDiscount(10);
        visa.setLimit(1000.0);
        paymentMethods.add(visa);

        PaymentMethod mastercard = new PaymentMethod();
        mastercard.setId("Santander");
        mastercard.setDiscount(5);
        mastercard.setLimit(2000.0);
        paymentMethods.add(mastercard);

        PaymentMethod points = new PaymentMethod();
        points.setId("PUNKTY");
        points.setDiscount(100);
        points.setLimit(50.0);
        paymentMethods.add(points);

        return paymentMethods;
    }
}