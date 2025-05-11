package dev.jlipka.order;

import java.util.ArrayList;
import java.util.List;

class OrderService {

    private final List<Order> orders;

    public OrderService() {
        this.orders = new ArrayList<>();
    }

    public List<Order> getOrders() {
        return orders;
    }

    public void addOrder(Order order) {
        orders.add(order);
    }
}
