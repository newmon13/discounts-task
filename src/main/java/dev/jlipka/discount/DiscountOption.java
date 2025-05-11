package dev.jlipka.discount;

import dev.jlipka.payment.PaymentMethod;

record DiscountOption(
    DiscountType type,
    String primaryMethodId,
    double amountAfterDiscount,
    double discountAmount,
    double pointsAmount,
    PaymentMethod secondaryMethod
) {}