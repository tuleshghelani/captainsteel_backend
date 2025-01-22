package com.inventory.enums;

import lombok.Getter;

@Getter
public enum ProductType {
    NOS("Nos"),
    REGULAR("Regular"),
    POLY_CARBONATE("Poly Carbonate");

    private final String text;
    private final String value;

    ProductType(String text) {
        this.value = this.name();
        this.text = text;
    }

    public static String getEnumByString(String code) {
        for(ProductType e : ProductType.values()) {
            if(e.value.equals(code)) return e.getText();
        }
        return null;
    }
} 