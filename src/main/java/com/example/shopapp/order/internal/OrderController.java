package com.example.shopapp.order.internal;

import com.example.shopapp.order.OrderResult;
import com.example.shopapp.order.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
class OrderController {

    private final OrderService orderService;

    record PlaceOrderRequest(String customerEmail, String productSku, int quantity) {}

    @PostMapping
    ResponseEntity<OrderResult> placeOrder(@RequestBody PlaceOrderRequest request) {
        OrderResult result = orderService.placeOrder(
                request.customerEmail(), request.productSku(), request.quantity());
        if ("FAILED".equals(result.status())) {
            return ResponseEntity.unprocessableEntity().body(result);
        }
        return ResponseEntity.ok(result);
    }
}
