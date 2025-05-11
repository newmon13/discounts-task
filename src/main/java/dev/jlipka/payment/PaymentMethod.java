package dev.jlipka.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class PaymentMethod {

    @NotBlank
    private String id;

    @Positive
    private int discount;

    @Positive
    private double limit;

    public String getId() {
        return id;
    }

    public int getDiscount() {
        return discount;
    }

    public double getLimit() {
        return limit;
    }

    void setId(String id) {
        this.id = id;
    }

    void setDiscount(int discount) {
        this.discount = discount;
    }

    void setLimit(double limit) {
        this.limit = limit;
    }

    @Override
    public String toString() {
        return "Promotion{" +
                "id='" + id + '\'' +
                ", discount=" + discount +
                ", limit=" + limit +
                '}';
    }

    public boolean updateLimit(double amount) {
        if (this.limit - amount >= 0) {
            this.limit -= amount;
            return true;
        }
        return false;
    }
}
