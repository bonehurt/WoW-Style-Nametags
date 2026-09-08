package com.wownpc;

import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

/**
 * Handles spatial layout, candidate culling, per-tile grouping,
 * and vertical overlap resolution (stacking) for nametags.
 */
public final class NametagLayoutManager {
    private NametagLayoutManager() {
    }

    /**
     * Filters players by grouping them per tile and keeping only up to
     * {@code maxPerTile}.
     * Prioritizes the local player first, then top-rendered players (higher ID
     * drawn on top).
     */
    public static List<Player> filterPlayersPerTile(Iterable<? extends Player> players, Player localPlayer,
            int maxPerTile) {
        if (players == null) {
            return Collections.emptyList();
        }

        // Group all visible players by their world tile coordinate
        Map<WorldPoint, List<Player>> playersByTile = new HashMap<>();
        List<Player> allPlayers = new ArrayList<>();
        for (Player p : players) {
            if (p != null) {
                allPlayers.add(p);
                WorldPoint wp = p.getWorldLocation();
                if (wp != null) {
                    playersByTile.computeIfAbsent(wp, k -> new ArrayList<>()).add(p);
                }
            }
        }

        if (maxPerTile <= 0) {
            return allPlayers;
        }

        List<Player> filtered = new ArrayList<>();
        for (List<Player> tilePlayers : playersByTile.values()) {
            if (tilePlayers.size() > 1) {
                // Sort tile occupants: local player first, then descending player ID (higher
                // rendered on top)
                tilePlayers.sort((p1, p2) -> {
                    boolean s1 = p1.equals(localPlayer);
                    boolean s2 = p2.equals(localPlayer);
                    if (s1 != s2) {
                        return s1 ? -1 : 1;
                    }
                    return Integer.compare(p2.getId(), p1.getId());
                });
            }

            // Retain up to the configured limit per tile
            int count = 0;
            for (Player p : tilePlayers) {
                filtered.add(p);
                count++;
                if (count >= maxPerTile) {
                    break;
                }
            }
        }
        return filtered;
    }

    /**
     * Sorts tag entries closest-first by Chebyshev distance and truncates to
     * {@code maxEntities}.
     */
    public static List<TagEntry> cullByDistance(List<TagEntry> entries, int maxEntities) {
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyList();
        }

        entries.sort(Comparator.comparingInt(e -> e.worldDist));
        if (maxEntities > 0 && entries.size() > maxEntities) {
            return new ArrayList<>(entries.subList(0, maxEntities));
        }
        return entries;
    }

    /**
     * Resolves overlapping nametags by shifting them vertically, WoW-style.
     * Entries must be sorted closest-first on entry. The closest entity keeps
     * its natural screen position; each subsequent entry is nudged upward (or
     * downward when anchor-below is active) until it no longer overlaps any
     * already-placed tag.
     */
    public static void resolveOverlaps(List<TagEntry> entries, boolean anchorBelow,
            IntFunction<FontMetrics> fontMetricsProvider) {
        if (entries == null || entries.size() <= 1) {
            return;
        }

        // Each int[] stores [left, top, right, bottom] of a placed tag's bounding box.
        List<int[]> placed = new ArrayList<>(entries.size());

        for (TagEntry entry : entries) {
            FontMetrics fm = fontMetricsProvider.apply(entry.fontSize);
            int w = fm.stringWidth(entry.getFullText());
            int h = fm.getAscent();

            // Text baseline is at (screenX, screenY).
            // Bounding box: top = baseline - ascent, bottom = baseline.
            int left = entry.screenX;
            int right = left + w;
            int bottom = entry.screenY;
            int top = bottom - h;

            // Nudge the tag vertically until it clears all previously placed bounding boxes
            boolean overlapping = true;
            while (overlapping) {
                overlapping = false;
                int bestEdge = anchorBelow ? Integer.MIN_VALUE : Integer.MAX_VALUE;

                for (int[] b : placed) {
                    boolean xOverlap = left < b[2] && right > b[0];
                    boolean yOverlap = top < b[3] && bottom > b[1];
                    if (xOverlap && yOverlap) {
                        overlapping = true;
                        if (anchorBelow) {
                            if (b[3] > bestEdge) {
                                bestEdge = b[3];
                            }
                        } else {
                            if (b[1] < bestEdge) {
                                bestEdge = b[1];
                            }
                        }
                    }
                }

                if (overlapping) {
                    if (anchorBelow) {
                        top = bestEdge + 2;
                        bottom = top + h;
                    } else {
                        bottom = bestEdge - 2;
                        top = bottom - h;
                    }
                }
            }

            entry.screenY = bottom;
            placed.add(new int[] { left, top, right, bottom });
        }
    }
}
