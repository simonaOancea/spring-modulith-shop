package com.example.shopapp.order;

import com.example.shopapp.order.internal.OrderProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderProcessor processor;

    public OrderResult placeOrder(String customerEmail, String productSku, int quantity) {
        try {
            return processor.execute(customerEmail, productSku, quantity);
        } catch (Exception e) {
            return processor.fail(customerEmail, productSku, quantity, e.getMessage());
        }
    }
}
