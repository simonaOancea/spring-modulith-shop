package com.example.shopapp.catalog;

import java.math.BigDecimal;

public record ProductInfo(String sku, String name, BigDecimal price, int stock) {
}
