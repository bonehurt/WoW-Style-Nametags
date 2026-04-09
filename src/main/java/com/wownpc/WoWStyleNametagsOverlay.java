package com.wownpc;

import com.google.inject.Inject;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.clan.ClanRank;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

public class WoWStyleNametagsOverlay extends Overlay
{
    /**
     * All data required to render a single nametag, including its resolved
     * screen position (which may be shifted from the natural position when
     * stack-tags mode is active).
     */
    private static final class TagEntry
    {
        final String text;
        final Color colour;
        final boolean outlineEnabled;
        final Color outlineColour;
        final int outlineThickness;
        /** Chebyshev tile distance from the local player — used for culling. */
        final int worldDist;
        /** Screen X of the text baseline (left edge). Immutable. */
        final int screenX;
        /** Screen Y of the text baseline. Mutated by resolveOverlaps() when stacking. */
        int screenY;

        TagEntry(String text, Color colour, boolean outlineEnabled, Color outlineColour,
                 int outlineThickness, int worldDist, int screenX, int screenY)
        {
            this.text            = text;
            this.colour          = colour;
            this.outlineEnabled  = outlineEnabled;
            this.outlineColour   = outlineColour;
            this.outlineThickness = outlineThickness;
            this.worldDist       = worldDist;
            this.screenX         = screenX;
            this.screenY         = screenY;
        }
    }

    private final WoWStyleNametagsPlugin plugin;
    private final Client client;

    @Inject
    WoWStyleNametagsOverlay(Client client, WoWStyleNametagsPlugin plugin)
    {
        this.plugin = plugin;
        this.client = client;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null)
        {
            return null;
        }

        WorldPoint localWp = localPlayer.getWorldLocation();
        List<TagEntry> entries = new ArrayList<>();

        // Populate stacked tiles and visible player tiles for client-side stacking detection.
        plugin.stackedTiles.clear();
        plugin.visiblePlayerTiles.clear();
        if (localWp != null)
        {
            plugin.visiblePlayerTiles.add(localWp);
        }

        Map<WorldPoint, Integer> playerCounts = new HashMap<>();
        try
        {
            var wv = client.getTopLevelWorldView();
            if (wv != null)
            {
                for (var p : wv.players())
                {
                    if (p != null)
                    {
                        WorldPoint wp = p.getWorldLocation();
                        playerCounts.put(wp, playerCounts.getOrDefault(wp, 0) + 1);
                        if (plugin.isActorVisibleThisFrame(p))
                        {
                            plugin.visiblePlayerTiles.add(wp);
                        }
                    }
                }
            }
        }
        catch (Exception ignored) {}
        for (Map.Entry<WorldPoint, Integer> e : playerCounts.entrySet())
        {
            if (e.getValue() > 1)
            {
                plugin.stackedTiles.add(e.getKey());
            }
        }

        // --- Collect NPC entries ---
        for (NPC npc : plugin.getTrackedNpcs())
        {
            TagEntry entry = collectNpcEntry(graphics, npc, localPlayer, localWp);
            if (entry != null)
            {
                entries.add(entry);
            }
        }

        // --- Collect player entries ---
        try
        {
            var wv = client.getTopLevelWorldView();
            if (wv != null)
            {
                for (var p : wv.players())
                {
                    TagEntry entry = collectPlayerEntry(graphics, p, localPlayer, localWp);
                    if (entry != null)
                    {
                        entries.add(entry);
                    }
                }
            }
        }
        catch (Exception ignored) {}

        if (entries.isEmpty())
        {
            return null;
        }

        // --- Distance-based culling: sort closest first, then truncate ---
        entries.sort(Comparator.comparingInt(e -> e.worldDist));
        int max = plugin.maxEntities;
        if (max > 0 && entries.size() > max)
        {
            entries = entries.subList(0, max);
        }

        // --- Optional vertical stacking to prevent overlapping nametags ---
        if (plugin.stackTags)
        {
            resolveOverlaps(graphics, entries);
        }

        // --- Render nametags ---
        for (TagEntry entry : entries)
        {
            renderTag(graphics, entry);
        }

        return null;
    }

    /**
     * Resolves overlapping nametags by shifting them vertically, WoW-style.
     * Entries must be sorted closest-first on entry. The closest entity keeps
     * its natural screen position; each subsequent entry is nudged upward (or
     * downward when anchor-below is active) until it no longer overlaps any
     * already-placed tag.  On each overlap check we jump past the most extreme
     * conflicting box so the algorithm always converges.
     */
    private void resolveOverlaps(Graphics2D graphics, List<TagEntry> entries)
    {
        FontMetrics fm = graphics.getFontMetrics();
        // Each int[] stores [left, top, right, bottom] of a placed tag's bounding box.
        List<int[]> placed = new ArrayList<>(entries.size());

        for (TagEntry entry : entries)
        {
            int w = fm.stringWidth(entry.text);
            int h = fm.getAscent();

            // The text baseline is at (screenX, screenY).
            // Bounding box: top = baseline − ascent, bottom = baseline.
            int left   = entry.screenX;
            int right  = left + w;
            int bottom = entry.screenY;
            int top    = bottom - h;

            boolean overlapping = true;
            while (overlapping)
            {
                overlapping = false;
                int bestEdge = plugin.anchorBelow ? Integer.MIN_VALUE : Integer.MAX_VALUE;

                for (int[] b : placed)
                {
                    boolean xOverlap = left < b[2] && right > b[0];
                    boolean yOverlap = top  < b[3] && bottom > b[1];
                    if (xOverlap && yOverlap)
                    {
                        overlapping = true;
                        if (plugin.anchorBelow)
                        {
                            // Nudging downward: jump past the furthest-down placed box.
                            if (b[3] > bestEdge) bestEdge = b[3];
                        }
                        else
                        {
                            // Nudging upward: jump past the highest placed box.
                            if (b[1] < bestEdge) bestEdge = b[1];
                        }
                    }
                }

                if (overlapping)
                {
                    if (plugin.anchorBelow)
                    {
                        top    = bestEdge + 2;
                        bottom = top + h;
                    }
                    else
                    {
                        bottom = bestEdge - 2;
                        top    = bottom - h;
                    }
                }
            }

            entry.screenY = bottom;
            placed.add(new int[]{left, top, right, bottom});
        }
    }

    /**
     * Evaluates whether an NPC should receive a nametag and, if so, builds a
     * {@link TagEntry} with resolved colour/outline settings and natural screen
     * position. Returns {@code null} if the NPC should be skipped.
     */
    private TagEntry collectNpcEntry(Graphics2D graphics, NPC npc, Player localPlayer, WorldPoint localWp)
    {
        if (npc == null || npc.getId() < 0)
        {
            return null;
        }
        String text = plugin.sanitizeEntityName(npc.getName());
        if (text == null)
        {
            return null;
        }

        if (!plugin.shouldRenderNametagForActor(npc))
        {
            return null;
        }

        if (plugin.isNpcNameExcluded(text))
        {
            return null;
        }

        if (plugin.isSuppressedResourceNpc(npc, text))
        {
            return null;
        }

        // Hover-only gate: show if the cursor is over this NPC (by index or name match).
        if (plugin.hoverOnly
                && plugin.hoverIndex != npc.getIndex()
                && (plugin.hoverTarget == null || !plugin.hoverTarget.equalsIgnoreCase(text)))
        {
            return null;
        }

        Color colour = null;
        boolean outlineEnabled = false;
        Color outlineColour = null;
        int outlineThickness = 2;

        boolean follower = npc.getComposition() != null && npc.getComposition().isFollower();

        if (follower)
        {
            Actor owner = npc.getInteracting();
            boolean myFollower = owner != null && owner.equals(localPlayer);

            if (myFollower)
            {
                if (!plugin.enableMyFollowers) return null;
                colour           = plugin.myFollowerColour;
                outlineEnabled   = plugin.myFollowerOutlineEnabled;
                outlineColour    = plugin.myFollowerOutlineColour;
                outlineThickness = plugin.myFollowerOutlineThickness;
            }
            else
            {
                if (!plugin.enableOtherPlayersFollowers) return null;
                colour           = plugin.otherPlayersFollowerColour;
                outlineEnabled   = plugin.otherPlayersFollowerOutlineEnabled;
                outlineColour    = plugin.otherPlayersFollowerOutlineColour;
                outlineThickness = plugin.otherPlayersFollowerOutlineThickness;
            }
        }
        else
        {
            boolean attack = plugin.hasAttackOption(npc);
            boolean talk   = plugin.hasTalkOption(npc);
            boolean nonTalkInteraction = plugin.hasNonTalkInteractionOption(npc);

            // Actively targeting the player — definitively aggressive regardless of level.
            // This catches always-aggressive NPCs (e.g. Lizardmen) that would otherwise
            // fall below the 2x threshold and show as passive.
            boolean targetingPlayer = false;
            try
            {
                targetingPlayer = npc.getInteracting() != null
                        && npc.getInteracting().equals(localPlayer);
                if (targetingPlayer)
                {
                    plugin.rememberAggressiveNpcType(npc);
                }
            }
            catch (Exception ignored) {}

            boolean observedAggressiveType = plugin.wasNpcTypeObservedAggressive(npc);

            // Passive: attack-only NPCs that are NOT currently targeting the player AND whose
            // level does not exceed twice the player's combat level.  This mirrors the OSRS
            // aggression rule (NPCs stop attacking once the player's level is over double
            // theirs), so only truly out-of-league NPCs show red.
            // NPCs that are both attackable AND have either Talk-to or another non-attack
            // interaction (e.g. Man/Woman, catchable/pettable/shearable NPCs) are Neutral
            // regardless of level.
            boolean passive = false;
            try
            {
                if (attack && !talk && !nonTalkInteraction && !targetingPlayer && !observedAggressiveType)
                {
                    int npcLevel    = npc.getCombatLevel();
                    int playerLevel = localPlayer.getCombatLevel();
                    if (npcLevel > 0 && playerLevel > 0 && npcLevel <= playerLevel * 2)
                    {
                        passive = true;
                    }
                }
            }
            catch (Exception ignored) {}

            if (attack && (talk || nonTalkInteraction))
            {
                if (observedAggressiveType)
                {
                    if (!plugin.enableAttackable) return null;
                    colour           = plugin.attackableColour;
                    outlineEnabled   = plugin.attackableOutlineEnabled;
                    outlineColour    = plugin.attackableOutlineColour;
                    outlineThickness = plugin.attackableOutlineThickness;
                }
                else
                {
                    if (!plugin.enableAttackableTalkable) return null;
                    colour           = plugin.attackableTalkableColour;
                    outlineEnabled   = plugin.attackableTalkableOutlineEnabled;
                    outlineColour    = plugin.attackableTalkableOutlineColour;
                    outlineThickness = plugin.attackableTalkableOutlineThickness;
                }
            }
            else if (attack)
            {
                if (passive)
                {
                    if (!plugin.enablePassive) return null;
                    colour           = plugin.passiveColour;
                    outlineEnabled   = plugin.passiveOutlineEnabled;
                    outlineColour    = plugin.passiveOutlineColour;
                    outlineThickness = plugin.passiveOutlineThickness;
                }
                else
                {
                    if (!plugin.enableAttackable) return null;
                    colour           = plugin.attackableColour;
                    outlineEnabled   = plugin.attackableOutlineEnabled;
                    outlineColour    = plugin.attackableOutlineColour;
                    outlineThickness = plugin.attackableOutlineThickness;
                }
            }
            else if (talk)
            {
                if (!plugin.enableTalkable) return null;
                colour           = plugin.talkableColour;
                outlineEnabled   = plugin.talkableOutlineEnabled;
                outlineColour    = plugin.talkableOutlineColour;
                outlineThickness = plugin.talkableOutlineThickness;
            }
            else if (nonTalkInteraction)
            {
                if (!plugin.enableNonTalkInteraction) return null;
                colour           = plugin.nonTalkInteractionColour;
                outlineEnabled   = plugin.nonTalkInteractionOutlineEnabled;
                outlineColour    = plugin.nonTalkInteractionOutlineColour;
                outlineThickness = plugin.nonTalkInteractionOutlineThickness;
            }
        }

        if (colour == null)
        {
            return null;
        }

        int offset = plugin.anchorBelow
                ? -plugin.verticalOffset
                : npc.getLogicalHeight() + plugin.verticalOffset;
        Point loc = npc.getCanvasTextLocation(graphics, text, offset);
        if (loc == null)
        {
            return null;
        }

        int dist = localWp.distanceTo(npc.getWorldLocation());
        return new TagEntry(text, colour, outlineEnabled, outlineColour, outlineThickness,
                dist, loc.getX(), loc.getY());
    }

    /**
     * Evaluates whether a player should receive a nametag and, if so, builds a
     * {@link TagEntry}. Returns {@code null} if the player should be skipped.
     */
    private TagEntry collectPlayerEntry(Graphics2D graphics, Player p, Player localPlayer, WorldPoint localWp)
    {
        if (p == null)
        {
            return null;
        }
        String name = plugin.sanitizeEntityName(p.getName());
        if (name == null)
        {
            return null;
        }

        if (!plugin.shouldRenderNametagForActor(p))
        {
            return null;
        }

        boolean isSelf = p.equals(localPlayer);
        Color colour;
        boolean outlineEnabled;
        Color outlineColour;
        int outlineThickness;

        if (isSelf)
        {
            if (!plugin.enableSelfPlayer) return null;
            colour           = plugin.selfPlayerColour;
            outlineEnabled   = plugin.selfPlayerOutlineEnabled;
            outlineColour    = plugin.selfPlayerOutlineColour;
            outlineThickness = plugin.selfPlayerOutlineThickness;
        }
        else
        {
            if (plugin.isPlayerNameExcluded(name))
            {
                return null;
            }

            // Apply hover-only to other players (matched by name via hoverTarget).
            if (plugin.hoverOnly
                    && (plugin.hoverTarget == null || !plugin.hoverTarget.equalsIgnoreCase(name)))
            {
                return null;
            }

            boolean isFriend = false;
            boolean isClanMember = false;
            boolean isClanChatMember = false;
            boolean isGuestClanMember = false;
            boolean isGuestInYourClan = false;
            try
            {
                isFriend = p.isFriend();
                isClanMember = p.isClanMember();
                isClanChatMember = p.isFriendsChatMember();
                isGuestClanMember = isGuestClanMember(p);
                isGuestInYourClan = isGuestInYourClan(p);
            }
            catch (Exception ignored) {}

            // Priority order for overlapping relationships:
            // friends > clan members > clan members (guest) > guests in your clan
            // > chat channel members > other players.
            if (isFriend && plugin.enableFriendPlayers)
            {
                colour           = plugin.friendPlayersColour;
                outlineEnabled   = plugin.friendPlayersOutlineEnabled;
                outlineColour    = plugin.friendPlayersOutlineColour;
                outlineThickness = plugin.friendPlayersOutlineThickness;
            }
            else if (isClanMember && plugin.enableClanMembers)
            {
                colour           = plugin.clanMembersColour;
                outlineEnabled   = plugin.clanMembersOutlineEnabled;
                outlineColour    = plugin.clanMembersOutlineColour;
                outlineThickness = plugin.clanMembersOutlineThickness;
            }
            else if (isGuestClanMember && plugin.enableGuestClanMembers)
            {
                colour           = plugin.guestClanMembersColour;
                outlineEnabled   = plugin.guestClanMembersOutlineEnabled;
                outlineColour    = plugin.guestClanMembersOutlineColour;
                outlineThickness = plugin.guestClanMembersOutlineThickness;
            }
            else if (isGuestInYourClan && plugin.enableGuestsInYourClan)
            {
                colour           = plugin.guestsInYourClanColour;
                outlineEnabled   = plugin.guestsInYourClanOutlineEnabled;
                outlineColour    = plugin.guestsInYourClanOutlineColour;
                outlineThickness = plugin.guestsInYourClanOutlineThickness;
            }
            else if (isClanChatMember && plugin.enableClanChatMembers)
            {
                colour           = plugin.clanChatMembersColour;
                outlineEnabled   = plugin.clanChatMembersOutlineEnabled;
                outlineColour    = plugin.clanChatMembersOutlineColour;
                outlineThickness = plugin.clanChatMembersOutlineThickness;
            }
            else
            {
                if (!plugin.enableOtherPlayers) return null;
                colour           = plugin.otherPlayersColour;
                outlineEnabled   = plugin.otherPlayersOutlineEnabled;
                outlineColour    = plugin.otherPlayersOutlineColour;
                outlineThickness = plugin.otherPlayersOutlineThickness;
            }
        }

        int offset = plugin.anchorBelow
                ? -plugin.verticalOffset
                : p.getLogicalHeight() + plugin.verticalOffset;

        // Adjust for overhead prayer/icon to prevent nameplate from being hidden below it
        if (p.getOverheadIcon() != null)
        {
            offset += plugin.overheadIconOffset;
        }

        Point loc = p.getCanvasTextLocation(graphics, name, offset);
        if (loc == null)
        {
            return null;
        }

        // Self uses distance 0, so it is strongly prioritized during culling.
        int dist = isSelf ? 0 : localWp.distanceTo(p.getWorldLocation());
        return new TagEntry(name, colour, outlineEnabled, outlineColour, outlineThickness,
                dist, loc.getX(), loc.getY());
    }

    private boolean isGuestClanMember(Player p)
    {
        ClanChannel guestChannel = client.getGuestClanChannel();
        if (guestChannel == null || p == null || p.getName() == null)
        {
            return false;
        }

        ClanChannelMember member = guestChannel.findMember(p.getName());
        if (member == null || member.getRank() == null)
        {
            return false;
        }

        return !ClanRank.GUEST.equals(member.getRank());
    }

    private boolean isGuestInYourClan(Player p)
    {
        ClanChannel clanChannel = client.getClanChannel();
        if (clanChannel == null || p == null || p.getName() == null)
        {
            return false;
        }

        ClanChannelMember member = clanChannel.findMember(p.getName());
        if (member == null || member.getRank() == null)
        {
            return false;
        }

        return ClanRank.GUEST.equals(member.getRank());
    }

    /** Draws a fully-resolved {@link TagEntry} at its (possibly stacked) screen position. */
    private void renderTag(Graphics2D graphics, TagEntry entry)
    {
        Point loc = new Point(entry.screenX, entry.screenY);

        if (entry.outlineEnabled)
        {
            try
            {
                Color base    = entry.outlineColour != null ? entry.outlineColour : Color.BLACK;
                Color outline = new Color(base.getRed(), base.getGreen(), base.getBlue(), 255);

                FontRenderContext frc    = graphics.getFontRenderContext();
                TextLayout        layout = new TextLayout(entry.text, graphics.getFont(), frc);
                Shape outlineShape = layout.getOutline(null);
                AffineTransform transform = AffineTransform.getTranslateInstance(loc.getX(), loc.getY());
                Shape transformed = transform.createTransformedShape(outlineShape);

                graphics.setColor(outline);
                graphics.setStroke(new BasicStroke(Math.max(1, entry.outlineThickness),
                        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                graphics.draw(transformed);

                graphics.setColor(entry.colour);
                graphics.fill(transformed);
            }
            catch (Exception e)
            {
                OverlayUtil.renderTextLocation(graphics, loc, entry.text, entry.colour);
            }
        }
        else
        {
            OverlayUtil.renderTextLocation(graphics, loc, entry.text, entry.colour);
        }
    }
}
