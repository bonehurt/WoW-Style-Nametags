package com.wownpc;

import java.awt.Color;

/**
 * All data required to render a single nametag, including its resolved
 * screen position (which may be shifted from the natural position when
 * stack-tags mode is active).
 */
class TagEntry {
    final String text;
    final Color colour;
    final String levelText;
    final Color levelColour;
    final boolean levelBefore;
    final boolean outlineEnabled;
    final Color outlineColour;
    final int outlineThickness;
    final int fontSize;
    final Color levelOutlineColour;
    final String maxHitText;
    final Color maxHitColour;
    final boolean maxHitBefore;
    final Color maxHitOutlineColour;
    /** Chebyshev tile distance from the local player — used for culling. */
    final int worldDist;
    /** Screen X of the text baseline (left edge). Immutable. */
    final int screenX;
    /**
     * Screen Y of the text baseline. Mutated by resolveOverlaps() when stacking.
     */
    int screenY;

    TagEntry(String text, Color colour, String levelText, Color levelColour, boolean levelBefore,
            boolean outlineEnabled, Color outlineColour, int outlineThickness, int fontSize,
            Color levelOutlineColour, String maxHitText, Color maxHitColour, boolean maxHitBefore,
            Color maxHitOutlineColour, int worldDist, int screenX, int screenY) {
        this.text = text;
        this.colour = colour;
        this.levelText = levelText;
        this.levelColour = levelColour;
        this.levelBefore = levelBefore;
        this.outlineEnabled = outlineEnabled;
        this.outlineColour = outlineColour;
        this.outlineThickness = outlineThickness;
        this.fontSize = fontSize;
        this.levelOutlineColour = levelOutlineColour;
        this.maxHitText = maxHitText;
        this.maxHitColour = maxHitColour;
        this.maxHitBefore = maxHitBefore;
        this.maxHitOutlineColour = maxHitOutlineColour;
        this.worldDist = worldDist;
        this.screenX = screenX;
        this.screenY = screenY;
    }

    TagEntry(String text, Color colour, String levelText, Color levelColour, boolean levelBefore,
            boolean outlineEnabled, Color outlineColour, int outlineThickness, int fontSize,
            Color levelOutlineColour, int worldDist, int screenX, int screenY) {
        this(text, colour, levelText, levelColour, levelBefore, outlineEnabled, outlineColour,
                outlineThickness, fontSize, levelOutlineColour, null, null, false, null,
                worldDist, screenX, screenY);
    }

    // Combines the entity name, combat level, and max hit in the configured display
    // order.
    String getFullText() {
        if (levelText == null && maxHitText == null) {
            return text;
        }

        if (maxHitText == null) {
            return levelBefore ? levelText + text : text + levelText;
        }

        if (levelText == null) {
            return maxHitBefore ? maxHitText + text : text + maxHitText;
        }

        if (maxHitBefore && levelBefore) {
            return maxHitText + levelText + text;
        } else if (!maxHitBefore && !levelBefore) {
            return text + levelText + maxHitText;
        } else if (maxHitBefore) {
            return maxHitText + text + levelText;
        } else {
            return levelText + text + maxHitText;
        }
    }
}
