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
        position = 3,
        keyName = "excludedNpcNames",
        name = "Excluded NPC names",
        description = "Comma-separated NPC names to always hide, e.g. banker, man"
    )
    default String excludedNpcNames()
    {
        return "";
    }

    @ConfigItem(
        position = 4,
        keyName = "excludedPlayerNames",
        name = "Excluded other player names",
        description = "Comma-separated other player names to always hide, e.g. zezima, bonecute"
    )
    default String excludedPlayerNames()
    {
        return "";
    }

    @ConfigItem(
        position = 5,
        keyName = "enableAttackable",
        name = "Enable for Aggressive NPCs",
        description = "Show nametags for NPCs that can be attacked and are aggressive towards the player"
    )
    default boolean enableAttackable()
    {
        return true;
    }

    @ConfigItem(
        position = 6,
        keyName = "enablePassive",
        name = "Enable for Passive NPCs",
        description = "Show nametags for attackable NPCs whose combat level is at most twice yours (unlikely to aggro you)"
    )
    default boolean enablePassive()
    {
        return true;
    }

    @ConfigItem(
        position = 8,
        keyName = "enableAttackableTalkable",
        name = "Enable for Neutral NPCs",
        description = "Show nametags for NPCs that are 'neutral' (both attackable and have a non-attack interaction option)"
    )
    default boolean enableAttackableTalkable()
    {
        return true;
    }

    @ConfigItem(
        position = 9,
        keyName = "enableTalkable",
        name = "Enable for Friendly NPCs",
        description = "Show nametags for NPCs that are friendly / can be talked to"
    )
    default boolean enableTalkable()
    {
        return true;
    }

    @ConfigItem(
        position = 14,
        keyName = "enableMyFollowers",
        name = "Enable for your followers",
        description = "Show nametags for follower NPCs owned by you"
    )
    default boolean enableMyFollowers()
    {
        return true;
    }

    @ConfigItem(
        position = 15,
        keyName = "enableOtherPlayersFollowers",
        name = "Enable for other players' followers",
        description = "Show nametags for follower NPCs owned by other players"
    )
    default boolean enableOtherPlayersFollowers()
    {
        return true;
    }

    @ConfigItem(
        position = 10,
        keyName = "enableNonTalkInteraction",
        name = "Enable for Friendly non-talkers",
        description = "Show nametags for NPCs with non-attack interactions that are not Talk-to (e.g. Catch, Shear, Pet)"
    )
    default boolean enableNonTalkInteraction()
    {
        return true;
    }

    @ConfigItem(
        position = 11,
        keyName = "enableShopkeepers",
        name = "Enable for Shopkeepers",
        description = "Show nametags for NPCs with a Trade interaction option"
    )
    default boolean enableShopkeepers()
    {
        return true;
    }

    @ConfigItem(
        position = 13,
        keyName = "enableOtherPlayers",
        name = "Enable for other players",
        description = "Show nametags for other players"
    )
    default boolean enableOtherPlayers()
    {
        return true;
    }

    @ConfigItem(
        position = 12,
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
        position = 16,
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
        position = 17,
        keyName = "passiveColour",
        name = "Passive NPC colour",
        description = "Colour used for attackable NPCs within the non-aggression threshold (combat level at most 2x yours)"
    )
    default Color passiveColour()
    {
        return new Color(0xFFCC6600, true);
    }

    @Alpha
    @ConfigItem(
        position = 18,
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
        position = 19,
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
        position = 20,
        keyName = "nonTalkInteractionColour",
        name = "Friendly non-talkers colour",
        description = "Colour used for NPCs with non-attack interactions that are not Talk-to (e.g. Catch, Shear, Pet)"
    )
    default Color nonTalkInteractionColour()
    {
        return new Color(0xFFFF66FF, true);
    }

    @Alpha
    @ConfigItem(
        position = 22,
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
        position = 23,
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
        position = 24,
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
        position = 25,
        keyName = "otherPlayersFollowerColour",
        name = "Other players' followers colour",
        description = "Colour used for follower NPCs owned by other players"
    )
    default Color otherPlayersFollowerColour()
    {
        return new Color(0xFFD3BDFF, true);
    }

    @Alpha
    @ConfigItem(
        position = 21,
        keyName = "shopkeeperColour",
        name = "Shopkeepers colour",
        description = "Colour used for NPCs with a Trade interaction option"
    )
    default Color shopkeeperColour()
    {
        return new Color(0xFFF4F4F4, true);
    }

    @ConfigSection(
        name = "NPC Outline",
        description = "Outline settings for NPC categories",
        position = 40
    )
    String npcOutlineSection = "npcOutlineSection";

    @ConfigSection(
        name = "Player Outline",
        description = "Outline settings for your nametags",
        position = 53
    )
    String playerOutlineSection = "playerOutlineSection";

    @ConfigSection(
        name = "Player Relationship Styles",
        description = "Enable style overrides and set colours for friends, clan members, chat channel members, and guest clan relationships",
        position = 37
    )
    String playerRelationsSection = "playerRelationsSection";

    @ConfigSection(
        name = "Player Relationship Outline",
        description = "Outline settings for friends, clan members, chat channel members, and guest clan relationships",
        position = 90
    )
    String playerRelationsOutlineSection = "playerRelationsOutlineSection";

    @ConfigSection(
        name = "Follower Outline",
        description = "Outline settings for follower nametags",
        position = 100
    )
    String followerOutlineSection = "followerOutlineSection";

    @ConfigSection(
        name = "Font Sizes",
        description = "Per-category nametag font size settings",
        position = 39
    )
    String fontSizeSection = "fontSizeSection";

    // --- Font Sizes (positions 110-124) ---
    @ConfigItem(
        position = 110,
        keyName = "attackableFontSize",
        name = "Aggressive NPC font size",
        description = "Font size for aggressive NPC nametags",
        section = "fontSizeSection"
    )
    @Range(min = 8, max = 30)
    default int attackableFontSize()
    {
        return 16;
    }

    @ConfigItem(
        position = 111,
        keyName = "passiveFontSize",
        name = "Passive NPC font size",
        description = "Font size for passive NPC nametags",
        section = "fontSizeSection"
    )
    @Range(min = 8, max = 30)
    default int passiveFontSize()
    {
        return 16;
    }

    @ConfigItem(
        position = 112,
        keyName = "attackableTalkableFontSize",
        name = "Neutral NPC font size",
        description = "Font size for neutral NPC nametags",
        section = "fontSizeSection"
    )
    @Range(min = 8, max = 30)
    default int attackableTalkableFontSize()
    {
        return 16;
    }

    @ConfigItem(
        position = 113,
        keyName = "talkableFontSize",
        name = "Friendly NPC font size",
        description = "Font size for friendly NPC nametags",
        section = "fontSizeSection"
    )
    @Range(min = 8, max = 30)
    default int talkableFontSize()
    {
        return 16;
    }

    @ConfigItem(
        position = 114,
        keyName = "nonTalkInteractionFontSize",
        name = "Friendly non-talkers font size",
        description = "Font size for Friendly non-talkers nametags",
        section = "fontSizeSection"
    )
    @Range(min = 8, max = 30)
    default int nonTalkInteractionFontSize()
    {
        return 16;
    }

    @ConfigItem(
        position = 115,
        keyName = "shopkeeperFontSize",
        name = "Shopkeepers font size",
        description = "Font size for shopkeeper nametags",
        section = "fontSizeSection"
    )
    @Range(min = 8, max = 30)
    default int shopkeeperFontSize()
    {
        return 16;
    }

    @ConfigItem(
        position = 116,
        keyName = "selfPlayerFontSize",
        name = "Your nametag font size",
        description = "Font size for your own nametag",
        section = "fontSizeSection"
    )
    @Range(min = 8, max = 30)
    default int selfPlayerFontSize()
    {
        return 16;
    }

    @ConfigItem(
        position = 117,
        keyName = "otherPlayersFontSize",
        name = "Other players font size",
        description = "Font size for other players' nametags",
        section = "fontSizeSection"
    )
    @Range(min = 8, max = 30)
    default int otherPlayersFontSize()
    {
        return 16;
    }

    @ConfigItem(
        position = 118,
        keyName = "friendPlayersFontSize",
        name = "Friends font size",
        description = "Font size for friends' nametags",
        section = "fontSizeSection"
    )
    @Range(min = 8, max = 30)
    default int friendPlayersFontSize()
    {
        return 16;
    }

    @ConfigItem(
        position = 119,
        keyName = "clanMembersFontSize",
        name = "Clan members font size",
        description = "Font size for clan members' nametags",
        section = "fontSizeSection"
    )
    @Range(min = 8, max = 30)
    default int clanMembersFontSize()
    {
        return 16;
    }

    @ConfigItem(
        position = 120,
        keyName = "clanChatMembersFontSize",
        name = "Chat channel members font size",
        description = "Font size for chat channel members' nametags",
        section = "fontSizeSection"
    )
    @Range(min = 8, max = 30)
    default int clanChatMembersFontSize()
    {
        return 16;
    }

    @ConfigItem(
        position = 121,
        keyName = "guestClanMembersFontSize",
        name = "Clan members (Guest) font size",
        description = "Font size for clan members (Guest) nametags",
        section = "fontSizeSection"
    )
    @Range(min = 8, max = 30)
    default int guestClanMembersFontSize()
    {
        return 16;
    }

    @ConfigItem(
        position = 122,
        keyName = "guestsInYourClanFontSize",
        name = "Guests in your clan font size",
        description = "Font size for guests in your clan nametags",
        section = "fontSizeSection"
    )
    @Range(min = 8, max = 30)
    default int guestsInYourClanFontSize()
    {
        return 16;
    }

    @ConfigItem(
        position = 123,
        keyName = "myFollowerFontSize",
        name = "Your followers font size",
        description = "Font size for your followers' nametags",
        section = "fontSizeSection"
    )
    @Range(min = 8, max = 30)
    default int myFollowerFontSize()
    {
        return 16;
    }

    @ConfigItem(
        position = 124,
        keyName = "otherPlayersFollowerFontSize",
        name = "Other players' followers font size",
        description = "Font size for other players' followers' nametags",
        section = "fontSizeSection"
    )
    @Range(min = 8, max = 30)
    default int otherPlayersFollowerFontSize()
    {
        return 16;
    }

    // --- NPC Outline (positions 41-58) ---
    @ConfigItem(
        position = 41,
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
        position = 42,
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
        position = 43,
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
        position = 44,
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
        position = 45,
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
        position = 46,
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
        position = 50,
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
        position = 51,
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
        position = 52,
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
        position = 47,
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
        position = 48,
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
        position = 49,
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

    @ConfigItem(
        position = 53,
        keyName = "nonTalkInteractionOutlineEnabled",
        name = "Enable outline for Friendly non-talkers",
        description = "Show outline for Friendly non-talkers",
        section = "npcOutlineSection"
    )
    default boolean nonTalkInteractionOutlineEnabled()
    {
        return true;
    }

    @Alpha
    @ConfigItem(
        position = 54,
        keyName = "nonTalkInteractionOutlineColour",
        name = "Friendly non-talkers outline colour",
        description = "Outline colour for Friendly non-talkers",
        section = "npcOutlineSection"
    )
    default Color nonTalkInteractionOutlineColour()
    {
        return null;
    }

    @ConfigItem(
        position = 55,
        keyName = "nonTalkInteractionOutlineThickness",
        name = "Friendly non-talkers outline thickness",
        description = "Thickness for Friendly non-talkers outline",
        section = "npcOutlineSection"
    )
    @Range(min = 1, max = 10)
    default int nonTalkInteractionOutlineThickness()
    {
        return 2;
    }

    @ConfigItem(
        position = 56,
        keyName = "shopkeeperOutlineEnabled",
        name = "Enable outline for Shopkeepers",
        description = "Show outline for Shopkeepers",
        section = "npcOutlineSection"
    )
    default boolean shopkeeperOutlineEnabled()
    {
        return true;
    }

    @Alpha
    @ConfigItem(
        position = 57,
        keyName = "shopkeeperOutlineColour",
        name = "Shopkeepers outline colour",
        description = "Outline colour for Shopkeepers",
        section = "npcOutlineSection"
    )
    default Color shopkeeperOutlineColour()
    {
        return null;
    }

    @ConfigItem(
        position = 58,
        keyName = "shopkeeperOutlineThickness",
        name = "Shopkeepers outline thickness",
        description = "Thickness for Shopkeepers outline",
        section = "npcOutlineSection"
    )
    @Range(min = 1, max = 10)
    default int shopkeeperOutlineThickness()
    {
        return 2;
    }

    // --- Player Outline (positions 79-84) ---
    @ConfigItem(
        position = 82,
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
        position = 83,
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
        position = 84,
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
        position = 79,
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
        position = 80,
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
        position = 81,
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

    // --- Follower Outline (positions 77-82) ---

    @ConfigItem(
        position = 101,
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
        position = 102,
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
        position = 103,
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
        position = 104,
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
        position = 105,
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
        position = 106,
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

    // --- Player Relationships (styles 26-35, outlines 61-75) ---
    @ConfigItem(
        position = 26,
        keyName = "enableFriendPlayers",
        name = "Enable friend style",
        description = "Use friend-specific nametag style instead of other players style",
        section = "playerRelationsSection"
    )
    default boolean enableFriendPlayers()
    {
        return false;
    }

    @Alpha
    @ConfigItem(
        position = 27,
        keyName = "friendPlayersColour",
        name = "Friends colour",
        description = "Colour used for friends",
        section = "playerRelationsSection"
    )
    default Color friendPlayersColour()
    {
        return new Color(0xFF4CAF50, true);
    }

    @ConfigItem(
        position = 61,
        keyName = "friendPlayersOutlineEnabled",
        name = "Enable friends outline",
        description = "Show outline for friends",
        section = "playerRelationsOutlineSection"
    )
    default boolean friendPlayersOutlineEnabled()
    {
        return true;
    }

    @Alpha
    @ConfigItem(
        position = 62,
        keyName = "friendPlayersOutlineColour",
        name = "Friends outline colour",
        description = "Outline colour for friends",
        section = "playerRelationsOutlineSection"
    )
    default Color friendPlayersOutlineColour()
    {
        return null;
    }

    @ConfigItem(
        position = 63,
        keyName = "friendPlayersOutlineThickness",
        name = "Friends outline thickness",
        description = "Outline thickness for friends",
        section = "playerRelationsOutlineSection"
    )
    @Range(min = 1, max = 10)
    default int friendPlayersOutlineThickness()
    {
        return 2;
    }

    @ConfigItem(
        position = 28,
        keyName = "enableClanMembers",
        name = "Enable clan member style",
        description = "Use clan member-specific nametag style instead of other players style",
        section = "playerRelationsSection"
    )
    default boolean enableClanMembers()
    {
        return false;
    }

    @Alpha
    @ConfigItem(
        position = 29,
        keyName = "clanMembersColour",
        name = "Clan members colour",
        description = "Colour used for clan members",
        section = "playerRelationsSection"
    )
    default Color clanMembersColour()
    {
        return new Color(0xFF42A5F5, true);
    }

    @ConfigItem(
        position = 64,
        keyName = "clanMembersOutlineEnabled",
        name = "Enable clan members outline",
        description = "Show outline for clan members",
        section = "playerRelationsOutlineSection"
    )
    default boolean clanMembersOutlineEnabled()
    {
        return true;
    }

    @Alpha
    @ConfigItem(
        position = 65,
        keyName = "clanMembersOutlineColour",
        name = "Clan members outline colour",
        description = "Outline colour for clan members",
        section = "playerRelationsOutlineSection"
    )
    default Color clanMembersOutlineColour()
    {
        return null;
    }

    @ConfigItem(
        position = 66,
        keyName = "clanMembersOutlineThickness",
        name = "Clan members outline thickness",
        description = "Outline thickness for clan members",
        section = "playerRelationsOutlineSection"
    )
    @Range(min = 1, max = 10)
    default int clanMembersOutlineThickness()
    {
        return 2;
    }

    @ConfigItem(
        position = 30,
        keyName = "enableClanChatMembers",
        name = "Enable chat channel member style",
        description = "Use chat channel member-specific nametag style instead of other players style",
        section = "playerRelationsSection"
    )
    default boolean enableClanChatMembers()
    {
        return false;
    }

    @Alpha
    @ConfigItem(
        position = 31,
        keyName = "clanChatMembersColour",
        name = "Chat channel members colour",
        description = "Colour used for chat channel members",
        section = "playerRelationsSection"
    )
    default Color clanChatMembersColour()
    {
        return new Color(0xFFFFB74D, true);
    }

    @ConfigItem(
        position = 32,
        keyName = "enableGuestClanMembers",
        name = "Enable clan members (Guest) style",
        description = "Use a separate style for members of the clan channel you joined as a guest",
        section = "playerRelationsSection"
    )
    default boolean enableGuestClanMembers()
    {
        return false;
    }

    @Alpha
    @ConfigItem(
        position = 33,
        keyName = "guestClanMembersColour",
        name = "Clan members (Guest) colour",
        description = "Colour used for members of the clan channel you joined as a guest",
        section = "playerRelationsSection"
    )
    default Color guestClanMembersColour()
    {
        return new Color(0xFFFF7043, true);
    }

    @ConfigItem(
        position = 34,
        keyName = "enableGuestsInYourClan",
        name = "Enable guests in your clan style",
        description = "Use a separate style for guest players in your clan channel",
        section = "playerRelationsSection"
    )
    default boolean enableGuestsInYourClan()
    {
        return false;
    }

    @Alpha
    @ConfigItem(
        position = 35,
        keyName = "guestsInYourClanColour",
        name = "Guests in your clan colour",
        description = "Colour used for guest players in your clan channel",
        section = "playerRelationsSection"
    )
    default Color guestsInYourClanColour()
    {
        return new Color(0xFF8D6E63, true);
    }

    @ConfigItem(
        position = 67,
        keyName = "clanChatMembersOutlineEnabled",
        name = "Enable chat channel members outline",
        description = "Show outline for chat channel members",
        section = "playerRelationsOutlineSection"
    )
    default boolean clanChatMembersOutlineEnabled()
    {
        return true;
    }

    @Alpha
    @ConfigItem(
        position = 68,
        keyName = "clanChatMembersOutlineColour",
        name = "Chat channel members outline colour",
        description = "Outline colour for chat channel members",
        section = "playerRelationsOutlineSection"
    )
    default Color clanChatMembersOutlineColour()
    {
        return null;
    }

    @ConfigItem(
        position = 69,
        keyName = "clanChatMembersOutlineThickness",
        name = "Chat channel members outline thickness",
        description = "Outline thickness for chat channel members",
        section = "playerRelationsOutlineSection"
    )
    @Range(min = 1, max = 10)
    default int clanChatMembersOutlineThickness()
    {
        return 2;
    }

    @ConfigItem(
        position = 70,
        keyName = "guestClanMembersOutlineEnabled",
        name = "Enable clan members (Guest) outline",
        description = "Show outline for members of the clan channel you joined as a guest",
        section = "playerRelationsOutlineSection"
    )
    default boolean guestClanMembersOutlineEnabled()
    {
        return true;
    }

    @Alpha
    @ConfigItem(
        position = 71,
        keyName = "guestClanMembersOutlineColour",
        name = "Clan members (Guest) outline colour",
        description = "Outline colour for members of the clan channel you joined as a guest",
        section = "playerRelationsOutlineSection"
    )
    default Color guestClanMembersOutlineColour()
    {
        return null;
    }

    @ConfigItem(
        position = 72,
        keyName = "guestClanMembersOutlineThickness",
        name = "Clan members (Guest) outline thickness",
        description = "Outline thickness for members of the clan channel you joined as a guest",
        section = "playerRelationsOutlineSection"
    )
    @Range(min = 1, max = 10)
    default int guestClanMembersOutlineThickness()
    {
        return 2;
    }

    @ConfigItem(
        position = 73,
        keyName = "guestsInYourClanOutlineEnabled",
        name = "Enable guests in your clan outline",
        description = "Show outline for guest players in your clan channel",
        section = "playerRelationsOutlineSection"
    )
    default boolean guestsInYourClanOutlineEnabled()
    {
        return true;
    }

    @Alpha
    @ConfigItem(
        position = 74,
        keyName = "guestsInYourClanOutlineColour",
        name = "Guests in your clan outline colour",
        description = "Outline colour for guest players in your clan channel",
        section = "playerRelationsOutlineSection"
    )
    default Color guestsInYourClanOutlineColour()
    {
        return null;
    }

    @ConfigItem(
        position = 75,
        keyName = "guestsInYourClanOutlineThickness",
        name = "Guests in your clan outline thickness",
        description = "Outline thickness for guest players in your clan channel",
        section = "playerRelationsOutlineSection"
    )
    @Range(min = 1, max = 10)
    default int guestsInYourClanOutlineThickness()
    {
        return 2;
    }

    @ConfigSection(
        name = "Culling & Positioning",
        description = "Control nametag placement, overlap behaviour, and compatibility with entity-hiding plugins",
        position = 30
    )
    String cullingSection = "cullingSection";

    @ConfigItem(
        position = 31,
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
        position = 32,
        keyName = "verticalOffset",
        name = "Vertical offset",
        description = "Pixels to offset the nametag (when above: adds to height; when below: subtracts from height)",
        section = "cullingSection"
    )
    @Range(min = 0, max = 9999)
    default int verticalOffset()
    {
        return 40;
    }

    @ConfigItem(
        position = 33,
        keyName = "anchorBelow",
        name = "Anchor below",
        description = "If enabled, position the nametag below entities instead of above",
        section = "cullingSection"
    )
    default boolean anchorBelow()
    {
        return false;
    }

    @ConfigItem(
        position = 34,
        keyName = "stackTags",
        name = "Stack overlapping nametags",
        description = "Shift nametags that would overlap vertically, closest entity keeps its natural position",
        section = "cullingSection"
    )
    default boolean stackTags()
    {
        return false;
    }

    @ConfigItem(
        position = 35,
        keyName = "respectEntityHiders",
        name = "Respect entity hiders",
        description = "Hide nametags when another render-hiding plugin suppresses the actor, e.g. Dynamic Entity Hider",
        section = "cullingSection"
    )
    default boolean respectEntityHiders()
    {
        return true;
    }

    @ConfigItem(
        position = 36,
        keyName = "overheadIconOffset",
        name = "Extra overhead lift for icons",
        description = "Extra upward offset added to nametags when an overhead icon is active (prayers, PK skull, etc.). Increase if nametag still overlaps the icon",
        section = "cullingSection"
    )
    @Range(min = 0, max = 150)
    default int overheadIconOffset()
    {
        return 25;
    }
}
