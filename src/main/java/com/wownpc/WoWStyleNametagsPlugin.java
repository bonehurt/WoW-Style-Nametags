package com.wownpc;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Provides;
import java.awt.Color;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
    name = "WoW-Style Nametags",
    description = "Overlays NPC/Player names above their heads, with optional color-coding and outlines & other options similar to World of Warcraft.",
    tags = {"NPC", "names", "overlay", "WoW", "nametags"}
)
public class WoWStyleNametagsPlugin extends Plugin
{
    private static final Set<Integer> NPC_MENU_ACTIONS = ImmutableSet.of(
        MenuAction.NPC_FIRST_OPTION.getId(),
        MenuAction.NPC_SECOND_OPTION.getId(),
        MenuAction.NPC_THIRD_OPTION.getId(),
        MenuAction.NPC_FOURTH_OPTION.getId(),
        MenuAction.NPC_FIFTH_OPTION.getId(),
        MenuAction.EXAMINE_NPC.getId()
    );

    // Interaction-only actions (excludes Examine). Used to distinguish
    // "examine-only" NPCs from NPCs with real interaction options (Talk-to, Bank, etc.).
    private static final Set<Integer> NPC_INTERACTION_ACTIONS = ImmutableSet.of(
        MenuAction.NPC_FIRST_OPTION.getId(),
        MenuAction.NPC_SECOND_OPTION.getId(),
        MenuAction.NPC_THIRD_OPTION.getId(),
        MenuAction.NPC_FOURTH_OPTION.getId(),
        MenuAction.NPC_FIFTH_OPTION.getId()
    );

    // Menu actions relevant for hover detection (both NPCs and players)
    private static final Set<Integer> ENTITY_MENU_ACTIONS = ImmutableSet.of(
        MenuAction.NPC_FIRST_OPTION.getId(),
        MenuAction.NPC_SECOND_OPTION.getId(),
        MenuAction.NPC_THIRD_OPTION.getId(),
        MenuAction.NPC_FOURTH_OPTION.getId(),
        MenuAction.NPC_FIFTH_OPTION.getId(),
        MenuAction.EXAMINE_NPC.getId(),
        MenuAction.PLAYER_FIRST_OPTION.getId(),
        MenuAction.PLAYER_SECOND_OPTION.getId(),
        MenuAction.PLAYER_THIRD_OPTION.getId(),
        MenuAction.PLAYER_FOURTH_OPTION.getId(),
        MenuAction.PLAYER_FIFTH_OPTION.getId()
    );

    // --- Hover state (written by onMenuEntryAdded / onGameTick, read by overlay) ---
    int hoverIndex = -1;
    String hoverTarget;

    // --- Cached config values (refreshed on startUp and onConfigChanged) ---
    boolean hoverOnly;
    boolean anchorBelow;
    int verticalOffset;
    int maxEntities;
    boolean stackTags;

    // NPC categories
    boolean enableAttackable;
    Color attackableColour;
    boolean attackableOutlineEnabled;
    Color attackableOutlineColour;
    int attackableOutlineThickness;

    boolean enablePassive;
    Color passiveColour;
    boolean passiveOutlineEnabled;
    Color passiveOutlineColour;
    int passiveOutlineThickness;

    boolean enableTalkable;
    Color talkableColour;
    boolean talkableOutlineEnabled;
    Color talkableOutlineColour;
    int talkableOutlineThickness;

    boolean enableAttackableTalkable;
    Color attackableTalkableColour;
    boolean attackableTalkableOutlineEnabled;
    Color attackableTalkableOutlineColour;
    int attackableTalkableOutlineThickness;

    // Players
    boolean enableSelfPlayer;
    Color selfPlayerColour;
    boolean selfPlayerOutlineEnabled;
    Color selfPlayerOutlineColour;
    int selfPlayerOutlineThickness;

    boolean enableOtherPlayers;
    Color otherPlayersColour;
    boolean otherPlayersOutlineEnabled;
    Color otherPlayersOutlineColour;
    int otherPlayersOutlineThickness;

    // Followers
    boolean enableMyFollowers;
    Color myFollowerColour;
    boolean myFollowerOutlineEnabled;
    Color myFollowerOutlineColour;
    int myFollowerOutlineThickness;

    boolean enableOtherPlayersFollowers;
    Color otherPlayersFollowerColour;
    boolean otherPlayersFollowerOutlineEnabled;
    Color otherPlayersFollowerOutlineColour;
    int otherPlayersFollowerOutlineThickness;

    // --- Runtime NPC tracking ---
    private final Map<Integer, NPC> trackedNpcs = new ConcurrentHashMap<>();
    /**
     * NPC <em>composition IDs</em> ({@link NPC#getId()}) where a Talk-to option has been
     * observed. Keyed on composition ID (NPC type), not instance index, so the result
     * applies to every instance of that NPC type and survives despawn/respawn.
     */
    private final Set<Integer> persistentTalkable = ConcurrentHashMap.newKeySet();
    /**
     * NPC <em>composition IDs</em> ({@link NPC#getId()}) confirmed as having no Talk-to
     * option (e.g. examine-only NPCs such as Ducklings). Same lifetime guarantees as
     * {@link #persistentTalkable}.
     */
    private final Set<Integer> persistentNotTalkable = ConcurrentHashMap.newKeySet();

    @Inject
    private WoWStyleNametagsConfig config;

    @Inject
    private Client client;

    @Inject
    private WoWStyleNametagsOverlay overlay;

    @Inject
    private OverlayManager overlayManager;

    @Provides
    WoWStyleNametagsConfig providesConfig(ConfigManager configManager)
    {
        return configManager.getConfig(WoWStyleNametagsConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        cacheConfig();
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(overlay);
    }

    private void cacheConfig()
    {
        // Order matches config menu positions
        hoverOnly = config.hoverOnly();
        anchorBelow = config.anchorBelow();

        // NPC enable toggles
        enableAttackable = config.enableAttackable();
        enablePassive = config.enablePassive();
        enableAttackableTalkable = config.enableAttackableTalkable();
        enableTalkable = config.enableTalkable();

        // Player and follower toggles
        enableSelfPlayer = config.enableSelfPlayer();
        enableOtherPlayers = config.enableOtherPlayers();
        enableMyFollowers = config.enableMyFollowers();
        enableOtherPlayersFollowers = config.enableOtherPlayersFollowers();

        // Positioning
        verticalOffset = config.verticalOffset();
        maxEntities = config.maxEntities();
        stackTags = config.stackTags();

        // Colours
        attackableColour = config.attackableColour();
        passiveColour = config.passiveColour();
        attackableTalkableColour = config.attackableTalkableColour();
        talkableColour = config.talkableColour();
        selfPlayerColour = config.selfPlayerColour();
        otherPlayersColour = config.otherPlayersColour();
        myFollowerColour = config.myFollowerColour();
        otherPlayersFollowerColour = config.otherPlayersFollowerColour();

        // Outlines
        attackableOutlineEnabled = config.attackableOutlineEnabled();
        attackableOutlineColour = config.attackableOutlineColour();
        attackableOutlineThickness = config.attackableOutlineThickness();

        passiveOutlineEnabled = config.passiveOutlineEnabled();
        passiveOutlineColour = config.passiveOutlineColour();
        passiveOutlineThickness = config.passiveOutlineThickness();

        attackableTalkableOutlineEnabled = config.attackableTalkableOutlineEnabled();
        attackableTalkableOutlineColour = config.attackableTalkableOutlineColour();
        attackableTalkableOutlineThickness = config.attackableTalkableOutlineThickness();

        talkableOutlineEnabled = config.talkableOutlineEnabled();
        talkableOutlineColour = config.talkableOutlineColour();
        talkableOutlineThickness = config.talkableOutlineThickness();

        otherPlayersOutlineEnabled = config.otherPlayersOutlineEnabled();
        otherPlayersOutlineColour = config.otherPlayersOutlineColour();
        otherPlayersOutlineThickness = config.otherPlayersOutlineThickness();

        selfPlayerOutlineEnabled = config.selfPlayerOutlineEnabled();
        selfPlayerOutlineColour = config.selfPlayerOutlineColour();
        selfPlayerOutlineThickness = config.selfPlayerOutlineThickness();

        myFollowerOutlineEnabled = config.myFollowerOutlineEnabled();
        myFollowerOutlineColour = config.myFollowerOutlineColour();
        myFollowerOutlineThickness = config.myFollowerOutlineThickness();
        otherPlayersFollowerOutlineEnabled = config.otherPlayersFollowerOutlineEnabled();
        otherPlayersFollowerOutlineColour = config.otherPlayersFollowerOutlineColour();
        otherPlayersFollowerOutlineThickness = config.otherPlayersFollowerOutlineThickness();
    }

    public boolean hasAttackOption(NPC npc)
    {
        if (npc == null || npc.getComposition() == null)
        {
            return false;
        }

        String[] actions = npc.getComposition().getActions();
        if (actions == null)
        {
            return false;
        }

        for (String a : actions)
        {
            if (a == null)
            {
                continue;
            }
            String s = a.toLowerCase(Locale.ROOT);
            if (s.contains("attack"))
            {
                return true;
            }
        }

        return false;
    }

    public boolean hasTalkOption(NPC npc)
    {
        if (npc == null || npc.getComposition() == null)
        {
            return false;
        }

        String[] actions = npc.getComposition().getActions();

        // If the composition has real (non-null) actions, check them directly.
        if (actions != null)
        {
            boolean allNull = true;
            for (String a : actions)
            {
                if (a != null)
                {
                    allNull = false;
                    break;
                }
            }

            if (!allNull)
            {
                for (String a : actions)
                {
                    if (a != null && a.toLowerCase(Locale.ROOT).contains("talk"))
                    {
                        return true;
                    }
                }
                // Has real actions but none are talk - not talkable.
                return false;
            }
        }

        // Regular composition is null or all-null. Some NPCs (e.g. Doomsayer, bankers) hide
        // their actions behind varbit/varp transforms. getTransformedComposition() applies the
        // current player state and exposes those hidden actions — check it next.
        net.runelite.api.NPCComposition transformed = npc.getTransformedComposition();
        if (transformed != null)
        {
            String[] tActions = transformed.getActions();
            if (tActions != null)
            {
                boolean allNull = true;
                for (String a : tActions)
                {
                    if (a != null)
                    {
                        allNull = false;
                        break;
                    }
                }
                if (!allNull)
                {
                    for (String a : tActions)
                    {
                        if (a != null && a.toLowerCase(Locale.ROOT).contains("talk"))
                        {
                                persistentTalkable.add(npc.getId());
                            return true;
                        }
                    }
                    // Transformed composition has real actions but no talk.
                        persistentNotTalkable.add(npc.getId());
                    return false;
                }
            }
        }

        // Both regular and transformed compositions are null/all-null (e.g. Ducklings).
        // We have no composition data to determine talk status. Check caches and live menu:

        // 1. Persistent positive cache: a talk option was seen for this NPC type.
        //    Uses composition ID so all instances of the same NPC type benefit.
        if (persistentTalkable.contains(npc.getId()))
        {
                return true;
        }

        // 2. Persistent negative cache: this NPC type was confirmed as having no
        //    talk option (e.g. examine-only, such as Ducklings).
        if (persistentNotTalkable.contains(npc.getId()))
        {
                return false;
        }

        // 3. Live menu entries - only present while the player is hovering this NPC.
        //    We use the interaction-only action set (FIRST-FIFTH) to distinguish between:
        //      a) Has interaction entries with talk -> talkable
        //      b) Has interaction entries but no talk -> not talkable (e.g. pure-attack NPC)
        //      c) Has only Examine entry (no interaction entries at all) -> examine-only, not talkable
        //      d) No entries at all -> player is not hovering this NPC, fall through to heuristic
        try
        {
            MenuEntry[] entries = client.getMenu().getMenuEntries();
            if (entries != null)
            {
                boolean hasInteraction = false;
                boolean hasExamine = false;
                for (MenuEntry e : entries)
                {
                    if (e.getIdentifier() != npc.getIndex()) continue;
                    int typeId = e.getType().getId();
                    if (NPC_INTERACTION_ACTIONS.contains(typeId))
                    {
                        hasInteraction = true;
                        String opt = e.getOption();
                        if (opt != null && opt.toLowerCase(Locale.ROOT).contains("talk"))
                        {
                            persistentTalkable.add(npc.getId());  // composition ID
                            return true;
                        }
                    }
                    else if (typeId == MenuAction.EXAMINE_NPC.getId())
                    {
                        hasExamine = true;
                    }
                }
                // Interaction entries seen but no talk, or only Examine - both definitively not talkable.
                if (hasInteraction || hasExamine)
                {
                        persistentNotTalkable.add(npc.getId());  // composition ID
                    return false;
                }
            }
        }
        catch (Exception ignored) {}

        // 4. No composition data and no live menu evidence. The NPC genuinely has
        //    no determinable talk option — return false. The persistent caches will
        //    correct this the first time the player hovers the NPC.
        return false;
    }

    @Subscribe
    public void onNpcSpawned(NpcSpawned event)
    {
        NPC npc = event.getNpc();
        if (npc != null)
        {
            trackedNpcs.put(npc.getIndex(), npc);
            // Scan current menu entries for this NPC (catch talk options
            // that might already be present) so we don't rely solely on hover/menu events.
            scanMenuForNpc(npc);
        }
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned event)
    {
        NPC npc = event.getNpc();
        if (npc != null)
        {
            trackedNpcs.remove(npc.getIndex());
            // persistentTalkable / persistentNotTalkable are keyed on composition ID
            // (NPC type), not instance index — intentionally not cleared on despawn
            // so the classification applies to all instances of the same NPC type
            // and survives stairs / zone transitions.
        }
    }

    @Subscribe
    public void onNpcChanged(NpcChanged event)
    {
        NPC npc = event.getNpc();
        if (npc != null)
        {
            trackedNpcs.put(npc.getIndex(), npc);
        }
    }

    public Collection<NPC> getTrackedNpcs()
    {
        return trackedNpcs.values();
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (WoWStyleNametagsConfig.GROUP.equals(event.getGroup()))
        {
            cacheConfig();
        }
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event)
    {
        try
        {
            int typeId = event.getType();

            // Update hover state immediately (fires every frame).
            // Only set on entity entries; onGameTick clears when
            // the mouse moves away from any entity.
            if (ENTITY_MENU_ACTIONS.contains(typeId))
            {
                hoverIndex = event.getIdentifier();
                hoverTarget = sanitizeTarget(event.getTarget());
            }

            // Cache talk options for NPCs as the menu is built so we can classify
            // them even before the player explicitly right-clicks.
            String op = event.getOption();
            // Cache talk options using composition ID so all instances of the same
            if (op != null && op.toLowerCase(Locale.ROOT).contains("talk") && NPC_MENU_ACTIONS.contains(typeId))
            {
                NPC trackedNpc = trackedNpcs.get(event.getIdentifier());
                if (trackedNpc != null)
                {
                    persistentTalkable.add(trackedNpc.getId());
                }
            }
        }
        catch (Exception ignored) {}
    }

    private String sanitizeTarget(String raw)
    {
        if (raw == null)
        {
            return null;
        }
        try
        {
            // Strip RuneScape colour/style tags e.g. <col=ff0000>, </col>, <img=N>
            String s = raw.replaceAll("<[^>]+>", "").trim();
            // Strip combat-level suffix that appears in player targets: " (level-126)"
            s = s.replaceAll("(?i)\\s*\\(level-\\d+\\)\\s*$", "").trim();
            // Strip common action prefixes that sometimes appear on target strings
            s = s.replaceAll("(?i)^(talk-?to\\s+|examine\\s+|walk here\\s+)", "").trim();
            return s;
        }
        catch (Exception e)
        {
            return raw;
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        // Scan menu entries once per tick: cache talk/examine-only results and update hover state.
        try
        {
            MenuEntry[] entries = client.getMenu().getMenuEntries();
            boolean hoverFound = false;
            if (entries != null)
            {
                // Per-NPC: track whether any interaction (FIRST–FIFTH) or examine entry was seen.
                java.util.Set<Integer> withInteraction = new java.util.HashSet<>();
                java.util.Set<Integer> withTalk       = new java.util.HashSet<>();
                java.util.Set<Integer> examineOnly    = new java.util.HashSet<>();

                for (MenuEntry e : entries)
                {
                    int typeId = e.getType().getId();
                    int instanceIndex = e.getIdentifier();

                    // Hover tracking uses instance index (for targeting/highlighting).
                    if (!hoverFound && ENTITY_MENU_ACTIONS.contains(typeId))
                    {
                        hoverIndex = instanceIndex;
                        hoverTarget = sanitizeTarget(e.getTarget());
                        hoverFound = true;
                    }

                    // Classification uses composition ID (NPC type) so the result
                    // applies to all future instances of the same NPC.
                    if (NPC_INTERACTION_ACTIONS.contains(typeId) || typeId == MenuAction.EXAMINE_NPC.getId())
                    {
                        NPC trackedNpc = trackedNpcs.get(instanceIndex);
                        if (trackedNpc == null) continue;
                        int compId = trackedNpc.getId();

                        if (NPC_INTERACTION_ACTIONS.contains(typeId))
                        {
                            withInteraction.add(compId);
                            String opt = e.getOption();
                            if (opt != null && opt.toLowerCase(Locale.ROOT).contains("talk"))
                            {
                                withTalk.add(compId);
                                persistentTalkable.add(compId);
                            }
                        }
                        else // EXAMINE_NPC
                        {
                            examineOnly.add(compId);
                        }
                    }
                }

                // NPC types with interaction entries but no talk — definitively not talkable.
                for (int compId : withInteraction)
                {
                    if (!withTalk.contains(compId))
                    {
                        persistentNotTalkable.add(compId);
                    }
                }
                // NPC types that appeared only via Examine — examine-only.
                for (int compId : examineOnly)
                {
                    if (!withInteraction.contains(compId))
                    {
                        persistentNotTalkable.add(compId);
                    }
                }
            }
            if (!hoverFound)
            {
                hoverIndex = -1;
                hoverTarget = null;
            }
        }
        catch (Exception ignored) {}
    }

    private void scanMenuForNpc(NPC npc)
    {
        if (npc == null || client == null)
        {
            return;
        }

        try
        {
            MenuEntry[] entries = client.getMenu().getMenuEntries();
            if (entries != null)
            {
                boolean hasInteraction = false;
                boolean hasExamine = false;
                for (MenuEntry e : entries)
                {
                    if (e.getIdentifier() != npc.getIndex()) continue;
                    int typeId = e.getType().getId();
                    if (NPC_INTERACTION_ACTIONS.contains(typeId))
                    {
                        hasInteraction = true;
                        String opt = e.getOption();
                        if (opt != null && opt.toLowerCase(Locale.ROOT).contains("talk"))
                        {
                            persistentTalkable.add(npc.getId());  // composition ID
                            return;
                        }
                    }
                    else if (typeId == MenuAction.EXAMINE_NPC.getId())
                    {
                        hasExamine = true;
                    }
                }
                if (hasInteraction || hasExamine)
                {
                    persistentNotTalkable.add(npc.getId());  // composition ID
                }
            }
        }
        catch (Exception ignored) {}
    }
}
