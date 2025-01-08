package com.inventory.enums;

import lombok.Data;
import lombok.Getter;

@Getter
public enum QuotationStatus {
    Q("Quote"),
    A("Accepted"),
    D("Declined"),
    R("Ready"),
    P("Processing"),
    C("Completed");

    public final String text;
    public final String value;

    QuotationStatus(String text) {
        this.value = this.name();
        this.text = text;
    }

    public static String getEnumByString(String code) {
        for(QuotationStatus e : QuotationStatus.values()) {
            if(e.value.equals(code)) return e.getText();
        }
        return null;
    }
} 