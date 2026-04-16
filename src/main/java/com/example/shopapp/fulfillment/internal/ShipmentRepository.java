package com.example.shopapp.fulfillment.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    List<Shipment> findByOrderId(Long orderId);

    List<Shipment> findByStatus(Shipment.ShipmentStatus status);
}
