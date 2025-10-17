package com.inventory.enums;

import lombok.Getter;

@Getter
public enum QuotationStatusItem {
    O("Open"),
    I("In progress"),
    C("Completed"),
    B("Billed");

    public final String text;
    public final String value;

    QuotationStatusItem(String text) {
        this.value = this.name();
        this.text = text;
    }

    public static String getEnumByString(String code) {
        for(QuotationStatusItem e : QuotationStatusItem.values()) {
            if(e.value.equals(code)) return e.getText();
        }
        return null;
    }
}
