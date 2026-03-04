package com.wownpc;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Range;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(WoWStyleNametagsConfig.GROUP)
public interface WoWStyleNametagsConfig extends Config
{
    static final String GROUP = "wowstyle-nametags";
    @ConfigItem(
        position = 1,
        keyName = "hoverOnly",
        name = "Show on hover only",
        description = "Only show nametags when hovering over an entity with the mouse (your own nametag is always visible if enabled below)"
    )
    default boolean hoverOnly()
    {
        return false;
    }

    @ConfigItem(
        position = 2,
        keyName = "anchorBelow",
        name = "Anchor below",
        description = "If enabled, position the nametag below entities instead of above"
    )
    default boolean anchorBelow()
    {
        return false;
    }

    @ConfigItem(
        position = 11,
        keyName = "verticalOffset",
        name = "Vertical offset",
        description = "Pixels to offset the nametag (when above: adds to height; when below: subtracts from height)"
    )
    @Range(min = 0, max = 9999)
    default int verticalOffset()
    {
        return 40;
    }

    @ConfigItem(
        position = 3,
        keyName = "enableAttackable",
        name = "Enable for Aggressive NPCs",
        description = "Show nametags for NPCs that can be attacked and are aggressive towards the player)"
    )
    default boolean enableAttackable()
    {
        return true;
    }

    @ConfigItem(
        position = 4,
        keyName = "enablePassive",
        name = "Enable for Passive NPCs",
        description = "Show nametags for NPCs that are passive towards the player (lower level than player and/or won't attack unless provoked, or same level)"
    )
    default boolean enablePassive()
    {
        return true;
    }

    @ConfigItem(
        position = 6,
        keyName = "enableTalkable",
        name = "Enable for Friendly NPCs",
        description = "Show nametags for NPCs that are friendly / can be talked to"
    )
    default boolean enableTalkable()
    {
        return true;
    }

    @ConfigItem(
        position = 5,
        keyName = "enableAttackableTalkable",
        name = "Enable for Neutral NPCs",
        description = "Show nametags for NPCs that are 'neutral' (both attackable and can be talked to)"
    )
    default boolean enableAttackableTalkable()
    {
        return true;
    }

    @ConfigItem(
        position = 9,
        keyName = "enableMyFollowers",
        name = "Enable for your followers",
        description = "Show nametags for follower NPCs owned by you"
    )
    default boolean enableMyFollowers()
    {
        return true;
    }

    @ConfigItem(
        position = 10,
        keyName = "enableOtherPlayersFollowers",
        name = "Enable for other players' followers",
        description = "Show nametags for follower NPCs owned by other players"
    )
    default boolean enableOtherPlayersFollowers()
    {
        return true;
    }

    @ConfigItem(
        position = 8,
        keyName = "enableOtherPlayers",
        name = "Enable for other players",
        description = "Show nametags for other players"
    )
    default boolean enableOtherPlayers()
    {
        return true;
    }

    @ConfigItem(
        position = 7,
        keyName = "enableSelfPlayer",
        name = "Enable for your character",
        description = "Show your own nametag"
    )
    default boolean enableSelfPlayer()
    {
        return true;
    }

    // --- Colours ---
    @Alpha
    @ConfigItem(
        position = 12,
        keyName = "attackableColour",
        name = "Aggressive NPC colour",
        description = "Colour used for aggressive NPCs"
    )
    default Color attackableColour()
    {
        return Color.RED;
    }

    @Alpha
    @ConfigItem(
        position = 13,
        keyName = "passiveColour",
        name = "Passive NPC colour",
        description = "Colour used for passive NPCs"
    )
    default Color passiveColour()
    {
        return new Color(0xFFCC6600, true);
    }

    @Alpha
    @ConfigItem(
        position = 14,
        keyName = "attackableTalkableColour",
        name = "Neutral NPC colour",
        description = "Colour used for neutral NPCs"
    )
    default Color attackableTalkableColour()
    {
        return Color.YELLOW;
    }

    @Alpha
    @ConfigItem(
        position = 15,
        keyName = "talkableColour",
        name = "Friendly NPC colour",
        description = "Colour used for friendly NPCs"
    )
    default Color talkableColour()
    {
        return new Color(0xFF00FF00, true);
    }

    @Alpha
    @ConfigItem(
        position = 16,
        keyName = "selfPlayerColour",
        name = "Your nametag colour",
        description = "Colour used for your nametag"
    )
    default Color selfPlayerColour()
    {
        return new Color(0xFF66B2FF, true);
    }

    @Alpha
    @ConfigItem(
        position = 17,
        keyName = "otherPlayersColour",
        name = "Other players colour",
        description = "Colour used for other players"
    )
    default Color otherPlayersColour()
    {
        return new Color(0xFFA373FF, true);
    }

    @Alpha
    @ConfigItem(
        position = 18,
        keyName = "myFollowerColour",
        name = "Your followers colour",
        description = "Colour used for follower NPCs owned by you"
    )
    default Color myFollowerColour()
    {
        return new Color(0xFF859BC0, true);
    }

    @Alpha
    @ConfigItem(
        position = 19,
        keyName = "otherPlayersFollowerColour",
        name = "Other players' followers colour",
        description = "Colour used for follower NPCs owned by other players"
    )
    default Color otherPlayersFollowerColour()
    {
        return new Color(0xFFD3BDFF, true);
    }

    @ConfigSection(
        name = "NPC Outline",
        description = "Outline settings for NPC categories",
        position = 30
    )
    String npcOutlineSection = "npcOutlineSection";

    @ConfigSection(
        name = "Player Outline",
        description = "Outline settings for your nametags",
        position = 31
    )
    String playerOutlineSection = "playerOutlineSection";

    @ConfigSection(
        name = "Follower Outline",
        description = "Outline settings for follower nametags",
        position = 32
    )
    String followerOutlineSection = "followerOutlineSection";

    // --- NPC Outline (positions 30-41) ---
    @ConfigItem(
        position = 30,
        keyName = "attackableOutlineEnabled",
        name = "Enable outline for aggressive NPCs",
        description = "Show outline for aggressive NPCs",
        section = "npcOutlineSection"
    )
    default boolean attackableOutlineEnabled()
    {
        return true;
    }

    @Alpha
    @ConfigItem(
        position = 31,
        keyName = "attackableOutlineColour",
        name = "Aggressive NPC outline colour",
        description = "Outline colour for aggressive NPCs",
        section = "npcOutlineSection"
    )
    default Color attackableOutlineColour()
    {
        return null;
    }

    @ConfigItem(
        position = 32,
        keyName = "attackableOutlineThickness",
        name = "Aggressive NPC outline thickness",
        description = "Thickness for aggressive NPC outline",
        section = "npcOutlineSection"
    )
    @Range(min = 1, max = 10)
    default int attackableOutlineThickness()
    {
        return 2;
    }

    @ConfigItem(
        position = 33,
        keyName = "passiveOutlineEnabled",
        name = "Enable outline for passive NPCs",
        description = "Show outline for passive NPCs",
        section = "npcOutlineSection"
    )
    default boolean passiveOutlineEnabled()
    {
        return true;
    }

    @Alpha
    @ConfigItem(
        position = 34,
        keyName = "passiveOutlineColour",
        name = "Passive NPC outline colour",
        description = "Outline colour for passive NPCs",
        section = "npcOutlineSection"
    )
    default Color passiveOutlineColour()
    {
        return null;
    }

    @ConfigItem(
        position = 35,
        keyName = "passiveOutlineThickness",
        name = "Passive NPC outline thickness",
        description = "Thickness for passive NPC outline",
        section = "npcOutlineSection"
    )
    @Range(min = 1, max = 10)
    default int passiveOutlineThickness()
    {
        return 2;
    }

    @ConfigItem(
        position = 36,
        keyName = "talkableOutlineEnabled",
        name = "Enable outline for friendly NPCs",
        description = "Show outline for friendly NPCs",
        section = "npcOutlineSection"
    )
    default boolean talkableOutlineEnabled()
    {
        return true;
    }

    @Alpha
    @ConfigItem(
        position = 37,
        keyName = "talkableOutlineColour",
        name = "Friendly NPC outline colour",
        description = "Outline colour for friendly NPCs",
        section = "npcOutlineSection"
    )
    default Color talkableOutlineColour()
    {
        return null;
    }

    @ConfigItem(
        position = 38,
        keyName = "talkableOutlineThickness",
        name = "Friendly NPC outline thickness",
        description = "Thickness for friendly NPC outline",
        section = "npcOutlineSection"
    )
    @Range(min = 1, max = 10)
    default int talkableOutlineThickness()
    {
        return 2;
    }

    @ConfigItem(
        position = 39,
        keyName = "attackableTalkableOutlineEnabled",
        name = "Enable outline for neutral NPCs",
        description = "Show outline for neutral NPCs",
        section = "npcOutlineSection"
    )
    default boolean attackableTalkableOutlineEnabled()
    {
        return true;
    }

    @Alpha
    @ConfigItem(
        position = 40,
        keyName = "attackableTalkableOutlineColour",
        name = "Neutral NPC outline colour",
        description = "Outline colour for neutral NPCs",
        section = "npcOutlineSection"
    )
    default Color attackableTalkableOutlineColour()
    {
        return null;
    }

    @ConfigItem(
        position = 41,
        keyName = "attackableTalkableOutlineThickness",
        name = "Neutral NPC outline thickness",
        description = "Thickness for neutral NPC outline",
        section = "npcOutlineSection"
    )
    @Range(min = 1, max = 10)
    default int attackableTalkableOutlineThickness()
    {
        return 2;
    }

    // --- Player Outline (positions 42-47) ---
    @ConfigItem(
        position = 42,
        keyName = "otherPlayersOutlineEnabled",
        name = "Enable outline for other players",
        description = "Enable outline for other players",
        section = "playerOutlineSection"
    )
    default boolean otherPlayersOutlineEnabled()
    {
        return true;
    }

    @Alpha
    @ConfigItem(
        position = 43,
        keyName = "otherPlayersOutlineColour",
        name = "Other players outline colour",
        description = "Outline colour for other players",
        section = "playerOutlineSection"
    )
    default Color otherPlayersOutlineColour()
    {
        return null;
    }

    @ConfigItem(
        position = 44,
        keyName = "otherPlayersOutlineThickness",
        name = "Other players outline thickness",
        description = "Thickness for other players outline",
        section = "playerOutlineSection"
    )
    @Range(min = 1, max = 10)
    default int otherPlayersOutlineThickness()
    {
        return 2;
    }

    @ConfigItem(
        position = 45,
        keyName = "selfPlayerOutlineEnabled",
        name = "Enable outline for your nametag",
        description = "Show outline for your nametag",
        section = "playerOutlineSection"
    )
    default boolean selfPlayerOutlineEnabled()
    {
        return true;
    }

    @Alpha
    @ConfigItem(
        position = 46,
        keyName = "selfPlayerOutlineColour",
        name = "Your nametag outline colour",
        description = "Outline colour for your nametag",
        section = "playerOutlineSection"
    )
    default Color selfPlayerOutlineColour()
    {
        return null;
    }

    @ConfigItem(
        position = 47,
        keyName = "selfPlayerOutlineThickness",
        name = "Your nametag outline thickness",
        description = "Thickness for your nametag outline",
        section = "playerOutlineSection"
    )
    @Range(min = 1, max = 10)
    default int selfPlayerOutlineThickness()
    {
        return 2;
    }

    @ConfigItem(
        position = 51,
        keyName = "myFollowerOutlineEnabled",
        name = "Enable outline for your followers",
        description = "Show outline for your followers",
        section = "followerOutlineSection"
    )
    default boolean myFollowerOutlineEnabled()
    {
        return true;
    }

    @Alpha
    @ConfigItem(
        position = 52,
        keyName = "myFollowerOutlineColour",
        name = "Your followers outline colour",
        description = "Outline colour for your followers",
        section = "followerOutlineSection"
    )
    default Color myFollowerOutlineColour()
    {
        return null;
    }

    @ConfigItem(
        position = 53,
        keyName = "myFollowerOutlineThickness",
        name = "Your followers outline thickness",
        description = "Thickness for your followers outline",
        section = "followerOutlineSection"
    )
    @Range(min = 1, max = 10)
    default int myFollowerOutlineThickness()
    {
        return 2;
    }

    @ConfigItem(
        position = 54,
        keyName = "otherPlayersFollowerOutlineEnabled",
        name = "Enable outline for other players' followers",
        description = "Enable outline for other players' followers",
        section = "followerOutlineSection"
    )
    default boolean otherPlayersFollowerOutlineEnabled()
    {
        return true;
    }

    @Alpha
    @ConfigItem(
        position = 55,
        keyName = "otherPlayersFollowerOutlineColour",
        name = "Other players' followers outline colour",
        description = "Outline colour for other players' followers",
        section = "followerOutlineSection"
    )
    default Color otherPlayersFollowerOutlineColour()
    {
        return null;
    }

    @ConfigItem(
        position = 56,
        keyName = "otherPlayersFollowerOutlineThickness",
        name = "Other players' followers outline thickness",
        description = "Outline thickness for other players' followers",
        section = "followerOutlineSection"
    )
    @Range(min = 1, max = 10)
    default int otherPlayersFollowerOutlineThickness()
    {
        return 2;
    }

    @ConfigSection(
        name = "Culling & Stacking",
        description = "Limit the number of visible nametags and control overlap behaviour",
        position = 20
    )
    String cullingSection = "cullingSection";

    @ConfigItem(
        position = 21,
        keyName = "maxEntities",
        name = "Max nametags shown",
        description = "Maximum number of nametags visible at once, sorted by distance (0 = unlimited)",
        section = "cullingSection"
    )
    @Range(min = 0, max = 100)
    default int maxEntities()
    {
        return 0;
    }

    @ConfigItem(
        position = 22,
        keyName = "stackTags",
        name = "Stack overlapping nametags",
        description = "Shift nametags that would overlap vertically, closest entity keeps its natural position",
        section = "cullingSection"
    )
    default boolean stackTags()
    {
        return false;
    }
}
