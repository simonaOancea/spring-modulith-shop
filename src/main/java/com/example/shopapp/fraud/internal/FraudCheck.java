package com.example.shopapp.fraud.internal;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "fraud_checks", schema = "fraud")
@Getter
@NoArgsConstructor
public class FraudCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;

    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    private Instant checkedAt;

    public FraudCheck(Long orderId, RiskLevel riskLevel) {
        this.orderId = orderId;
        this.riskLevel = riskLevel;
        this.checkedAt = Instant.now();
    }

    public enum RiskLevel {
        LOW, MEDIUM, HIGH
    }
}
