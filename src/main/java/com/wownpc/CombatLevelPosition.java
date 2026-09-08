package com.wownpc;

// Controls whether the combat level appears before or after the entity's name.
public enum CombatLevelPosition {
    AFTER("After"),
    BEFORE("Before");

    private final String name;

    CombatLevelPosition(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
