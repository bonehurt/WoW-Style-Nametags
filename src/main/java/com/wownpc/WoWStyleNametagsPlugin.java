package com.wownpc;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Provides;
import java.awt.Color;
import java.util.Collection;
import java.util.HashSet;
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

@PluginDescriptor(name = "WoW-Style Nametags", description = "Overlays NPC/Player names above their heads, with optional color-coding and outlines & other options similar to World of Warcraft.", tags = {
        "NPC", "names", "overlay", "WoW", "nametags", "nametag", "max hit", "level" })
public class WoWStyleNametagsPlugin extends Plugin {
    private static final String CURRENT_VERSION = "2.1.1";
    private static final String UPDATE_NOTICE_VERSION_KEY = "updateNoticeVersion";
    private static final String UPDATE_NOTICE_TEXT = "Added 'Max nametags per tile', 'Combat level', 'Max hit' & 'Hide boss nametags' settings + Aggro tweaks + some bugfixes!";
    private boolean updateNoticePending = false;

    // Checks if an update notification should be shown in chat for the current
    // version.
    private boolean shouldShowUpdateNotice() {
        String seenVersion = configManager.getConfiguration(WoWStyleNametagsConfig.GROUP, UPDATE_NOTICE_VERSION_KEY);
        return !CURRENT_VERSION.equals(seenVersion);
    }

    // Sends a one-time chat update message when the player is logged in.
    private void showUpdateNoticeIfReady() {
        if (!updateNoticePending || client.getGameState() != GameState.LOGGED_IN) {
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
            MenuAction.PLAYER_FIFTH_OPTION.getId());

    // --- Hover state (written by onMenuEntryAdded / onGameTick, read by overlay)
    int hoverIndex = -1;
    String hoverTarget;

    // --- Cached config values (refreshed on startUp and onConfigChanged) ---
    boolean hoverOnly;
    boolean anchorBelow;
    int verticalOffset;
    int maxEntities;
    boolean stackTags;
    boolean respectEntityHiders;
    int maxNametagsPerTile;

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

    boolean enablePets;
    Color petsColour;
    int petsFontSize;
    boolean petsOutlineEnabled;
    Color petsOutlineColour;
    int petsOutlineThickness;

    boolean enableHunterMobs;
    Color hunterMobsColour;
    int hunterMobsFontSize;
    boolean hunterMobsOutlineEnabled;
    Color hunterMobsOutlineColour;
    int hunterMobsOutlineThickness;

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
    Set<String> includedNpcNames;
    Set<String> includedPlayerNames;
    boolean hideBosses;
    boolean hideBossMinions;
    boolean hideQuestBosses;

    // Combat Levels
    boolean showPlayerCombatLevel;
    boolean showNpcCombatLevel;
    boolean combatLevelInCombatOnly;
    int minCombatLevel;
    CombatLevelFormat combatLevelFormat;
    CombatLevelPosition combatLevelPosition;
    boolean matchNameColour;
    Color combatLevelColour;
    Color combatLevelOutlineColour;

    // NPC Max Hit
    boolean showNpcMaxHit;
    boolean maxHitInCombatOnly;
    int minNpcMaxHit;
    NpcMaxHitFormat npcMaxHitFormat;
    NpcMaxHitPosition npcMaxHitPosition;
    boolean matchMaxHitNameColour;
    Color npcMaxHitColour;
    Color npcMaxHitOutlineColour;

    // --- Runtime NPC tracking ---
    private final Map<Integer, NPC> trackedNpcs = new ConcurrentHashMap<>();
    private final Set<Actor> visibleActorsThisFrame = ConcurrentHashMap.newKeySet();
    final Set<WorldPoint> stackedTiles = ConcurrentHashMap.newKeySet();
    final Set<WorldPoint> visiblePlayerTiles = ConcurrentHashMap.newKeySet();
    private NpcClassifier npcClassifier;

    int overheadIconOffset;

    // --- Transient scene state (cleared on game state changes) ---
    private boolean sawSceneActorThisFrame = false;
    private final RenderCallback visibilityTracker = new RenderCallback() {
        @Override
        public boolean addEntity(Renderable renderable, boolean ui) {
            if (!ui && renderable instanceof Actor) {
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

    @Inject
    private NpcMaxHitManager npcMaxHitManager;

    @Provides
    WoWStyleNametagsConfig providesConfig(ConfigManager configManager) {
        return configManager.getConfig(WoWStyleNametagsConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        npcClassifier = new NpcClassifier(client);
        cacheConfig();
        npcMaxHitManager.preloadAll();
        clearTransientSceneState();
        updateRenderCallbackRegistration();
        updateNoticePending = shouldShowUpdateNotice();
        overlayManager.add(overlay);
        showUpdateNoticeIfReady();
    }

    @Override
    protected void shutDown() throws Exception {
        renderCallbackManager.unregister(visibilityTracker);
        clearTransientSceneState();
        if (npcClassifier != null) {
            npcClassifier.clearCaches();
        }
        if (npcMaxHitManager != null) {
            npcMaxHitManager.clearCache();
        }
        updateNoticePending = false;
        overlayManager.remove(overlay);
    }

    private void cacheConfig() {
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
        enablePets = config.enablePets();
        enableHunterMobs = config.enableHunterMobs();

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
        includedNpcNames = parseNameSet(config.includedNpcNames());
        includedPlayerNames = parseNameSet(config.includedPlayerNames());
        hideBosses = config.hideBosses();
        hideBossMinions = config.hideBossMinions();
        hideQuestBosses = config.hideQuestBosses();

        // Positioning
        verticalOffset = config.verticalOffset();
        maxEntities = config.maxEntities();
        stackTags = config.stackTags();
        maxNametagsPerTile = config.maxNametagsPerTile();

        // Combat Levels
        showPlayerCombatLevel = config.showPlayerCombatLevel();
        showNpcCombatLevel = config.showNpcCombatLevel();
        combatLevelInCombatOnly = config.combatLevelInCombatOnly();
        minCombatLevel = config.minCombatLevel();
        combatLevelFormat = config.combatLevelFormat();
        combatLevelPosition = config.combatLevelPosition();
        if (combatLevelPosition == null) {
            combatLevelPosition = CombatLevelPosition.AFTER;
        }
        matchNameColour = config.matchNameColour();
        combatLevelColour = config.combatLevelColour();
        if (combatLevelColour == null) {
            combatLevelColour = Color.WHITE;
        }
        combatLevelOutlineColour = config.combatLevelOutlineColour();
        if (combatLevelOutlineColour == null) {
            combatLevelOutlineColour = Color.BLACK;
        }

        // NPC Max Hit
        showNpcMaxHit = config.showNpcMaxHit();
        maxHitInCombatOnly = config.maxHitInCombatOnly();
        minNpcMaxHit = config.minNpcMaxHit();
        npcMaxHitFormat = config.npcMaxHitFormat();
        if (npcMaxHitFormat == null) {
            npcMaxHitFormat = NpcMaxHitFormat.BRACKETS_MH;
        }
        npcMaxHitPosition = config.npcMaxHitPosition();
        if (npcMaxHitPosition == null) {
            npcMaxHitPosition = NpcMaxHitPosition.AFTER;
        }
        matchMaxHitNameColour = config.matchMaxHitNameColour();
        npcMaxHitColour = config.npcMaxHitColour();
        if (npcMaxHitColour == null) {
            npcMaxHitColour = Color.WHITE;
        }
        npcMaxHitOutlineColour = config.npcMaxHitOutlineColour();
        if (npcMaxHitOutlineColour == null) {
            npcMaxHitOutlineColour = Color.BLACK;
        }
        if (showNpcMaxHit && npcMaxHitManager != null) {
            npcMaxHitManager.preloadAll();
        }

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
        petsColour = config.petsColour();
        petsFontSize = config.petsFontSize();
        hunterMobsColour = config.hunterMobsColour();
        hunterMobsFontSize = config.hunterMobsFontSize();
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

        petsOutlineEnabled = config.petsOutlineEnabled();
        petsOutlineColour = config.petsOutlineColour();
        petsOutlineThickness = config.petsOutlineThickness();

        hunterMobsOutlineEnabled = config.hunterMobsOutlineEnabled();
        hunterMobsOutlineColour = config.hunterMobsOutlineColour();
        hunterMobsOutlineThickness = config.hunterMobsOutlineThickness();

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

    // Registers scene render callback to track which actor models are rendered on
    // top
    // and honor entity hiders.
    private void updateRenderCallbackRegistration() {
        renderCallbackManager.unregister(visibilityTracker);
        visibleActorsThisFrame.clear();
        sawSceneActorThisFrame = false;
        renderCallbackManager.register(visibilityTracker);
    }

    // Clears frame and per-scene caches on world/scene transitions.
    private void clearTransientSceneState() {
        trackedNpcs.clear();
        hoverIndex = -1;
        hoverTarget = null;
        visibleActorsThisFrame.clear();
        sawSceneActorThisFrame = false;
        stackedTiles.clear();
        visiblePlayerTiles.clear();
        if (npcClassifier != null) {
            npcClassifier.clearTransientSceneState();
        }
    }

    // Synchronizes tracked NPCs from both top-level and player instanced world
    // views
    private void syncTrackedNpcsFromScene() {
        if (client == null) {
            return;
        }

        try {
            Set<Integer> liveIndexes = new HashSet<>();
            var tlwv = client.getTopLevelWorldView();
            var lp = client.getLocalPlayer();
            var pwv = lp != null ? lp.getWorldView() : null;

            java.util.List<net.runelite.api.WorldView> viewsToSync = new java.util.ArrayList<>();
            if (tlwv != null)
                viewsToSync.add(tlwv);
            if (pwv != null && pwv != tlwv)
                viewsToSync.add(pwv);

            for (var wv : viewsToSync) {
                for (NPC npc : wv.npcs()) {
                    if (npc == null) {
                        continue;
                    }

                    int index = npc.getIndex();
                    liveIndexes.add(index);
                    trackedNpcs.put(index, npc);
                }
            }

            trackedNpcs.keySet().removeIf(index -> !liveIndexes.contains(index));
        } catch (Exception ignored) {
        }
    }

    // --- Whitelist and Blacklist Filter Checks ---

    public boolean isNpcNameExcluded(String npcName) {
        return excludedNpcNames != null && excludedNpcNames.contains(normalizeName(npcName));
    }

    public boolean isPlayerNameExcluded(String playerName) {
        return excludedPlayerNames != null && excludedPlayerNames.contains(normalizeName(playerName));
    }

    public boolean isNpcNameIncluded(String npcName) {
        return includedNpcNames == null || includedNpcNames.isEmpty()
                || includedNpcNames.contains(normalizeName(npcName));
    }

    public boolean isPlayerNameIncluded(String playerName) {
        return includedPlayerNames == null || includedPlayerNames.isEmpty()
                || includedPlayerNames.contains(normalizeName(playerName));
    }

    /**
     * Determines whether an actor is actively engaged in combat.
     * Checks visible health bar, target interaction, and whether the local player
     * is targeting the actor.
     */
    public boolean isInCombat(Actor actor, Player localPlayer) {
        if (actor == null) {
            return false;
        }

        // Visible health bar indicates active damage
        try {
            if (actor.getHealthRatio() > 0) {
                return true;
            }
        } catch (Exception ignored) {
        }

        // Interacting with another entity (ignoring non-combat follower pets)
        try {
            Actor target = actor.getInteracting();
            if (target != null) {
                if (actor instanceof NPC) {
                    NPC npc = (NPC) actor;
                    if (npc.getComposition() != null && npc.getComposition().isFollower()) {
                        return false;
                    }
                }
                return true;
            }
        } catch (Exception ignored) {
        }

        // Local player is directly interacting with or attacking this actor
        try {
            if (localPlayer != null && localPlayer.getInteracting() != null
                    && localPlayer.getInteracting().equals(actor)) {
                return true;
            }
        } catch (Exception ignored) {
        }

        return false;
    }

    // Formats combat level string according to configured format style and
    // placement.
    public String formatCombatLevel(int level) {
        CombatLevelFormat format = combatLevelFormat != null ? combatLevelFormat : CombatLevelFormat.PARENTHESES;
        CombatLevelPosition position = combatLevelPosition != null ? combatLevelPosition : CombatLevelPosition.AFTER;
        return format.format(level, position);
    }

    public boolean isCombatLevelBefore() {
        return combatLevelPosition == CombatLevelPosition.BEFORE;
    }

    // Formats NPC max hit string according to configured format style and
    // placement.
    public String formatNpcMaxHit(int maxHit) {
        NpcMaxHitFormat format = npcMaxHitFormat != null ? npcMaxHitFormat : NpcMaxHitFormat.BRACKETS_MH;
        NpcMaxHitPosition position = npcMaxHitPosition != null ? npcMaxHitPosition : NpcMaxHitPosition.AFTER;
        return format.format(maxHit, position);
    }

    public boolean isNpcMaxHitBefore() {
        return npcMaxHitPosition == NpcMaxHitPosition.BEFORE;
    }

    public int getNpcMaxHit(int npcId) {
        return npcMaxHitManager != null ? npcMaxHitManager.getMaxHit(npcId) : -1;
    }

    // Checks whether an actor was actually rendered this frame, honoring entity
    // hider settings.
    public boolean shouldRenderNametagForActor(Actor actor) {
        if (actor == null) {
            return false;
        }

        if (actor instanceof Player) {
            if (!respectEntityHiders) {
                return true;
            }

            if (!sawSceneActorThisFrame || visibleActorsThisFrame.contains(actor)) {
                return true;
            }

            WorldPoint wp = ((Player) actor).getWorldLocation();
            if (wp != null && stackedTiles.contains(wp) && visiblePlayerTiles.contains(wp)) {
                return true;
            }

            return false;
        } else {
            // For NPCs, respect entity hiders as before
            if (!respectEntityHiders) {
                return true;
            }

            return !sawSceneActorThisFrame || visibleActorsThisFrame.contains(actor);
        }
    }

    boolean isActorVisibleThisFrame(Actor actor) {
        return visibleActorsThisFrame.contains(actor);
    }

    // Parses comma-separated or newline-delimited config lists into a normalized
    // set.
    private static Set<String> parseNameSet(String raw) {
        Set<String> out = new HashSet<>();
        if (raw == null || raw.trim().isEmpty()) {
            return out;
        }

        String[] parts = raw.split("[,;\\r\\n]+");
        for (String part : parts) {
            String normalized = normalizeName(part);
            if (!normalized.isEmpty()) {
                out.add(normalized);
            }
        }

        return out;
    }

    static String normalizeName(String raw) {
        return NpcClassifier.normalizeName(raw);
    }

    static String sanitizeEntityName(String raw) {
        return NpcClassifier.sanitizeEntityName(raw);
    }

    static String stripNameMarkup(String raw) {
        return NpcClassifier.stripNameMarkup(raw);
    }

    // --- Event Subscriptions ---

    // Tracks newly spawned NPCs and primes initial classification.
    @Subscribe
    public void onNpcSpawned(NpcSpawned event) {
        NPC npc = event.getNpc();
        if (npc != null) {
            trackedNpcs.put(npc.getIndex(), npc);
            if (npcClassifier != null) {
                npcClassifier.removeNpcInstance(npc.getIndex());
                npcClassifier.scanMenuForNpc(npc);
            }
        }
    }

    // Removes despawned NPCs and clears instance-specific tracking.
    @Subscribe
    public void onNpcDespawned(NpcDespawned event) {
        NPC npc = event.getNpc();
        if (npc != null) {
            trackedNpcs.remove(npc.getIndex());
            if (npcClassifier != null) {
                npcClassifier.removeNpcInstance(npc.getIndex());
            }
        }
    }

    // Forwards interaction clicks to classifier for aggression and interaction
    // tracking.
    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        if (npcClassifier != null) {
            npcClassifier.onMenuOptionClicked(event);
        }
    }

    // Updates cached reference when an NPC's composition changes.
    @Subscribe
    public void onNpcChanged(NpcChanged event) {
        NPC npc = event.getNpc();
        if (npc != null) {
            trackedNpcs.put(npc.getIndex(), npc);
        }
    }

    public Collection<NPC> getTrackedNpcs() {
        return trackedNpcs.values();
    }

    // Refreshes cached settings when user updates plugin configuration.
    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (WoWStyleNametagsConfig.GROUP.equals(event.getGroup())) {
            cacheConfig();
            updateRenderCallbackRegistration();
        }
    }

    // Clears transient state on hop/logout and displays pending update notes on
    // login.
    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        if (event.getGameState() != GameState.LOGGED_IN) {
            clearTransientSceneState();
            return;
        }

        showUpdateNoticeIfReady();
    }

    // Resets actor visibility buffer before each scene render.
    @Subscribe
    public void onBeforeRender(BeforeRender event) {
        visibleActorsThisFrame.clear();
        sawSceneActorThisFrame = false;
        stackedTiles.clear();
        visiblePlayerTiles.clear();
    }

    // Re-evaluates render callback registration if an external plugin is enabled or
    // disabled.
    @Subscribe
    public void onPluginChanged(PluginChanged event) {
        if (event.getPlugin() == this) {
            return;
        }

        updateRenderCallbackRegistration();
    }

    // Processes new menu entries for instant hover detection and classifier
    // updates.
    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event) {
        try {
            int typeId = event.getType();
            String sanitizedTarget = sanitizeTarget(event.getTarget());

            // Update hover state immediately (fires every frame).
            // Only set on entity entries; onGameTick clears when
            // the mouse moves away from any entity.
            if (ENTITY_MENU_ACTIONS.contains(typeId)) {
                hoverIndex = event.getIdentifier();
                hoverTarget = sanitizedTarget;
            }

            if (npcClassifier != null) {
                NPC trackedNpc = resolveTrackedNpc(event.getIdentifier(), event.getTarget());
                npcClassifier.processMenuEntryAdded(typeId, event.getOption(), trackedNpc);
            }
        } catch (Exception ignored) {
        }
    }

    static String sanitizeTarget(String raw) {
        return NpcClassifier.sanitizeTarget(raw);
    }

    // Resolves a tracked NPC by index identifier or matching sanitized name
    // fallback.
    private NPC resolveTrackedNpc(int identifier, String rawTarget) {
        NPC trackedNpc = trackedNpcs.get(identifier);
        if (trackedNpc != null) {
            return trackedNpc;
        }

        String targetName = sanitizeTarget(rawTarget);
        if (targetName == null) {
            return null;
        }

        for (NPC candidate : trackedNpcs.values()) {
            String candidateName = npcClassifier != null ? npcClassifier.getNpcDisplayName(candidate)
                    : sanitizeEntityName(candidate.getName());
            if (candidateName != null && candidateName.equalsIgnoreCase(targetName)) {
                return candidate;
            }
        }

        return null;
    }

    private NPC resolveTrackedNpcForMenuEntry(MenuEntry entry) {
        return entry == null ? null : resolveTrackedNpc(entry.getIdentifier(), entry.getTarget());
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        syncTrackedNpcsFromScene();

        // Scan menu entries once per tick: cache NPC classification and update hover
        // state.
        try {
            MenuEntry[] entries = client.getMenu().getMenuEntries();
            boolean hoverFound = false;
            if (entries != null) {
                for (MenuEntry e : entries) {
                    int typeId = e.getType().getId();
                    int instanceIndex = e.getIdentifier();

                    // Hover tracking uses instance index (for targeting/highlighting).
                    if (!hoverFound && ENTITY_MENU_ACTIONS.contains(typeId)) {
                        hoverIndex = instanceIndex;
                        hoverTarget = sanitizeTarget(e.getTarget());
                        hoverFound = true;
                        break;
                    }
                }

                if (npcClassifier != null) {
                    npcClassifier.scanLiveMenuEntries(entries, this::resolveTrackedNpcForMenuEntry);
                }
            }
            if (!hoverFound) {
                hoverIndex = -1;
                hoverTarget = null;
            }
        } catch (Exception ignored) {
        }
    }

    public NpcClassifier getNpcClassifier() {
        return npcClassifier;
    }
}
