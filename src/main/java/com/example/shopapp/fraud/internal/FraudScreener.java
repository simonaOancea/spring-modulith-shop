package com.example.shopapp.fraud.internal;

import com.example.shopapp.order.events.OrderInitiated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
class FraudScreener {

    private final FraudCheckRepository checks;

    @ApplicationModuleListener
    void on(OrderInitiated event) {
        FraudCheck.RiskLevel riskLevel = assessRisk(event.totalAmount());

        FraudCheck check = new FraudCheck(event.orderId(), riskLevel);
        checks.save(check);

        log.info("Screening order #{}, amount {} — {}",
                event.orderId(), event.totalAmount(), riskLevel);
    }

    private FraudCheck.RiskLevel assessRisk(BigDecimal amount) {
        if (amount.compareTo(new BigDecimal("1000")) > 0) {
            return FraudCheck.RiskLevel.HIGH;
        } else if (amount.compareTo(new BigDecimal("500")) > 0) {
            return FraudCheck.RiskLevel.MEDIUM;
        }
        return FraudCheck.RiskLevel.LOW;
    }
}
