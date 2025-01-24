package com.inventory.enums;

public enum PolyCarbonateType {
    SINGLE("Single"),
    DOUBLE("Double"),
    FULL_SHEET("Full sheet");

    private final String displayName;

    PolyCarbonateType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}