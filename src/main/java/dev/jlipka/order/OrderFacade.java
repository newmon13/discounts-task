package dev.jlipka.order;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.logging.Logger;

public class OrderFacade {

    private static final Logger LOGGER = Logger.getLogger(OrderFacade.class.getName());

    private final OrderExtractor orderExtractor;

    private final OrderService orderService;

    OrderFacade(OrderExtractor orderExtractor, OrderService orderService) {
        this.orderExtractor = orderExtractor;
        this.orderService = orderService;
    }

    public static OrderFacade createDefault() {
        OrderValidator orderValidator = new OrderValidator();
        return new OrderFacade(new OrderExtractor(orderValidator), new OrderService());
    }

    public List<Order> loadOrders(String path) {
        try {
            List<Order> orders = orderExtractor.getOrders(path);
            addOrders(orders);
            return orders;
        } catch (FileNotFoundException e) {
            LOGGER.warning("Could not find file: " + path);
        }
        return List.of();
    }

    private void addOrders(List<Order> orders) {
        for (Order order: orders) {
            orderService.addOrder(order);
        }
    }
}
