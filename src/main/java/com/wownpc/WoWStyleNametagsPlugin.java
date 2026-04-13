package com.wownpc;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Provides;
import java.awt.Color;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Renderable;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.callback.RenderCallback;
import net.runelite.client.callback.RenderCallbackManager;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PluginChanged;
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
    private static final String CURRENT_VERSION = "1.4";
    private static final String UPDATE_NOTICE_VERSION_KEY = "updateNoticeVersion";
    private static final String UPDATE_NOTICE_TEXT = "New 'Shopkeeper' nametag + adjustable font sizes!";

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

    private static final String ATTACK_KEYWORD = "attack";
    private static final String TALK_KEYWORD = "talk";
    private static final String TRADE_KEYWORD = "trade";
    private static final String FISHING_SPOT_KEYWORD = "fishing spot";

    // --- Hover state (written by onMenuEntryAdded / onGameTick, read by overlay) ---
    int hoverIndex = -1;
    String hoverTarget;

    // --- Cached config values (refreshed on startUp and onConfigChanged) ---
    boolean hoverOnly;
    boolean anchorBelow;
    int verticalOffset;
    int maxEntities;
    boolean stackTags;
    boolean respectEntityHiders;

    // NPC categories
    boolean enableAttackable;
    Color attackableColour;
    int attackableFontSize;
    boolean attackableOutlineEnabled;
    Color attackableOutlineColour;
    int attackableOutlineThickness;

    boolean enablePassive;
    Color passiveColour;
    int passiveFontSize;
    boolean passiveOutlineEnabled;
    Color passiveOutlineColour;
    int passiveOutlineThickness;

    boolean enableTalkable;
    Color talkableColour;
    int talkableFontSize;
    boolean talkableOutlineEnabled;
    Color talkableOutlineColour;
    int talkableOutlineThickness;

    boolean enableNonTalkInteraction;
    Color nonTalkInteractionColour;
    int nonTalkInteractionFontSize;
    boolean nonTalkInteractionOutlineEnabled;
    Color nonTalkInteractionOutlineColour;
    int nonTalkInteractionOutlineThickness;

    boolean enableShopkeepers;
    Color shopkeeperColour;
    int shopkeeperFontSize;
    boolean shopkeeperOutlineEnabled;
    Color shopkeeperOutlineColour;
    int shopkeeperOutlineThickness;

    boolean enableAttackableTalkable;
    Color attackableTalkableColour;
    int attackableTalkableFontSize;
    boolean attackableTalkableOutlineEnabled;
    Color attackableTalkableOutlineColour;
    int attackableTalkableOutlineThickness;

    // Players
    boolean enableSelfPlayer;
    Color selfPlayerColour;
    int selfPlayerFontSize;
    boolean selfPlayerOutlineEnabled;
    Color selfPlayerOutlineColour;
    int selfPlayerOutlineThickness;

    boolean enableOtherPlayers;
    Color otherPlayersColour;
    int otherPlayersFontSize;
    boolean otherPlayersOutlineEnabled;
    Color otherPlayersOutlineColour;
    int otherPlayersOutlineThickness;

    boolean enableFriendPlayers;
    Color friendPlayersColour;
    int friendPlayersFontSize;
    boolean friendPlayersOutlineEnabled;
    Color friendPlayersOutlineColour;
    int friendPlayersOutlineThickness;

    boolean enableClanMembers;
    Color clanMembersColour;
    int clanMembersFontSize;
    boolean clanMembersOutlineEnabled;
    Color clanMembersOutlineColour;
    int clanMembersOutlineThickness;

    boolean enableClanChatMembers;
    Color clanChatMembersColour;
    int clanChatMembersFontSize;
    boolean clanChatMembersOutlineEnabled;
    Color clanChatMembersOutlineColour;
    int clanChatMembersOutlineThickness;

    boolean enableGuestClanMembers;
    Color guestClanMembersColour;
    int guestClanMembersFontSize;
    boolean guestClanMembersOutlineEnabled;
    Color guestClanMembersOutlineColour;
    int guestClanMembersOutlineThickness;

    boolean enableGuestsInYourClan;
    Color guestsInYourClanColour;
    int guestsInYourClanFontSize;
    boolean guestsInYourClanOutlineEnabled;
    Color guestsInYourClanOutlineColour;
    int guestsInYourClanOutlineThickness;

    // Followers
    boolean enableMyFollowers;
    Color myFollowerColour;
    int myFollowerFontSize;
    boolean myFollowerOutlineEnabled;
    Color myFollowerOutlineColour;
    int myFollowerOutlineThickness;

    boolean enableOtherPlayersFollowers;
    Color otherPlayersFollowerColour;
    int otherPlayersFollowerFontSize;
    boolean otherPlayersFollowerOutlineEnabled;
    Color otherPlayersFollowerOutlineColour;
    int otherPlayersFollowerOutlineThickness;

    Set<String> excludedNpcNames;
    Set<String> excludedPlayerNames;

    // --- Runtime NPC tracking ---
    private final Map<Integer, NPC> trackedNpcs = new ConcurrentHashMap<>();
    private final Set<Actor> visibleActorsThisFrame = ConcurrentHashMap.newKeySet();
    /**
     * NPC <em>composition IDs</em> ({@link NPC#getId()}) where a Talk-to option has been
     * observed. Keyed on composition ID (NPC type), not instance index, so the result
     * applies to every instance of that NPC type and survives despawn/respawn.
     */
    private final Set<Integer> persistentTalkable = ConcurrentHashMap.newKeySet();
    private final Set<Integer> persistentTalkTo = ConcurrentHashMap.newKeySet();
    private final Set<Integer> persistentTradeable = ConcurrentHashMap.newKeySet();
    /**
     * NPC <em>composition IDs</em> ({@link NPC#getId()}) confirmed as having no Talk-to
     * option (e.g. examine-only NPCs such as Ducklings). Same lifetime guarantees as
     * {@link #persistentTalkable}.
     */
    private final Set<Integer> persistentNotTalkable = ConcurrentHashMap.newKeySet();
    private final Set<Integer> persistentAttackable = ConcurrentHashMap.newKeySet();
    private final Set<Integer> persistentNotAttackable = ConcurrentHashMap.newKeySet();
    // Tracks NPC instance indices observed targeting the player during their current lifetime.
    private final Set<Integer> observedAggressiveNpcInstances = ConcurrentHashMap.newKeySet();
    // Tracks composition IDs observed as genuinely pre-aggro aggressive this session.
    private final Set<Integer> persistentObservedAggressiveTypes = ConcurrentHashMap.newKeySet();
    // Tracks NPC instances the player explicitly initiated combat against.
    private final Set<Integer> playerProvokedNpcInstances = ConcurrentHashMap.newKeySet();
    final Set<WorldPoint> stackedTiles = ConcurrentHashMap.newKeySet();
    final Set<WorldPoint> visiblePlayerTiles = ConcurrentHashMap.newKeySet();

    // --- Update notice state ---
    private boolean updateNoticePending = false;

    int overheadIconOffset;

    // --- Transient scene state (cleared on game state changes) ---
    private boolean sawSceneActorThisFrame = false;
    private final RenderCallback visibilityTracker = new RenderCallback()
    {
        @Override
        public boolean addEntity(Renderable renderable, boolean ui)
        {
            if (!ui && renderable instanceof Actor)
            {
                visibleActorsThisFrame.add((Actor) renderable);
                sawSceneActorThisFrame = true;
            }

            return true;
        }
    };

    @Inject
    private WoWStyleNametagsConfig config;

    @Inject
    private Client client;

    @Inject
    private WoWStyleNametagsOverlay overlay;

    @Inject
    private ConfigManager configManager;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private RenderCallbackManager renderCallbackManager;

    @Inject
    private ChatMessageManager chatMessageManager;

    @Provides
    WoWStyleNametagsConfig providesConfig(ConfigManager configManager)
    {
        return configManager.getConfig(WoWStyleNametagsConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        cacheConfig();
        clearTransientSceneState();
        updateRenderCallbackRegistration();
        updateNoticePending = shouldShowUpdateNotice();
        overlayManager.add(overlay);
        showUpdateNoticeIfReady();
    }

    @Override
    protected void shutDown() throws Exception
    {
        renderCallbackManager.unregister(visibilityTracker);
        clearTransientSceneState();
        updateNoticePending = false;
        overlayManager.remove(overlay);
    }

    private void cacheConfig()
    {
        // Order matches config menu positions
        hoverOnly = config.hoverOnly();
        anchorBelow = config.anchorBelow();
        respectEntityHiders = config.respectEntityHiders();
        overheadIconOffset = config.overheadIconOffset();

        // NPC enable toggles
        enableAttackable = config.enableAttackable();
        enablePassive = config.enablePassive();
        enableAttackableTalkable = config.enableAttackableTalkable();
        enableTalkable = config.enableTalkable();
        enableNonTalkInteraction = config.enableNonTalkInteraction();
        enableShopkeepers = config.enableShopkeepers();

        // Player and follower toggles
        enableSelfPlayer = config.enableSelfPlayer();
        enableOtherPlayers = config.enableOtherPlayers();
        enableFriendPlayers = config.enableFriendPlayers();
        enableClanMembers = config.enableClanMembers();
        enableClanChatMembers = config.enableClanChatMembers();
        enableGuestClanMembers = config.enableGuestClanMembers();
        enableGuestsInYourClan = config.enableGuestsInYourClan();
        enableMyFollowers = config.enableMyFollowers();
        enableOtherPlayersFollowers = config.enableOtherPlayersFollowers();
        excludedNpcNames = parseNameSet(config.excludedNpcNames());
        excludedPlayerNames = parseNameSet(config.excludedPlayerNames());

        // Positioning
        verticalOffset = config.verticalOffset();
        maxEntities = config.maxEntities();
        stackTags = config.stackTags();

        // Colours
        attackableColour = config.attackableColour();
        attackableFontSize = config.attackableFontSize();
        passiveColour = config.passiveColour();
        passiveFontSize = config.passiveFontSize();
        attackableTalkableColour = config.attackableTalkableColour();
        attackableTalkableFontSize = config.attackableTalkableFontSize();
        talkableColour = config.talkableColour();
        talkableFontSize = config.talkableFontSize();
        nonTalkInteractionColour = config.nonTalkInteractionColour();
        nonTalkInteractionFontSize = config.nonTalkInteractionFontSize();
        shopkeeperColour = config.shopkeeperColour();
        shopkeeperFontSize = config.shopkeeperFontSize();
        selfPlayerColour = config.selfPlayerColour();
        selfPlayerFontSize = config.selfPlayerFontSize();
        otherPlayersColour = config.otherPlayersColour();
        otherPlayersFontSize = config.otherPlayersFontSize();
        friendPlayersColour = config.friendPlayersColour();
        friendPlayersFontSize = config.friendPlayersFontSize();
        clanMembersColour = config.clanMembersColour();
        clanMembersFontSize = config.clanMembersFontSize();
        clanChatMembersColour = config.clanChatMembersColour();
        clanChatMembersFontSize = config.clanChatMembersFontSize();
        guestClanMembersColour = config.guestClanMembersColour();
        guestClanMembersFontSize = config.guestClanMembersFontSize();
        guestsInYourClanColour = config.guestsInYourClanColour();
        guestsInYourClanFontSize = config.guestsInYourClanFontSize();
        myFollowerColour = config.myFollowerColour();
        myFollowerFontSize = config.myFollowerFontSize();
        otherPlayersFollowerColour = config.otherPlayersFollowerColour();
        otherPlayersFollowerFontSize = config.otherPlayersFollowerFontSize();

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

        nonTalkInteractionOutlineEnabled = config.nonTalkInteractionOutlineEnabled();
        nonTalkInteractionOutlineColour = config.nonTalkInteractionOutlineColour();
        nonTalkInteractionOutlineThickness = config.nonTalkInteractionOutlineThickness();

        shopkeeperOutlineEnabled = config.shopkeeperOutlineEnabled();
        shopkeeperOutlineColour = config.shopkeeperOutlineColour();
        shopkeeperOutlineThickness = config.shopkeeperOutlineThickness();

        otherPlayersOutlineEnabled = config.otherPlayersOutlineEnabled();
        otherPlayersOutlineColour = config.otherPlayersOutlineColour();
        otherPlayersOutlineThickness = config.otherPlayersOutlineThickness();

        friendPlayersOutlineEnabled = config.friendPlayersOutlineEnabled();
        friendPlayersOutlineColour = config.friendPlayersOutlineColour();
        friendPlayersOutlineThickness = config.friendPlayersOutlineThickness();

        clanMembersOutlineEnabled = config.clanMembersOutlineEnabled();
        clanMembersOutlineColour = config.clanMembersOutlineColour();
        clanMembersOutlineThickness = config.clanMembersOutlineThickness();

        clanChatMembersOutlineEnabled = config.clanChatMembersOutlineEnabled();
        clanChatMembersOutlineColour = config.clanChatMembersOutlineColour();
        clanChatMembersOutlineThickness = config.clanChatMembersOutlineThickness();

        guestClanMembersOutlineEnabled = config.guestClanMembersOutlineEnabled();
        guestClanMembersOutlineColour = config.guestClanMembersOutlineColour();
        guestClanMembersOutlineThickness = config.guestClanMembersOutlineThickness();

        guestsInYourClanOutlineEnabled = config.guestsInYourClanOutlineEnabled();
        guestsInYourClanOutlineColour = config.guestsInYourClanOutlineColour();
        guestsInYourClanOutlineThickness = config.guestsInYourClanOutlineThickness();

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

    private void updateRenderCallbackRegistration()
    {
        renderCallbackManager.unregister(visibilityTracker);
        visibleActorsThisFrame.clear();
        sawSceneActorThisFrame = false;

        if (respectEntityHiders)
        {
            renderCallbackManager.register(visibilityTracker);
        }
    }

    private void clearTransientSceneState()
    {
        trackedNpcs.clear();
        hoverIndex = -1;
        hoverTarget = null;
        visibleActorsThisFrame.clear();
        sawSceneActorThisFrame = false;
        observedAggressiveNpcInstances.clear();
        playerProvokedNpcInstances.clear();
        stackedTiles.clear();
        visiblePlayerTiles.clear();
    }

    private void syncTrackedNpcsFromScene()
    {
        if (client == null)
        {
            return;
        }

        try
        {
            var wv = client.getTopLevelWorldView();
            if (wv == null)
            {
                return;
            }

            Set<Integer> liveIndexes = new HashSet<>();
            for (NPC npc : wv.npcs())
            {
                if (npc == null)
                {
                    continue;
                }

                int index = npc.getIndex();
                liveIndexes.add(index);
                trackedNpcs.put(index, npc);
            }

            trackedNpcs.keySet().removeIf(index -> !liveIndexes.contains(index));
        }
        catch (Exception ignored) {}
    }

    private boolean shouldShowUpdateNotice()
    {
        String seenVersion = configManager.getConfiguration(WoWStyleNametagsConfig.GROUP, UPDATE_NOTICE_VERSION_KEY);
        return !CURRENT_VERSION.equals(seenVersion);
    }

    private void showUpdateNoticeIfReady()
    {
        if (!updateNoticePending || client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }

        String message = new ChatMessageBuilder()
            .append(ChatColorType.HIGHLIGHT)
            .append("WoW-Style Nametags Updated: v")
            .append(CURRENT_VERSION)
            .append(" - ")
            .append(UPDATE_NOTICE_TEXT)
            .build();

        chatMessageManager.queue(QueuedMessage.builder()
            .type(ChatMessageType.CONSOLE)
            .runeLiteFormattedMessage(message)
            .build());

        configManager.setConfiguration(WoWStyleNametagsConfig.GROUP, UPDATE_NOTICE_VERSION_KEY, CURRENT_VERSION);
        updateNoticePending = false;
    }

    public boolean isNpcNameExcluded(String npcName)
    {
        return excludedNpcNames != null && excludedNpcNames.contains(normalizeName(npcName));
    }

    public boolean isPlayerNameExcluded(String playerName)
    {
        return excludedPlayerNames != null && excludedPlayerNames.contains(normalizeName(playerName));
    }

    public void rememberAggressiveNpcType(NPC npc)
    {
        if (npc == null)
        {
            return;
        }

        int index = npc.getIndex();
        if (index >= 0)
        {
            observedAggressiveNpcInstances.add(index);
            if (!playerProvokedNpcInstances.contains(index))
            {
                persistentObservedAggressiveTypes.add(npc.getId());
            }
        }
    }

    public boolean wasNpcTypeObservedAggressive(NPC npc)
    {
        if (npc == null)
        {
            return false;
        }

        return observedAggressiveNpcInstances.contains(npc.getIndex())
                || persistentObservedAggressiveTypes.contains(npc.getId());
    }

    public String getNpcDisplayName(NPC npc)
    {
        if (npc == null)
        {
            return null;
        }

        String rawName = null;
        try
        {
            net.runelite.api.NPCComposition transformed = npc.getTransformedComposition();
            if (transformed != null)
            {
                rawName = transformed.getName();
            }
        }
        catch (Exception ignored) {}

        if (rawName == null || rawName.trim().isEmpty() || "null".equalsIgnoreCase(rawName.trim()))
        {
            rawName = npc.getName();
        }

        return sanitizeEntityName(rawName);
    }

    public boolean isSuppressedResourceNpc(NPC npc, String sanitizedName)
    {
        if (npc == null)
        {
            return false;
        }

        String normalizedName = normalizeName(sanitizedName);
        if (normalizedName.contains(FISHING_SPOT_KEYWORD))
        {
            return true;
        }

        String[] actions = null;
        net.runelite.api.NPCComposition transformed = npc.getTransformedComposition();
        if (transformed != null)
        {
            actions = transformed.getActions();
        }
        else
        {
            net.runelite.api.NPCComposition base = npc.getComposition();
            if (base != null)
            {
                actions = base.getActions();
            }
        }

        if (actions == null)
        {
            return false;
        }

        boolean hasFishingAction = false;
        for (String action : actions)
        {
            if (action == null)
            {
                continue;
            }

            String a = action.trim().toLowerCase(Locale.ROOT);
            if (a.isEmpty())
            {
                continue;
            }

            if (a.contains(ATTACK_KEYWORD) || a.contains(TALK_KEYWORD))
            {
                return false;
            }

            if (a.contains("net") || a.contains("bait") || a.contains("lure")
                    || a.contains("harpoon") || a.contains("cage") || a.contains("rod"))
            {
                hasFishingAction = true;
            }
        }

        return hasFishingAction;
    }

    public boolean shouldRenderNametagForActor(Actor actor)
    {
        if (actor == null)
        {
            return false;
        }

        if (actor instanceof Player)
        {
            if (!respectEntityHiders)
            {
                return true;
            }

            if (visibleActorsThisFrame.contains(actor))
            {
                return true;
            }

            // If not visible but on a stacked tile with another visible player on the same tile,
            // assume this actor was only culled by client stacking and show its nametag.
            WorldPoint wp = ((Player) actor).getWorldLocation();
            if (wp != null && stackedTiles.contains(wp) && visiblePlayerTiles.contains(wp))
            {
                return true;
            }

            // Otherwise, likely hidden by an entity hider plugin, hide nametag
            return false;
        }
        else
        {
            // For NPCs, respect entity hiders as before
            if (!respectEntityHiders)
            {
                return true;
            }

            return !sawSceneActorThisFrame || visibleActorsThisFrame.contains(actor);
        }
    }

    boolean isActorVisibleThisFrame(Actor actor)
    {
        return visibleActorsThisFrame.contains(actor);
    }

    private static Set<String> parseNameSet(String raw)
    {
        Set<String> out = new HashSet<>();
        if (raw == null || raw.trim().isEmpty())
        {
            return out;
        }

        String[] parts = raw.split("[,;\\r\\n]+");
        for (String part : parts)
        {
            String normalized = normalizeName(part);
            if (!normalized.isEmpty())
            {
                out.add(normalized);
            }
        }

        return out;
    }

    private static String normalizeName(String raw)
    {
        String cleaned = stripNameMarkup(raw);
        return cleaned == null ? "" : cleaned.toLowerCase(Locale.ROOT);
    }

    // Strips formatting prefixes from names (e.g. <col=...>, <cox=...>) so
    // matching and rendering use the visible text only.
    String sanitizeEntityName(String raw)
    {
        return stripNameMarkup(raw);
    }

    private static String stripNameMarkup(String raw)
    {
        if (raw == null)
        {
            return null;
        }

        try
        {
            String cleaned = raw.replaceAll("<[^>]+>", "").trim();
            if (cleaned.isEmpty() || "null".equalsIgnoreCase(cleaned))
            {
                return null;
            }
            return cleaned;
        }
        catch (Exception ignored)
        {
            String cleaned = raw.trim();
            if (cleaned.isEmpty() || "null".equalsIgnoreCase(cleaned))
            {
                return null;
            }
            return cleaned;
        }
    }

    public boolean hasAttackOption(NPC npc)
    {
        if (npc == null)
        {
            return false;
        }

        int compId = npc.getId();

        if (persistentAttackable.contains(compId))
        {
            return true;
        }

        if (persistentNotAttackable.contains(compId))
        {
            return false;
        }

        try
        {
            MenuEntry[] entries = client.getMenu().getMenuEntries();
            if (entries != null)
            {
                boolean hasInteraction = false;
                for (MenuEntry e : entries)
                {
                    if (!menuEntryTargetsNpc(e, npc)) continue;
                    if (!NPC_INTERACTION_ACTIONS.contains(e.getType().getId())) continue;

                    hasInteraction = true;
                    if (isAttackInteractionOption(e.getOption()))
                    {
                        persistentAttackable.add(compId);
                        persistentNotAttackable.remove(compId);
                        return true;
                    }
                }

                if (hasInteraction)
                {
                    persistentNotAttackable.add(compId);
                    persistentAttackable.remove(compId);
                    return false;
                }
            }
        }
        catch (Exception ignored) {}

        net.runelite.api.NPCComposition transformed = npc.getTransformedComposition();
        if (transformed != null)
        {
            Boolean transformedAttackable = classifyAttackFromActions(transformed.getActions());
            if (transformedAttackable != null)
            {
                cacheAttackability(compId, transformedAttackable);
                return transformedAttackable;
            }
        }

        if (npc.getComposition() != null)
        {
            Boolean baseAttackable = classifyAttackFromActions(npc.getComposition().getActions());
            if (baseAttackable != null)
            {
                cacheAttackability(compId, baseAttackable);
                return baseAttackable;
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

        if (persistentTalkTo.contains(npc.getId()))
        {
            return true;
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
                    if (isTalkInteractionOption(a))
                    {
                        persistentTalkTo.add(npc.getId());
                        return true;
                    }
                }
                // Has real actions but no Talk-to entry.
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
                        if (isTalkInteractionOption(a))
                        {
                            persistentTalkTo.add(npc.getId());
                            return true;
                        }
                    }
                    // Transformed composition has real actions but no Talk-to entry.
                    return false;
                }
            }
        }

        // Both regular and transformed compositions are null/all-null (e.g. Ducklings).
        // We have no composition data to determine talk status. Check caches and live menu:

        // Live menu entries are only present while hovering this NPC.
        try
        {
            MenuEntry[] entries = client.getMenu().getMenuEntries();
            if (entries != null)
            {
                for (MenuEntry e : entries)
                {
                    if (!menuEntryTargetsNpc(e, npc)) continue;
                    int typeId = e.getType().getId();
                    if (NPC_INTERACTION_ACTIONS.contains(typeId))
                    {
                        String opt = e.getOption();
                        if (isTalkInteractionOption(opt))
                        {
                            persistentTalkTo.add(npc.getId());
                            return true;
                        }
                    }
                }
            }
        }
        catch (Exception ignored) {}

        return false;
    }

    public boolean hasNonTalkInteractionOption(NPC npc)
    {
        if (npc == null || hasTalkOption(npc))
        {
            return false;
        }

        if (persistentTalkable.contains(npc.getId()) && !persistentTalkTo.contains(npc.getId()))
        {
            return true;
        }

        net.runelite.api.NPCComposition composition = npc.getComposition();
        if (composition != null)
        {
            if (hasNonTalkInteraction(composition.getActions()))
            {
                persistentTalkable.add(npc.getId());
                return true;
            }
        }

        net.runelite.api.NPCComposition transformed = npc.getTransformedComposition();
        if (transformed != null)
        {
            if (hasNonTalkInteraction(transformed.getActions()))
            {
                persistentTalkable.add(npc.getId());
                return true;
            }
        }

        try
        {
            MenuEntry[] entries = client.getMenu().getMenuEntries();
            if (entries != null)
            {
                for (MenuEntry e : entries)
                {
                    if (!menuEntryTargetsNpc(e, npc)) continue;
                    int typeId = e.getType().getId();
                    if (NPC_INTERACTION_ACTIONS.contains(typeId) && isNonTalkInteractionOption(e.getOption()))
                    {
                        persistentTalkable.add(npc.getId());
                        return true;
                    }
                }
            }
        }
        catch (Exception ignored) {}

        return false;
    }

    public boolean hasTradeOption(NPC npc)
    {
        if (npc == null)
        {
            return false;
        }

        int compId = npc.getId();
        if (persistentTradeable.contains(compId))
        {
            return true;
        }

        net.runelite.api.NPCComposition composition = npc.getComposition();
        if (composition != null && hasTradeInteraction(composition.getActions()))
        {
            persistentTradeable.add(compId);
            return true;
        }

        net.runelite.api.NPCComposition transformed = npc.getTransformedComposition();
        if (transformed != null && hasTradeInteraction(transformed.getActions()))
        {
            persistentTradeable.add(compId);
            return true;
        }

        try
        {
            MenuEntry[] entries = client.getMenu().getMenuEntries();
            if (entries != null)
            {
                for (MenuEntry e : entries)
                {
                    if (!menuEntryTargetsNpc(e, npc)) continue;
                    if (!NPC_INTERACTION_ACTIONS.contains(e.getType().getId())) continue;
                    if (isTradeInteractionOption(e.getOption()))
                    {
                        persistentTradeable.add(compId);
                        return true;
                    }
                }
            }
        }
        catch (Exception ignored) {}

        return false;
    }

    @Subscribe
    public void onNpcSpawned(NpcSpawned event)
    {
        NPC npc = event.getNpc();
        if (npc != null)
        {
            trackedNpcs.put(npc.getIndex(), npc);
            observedAggressiveNpcInstances.remove(npc.getIndex());
            playerProvokedNpcInstances.remove(npc.getIndex());
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
            observedAggressiveNpcInstances.remove(npc.getIndex());
            playerProvokedNpcInstances.remove(npc.getIndex());
            // persistentTalkable / persistentNotTalkable are keyed on composition ID
            // (NPC type), not instance index — intentionally not cleared on despawn
            // so the classification applies to all instances of the same NPC type
            // and survives stairs / zone transitions.
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        if (event == null)
        {
            return;
        }

        try
        {
            MenuAction action = event.getMenuAction();
            if (action == null || !NPC_INTERACTION_ACTIONS.contains(action.getId()))
            {
                return;
            }

            if (!isAttackInteractionOption(event.getMenuOption()))
            {
                return;
            }

            int npcIndex = event.getId();
            if (npcIndex >= 0)
            {
                playerProvokedNpcInstances.add(npcIndex);
            }
        }
        catch (Exception ignored) {}
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
            updateRenderCallbackRegistration();
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() != GameState.LOGGED_IN)
        {
            clearTransientSceneState();
            return;
        }

        showUpdateNoticeIfReady();
    }

    @Subscribe
    public void onBeforeRender(BeforeRender event)
    {
        visibleActorsThisFrame.clear();
        sawSceneActorThisFrame = false;
    }

    @Subscribe
    public void onPluginChanged(PluginChanged event)
    {
        if (event.getPlugin() == this || !respectEntityHiders)
        {
            return;
        }

        updateRenderCallbackRegistration();
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event)
    {
        try
        {
            int typeId = event.getType();
            String sanitizedTarget = sanitizeTarget(event.getTarget());

            // Update hover state immediately (fires every frame).
            // Only set on entity entries; onGameTick clears when
            // the mouse moves away from any entity.
            if (ENTITY_MENU_ACTIONS.contains(typeId))
            {
                hoverIndex = event.getIdentifier();
                hoverTarget = sanitizedTarget;
            }

            // Cache interaction options for NPCs as the menu is built so we can classify
            // them even before the player explicitly right-clicks.
            String op = event.getOption();
            // Cache using composition ID so all instances of the same NPC type benefit.
            if (NPC_INTERACTION_ACTIONS.contains(typeId))
            {
                NPC trackedNpc = trackedNpcs.get(event.getIdentifier());
                if (trackedNpc == null)
                {
                    String targetName = sanitizeTarget(event.getTarget());
                    if (targetName != null)
                    {
                        for (NPC candidate : trackedNpcs.values())
                        {
                            String candidateName = getNpcDisplayName(candidate);
                            if (candidateName != null && candidateName.equalsIgnoreCase(targetName))
                            {
                                trackedNpc = candidate;
                                break;
                            }
                        }
                    }
                }
                if (trackedNpc != null)
                {
                    int compId = trackedNpc.getId();
                    if (isTradeInteractionOption(op))
                    {
                        persistentTradeable.add(compId);
                    }
                    if (isAttackInteractionOption(op))
                    {
                        cacheAttackability(compId, true);
                        persistentNotTalkable.add(compId);
                    }
                    else if (isFriendlyInteractionOption(op))
                    {
                        persistentTalkable.add(compId);
                        if (isTalkInteractionOption(op))
                        {
                            persistentTalkTo.add(compId);
                        }
                        persistentNotTalkable.remove(compId);
                        cacheAttackability(compId, false);
                    }
                }
            }
        }
        catch (Exception ignored) {}
    }

    private String sanitizeTarget(String raw)
    {
        String s = stripNameMarkup(raw);
        if (s == null)
        {
            return null;
        }

        try
        {
            // Strip combat-level suffix that appears in player targets: " (level-126)"
            s = s.replaceAll("(?i)\\s*\\(level-\\d+\\)\\s*$", "").trim();
            // Strip common action prefixes that sometimes appear on target strings
            s = s.replaceAll("(?i)^(talk-?to\\s+|examine\\s+|walk here\\s+)", "").trim();
            return s.isEmpty() ? null : s;
        }
        catch (Exception ignored)
        {
            return s;
        }
    }

    private boolean menuEntryTargetsNpc(MenuEntry entry, NPC npc)
    {
        if (entry == null || npc == null)
        {
            return false;
        }

        if (entry.getIdentifier() == npc.getIndex())
        {
            return true;
        }

        String targetName = sanitizeTarget(entry.getTarget());
        String npcName = getNpcDisplayName(npc);
        return targetName != null && npcName != null && targetName.equalsIgnoreCase(npcName);
    }

    private NPC resolveTrackedNpcForMenuEntry(MenuEntry entry)
    {
        if (entry == null)
        {
            return null;
        }

        NPC trackedNpc = trackedNpcs.get(entry.getIdentifier());
        if (trackedNpc != null)
        {
            return trackedNpc;
        }

        String targetName = sanitizeTarget(entry.getTarget());
        if (targetName == null)
        {
            return null;
        }

        for (NPC candidate : trackedNpcs.values())
        {
            String candidateName = getNpcDisplayName(candidate);
            if (candidateName != null && candidateName.equalsIgnoreCase(targetName))
            {
                return candidate;
            }
        }

        return null;
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        syncTrackedNpcsFromScene();

        // Scan menu entries once per tick: cache NPC classification and update hover state.
        try
        {
            MenuEntry[] entries = client.getMenu().getMenuEntries();
            boolean hoverFound = false;
            if (entries != null)
            {
                // Per-NPC: track whether any interaction (FIRST–FIFTH) or examine entry was seen.
                java.util.Set<Integer> withInteraction = new java.util.HashSet<>();
                java.util.Set<Integer> withFriendly   = new java.util.HashSet<>();
                java.util.Set<Integer> withAttack     = new java.util.HashSet<>();
                java.util.Set<Integer> withTrade      = new java.util.HashSet<>();
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
                        NPC trackedNpc = resolveTrackedNpcForMenuEntry(e);
                        if (trackedNpc == null) continue;
                        int compId = trackedNpc.getId();

                        if (NPC_INTERACTION_ACTIONS.contains(typeId))
                        {
                            withInteraction.add(compId);
                            String opt = e.getOption();
                            if (isAttackInteractionOption(opt))
                            {
                                withAttack.add(compId);
                                cacheAttackability(compId, true);
                            }
                            if (isTradeInteractionOption(opt))
                            {
                                withTrade.add(compId);
                            }
                            if (isFriendlyInteractionOption(opt))
                            {
                                withFriendly.add(compId);
                                persistentTalkable.add(compId);
                                if (isTalkInteractionOption(opt))
                                {
                                    persistentTalkTo.add(compId);
                                }
                                persistentNotTalkable.remove(compId);
                                cacheAttackability(compId, false);
                            }
                        }
                        else // EXAMINE_NPC
                        {
                            examineOnly.add(compId);
                        }
                    }
                }

                // NPC types with interaction entries but no friendly action are not friendly.
                for (int compId : withInteraction)
                {
                    if (!withFriendly.contains(compId))
                    {
                        persistentNotTalkable.add(compId);
                    }

                    if (!withAttack.contains(compId))
                    {
                        cacheAttackability(compId, false);
                    }
                }

                for (int compId : withAttack)
                {
                    cacheAttackability(compId, true);
                }

                for (int compId : withTrade)
                {
                    persistentTradeable.add(compId);
                }

                // NPC types that appeared only via Examine are hidden by default.
                for (int compId : examineOnly)
                {
                    if (!withInteraction.contains(compId))
                    {
                        persistentNotTalkable.add(compId);
                        persistentTalkable.remove(compId);
                        cacheAttackability(compId, false);
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
                    if (!menuEntryTargetsNpc(e, npc)) continue;
                    int typeId = e.getType().getId();
                    if (NPC_INTERACTION_ACTIONS.contains(typeId))
                    {
                        hasInteraction = true;
                        String opt = e.getOption();
                        if (isTradeInteractionOption(opt))
                        {
                            persistentTradeable.add(npc.getId());
                        }
                        if (isFriendlyInteractionOption(opt))
                        {
                            persistentTalkable.add(npc.getId());  // composition ID
                            if (isTalkInteractionOption(opt))
                            {
                                persistentTalkTo.add(npc.getId());
                            }
                            persistentNotTalkable.remove(npc.getId());
                            cacheAttackability(npc.getId(), false);
                            return;
                        }
                        if (isAttackInteractionOption(opt))
                        {
                            cacheAttackability(npc.getId(), true);
                        }
                    }
                    else if (typeId == MenuAction.EXAMINE_NPC.getId())
                    {
                        hasExamine = true;
                    }
                }
                if (hasInteraction)
                {
                    persistentNotTalkable.add(npc.getId());  // composition ID
                }
                else if (hasExamine)
                {
                    persistentNotTalkable.add(npc.getId());
                    persistentTalkable.remove(npc.getId());
                    cacheAttackability(npc.getId(), false);
                }
            }
        }
        catch (Exception ignored) {}
    }

    private void cacheAttackability(int compId, boolean attackable)
    {
        if (attackable)
        {
            persistentAttackable.add(compId);
            persistentNotAttackable.remove(compId);
            return;
        }

        persistentNotAttackable.add(compId);
        persistentAttackable.remove(compId);
    }

    private Boolean classifyAttackFromActions(String[] actions)
    {
        if (actions == null)
        {
            return null;
        }

        boolean sawAction = false;
        for (String action : actions)
        {
            if (action == null)
            {
                continue;
            }

            sawAction = true;
            if (isAttackInteractionOption(action))
            {
                return true;
            }
        }

        return sawAction ? Boolean.FALSE : null;
    }

    private boolean isAttackInteractionOption(String option)
    {
        if (option == null)
        {
            return false;
        }

        String normalized = option.trim().toLowerCase(Locale.ROOT);
        return !normalized.isEmpty() && normalized.contains(ATTACK_KEYWORD);
    }

    private boolean isTalkInteractionOption(String option)
    {
        if (option == null)
        {
            return false;
        }

        String normalized = option.trim().toLowerCase(Locale.ROOT);
        return !normalized.isEmpty() && normalized.contains(TALK_KEYWORD);
    }

    private boolean hasNonTalkInteraction(String[] actions)
    {
        if (actions == null)
        {
            return false;
        }

        for (String action : actions)
        {
            if (isNonTalkInteractionOption(action))
            {
                return true;
            }
        }

        return false;
    }

    private boolean hasTradeInteraction(String[] actions)
    {
        if (actions == null)
        {
            return false;
        }

        for (String action : actions)
        {
            if (isTradeInteractionOption(action))
            {
                return true;
            }
        }

        return false;
    }

    private boolean isNonTalkInteractionOption(String option)
    {
        if (option == null)
        {
            return false;
        }

        String normalized = option.trim().toLowerCase(Locale.ROOT);
        return !normalized.isEmpty()
                && !normalized.contains(ATTACK_KEYWORD)
                && !normalized.contains(TALK_KEYWORD);
    }

    private boolean isTradeInteractionOption(String option)
    {
        if (option == null)
        {
            return false;
        }

        String normalized = option.trim().toLowerCase(Locale.ROOT);
        return !normalized.isEmpty() && normalized.contains(TRADE_KEYWORD);
    }

    private boolean isFriendlyInteractionOption(String option)
    {
        if (option == null)
        {
            return false;
        }

        String normalized = option.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty())
        {
            return false;
        }

        // Treat any non-attack interaction as "friendly" so NPCs like pickpocket/catch
        // are shown under the friendly category rather than being permanently culled.
        return !normalized.contains(ATTACK_KEYWORD);
    }
}
