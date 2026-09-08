package com.wownpc;

import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.events.MenuOptionClicked;

/**
 * Handles NPC classification heuristics (Aggressive, Passive, Neutral,
 * Friendly, Friendly non-talkers, Animals, Hunter mobs, Shopkeepers, etc.)
 * along with persistent menu and interaction caches.
 */
public class NpcClassifier {
    private static final String ATTACK_KEYWORD = "attack";
    private static final String TALK_KEYWORD = "talk";
    private static final String TRADE_KEYWORD = "trade";
    private static final String FISHING_SPOT_KEYWORD = "fishing spot";

    private static final Set<Integer> NPC_INTERACTION_ACTIONS = ImmutableSet.of(
            MenuAction.NPC_FIRST_OPTION.getId(),
            MenuAction.NPC_SECOND_OPTION.getId(),
            MenuAction.NPC_THIRD_OPTION.getId(),
            MenuAction.NPC_FOURTH_OPTION.getId(),
            MenuAction.NPC_FIFTH_OPTION.getId());

    private static final Set<String> ANIMAL_NAMES = ImmutableSet.of(
            "dog", "stray dog", "puppy", "mutt",
            "cat", "kitten", "hellcat", "lazy cat", "wily cat", "overgrown cat",
            "overgrown hellcat",
            "dairy cow",
            "sheep", "golden sheep",
            "pig", "piglet",
            "hen",
            "duckling",
            "pelican", "penguin",
            "camel",
            "squirrel",
            "horse",
            "parrot");

    private static final Set<String> HUNTER_MOB_NAMES = ImmutableSet.of(
            // Bird snaring
            "crimson swift", "golden warbler", "copper longtail", "cerulean twitch", "tropical wagtail",
            // Kebbits
            "polar kebbit", "common kebbit", "wild kebbit", "barb-tailed kebbit", "prickly kebbit",
            "sabre-toothed kebbit", "spotted kebbit", "dark kebbit", "dashing kebbit", "razor-backed kebbit",
            // Trapping & Tracking
            "chinchompa", "carnivorous chinchompa", "black chinchompa", "ferret", "embertailed jerboa",
            "pyre fox", "sunlight antelope", "moonlight antelope", "herbiboar", "feldip weasel",
            "desert devil", "swamp lizard", "orange salamander", "red salamander", "black salamander",
            "tecu salamander", "spined larupia", "horned graahk", "sabre-toothed kyatt", "wild broav",
            "letvek", "maniacal monkey", "moss lizard", "white rabbit", "stymphike",
            // Aerial fishing / Cormorant
            "bluegill", "common tench", "mottled eel", "greater siren",
            // Crab trapping
            "red crab", "blue crab", "rainbow crab",
            // Butterflies & Moths
            "ruby harvest", "sapphire glacialis", "snowy knight", "black warlock", "sunlight moth", "moonlight moth");

    private static final Set<String> BOSS_NAMES = ImmutableSet.of(
            // God Wars Dungeon
            "general graardor", "kree'arra", "commander zilyana", "k'ril tsutsaroth", "nex",
            // Chambers of Xeric (CoX)
            "great olm", "great olm (left hand)", "great olm (right hand)", "tekton", "vasa nistirio",
            "vanguard", "muttadile", "vespula", "skeletal mystic", "ice demon",
            // Theatre of Blood (ToB)
            "the maiden of sugadinti", "maiden of sugadinti", "the maiden of suginti", "maiden of suginti",
            "pestilent bloat", "nylocas vasilias", "sotetseg", "xarpus", "verzik vitur",
            // Tombs of Amascut (ToA)
            "ba-ba", "kephri", "akkha", "zebak", "tumeken's warden", "elidinis' warden",
            // Desert Treasure II
            "duke sucellus", "the leviathan", "the whisperer", "vardorvis",
            "awakened duke sucellus", "awakened the leviathan", "awakened the whisperer", "awakened vardorvis",
            // The Nightmare
            "the nightmare", "phosani's nightmare",
            // Gauntlet
            "crystalline hunllef", "corrupted hunllef",
            // Wilderness Bosses & Demi-Bosses
            "callisto", "artio", "venenatis", "spindel", "vet'ion", "calvar'ion",
            "chaos elemental", "chaos fanatic", "crazy archaeologist", "scorpia", "king black dragon",
            "revenant maledictus",
            // Slayer Bosses
            "alchemical hydra", "cerberus", "dusk", "dawn", "kraken", "cave kraken",
            "thermonuclear smoke devil", "abyssal sire", "araxxor",
            // Instanced & Solo Bosses
            "zulrah", "vorkath", "phantom muspah", "corporeal beast", "kalphite queen",
            "giant mole", "scurrius", "deranged archaeologist", "bryophyta", "obor",
            "hespori", "the mimic", "mimic", "skotizo", "sarachnis", "penance queen",
            // Skilling / Minigame Bosses
            "tempoross", "wintertodt", "zalcano",
            // TzHaar
            "tztok-jad", "tzkal-zuk",
            // Varlamore / Fortis / Perilous Moons
            "sol heredit", "the hueycoatl", "amoxliatl", "blood moon", "blue moon", "eclipse moon",
            // Varlamore Delve & Future Bosses
            "doom of mokhaiotl", "doom of mokhaiotl (shielded)", "doom of mokhaiotl (burrowed)",
            "tormented demon", "yama", "judge of yama", "judge of yama (a kingdom divided)",
            // Barrows Brothers
            "ahrim the blighted", "dharok the wretched", "guthan the infested",
            "karil the tainted", "torag the corrupted", "verac the defiled",
            // Dagannoth Kings
            "dagannoth prime", "dagannoth rex", "dagannoth supreme");

    private static final Set<String> QUEST_BOSS_NAMES = ImmutableSet.of(
            // Grandmaster & Quest Bosses
            "galvek", "glough", "fragment of seren", "vanstrom klause", "the inadequacy",
            "the everlasting", "the untouchable", "the illusive", "ranis drakan", "lowerniel drakan",
            "robert the strong", "culinaromancer", "agrith naar", "agrith-na-na",
            "flambeed", "karamel", "dessourt", "gelatinnoth mother", "dagannoth mother",
            "treus dayth", "barrelchest", "damis", "fareed", "kamil", "dessous",
            "elvarg", "delrith", "count draynor", "jungle demon", "black knight titan",
            "nezikchened", "giant scarab", "giant roc", "giant sea snake", "glod",
            "ice troll king", "koschei the deathless", "sea troll queen", "slagilith",
            "slash bash", "slug prince", "tarn", "xamphur", "the draugen", "chronozon");

    private static final Set<String> BOSS_MINION_NAMES = ImmutableSet.of(
            // God Wars Dungeon Bodyguards
            "sergeant strongstack", "sergeant steelwill", "sergeant grimspike",
            "flight kilisa", "wingman skree", "flockleader geerin",
            "starlight", "bree", "growler",
            "balfrug kreeyath", "zakl'n grit", "zakl'n gritch", "tstanon karlak",
            // Nex adds
            "blood reaver", "fumus", "umbra", "cruor", "glacies",
            // Zulrah
            "snakeling",
            // Vorkath
            "zombified spawn",
            // Corporeal Beast
            "dark energy core",
            // Abyssal Sire
            "scion", "respiratory system", "spawn",
            // Grotesque Guardians
            "energy sphere",
            // Cerberus
            "summoned soul",
            // Kraken
            "enormous tentacle", "tentacle",
            // Araxxor
            "mirrorback araxyte", "acidic araxyte", "exploding araxyte",
            // Sarachnis
            "spawn of sarachnis",
            // Bryophyta
            "growthling",
            // Scorpia
            "scorpia's offspring", "scorpia's guardian",
            // Kalphite Queen
            "kalphite larva", "kalphite worker", "kalphite soldier", "kalphite guardian",
            // The Nightmare / Phosani's Nightmare
            "parasite", "husk", "sleepwalker", "totem",
            // TzHaar / Inferno
            "yt-hurkot", "jal-ak", "jal-mejrah", "jal-akrek-mej", "jal-akrek-xil", "jal-akrek-ket",
            "jal-imkot", "jal-xil", "jal-zek", "jal-nib", "jal-nib-rek", "ancestral glyph",
            // ToA Minions
            "baboon brawler", "baboon thrower", "baboon mage", "baboon shaman", "cursed baboon", "volatile baboon",
            "scarab swarm", "spitting scarab", "soldier scarab", "arcane scarab", "agile scarab", "megascarab",
            "shadow of akkha", "kephri's phantom", "zebak's phantom", "ba-ba's phantom", "akkha's phantom",
            "energy siphon",
            // ToB Minions
            "nylocas matomenos", "nylocas ischyros", "nylocas toxobolos", "nylocas hagios", "nylocas athanatos",
            "nylocas prinkipas", "blood spawn",
            // CoX Minions
            "great olm (left hand)", "great olm (right hand)", "meat tree", "lux grub", "vespine soldier",
            "abyssal portal", "glowing crystal", "jewelled crab",
            // DT2 Minions
            "abyssal pathfinder", "eye of the leviathan", "vitreous jelly", "vitreous warp", "vitreous hand",
            "extremity", "gaze", "lost soul", "alluring vita", "corrupted vita",
            // Varlamore Minions & Delve (Doom of Mokhaiotl)
            "jagged stone", "tail of the hueycoatl", "minion of sol", "hueycoatl hatchling",
            "volatile earth", "demonic larva", "demonic range larva", "demonic magic larva", "demonic melee larva",
            "giant demonic range larva", "giant demonic magic larva",
            // Moons of Peril / Varlamore adds
            "blood jaguar",
            // Royal Titans adds
            "fire elemental", "ice elemental",
            // Maggot King adds
            "carrion", "maggot marquess", "ur-maggot larvae",
            // Yama adds
            "disciple of yama", "void flare",
            // Wilderness & Muspah Minions
            "venenatis spiderling", "spindel spiderling", "dark spikes",
            "skeleton hellhound", "greater skeleton hellhound",
            // Mimic & Skotizo adds
            "third age minion", "awakened altar", "reanimated demon");

    private static final Set<String> PET_NAMES = ImmutableSet.of(
            // Boss Pets
            "abyssal orphan", "baby mole", "baron", "butch", "callisto cub", "cerberus pup", "chompy chick",
            "corrupted youngllef", "dark core", "general ahrim", "general graardor jr.", "ikkle hydra",
            "jal-nib-rek", "kalphite princess", "lil' creator", "lil' zik", "lil'viathan", "little nightmare",
            "midnight", "muphin", "nexling", "noon", "olmlet", "pet chaos elemental", "pet dagannoth prime",
            "pet dagannoth rex", "pet dagannoth supreme", "pet dark core", "pet general graardor", "pet kree'arra",
            "pet k'ril tsutsaroth", "pet kraken", "pet penance queen", "pet smoke devil", "pet snakeling",
            "pet zilyana", "prince black dragon", "scurry", "skotizo", "skotos", "smolcano", "smokey", "sraracha",
            "tiny tempor", "tumeken's guardian", "elidinis' guardian", "tzrek-jad", "tzrek-zuk", "venenatis spiderling",
            "vet'ion jr.", "vorki", "wisp", "youngllef", "herbi", "hellpuppy", "nid", "moxi", "dom", "huberte",
            "smol heredit", "yami", "bran",
            // Skilling Pets
            "abyssal protector", "baby chinchompa", "beaver", "bloodhound", "giant squirrel",
            "heron", "phoenix", "rift guardian", "rock golem", "rocky", "tangleroot", "quetzin",
            // Other / House Pets
            "broav", "clockwork cat", "pet cat", "pet dog", "pet fish", "pet rock", "toy cat",
            "chameleon", "gecko", "iguana", "platypus", "raccoon", "vulture",
            "cat", "kitten", "hellcat", "overgrown cat", "lazy cat", "wily cat");

    // Inherently passive farm/domestic animals that never attack players, even if a
    // level 3 player is nearby.
    private static final Set<String> INHERENTLY_PASSIVE_NPC_NAMES = ImmutableSet.of(
            "cow", "cow calf", "dairy cow", "chicken", "rooster", "duck", "ducklings",
            "sheep", "lamb", "ram", "goat", "rabbit");

    // Strictly universally always-aggressive NPCs that attack ANY player regardless
    // of combat level (including level 126).
    // Level-dependent monsters (such as Tribesmen, Goblins, or Jogres) rely on
    // dynamic level scaling (playerLevel > npcLevel * 2).
    private static final Set<String> ALWAYS_AGGRESSIVE_NPC_NAMES = ImmutableSet.<String>builder()
            .add("abyssal guardian", "abyssal leech", "abyssal walker", "acidic araxyte", "araxyte",
                    "armoured zombie", "artio", "callisto")
            .add("calvar'ion", "chaos elemental", "chaos fanatic", "corrupted scorpion",
                    "crazy archaeologist", "crystalline scorpion", "dark beast", "demonic gorilla")
            .add("deranged archaeologist", "dreadborn araxyte", "elder chaos druid", "feral vampyre",
                    "gorak", "jal-ak", "jal-akrek-ket", "jal-akrek-mej")
            .add("jal-akrek-xil", "jal-imkot", "jal-mejrah", "jal-nib",
                    "jal-xil", "jal-zek", "ket-zek", "lava dragon")
            .add("lizardman shaman", "mirrorback araxyte", "revenant dark beast", "revenant demon",
                    "revenant dragon", "revenant goblin", "revenant hellhound", "revenant hobgoblin")
            .add("revenant imp", "revenant knight", "revenant maledictus", "revenant ork",
                    "revenant pyrefiend", "scorpia", "scorpia's offspring", "spindel")
            .add("spindel's spiderling", "tok-xil", "tortured gorilla", "tzkal-zuk",
                    "tz-kek", "tz-kih", "tztok-jad", "vampyre juvinate")
            .add("venenatis", "vet'ion", "vyrewatch", "vyrewatch sentinel",
                    "wall beast", "warped terrorbird", "yt-mejkot")
            .build();

    private final Client client;

    // --- Persistent interaction caches (by NPC composition ID) ---
    // Remembers classification across despawn/spawn cycles.
    private final Set<Integer> persistentAttackable = ConcurrentHashMap.newKeySet();
    private final Set<Integer> persistentNotAttackable = ConcurrentHashMap.newKeySet();
    private final Set<Integer> persistentTalkTo = ConcurrentHashMap.newKeySet();
    private final Set<Integer> persistentTalkable = ConcurrentHashMap.newKeySet();
    private final Set<Integer> persistentNotTalkable = ConcurrentHashMap.newKeySet();
    private final Set<Integer> persistentTradeable = ConcurrentHashMap.newKeySet();
    private final Set<Integer> persistentPets = ConcurrentHashMap.newKeySet();
    private final Set<Integer> persistentHunterMobs = ConcurrentHashMap.newKeySet();

    // --- Aggression tracking ---
    // Tracks NPCs that actively attacked or were provoked by the local player.
    private final Set<Integer> observedAggressiveNpcInstances = ConcurrentHashMap.newKeySet();
    private final Set<Integer> persistentObservedAggressiveTypes = ConcurrentHashMap.newKeySet();
    private final Set<Integer> playerProvokedNpcInstances = ConcurrentHashMap.newKeySet();

    public NpcClassifier(Client client) {
        this.client = client;
    }

    public void clearTransientSceneState() {
        observedAggressiveNpcInstances.clear();
        playerProvokedNpcInstances.clear();
    }

    public void clearCaches() {
        persistentAttackable.clear();
        persistentNotAttackable.clear();
        persistentTalkTo.clear();
        persistentTalkable.clear();
        persistentNotTalkable.clear();
        persistentTradeable.clear();
        persistentPets.clear();
        persistentHunterMobs.clear();
        observedAggressiveNpcInstances.clear();
        persistentObservedAggressiveTypes.clear();
        playerProvokedNpcInstances.clear();
    }

    public void removeNpcInstance(int index) {
        if (index >= 0) {
            observedAggressiveNpcInstances.remove(index);
            playerProvokedNpcInstances.remove(index);
        }
    }

    // Records when the player attacks an NPC to avoid falsely flagging retaliating
    // NPCs as naturally aggressive.
    public void recordPlayerAttack(int npcIndex) {
        if (npcIndex >= 0) {
            playerProvokedNpcInstances.add(npcIndex);
        }
    }

    // Remembers an NPC type as aggressive when observed attacking unprovoked.
    public void rememberAggressiveNpcType(NPC npc) {
        if (npc == null) {
            return;
        }

        try {
            int index = npc.getIndex();
            if (index >= 0) {
                observedAggressiveNpcInstances.add(index);
                if (!playerProvokedNpcInstances.contains(index)) {
                    persistentObservedAggressiveTypes.add(npc.getId());
                }
            }
        } catch (Exception ignored) {
        }
    }

    public boolean wasNpcTypeObservedAggressive(NPC npc) {
        if (npc == null) {
            return false;
        }

        if (isAlwaysAggressive(npc)) {
            return true;
        }

        try {
            int index = npc.getIndex();
            if (index >= 0 && observedAggressiveNpcInstances.contains(index)) {
                return true;
            }
        } catch (Exception ignored) {
        }

        try {
            int id = npc.getId();
            if (persistentObservedAggressiveTypes.contains(id)) {
                return true;
            }
        } catch (Exception ignored) {
        }

        return false;
    }

    public boolean isAlwaysAggressive(NPC npc) {
        if (npc == null) {
            return false;
        }
        String name = getNpcDisplayName(npc);
        return isAlwaysAggressiveName(name);
    }

    public boolean isAlwaysAggressiveName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        return ALWAYS_AGGRESSIVE_NPC_NAMES.contains(name.toLowerCase());
    }

    public boolean isInherentlyPassive(NPC npc) {
        if (npc == null) {
            return false;
        }
        String name = getNpcDisplayName(npc);
        return isInherentlyPassiveName(name);
    }

    public boolean isInherentlyPassiveName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        return INHERENTLY_PASSIVE_NPC_NAMES.contains(name.toLowerCase());
    }

    // Suppresses nametags for interactive resource objects like fishing spots.
    public boolean isSuppressedResourceNpc(NPC npc, String sanitizedName) {
        if (npc == null) {
            return false;
        }

        String normalizedName = normalizeName(sanitizedName);
        if (normalizedName.contains(FISHING_SPOT_KEYWORD)) {
            return true;
        }

        String[] actions = null;
        NPCComposition transformed = npc.getTransformedComposition();
        if (transformed != null) {
            actions = transformed.getActions();
        } else {
            NPCComposition base = npc.getComposition();
            if (base != null) {
                actions = base.getActions();
            }
        }

        if (actions == null) {
            return false;
        }

        boolean hasFishingAction = false;
        for (String action : actions) {
            if (action == null) {
                continue;
            }

            String a = action.trim().toLowerCase(Locale.ROOT);
            if (a.isEmpty()) {
                continue;
            }

            if (a.contains(ATTACK_KEYWORD) || a.contains(TALK_KEYWORD)) {
                return false;
            }

            if (a.contains("net") || a.contains("bait") || a.contains("lure")
                    || a.contains("harpoon") || a.contains("cage") || a.contains("rod")) {
                hasFishingAction = true;
            }
        }

        return hasFishingAction;
    }

    // --- NPC Interaction Heuristics ---

    // Determines if an NPC has an Attack option via cache, composition actions, or
    // live menu.
    public boolean hasAttackOption(NPC npc) {
        if (npc == null) {
            return false;
        }

        int compId = npc.getId();

        if (persistentAttackable.contains(compId)) {
            return true;
        }

        if (persistentNotAttackable.contains(compId)) {
            return false;
        }

        NPCComposition transformed = npc.getTransformedComposition();
        if (transformed != null) {
            Boolean transformedAttackable = classifyAttackFromActions(transformed.getActions());
            if (transformedAttackable != null) {
                cacheAttackability(compId, transformedAttackable);
                return transformedAttackable;
            }
        }

        if (npc.getComposition() != null) {
            Boolean baseAttackable = classifyAttackFromActions(npc.getComposition().getActions());
            if (baseAttackable != null) {
                cacheAttackability(compId, baseAttackable);
                return baseAttackable;
            }
        }

        try {
            if (client != null && client.getMenu() != null) {
                MenuEntry[] entries = client.getMenu().getMenuEntries();
                if (entries != null) {
                    for (MenuEntry e : entries) {
                        if (!menuEntryTargetsNpc(e, npc))
                            continue;
                        if (!NPC_INTERACTION_ACTIONS.contains(e.getType().getId()))
                            continue;

                        if (isAttackInteractionOption(e.getOption())) {
                            cacheAttackability(compId, true);
                            return true;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return false;
    }

    // Determines if an NPC has a Talk-to option.
    public boolean hasTalkOption(NPC npc) {
        if (npc == null || npc.getComposition() == null) {
            return false;
        }

        if (persistentTalkTo.contains(npc.getId())) {
            return true;
        }

        String[] actions = npc.getComposition().getActions();
        if (actions != null) {
            boolean allNull = true;
            for (String a : actions) {
                if (a != null) {
                    allNull = false;
                    break;
                }
            }

            if (!allNull) {
                for (String a : actions) {
                    if (isTalkInteractionOption(a)) {
                        persistentTalkTo.add(npc.getId());
                        return true;
                    }
                }
                return false;
            }
        }

        NPCComposition transformed = npc.getTransformedComposition();
        if (transformed != null) {
            String[] tActions = transformed.getActions();
            if (tActions != null) {
                boolean allNull = true;
                for (String a : tActions) {
                    if (a != null) {
                        allNull = false;
                        break;
                    }
                }
                if (!allNull) {
                    for (String a : tActions) {
                        if (isTalkInteractionOption(a)) {
                            persistentTalkTo.add(npc.getId());
                            return true;
                        }
                    }
                    return false;
                }
            }
        }

        try {
            if (client != null && client.getMenu() != null) {
                MenuEntry[] entries = client.getMenu().getMenuEntries();
                if (entries != null) {
                    for (MenuEntry e : entries) {
                        if (!menuEntryTargetsNpc(e, npc))
                            continue;
                        int typeId = e.getType().getId();
                        if (NPC_INTERACTION_ACTIONS.contains(typeId)) {
                            String opt = e.getOption();
                            if (isTalkInteractionOption(opt)) {
                                persistentTalkTo.add(npc.getId());
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return false;
    }

    // Determines if an NPC has non-talk, non-attack interactions (e.g. Bank,
    // Teleport).
    public boolean hasNonTalkInteractionOption(NPC npc) {
        if (npc == null || hasTalkOption(npc)) {
            return false;
        }

        if (persistentTalkable.contains(npc.getId()) && !persistentTalkTo.contains(npc.getId())) {
            return true;
        }

        NPCComposition composition = npc.getComposition();
        if (composition != null) {
            if (hasNonTalkInteraction(composition.getActions())) {
                persistentTalkable.add(npc.getId());
                return true;
            }
        }

        NPCComposition transformed = npc.getTransformedComposition();
        if (transformed != null) {
            if (hasNonTalkInteraction(transformed.getActions())) {
                persistentTalkable.add(npc.getId());
                return true;
            }
        }

        try {
            if (client != null && client.getMenu() != null) {
                MenuEntry[] entries = client.getMenu().getMenuEntries();
                if (entries != null) {
                    for (MenuEntry e : entries) {
                        if (!menuEntryTargetsNpc(e, npc))
                            continue;
                        int typeId = e.getType().getId();
                        if (NPC_INTERACTION_ACTIONS.contains(typeId) && isNonTalkInteractionOption(e.getOption())) {
                            persistentTalkable.add(npc.getId());
                            return true;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return false;
    }

    // Determines if an NPC has a Trade or Shop option (for Shopkeepers).
    public boolean hasTradeOption(NPC npc) {
        if (npc == null) {
            return false;
        }

        int compId = npc.getId();
        if (persistentTradeable.contains(compId)) {
            return true;
        }

        NPCComposition composition = npc.getComposition();
        if (composition != null && hasTradeInteraction(composition.getActions())) {
            persistentTradeable.add(compId);
            return true;
        }

        NPCComposition transformed = npc.getTransformedComposition();
        if (transformed != null && hasTradeInteraction(transformed.getActions())) {
            persistentTradeable.add(compId);
            return true;
        }

        try {
            if (client != null && client.getMenu() != null) {
                MenuEntry[] entries = client.getMenu().getMenuEntries();
                if (entries != null) {
                    for (MenuEntry e : entries) {
                        if (!menuEntryTargetsNpc(e, npc))
                            continue;
                        if (!NPC_INTERACTION_ACTIONS.contains(e.getType().getId()))
                            continue;
                        if (isTradeInteractionOption(e.getOption())) {
                            persistentTradeable.add(compId);
                            return true;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return false;
    }

    // Determines if an NPC is a pet or recognized animal.
    public boolean hasPetOption(NPC npc) {
        if (npc == null || hasAttackOption(npc)) {
            return false;
        }

        int compId = npc.getId();
        if (persistentPets.contains(compId)) {
            return true;
        }

        NPCComposition composition = npc.getComposition();
        if (composition != null && hasPetInteraction(composition.getActions())) {
            persistentPets.add(compId);
            return true;
        }

        NPCComposition transformed = npc.getTransformedComposition();
        if (transformed != null && hasPetInteraction(transformed.getActions())) {
            persistentPets.add(compId);
            return true;
        }

        try {
            if (client != null && client.getMenu() != null) {
                MenuEntry[] entries = client.getMenu().getMenuEntries();
                if (entries != null) {
                    for (MenuEntry e : entries) {
                        if (!menuEntryTargetsNpc(e, npc))
                            continue;
                        if (!NPC_INTERACTION_ACTIONS.contains(e.getType().getId()))
                            continue;
                        if (isPetInteractionOption(e.getOption())) {
                            persistentPets.add(compId);
                            return true;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        String name = sanitizeEntityName(npc.getName());
        if (name != null && ANIMAL_NAMES.contains(name.toLowerCase(Locale.ROOT))) {
            persistentPets.add(compId);
            return true;
        }

        return false;
    }

    // Determines if an NPC has Hunter catching options (Net, Trap, Catch, etc.) or
    // is a known Hunter creature.
    public boolean hasHunterOption(NPC npc) {
        if (npc == null) {
            return false;
        }

        int compId = npc.getId();
        if (persistentHunterMobs.contains(compId)) {
            return true;
        }

        String name = sanitizeEntityName(npc.getName());
        if (name != null) {
            String lower = name.toLowerCase(Locale.ROOT);
            if (HUNTER_MOB_NAMES.contains(lower) || lower.endsWith(" kebbit") || lower.endsWith(" impling")) {
                persistentHunterMobs.add(compId);
                return true;
            }
        }

        NPCComposition composition = npc.getComposition();
        if (composition != null && hasHunterInteraction(composition.getActions())) {
            persistentHunterMobs.add(compId);
            return true;
        }

        NPCComposition transformed = npc.getTransformedComposition();
        if (transformed != null && hasHunterInteraction(transformed.getActions())) {
            persistentHunterMobs.add(compId);
            return true;
        }

        try {
            if (client != null && client.getMenu() != null) {
                MenuEntry[] entries = client.getMenu().getMenuEntries();
                if (entries != null) {
                    for (MenuEntry e : entries) {
                        if (!menuEntryTargetsNpc(e, npc))
                            continue;
                        if (!NPC_INTERACTION_ACTIONS.contains(e.getType().getId()))
                            continue;
                        if (isHunterInteractionOption(e.getOption())) {
                            persistentHunterMobs.add(compId);
                            return true;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return false;
    }

    // Determines if an NPC is a recognized boss.
    public boolean isBoss(NPC npc) {
        if (npc == null) {
            return false;
        }
        String name = sanitizeEntityName(npc.getName());
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        if (BOSS_NAMES.contains(lower)) {
            return true;
        }
        return lower.startsWith("doom of mokhaiotl")
                || lower.startsWith("great olm")
                || lower.startsWith("verzik vitur")
                || lower.startsWith("kalphite queen")
                || lower.startsWith("alchemical hydra")
                || lower.startsWith("phantom muspah")
                || lower.startsWith("awakened ");
    }

    // Determines if an NPC is a recognized boss minion or add.
    public boolean isBossMinion(NPC npc) {
        if (npc == null) {
            return false;
        }
        String name = sanitizeEntityName(npc.getName());
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        if (BOSS_MINION_NAMES.contains(lower)) {
            return true;
        }
        return lower.contains("demonic") && lower.endsWith("larva");
    }

    // Determines if an NPC is a recognized quest boss.
    public boolean isQuestBoss(NPC npc) {
        if (npc == null) {
            return false;
        }
        String name = sanitizeEntityName(npc.getName());
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        return QUEST_BOSS_NAMES.contains(lower);
    }

    // Determines if an NPC is a known pet (boss pet, skilling pet, or house pet).
    public boolean isPet(NPC npc) {
        if (npc == null || hasAttackOption(npc)) {
            return false;
        }
        String name = sanitizeEntityName(npc.getName());
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        return PET_NAMES.contains(lower) || lower.startsWith("pet ") || lower.startsWith("lil' ")
                || lower.startsWith("baby ") || lower.endsWith(" jr.") || lower.endsWith(" jr");
    }

    // Analyzes a menu option string and updates persistent category caches.
    public void processMenuInteraction(int compId, String op) {
        if (isTradeInteractionOption(op)) {
            persistentTradeable.add(compId);
        }
        if (isPetInteractionOption(op)) {
            persistentPets.add(compId);
        }
        if (isHunterInteractionOption(op)) {
            persistentHunterMobs.add(compId);
        }
        if (isAttackInteractionOption(op)) {
            cacheAttackability(compId, true);
        } else if (isFriendlyInteractionOption(op)) {
            persistentTalkable.add(compId);
            if (isTalkInteractionOption(op)) {
                persistentTalkTo.add(compId);
            }
            persistentNotTalkable.remove(compId);
        }
    }

    // Scans live right-click menu entries to cache interaction types for all
    // visible NPCs.
    public void scanLiveMenuEntries(MenuEntry[] entries, Function<MenuEntry, NPC> npcResolver) {
        if (entries == null || entries.length == 0) {
            return;
        }

        Set<Integer> withInteraction = new HashSet<>();
        Set<Integer> withAttack = new HashSet<>();
        Set<Integer> withTrade = new HashSet<>();
        Set<Integer> withFriendly = new HashSet<>();
        Set<Integer> examineOnly = new HashSet<>();

        for (MenuEntry e : entries) {
            int typeId = e.getType().getId();
            if (NPC_INTERACTION_ACTIONS.contains(typeId) || typeId == MenuAction.EXAMINE_NPC.getId()) {
                NPC trackedNpc = npcResolver.apply(e);
                if (trackedNpc != null) {
                    int compId = trackedNpc.getId();

                    if (NPC_INTERACTION_ACTIONS.contains(typeId)) {
                        withInteraction.add(compId);
                        String opt = e.getOption();
                        if (isAttackInteractionOption(opt)) {
                            withAttack.add(compId);
                            cacheAttackability(compId, true);
                        }
                        if (isTradeInteractionOption(opt)) {
                            withTrade.add(compId);
                        }
                        if (isPetInteractionOption(opt)) {
                            persistentPets.add(compId);
                        }
                        if (isHunterInteractionOption(opt)) {
                            persistentHunterMobs.add(compId);
                        }
                        if (isFriendlyInteractionOption(opt)) {
                            withFriendly.add(compId);
                            persistentTalkable.add(compId);
                            if (isTalkInteractionOption(opt)) {
                                persistentTalkTo.add(compId);
                            }
                            persistentNotTalkable.remove(compId);
                        }
                    } else {
                        examineOnly.add(compId);
                    }
                }
            }
        }

        for (int compId : withInteraction) {
            if (!withFriendly.contains(compId)) {
                persistentNotTalkable.add(compId);
            }
        }

        for (int compId : withAttack) {
            cacheAttackability(compId, true);
        }

        for (int compId : withTrade) {
            persistentTradeable.add(compId);
        }

        for (int compId : examineOnly) {
            if (!withInteraction.contains(compId)) {
                persistentNotTalkable.add(compId);
                persistentTalkable.remove(compId);
                cacheAttackability(compId, false);
            }
        }
    }

    // Inspects current menu entries specifically targeting a single NPC.
    public void scanMenuForNpc(NPC npc) {
        if (npc == null || client == null || client.getMenu() == null) {
            return;
        }

        try {
            MenuEntry[] entries = client.getMenu().getMenuEntries();
            if (entries != null) {
                boolean hasInteraction = false;
                boolean hasExamine = false;
                for (MenuEntry e : entries) {
                    if (!menuEntryTargetsNpc(e, npc))
                        continue;
                    int typeId = e.getType().getId();
                    if (NPC_INTERACTION_ACTIONS.contains(typeId)) {
                        hasInteraction = true;
                        String opt = e.getOption();
                        if (isTradeInteractionOption(opt)) {
                            persistentTradeable.add(npc.getId());
                        }
                        if (isPetInteractionOption(opt)) {
                            persistentPets.add(npc.getId());
                        }
                        if (isHunterInteractionOption(opt)) {
                            persistentHunterMobs.add(npc.getId());
                        }
                        if (isFriendlyInteractionOption(opt)) {
                            persistentTalkable.add(npc.getId());
                            if (isTalkInteractionOption(opt)) {
                                persistentTalkTo.add(npc.getId());
                            }
                            persistentNotTalkable.remove(npc.getId());
                        }
                        if (isAttackInteractionOption(opt)) {
                            cacheAttackability(npc.getId(), true);
                        }
                    } else if (typeId == MenuAction.EXAMINE_NPC.getId()) {
                        hasExamine = true;
                    }
                }
                if (hasInteraction) {
                    persistentNotTalkable.add(npc.getId());
                } else if (hasExamine) {
                    persistentNotTalkable.add(npc.getId());
                    persistentTalkable.remove(npc.getId());
                    cacheAttackability(npc.getId(), false);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void cacheAttackability(int compId, boolean attackable) {
        if (attackable) {
            persistentAttackable.add(compId);
            persistentNotAttackable.remove(compId);
            return;
        }

        persistentNotAttackable.add(compId);
        persistentAttackable.remove(compId);
    }

    private Boolean classifyAttackFromActions(String[] actions) {
        if (actions == null) {
            return null;
        }

        boolean sawAction = false;
        for (String action : actions) {
            if (action == null) {
                continue;
            }

            sawAction = true;
            if (isAttackInteractionOption(action)) {
                return true;
            }
        }

        return sawAction ? Boolean.FALSE : null;
    }

    private boolean isAttackInteractionOption(String option) {
        if (option == null) {
            return false;
        }

        String normalized = option.trim().toLowerCase(Locale.ROOT);
        return !normalized.isEmpty() && normalized.contains(ATTACK_KEYWORD);
    }

    private boolean isTalkInteractionOption(String option) {
        if (option == null) {
            return false;
        }

        String normalized = option.trim().toLowerCase(Locale.ROOT);
        return !normalized.isEmpty() && normalized.contains(TALK_KEYWORD);
    }

    private boolean hasNonTalkInteraction(String[] actions) {
        if (actions == null) {
            return false;
        }

        for (String action : actions) {
            if (isNonTalkInteractionOption(action)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasTradeInteraction(String[] actions) {
        if (actions == null) {
            return false;
        }

        for (String action : actions) {
            if (isTradeInteractionOption(action)) {
                return true;
            }
        }

        return false;
    }

    private boolean isNonTalkInteractionOption(String option) {
        if (option == null) {
            return false;
        }

        String normalized = option.trim().toLowerCase(Locale.ROOT);
        return !normalized.isEmpty()
                && !normalized.contains(ATTACK_KEYWORD)
                && !normalized.contains(TALK_KEYWORD);
    }

    private boolean isTradeInteractionOption(String option) {
        if (option == null) {
            return false;
        }

        String normalized = option.trim().toLowerCase(Locale.ROOT);
        return !normalized.isEmpty() && normalized.contains(TRADE_KEYWORD);
    }

    private boolean isFriendlyInteractionOption(String option) {
        if (option == null) {
            return false;
        }

        String normalized = option.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return false;
        }

        return !normalized.contains(ATTACK_KEYWORD);
    }

    private boolean hasPetInteraction(String[] actions) {
        if (actions == null) {
            return false;
        }

        for (String action : actions) {
            if (isPetInteractionOption(action)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasHunterInteraction(String[] actions) {
        if (actions == null) {
            return false;
        }

        for (String action : actions) {
            if (isHunterInteractionOption(action)) {
                return true;
            }
        }

        return false;
    }

    private boolean isPetInteractionOption(String option) {
        if (option == null) {
            return false;
        }

        String normalized = option.trim().toLowerCase(Locale.ROOT);
        return !normalized.isEmpty() && normalized.contains("pet");
    }

    private boolean isHunterInteractionOption(String option) {
        if (option == null) {
            return false;
        }

        String normalized = option.trim().toLowerCase(Locale.ROOT);
        return !normalized.isEmpty() && (normalized.contains("catch") ||
                normalized.contains("net") ||
                normalized.contains("trap") ||
                normalized.contains("track") ||
                normalized.contains("lasso"));
    }

    // Resolves the visible display name for an NPC, handling transformed
    // compositions.
    public String getNpcDisplayName(NPC npc) {
        if (npc == null) {
            return null;
        }

        String rawName = null;
        try {
            NPCComposition transformed = npc.getTransformedComposition();
            if (transformed != null) {
                rawName = transformed.getName();
            }
        } catch (Exception ignored) {
        }

        if (rawName == null || rawName.trim().isEmpty() || "null".equalsIgnoreCase(rawName.trim())) {
            rawName = npc.getName();
        }

        return sanitizeEntityName(rawName);
    }

    // Checks whether a right-click menu entry targets a specific NPC instance.
    public boolean menuEntryTargetsNpc(MenuEntry entry, NPC npc) {
        if (entry == null || npc == null) {
            return false;
        }

        if (entry.getIdentifier() == npc.getIndex()) {
            return true;
        }

        String targetName = sanitizeTarget(entry.getTarget());
        String npcName = getNpcDisplayName(npc);
        return targetName != null && npcName != null && targetName.equalsIgnoreCase(npcName);
    }

    public void onMenuOptionClicked(MenuOptionClicked event) {
        if (event == null) {
            return;
        }

        try {
            MenuAction action = event.getMenuAction();
            if (action == null || !NPC_INTERACTION_ACTIONS.contains(action.getId())) {
                return;
            }

            if (!"attack".equalsIgnoreCase(event.getMenuOption())) {
                return;
            }

            int npcIndex = event.getId();
            if (npcIndex >= 0) {
                recordPlayerAttack(npcIndex);
            }
        } catch (Exception ignored) {
        }
    }

    public void processMenuEntryAdded(int typeId, String option, NPC trackedNpc) {
        if (NPC_INTERACTION_ACTIONS.contains(typeId) && trackedNpc != null) {
            processMenuInteraction(trackedNpc.getId(), option);
        }
    }

    // --- Name & Target Sanitization Utilities ---

    // Strips HTML/formatting tags (<col=...>) from entity names.
    public static String stripNameMarkup(String raw) {
        if (raw == null) {
            return null;
        }

        try {
            String cleaned = raw.replaceAll("<[^>]+>", "").trim();
            if (cleaned.isEmpty() || "null".equalsIgnoreCase(cleaned)) {
                return null;
            }
            return cleaned;
        } catch (Exception ignored) {
            String cleaned = raw.trim();
            if (cleaned.isEmpty() || "null".equalsIgnoreCase(cleaned)) {
                return null;
            }
            return cleaned;
        }
    }

    public static String sanitizeEntityName(String raw) {
        return stripNameMarkup(raw);
    }

    public static String normalizeName(String raw) {
        String cleaned = stripNameMarkup(raw);
        return cleaned == null ? "" : cleaned.toLowerCase(Locale.ROOT);
    }

    // Strips action prefixes and combat-level suffixes from menu target strings.
    public static String sanitizeTarget(String raw) {
        String s = stripNameMarkup(raw);
        if (s == null) {
            return null;
        }

        try {
            // Strip combat-level suffix that appears in player targets: " (level-126)"
            s = s.replaceAll("(?i)\\s*\\(level-\\d+\\)\\s*$", "").trim();
            // Strip common action prefixes that sometimes appear on target strings
            s = s.replaceAll("(?i)^(talk-?to\\s+|examine\\s+|walk here\\s+)", "").trim();
            return s.isEmpty() ? null : s;
        } catch (Exception ignored) {
            return s;
        }
    }
}
