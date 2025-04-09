package com.inventory.enums;

import lombok.Getter;

@Getter
public enum ProductMainType {
    NOS("Nos"),
    REGULAR("Regular"),
    POLY_CARBONATE("Poly Carbonate");

    private final String text;
    private final String value;

    ProductMainType(String text) {
        this.value = this.name();
        this.text = text;
    }

    public static String getEnumByString(String code) {
        for(ProductMainType e : ProductMainType.values()) {
            if(e.value.equals(code)) return e.getText();
        }
        return null;
    }
} 