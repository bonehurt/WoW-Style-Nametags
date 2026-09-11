package com.wownpc;

import com.google.inject.Inject;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
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

public class WoWStyleNametagsOverlay extends Overlay {
    private final WoWStyleNametagsPlugin plugin;
    private final Client client;

    @Inject
    WoWStyleNametagsOverlay(Client client, WoWStyleNametagsPlugin plugin) {
        this.plugin = plugin;
        this.client = client;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null) {
            return null;
        }

        WorldPoint localWp = localPlayer.getWorldLocation();
        List<TagEntry> entries = new ArrayList<>();

        java.util.List<net.runelite.api.WorldView> viewsToSync = new java.util.ArrayList<>();
        var tlwv = client.getTopLevelWorldView();
        var pwv = localPlayer.getWorldView();
        if (tlwv != null)
            viewsToSync.add(tlwv);
        if (pwv != null && pwv != tlwv)
            viewsToSync.add(pwv);

        // --- Collect NPC entries ---
        for (NPC npc : plugin.getTrackedNpcs()) {
            TagEntry entry = collectNpcEntry(graphics, npc, localPlayer, localWp);
            if (entry != null) {
                entries.add(entry);
            }
        }

        // --- Collect player entries ---
        int maxPerTile = plugin.maxNametagsPerTile;
        try {
            plugin.stackedTiles.clear();
            plugin.visiblePlayerTiles.clear();

            for (var wv : viewsToSync) {
                if (wv == null) {
                    continue;
                }

                Map<WorldPoint, List<Player>> playersByTile = NametagLayoutManager.groupAndSortPlayersByTile(
                        wv.players(), localPlayer, plugin::isActorVisibleThisFrame);

                // Populate stacked tiles and visible player tiles for client-side model
                // stacking detection
                for (Map.Entry<WorldPoint, List<Player>> entry : playersByTile.entrySet()) {
                    if (entry.getValue().size() > 1) {
                        plugin.stackedTiles.add(entry.getKey());
                    }
                    for (Player p : entry.getValue()) {
                        if (plugin.isActorVisibleThisFrame(p)) {
                            plugin.visiblePlayerTiles.add(entry.getKey());
                            break;
                        }
                    }
                }

                for (List<Player> tilePlayers : playersByTile.values()) {
                    int tileCount = 0;
                    for (Player p : tilePlayers) {
                        TagEntry entry = collectPlayerEntry(graphics, p, localPlayer, localWp);
                        if (entry != null) {
                            entries.add(entry);
                            tileCount++;
                            if (maxPerTile > 0 && tileCount >= maxPerTile) {
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        if (entries.isEmpty()) {
            return null;
        }

        // --- Distance-based culling: sort closest first, then truncate ---
        entries = NametagLayoutManager.cullByDistance(entries, plugin.maxEntities);

        // --- Optional vertical stacking to prevent overlapping nametags ---
        if (plugin.stackTags) {
            NametagLayoutManager.resolveOverlaps(entries, plugin.anchorBelow,
                    size -> getFontMetricsForSize(graphics, size));
        }

        // --- Render nametags ---
        for (TagEntry entry : entries) {
            renderTag(graphics, entry);
        }

        return null;
    }

    private Font getFontForSize(Graphics2D graphics, int size) {
        Font base = graphics.getFont();
        int clamped = Math.max(8, Math.min(30, size));
        return base.deriveFont((float) clamped);
    }

    private FontMetrics getFontMetricsForSize(Graphics2D graphics, int size) {
        Font original = graphics.getFont();
        try {
            graphics.setFont(getFontForSize(graphics, size));
            return graphics.getFontMetrics();
        } finally {
            graphics.setFont(original);
        }
    }

    /**
     * Evaluates whether an NPC should receive a nametag and, if so, builds a
     * {@link TagEntry} with resolved colour/outline settings and natural screen
     * position. Returns {@code null} if the NPC should be skipped.
     */
    private TagEntry collectNpcEntry(Graphics2D graphics, NPC npc, Player localPlayer, WorldPoint localWp) {
        if (npc == null) {
            return null;
        }
        NpcClassifier classifier = plugin.getNpcClassifier();
        String text = classifier != null ? classifier.getNpcDisplayName(npc)
                : WoWStyleNametagsPlugin.sanitizeEntityName(npc.getName());
        if (text == null) {
            return null;
        }

        if (!plugin.shouldRenderNametagForActor(npc)) {
            return null;
        }

        if (plugin.isNpcNameExcluded(text)) {
            return null;
        }

        if (!plugin.isNpcNameIncluded(text)) {
            return null;
        }

        if (classifier != null && classifier.isSuppressedResourceNpc(npc, text)) {
            return null;
        }

        if (plugin.hideBosses && classifier != null && classifier.isBoss(npc)) {
            return null;
        }

        if (plugin.hideBossMinions && classifier != null && classifier.isBossMinion(npc)) {
            return null;
        }

        if (plugin.hideQuestBosses && classifier != null && classifier.isQuestBoss(npc)) {
            return null;
        }

        // Hover-only gate: show if the cursor is over this NPC (by index or name
        // match).
        if (plugin.hoverOnly
                && plugin.hoverIndex != npc.getIndex()
                && (plugin.hoverTarget == null || !plugin.hoverTarget.equalsIgnoreCase(text))) {
            return null;
        }

        Color colour = null;
        boolean outlineEnabled = false;
        Color outlineColour = null;
        int outlineThickness = 2;
        int fontSize = 16;

        // Follower pet check (active follower or another player's follower)
        Actor owner = npc.getInteracting();
        boolean myFollower = (client != null && npc.equals(client.getFollower()))
                || (owner != null && owner.equals(localPlayer));

        boolean otherFollower = !myFollower && ((owner instanceof Player)
                || (npc.getComposition() != null && npc.getComposition().isFollower()));

        boolean follower = myFollower || otherFollower;

        if (follower) {
            if (myFollower) {
                if (!plugin.enableMyFollowers)
                    return null;
                colour = plugin.myFollowerColour;
                outlineEnabled = plugin.myFollowerOutlineEnabled;
                outlineColour = plugin.myFollowerOutlineColour;
                outlineThickness = plugin.myFollowerOutlineThickness;
                fontSize = plugin.myFollowerFontSize;
            } else {
                if (!plugin.enableOtherPlayersFollowers)
                    return null;
                colour = plugin.otherPlayersFollowerColour;
                outlineEnabled = plugin.otherPlayersFollowerOutlineEnabled;
                outlineColour = plugin.otherPlayersFollowerOutlineColour;
                outlineThickness = plugin.otherPlayersFollowerOutlineThickness;
                fontSize = plugin.otherPlayersFollowerFontSize;
            }
        } else {
            boolean trade = classifier != null && classifier.hasTradeOption(npc);
            boolean attack = classifier != null && classifier.hasAttackOption(npc);
            boolean talk = classifier != null && classifier.hasTalkOption(npc);
            boolean nonTalkInteraction = classifier != null && classifier.hasNonTalkInteractionOption(npc);

            // Pre-compute animal/hunter status for fallback use below.
            boolean isAnimal = classifier != null && classifier.hasPetOption(npc);
            boolean isHunter = classifier != null && classifier.hasHunterOption(npc);

            if (trade) {
                if (!plugin.enableShopkeepers)
                    return null;
                colour = plugin.shopkeeperColour;
                outlineEnabled = plugin.shopkeeperOutlineEnabled;
                outlineColour = plugin.shopkeeperOutlineColour;
                outlineThickness = plugin.shopkeeperOutlineThickness;
                fontSize = plugin.shopkeeperFontSize;
            } else {

                // Actively targeting the player (or player targeting the NPC in combat)
                boolean targetingPlayer = false;
                try {
                    targetingPlayer = (npc.getInteracting() != null && npc.getInteracting().equals(localPlayer))
                            || (localPlayer.getInteracting() != null && localPlayer.getInteracting().equals(npc));
                } catch (Exception ignored) {
                }

                boolean isAggressive = targetingPlayer || (classifier != null && classifier.isAlwaysAggressive(npc));

                // If the NPC is a recognised animal and the Animals category is
                // disabled, hide it — unless it is actively hostile (targeting the
                // player or always aggressive), in which case it should still
                // show under its combat category.
                if (isAnimal && !plugin.enablePets && !isAggressive) {
                    return null;
                }

                boolean passive = false;
                try {
                    if (attack && !talk && !nonTalkInteraction && !isAggressive) {
                        int npcLevel = npc.getCombatLevel();
                        int playerLevel = localPlayer.getCombatLevel();
                        if (classifier != null && classifier.isInherentlyPassive(npc)) {
                            passive = true;
                        } else if (npcLevel > 0 && playerLevel > 0 && playerLevel > npcLevel * 2) {
                            passive = true;
                        }
                    }
                } catch (Exception ignored) {
                }

                // Tracks whether the NPC's normal category was disabled so we can
                // fall back to the animal/pet category if the name matches.
                boolean categoryDisabled = false;

                if (attack && (talk || nonTalkInteraction)) {
                    if (isAggressive) {
                        if (!plugin.enableAttackable) {
                            categoryDisabled = true;
                        } else {
                            colour = plugin.attackableColour;
                            outlineEnabled = plugin.attackableOutlineEnabled;
                            outlineColour = plugin.attackableOutlineColour;
                            outlineThickness = plugin.attackableOutlineThickness;
                            fontSize = plugin.attackableFontSize;
                        }
                    } else {
                        if (!plugin.enableAttackableTalkable) {
                            categoryDisabled = true;
                        } else {
                            colour = plugin.attackableTalkableColour;
                            outlineEnabled = plugin.attackableTalkableOutlineEnabled;
                            outlineColour = plugin.attackableTalkableOutlineColour;
                            outlineThickness = plugin.attackableTalkableOutlineThickness;
                            fontSize = plugin.attackableTalkableFontSize;
                        }
                    }
                } else if (attack) {
                    if (passive) {
                        if (!plugin.enablePassive) {
                            categoryDisabled = true;
                        } else {
                            colour = plugin.passiveColour;
                            outlineEnabled = plugin.passiveOutlineEnabled;
                            outlineColour = plugin.passiveOutlineColour;
                            outlineThickness = plugin.passiveOutlineThickness;
                            fontSize = plugin.passiveFontSize;
                        }
                    } else {
                        if (!plugin.enableAttackable) {
                            categoryDisabled = true;
                        } else {
                            colour = plugin.attackableColour;
                            outlineEnabled = plugin.attackableOutlineEnabled;
                            outlineColour = plugin.attackableOutlineColour;
                            outlineThickness = plugin.attackableOutlineThickness;
                            fontSize = plugin.attackableFontSize;
                        }
                    }
                } else if (isAnimal) {
                    // Non-attackable animals and pets (including roaming Menagerie pets)
                    if (!plugin.enablePets)
                        return null;
                    colour = plugin.petsColour;
                    outlineEnabled = plugin.petsOutlineEnabled;
                    outlineColour = plugin.petsOutlineColour;
                    outlineThickness = plugin.petsOutlineThickness;
                    fontSize = plugin.petsFontSize;
                } else if (talk) {
                    if (!plugin.enableTalkable) {
                        categoryDisabled = true;
                    } else {
                        colour = plugin.talkableColour;
                        outlineEnabled = plugin.talkableOutlineEnabled;
                        outlineColour = plugin.talkableOutlineColour;
                        outlineThickness = plugin.talkableOutlineThickness;
                        fontSize = plugin.talkableFontSize;
                    }
                } else if (nonTalkInteraction) {
                    if (isHunter) {
                        if (!plugin.enableHunterMobs)
                            return null;
                        colour = plugin.hunterMobsColour;
                        outlineEnabled = plugin.hunterMobsOutlineEnabled;
                        outlineColour = plugin.hunterMobsOutlineColour;
                        outlineThickness = plugin.hunterMobsOutlineThickness;
                        fontSize = plugin.hunterMobsFontSize;
                    } else {
                        if (!plugin.enableNonTalkInteraction)
                            return null;
                        colour = plugin.nonTalkInteractionColour;
                        outlineEnabled = plugin.nonTalkInteractionOutlineEnabled;
                        outlineColour = plugin.nonTalkInteractionOutlineColour;
                        outlineThickness = plugin.nonTalkInteractionOutlineThickness;
                        fontSize = plugin.nonTalkInteractionFontSize;
                    }
                } else if (isHunter) {
                    // Hunter creatures without direct right-click actions on the NPC (e.g. snare
                    // birds, kebbits)
                    if (!plugin.enableHunterMobs)
                        return null;
                    colour = plugin.hunterMobsColour;
                    outlineEnabled = plugin.hunterMobsOutlineEnabled;
                    outlineColour = plugin.hunterMobsOutlineColour;
                    outlineThickness = plugin.hunterMobsOutlineThickness;
                    fontSize = plugin.hunterMobsFontSize;
                }

                // If the NPC's normal category was disabled, do not show it
                if (categoryDisabled && colour == null) {
                    return null;
                }
            }
        }

        if (colour == null) {
            return null;
        }

        // Format optional combat level text and colors
        String levelText = null;
        Color levelColour = null;
        boolean levelBefore = false;
        Color levelOutlineColour = outlineColour;
        if (plugin.showNpcCombatLevel && npc.getCombatLevel() > 0
                && (plugin.minCombatLevel <= 0 || npc.getCombatLevel() >= plugin.minCombatLevel)) {
            if (!plugin.combatLevelInCombatOnly || plugin.isInCombat(npc, localPlayer)) {
                levelText = plugin.formatCombatLevel(npc.getCombatLevel());
                levelColour = plugin.matchNameColour ? colour : plugin.combatLevelColour;
                levelOutlineColour = plugin.matchNameColour ? outlineColour : plugin.combatLevelOutlineColour;
                levelBefore = plugin.isCombatLevelBefore();
            }
        }

        // Format optional max hit text and colors
        String maxHitText = null;
        Color maxHitColour = null;
        boolean maxHitBefore = false;
        Color maxHitOutlineColour = outlineColour;
        if (plugin.showNpcMaxHit) {
            if (!plugin.maxHitInCombatOnly || plugin.isInCombat(npc, localPlayer)) {
                NPCComposition comp = npc.getTransformedComposition();
                int npcId = comp != null ? comp.getId() : npc.getId();
                int maxHit = plugin.getNpcMaxHit(npcId);
                if (maxHit >= 0 && (plugin.minNpcMaxHit <= 0 || maxHit >= plugin.minNpcMaxHit)) {
                    maxHitText = plugin.formatNpcMaxHit(maxHit);
                    maxHitColour = plugin.matchMaxHitNameColour ? colour : plugin.npcMaxHitColour;
                    maxHitOutlineColour = plugin.matchMaxHitNameColour ? outlineColour : plugin.npcMaxHitOutlineColour;
                    maxHitBefore = plugin.isNpcMaxHitBefore();
                }
            }
        }

        String fullText;
        if (levelText == null && maxHitText == null) {
            fullText = text;
        } else if (maxHitText == null) {
            fullText = levelBefore ? levelText + text : text + levelText;
        } else if (levelText == null) {
            fullText = maxHitBefore ? maxHitText + text : text + maxHitText;
        } else if (maxHitBefore && levelBefore) {
            fullText = maxHitText + levelText + text;
        } else if (!maxHitBefore && !levelBefore) {
            fullText = text + levelText + maxHitText;
        } else if (maxHitBefore) {
            fullText = maxHitText + text + levelText;
        } else {
            fullText = levelText + text + maxHitText;
        }

        // Project canvas text position with vertical offset
        int offset = plugin.anchorBelow
                ? -plugin.verticalOffset
                : npc.getLogicalHeight() + plugin.verticalOffset;
        Font original = graphics.getFont();
        Point loc;
        try {
            graphics.setFont(getFontForSize(graphics, fontSize));
            loc = npc.getCanvasTextLocation(graphics, fullText, offset);
        } finally {
            graphics.setFont(original);
        }
        if (loc == null) {
            return null;
        }

        int dist = localWp.distanceTo(npc.getWorldLocation());
        return new TagEntry(text, colour, levelText, levelColour, levelBefore, outlineEnabled, outlineColour,
                outlineThickness, fontSize,
                levelOutlineColour, maxHitText, maxHitColour, maxHitBefore, maxHitOutlineColour,
                dist, loc.getX(), loc.getY());
    }

    /**
     * Evaluates whether a player should receive a nametag and, if so, builds a
     * {@link TagEntry}. Returns {@code null} if the player should be skipped.
     */
    private TagEntry collectPlayerEntry(Graphics2D graphics, Player p, Player localPlayer, WorldPoint localWp) {
        if (p == null) {
            return null;
        }
        String name = WoWStyleNametagsPlugin.sanitizeEntityName(p.getName());
        if (name == null) {
            return null;
        }

        if (!plugin.shouldRenderNametagForActor(p)) {
            return null;
        }

        boolean isSelf = p.equals(localPlayer);
        Color colour;
        boolean outlineEnabled;
        Color outlineColour;
        int outlineThickness;
        int fontSize;

        // Self player styling
        if (isSelf) {
            if (!plugin.enableSelfPlayer)
                return null;
            colour = plugin.selfPlayerColour;
            outlineEnabled = plugin.selfPlayerOutlineEnabled;
            outlineColour = plugin.selfPlayerOutlineColour;
            outlineThickness = plugin.selfPlayerOutlineThickness;
            fontSize = plugin.selfPlayerFontSize;
        } else {
            if (plugin.isPlayerNameExcluded(name)) {
                return null;
            }

            if (!plugin.isPlayerNameIncluded(name)) {
                return null;
            }

            // Apply hover-only to other players (matched by name via hoverTarget).
            if (plugin.hoverOnly
                    && (plugin.hoverTarget == null || !plugin.hoverTarget.equalsIgnoreCase(name))) {
                return null;
            }

            boolean isFriend = false;
            boolean isClanMember = false;
            boolean isClanChatMember = false;
            boolean isGuestClanMember = false;
            boolean isGuestInYourClan = false;
            try {
                isFriend = p.isFriend();
                isClanMember = p.isClanMember();
                isClanChatMember = p.isFriendsChatMember();
                isGuestClanMember = isGuestClanMember(p);
                isGuestInYourClan = isGuestInYourClan(p);
            } catch (Exception ignored) {
            }

            // Priority order for overlapping relationships:
            // friends > clan members > clan members (guest) > guests in your clan
            // > chat channel members > other players.
            if (isFriend && plugin.enableFriendPlayers) {
                colour = plugin.friendPlayersColour;
                outlineEnabled = plugin.friendPlayersOutlineEnabled;
                outlineColour = plugin.friendPlayersOutlineColour;
                outlineThickness = plugin.friendPlayersOutlineThickness;
                fontSize = plugin.friendPlayersFontSize;
            } else if (isClanMember && plugin.enableClanMembers) {
                colour = plugin.clanMembersColour;
                outlineEnabled = plugin.clanMembersOutlineEnabled;
                outlineColour = plugin.clanMembersOutlineColour;
                outlineThickness = plugin.clanMembersOutlineThickness;
                fontSize = plugin.clanMembersFontSize;
            } else if (isGuestClanMember && plugin.enableGuestClanMembers) {
                colour = plugin.guestClanMembersColour;
                outlineEnabled = plugin.guestClanMembersOutlineEnabled;
                outlineColour = plugin.guestClanMembersOutlineColour;
                outlineThickness = plugin.guestClanMembersOutlineThickness;
                fontSize = plugin.guestClanMembersFontSize;
            } else if (isGuestInYourClan && plugin.enableGuestsInYourClan) {
                colour = plugin.guestsInYourClanColour;
                outlineEnabled = plugin.guestsInYourClanOutlineEnabled;
                outlineColour = plugin.guestsInYourClanOutlineColour;
                outlineThickness = plugin.guestsInYourClanOutlineThickness;
                fontSize = plugin.guestsInYourClanFontSize;
            } else if (isClanChatMember && plugin.enableClanChatMembers) {
                colour = plugin.clanChatMembersColour;
                outlineEnabled = plugin.clanChatMembersOutlineEnabled;
                outlineColour = plugin.clanChatMembersOutlineColour;
                outlineThickness = plugin.clanChatMembersOutlineThickness;
                fontSize = plugin.clanChatMembersFontSize;
            } else {
                if (!plugin.enableOtherPlayers)
                    return null;
                colour = plugin.otherPlayersColour;
                outlineEnabled = plugin.otherPlayersOutlineEnabled;
                outlineColour = plugin.otherPlayersOutlineColour;
                outlineThickness = plugin.otherPlayersOutlineThickness;
                fontSize = plugin.otherPlayersFontSize;
            }
        }

        int offset = plugin.anchorBelow
                ? -plugin.verticalOffset
                : p.getLogicalHeight() + plugin.verticalOffset;

        // Adjust for overhead prayer/icon to prevent nameplate from being hidden below
        // it
        if (p.getOverheadIcon() != null) {
            offset += plugin.overheadIconOffset;
        }

        // Format optional player combat level display
        String levelText = null;
        Color levelColour = null;
        boolean levelBefore = false;
        Color levelOutlineColour = outlineColour;
        if (plugin.showPlayerCombatLevel && p.getCombatLevel() > 0
                && (plugin.minCombatLevel <= 0 || p.getCombatLevel() >= plugin.minCombatLevel)) {
            if (!plugin.combatLevelInCombatOnly || plugin.isInCombat(p, localPlayer)) {
                levelText = plugin.formatCombatLevel(p.getCombatLevel());
                levelColour = plugin.matchNameColour ? colour : plugin.combatLevelColour;
                levelOutlineColour = plugin.matchNameColour ? outlineColour : plugin.combatLevelOutlineColour;
                levelBefore = plugin.isCombatLevelBefore();
            }
        }
        String fullText = levelText != null ? (levelBefore ? levelText + name : name + levelText) : name;

        Font original = graphics.getFont();
        Point loc;
        try {
            graphics.setFont(getFontForSize(graphics, fontSize));
            loc = p.getCanvasTextLocation(graphics, fullText, offset);
        } finally {
            graphics.setFont(original);
        }
        if (loc == null) {
            return null;
        }

        // Self uses distance 0, so it is strongly prioritized during culling.
        int dist = isSelf ? 0 : localWp.distanceTo(p.getWorldLocation());
        return new TagEntry(name, colour, levelText, levelColour, levelBefore, outlineEnabled, outlineColour,
                outlineThickness, fontSize,
                levelOutlineColour,
                dist, loc.getX(), loc.getY());
    }

    private boolean isGuestClanMember(Player p) {
        ClanChannel guestChannel = client.getGuestClanChannel();
        if (guestChannel == null || p == null || p.getName() == null) {
            return false;
        }

        ClanChannelMember member = guestChannel.findMember(p.getName());
        if (member == null || member.getRank() == null) {
            return false;
        }

        return !ClanRank.GUEST.equals(member.getRank());
    }

    private boolean isGuestInYourClan(Player p) {
        ClanChannel clanChannel = client.getClanChannel();
        if (clanChannel == null || p == null || p.getName() == null) {
            return false;
        }

        ClanChannelMember member = clanChannel.findMember(p.getName());
        if (member == null || member.getRank() == null) {
            return false;
        }

        return ClanRank.GUEST.equals(member.getRank());
    }

    /**
     * Draws a fully-resolved {@link TagEntry} at its (possibly stacked) screen
     * position.
     */
    private void renderTag(Graphics2D graphics, TagEntry entry) {
        Font original = graphics.getFont();
        graphics.setFont(getFontForSize(graphics, entry.fontSize));

        Point loc = new Point(entry.screenX, entry.screenY);
        Color nameOutlineCol = entry.outlineColour != null ? entry.outlineColour : Color.BLACK;
        Color lvlOutlineCol = entry.levelOutlineColour != null ? entry.levelOutlineColour : Color.BLACK;
        Color maxHitOutlineCol = entry.maxHitOutlineColour != null ? entry.maxHitOutlineColour : Color.BLACK;

        List<TextSegment> segments = new ArrayList<>(3);
        TextSegment nameSeg = new TextSegment(entry.text, entry.colour, nameOutlineCol);
        TextSegment levelSeg = entry.levelText != null
                ? new TextSegment(entry.levelText, entry.levelColour, lvlOutlineCol)
                : null;
        TextSegment maxHitSeg = entry.maxHitText != null
                ? new TextSegment(entry.maxHitText, entry.maxHitColour, maxHitOutlineCol)
                : null;

        if (entry.levelText != null && entry.maxHitText != null) {
            if (entry.maxHitBefore && entry.levelBefore) {
                segments.add(maxHitSeg);
                segments.add(levelSeg);
                segments.add(nameSeg);
            } else if (!entry.maxHitBefore && !entry.levelBefore) {
                segments.add(nameSeg);
                segments.add(levelSeg);
                segments.add(maxHitSeg);
            } else if (entry.maxHitBefore) {
                segments.add(maxHitSeg);
                segments.add(nameSeg);
                segments.add(levelSeg);
            } else {
                segments.add(levelSeg);
                segments.add(nameSeg);
                segments.add(maxHitSeg);
            }
        } else if (entry.levelText != null) {
            if (entry.levelBefore) {
                segments.add(levelSeg);
                segments.add(nameSeg);
            } else {
                segments.add(nameSeg);
                segments.add(levelSeg);
            }
        } else if (entry.maxHitText != null) {
            if (entry.maxHitBefore) {
                segments.add(maxHitSeg);
                segments.add(nameSeg);
            } else {
                segments.add(nameSeg);
                segments.add(maxHitSeg);
            }
        } else {
            segments.add(nameSeg);
        }

        try {
            if (entry.outlineEnabled) {
                boolean separateSegments = false;
                for (TextSegment seg : segments) {
                    if ((seg.colour != null && !seg.colour.equals(entry.colour))
                            || (seg.outlineColour != null && !seg.outlineColour.equals(nameOutlineCol))) {
                        separateSegments = true;
                        break;
                    }
                }

                graphics.setStroke(new BasicStroke(Math.max(1, entry.outlineThickness),
                        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                FontRenderContext frc = graphics.getFontRenderContext();

                if (separateSegments) {
                    FontMetrics fm = graphics.getFontMetrics();
                    int currentX = loc.getX();
                    for (TextSegment seg : segments) {
                        if (seg.text == null || seg.text.isEmpty()) {
                            continue;
                        }
                        TextLayout layout = new TextLayout(seg.text, graphics.getFont(), frc);
                        Shape shape = layout.getOutline(null);
                        Shape transformed = AffineTransform.getTranslateInstance(currentX, loc.getY())
                                .createTransformedShape(shape);
                        graphics.setColor(seg.outlineColour != null ? seg.outlineColour : Color.BLACK);
                        graphics.draw(transformed);
                        graphics.setColor(seg.colour != null ? seg.colour : Color.WHITE);
                        graphics.fill(transformed);
                        currentX += fm.stringWidth(seg.text);
                    }
                } else {
                    String fullText = entry.getFullText();
                    TextLayout layout = new TextLayout(fullText, graphics.getFont(), frc);
                    Shape outlineShape = layout.getOutline(null);
                    AffineTransform transform = AffineTransform.getTranslateInstance(loc.getX(), loc.getY());
                    Shape transformed = transform.createTransformedShape(outlineShape);

                    graphics.setColor(nameOutlineCol);
                    graphics.draw(transformed);
                    graphics.setColor(entry.colour);
                    graphics.fill(transformed);
                }
            } else {
                boolean separateColors = false;
                for (TextSegment seg : segments) {
                    if (seg.colour != null && !seg.colour.equals(entry.colour)) {
                        separateColors = true;
                        break;
                    }
                }

                if (separateColors) {
                    FontMetrics fm = graphics.getFontMetrics();
                    int currentX = loc.getX();
                    for (TextSegment seg : segments) {
                        if (seg.text == null || seg.text.isEmpty()) {
                            continue;
                        }
                        OverlayUtil.renderTextLocation(graphics, new Point(currentX, loc.getY()), seg.text,
                                seg.colour != null ? seg.colour : Color.WHITE);
                        currentX += fm.stringWidth(seg.text);
                    }
                } else {
                    OverlayUtil.renderTextLocation(graphics, loc, entry.getFullText(), entry.colour);
                }
            }
        } catch (Exception e) {
            OverlayUtil.renderTextLocation(graphics, loc, entry.getFullText(), entry.colour);
        } finally {
            graphics.setFont(original);
        }
    }

    private static class TextSegment {
        final String text;
        final Color colour;
        final Color outlineColour;

        TextSegment(String text, Color colour, Color outlineColour) {
            this.text = text;
            this.colour = colour;
            this.outlineColour = outlineColour;
        }
    }
}
