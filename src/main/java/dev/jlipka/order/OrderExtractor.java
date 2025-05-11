package dev.jlipka.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class OrderExtractor {

    private final ObjectMapper objectMapper;
    private final OrderValidator orderValidator;

    public OrderExtractor(OrderValidator orderValidator) {
        this.orderValidator = orderValidator;
        this.objectMapper = new ObjectMapper();
    }

    public List<Order> getOrders(String ordersPath) throws FileNotFoundException {
        try {
            Order[] orders = objectMapper.readValue(new File(ordersPath), Order[].class);
            List<Order> validatedOrders = new ArrayList<>();

            for (Order order : orders) {
                if (orderValidator.validate(order)) {
                    validatedOrders.add(order);
                }
            }

            return validatedOrders;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new FileNotFoundException(ordersPath);
        }
    }
}
