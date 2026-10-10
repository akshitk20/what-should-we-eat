package com.fooddecider.app.enums;

public enum PriceLevel {

    BUDGET(1),
    MODERATE(2),
    EXPENSIVE(3),
    LUXURY(4);

    private final int value;

    PriceLevel(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
