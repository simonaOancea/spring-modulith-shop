package com.example.shopapp.fraud.internal;

import org.springframework.data.jpa.repository.JpaRepository;

interface FraudCheckRepository extends JpaRepository<FraudCheck, Long> {
}
