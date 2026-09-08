package com.wownpc;

// Display format styles for rendering NPC max hit next to nametags.
public enum NpcMaxHitFormat {
    BRACKETS_MH("[MH 28]"),
    PARENTHESES_MH("(MH 28)"),
    PREFIX_MH("MH 28"),
    BRACKETS("[28]"),
    PARENTHESES("(28)"),
    SHORT_MH("MH: 28"),
    PLAIN("28");

    private final String name;

    NpcMaxHitFormat(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public String format(int maxHit) {
        return format(maxHit, NpcMaxHitPosition.AFTER);
    }

    // Formats the numeric max hit with appropriate label and spacing based on
    // placement.
    public String format(int maxHit, NpcMaxHitPosition position) {
        boolean before = position == NpcMaxHitPosition.BEFORE;
        switch (this) {
            case PARENTHESES_MH:
                return before ? "(MH " + maxHit + ") " : " (MH " + maxHit + ")";
            case PREFIX_MH:
                return before ? "MH " + maxHit + " " : " MH " + maxHit;
            case BRACKETS:
                return before ? "[" + maxHit + "] " : " [" + maxHit + "]";
            case PARENTHESES:
                return before ? "(" + maxHit + ") " : " (" + maxHit + ")";
            case SHORT_MH:
                return before ? "MH: " + maxHit + " " : " MH: " + maxHit;
            case PLAIN:
                return before ? maxHit + " " : " " + maxHit;
            case BRACKETS_MH:
            default:
                return before ? "[MH " + maxHit + "] " : " [MH " + maxHit + "]";
        }
    }
}
