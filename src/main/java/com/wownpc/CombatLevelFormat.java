package com.wownpc;

// Display format styles for rendering combat levels next to player or NPC names.
public enum CombatLevelFormat {
    PARENTHESES("(126)"),
    BRACKETS("[126]"),
    PARENTHESES_LVL("(lvl 126)"),
    BRACKETS_LVL("[lvl 126]"),
    PREFIX_LVL("lvl 126"),
    PLAIN("126");

    private final String name;

    CombatLevelFormat(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public String format(int level) {
        return format(level, CombatLevelPosition.AFTER);
    }

    // Formats the numeric level with appropriate symbols and spacing based on
    // placement.
    public String format(int level, CombatLevelPosition position) {
        boolean before = position == CombatLevelPosition.BEFORE;
        switch (this) {
            case BRACKETS:
                return before ? "[" + level + "] " : " [" + level + "]";
            case PARENTHESES_LVL:
                return before ? "(lvl " + level + ") " : " (lvl " + level + ")";
            case BRACKETS_LVL:
                return before ? "[lvl " + level + "] " : " [lvl " + level + "]";
            case PREFIX_LVL:
                return before ? "lvl " + level + " " : " lvl " + level;
            case PLAIN:
                return before ? level + " " : " " + level;
            case PARENTHESES:
            default:
                return before ? "(" + level + ") " : " (" + level + ")";
        }
    }
}
