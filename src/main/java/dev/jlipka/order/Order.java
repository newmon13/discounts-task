package dev.jlipka.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

public class Order {

    @NotBlank
    private String id;

    @PositiveOrZero
    private double value;

    private List<@NotBlank String> promotions;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public List<String> getPromotions() {
        return promotions;
    }

    public void setPromotions(List<String> promotions) {
        this.promotions = promotions;
    }

    @Override
    public String toString() {
        return "Order{" + "id='" + id + '\'' + ", value=" + value + ", promotions=" + promotions + '}';
    }


}
