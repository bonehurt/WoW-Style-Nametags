# WoW-Style Nametags

Displays floating nametags with optional max hit and combat level info above NPCs and players in a style inspired by World of Warcraft. Each nametag type can be made visible or hidden and is configurably colour-coded to make it immediately clear whether an NPC is aggressive, passive, neutral, or friendly - never right-click EVER again (slight exaggeration).

![Ooo look at all the nametags](screenshots/2026-03-17_18-08-19.png)

## Features

### Colour-coded NPC classification
NPCs are automatically classified and coloured by their type:

| Category | Default colour | Description |
|---|---|---|
| Aggressive | Red | Attackable NPCs that are currently targeting you, learned always-aggressive NPC types, or high-threat attack-only NPCs |
| Passive | Orange | Attack-only NPCs at or below the non-aggro threshold (combat level at most 2x yours) |
| Neutral | Yellow | NPCs that are both attackable and have a non-attack interaction ('Talk-to' or other interactions) |
| Friendly | Green | NPCs with a 'Talk-to' option (e.g. random towns folk, quest NPCs) |
| Friendly non-talkers | Pink | NPCs with non-attack interactions that are not 'Talk-to' (e.g. Catch, Shear, Pet) |
| Shopkeepers | White | NPCs with a 'Trade' interaction option |
| Non-Attackable Animals | Mint | Non-attackable animals detected by interaction or a built-in namelist (e.g. stray dogs, pet cats, ducklings). Attackable animals are categorized under their respective combat categories (Passive/Aggressive). |
| Hunter Mobs | Pale Yellow | Hunter skill creatures (including box-trapped kebbits, snare birds, butterflies, and implings) with Catch, Net, Trap, Track, or Lasso interactions |


Notes:
- Examine-only NPCs are hidden by default to avoid false positives (for example, trees/resource-like entities).
- Aggressive NPCs are automatically identified using an extensive catalog sourced from the OSRS Wiki (over 800 always-aggressive monsters). Any uncatalogued aggressive NPCs are learned dynamically per session upon first observed aggression towards the player. Player-initiated attacks are excluded from this learning.


![Angry Chicken](screenshots/2026-03-17_18-13-13.png)

### Player & follower nametags
- **Your character**
- **Other players**
- **Your followers** (e.g. Pets, Cats)
- **Other players' followers**

### Player relationship styles
Optionally style the following groups separately from other players:
- **Friends**
- **Clan members**
- **Clan members (Guest)** (members of the clan channel you joined as a guest)
- **Guests in your clan** (guest players in your clan channel)
- **Chat channel members** (Friends Chat / FC)

If a player matches multiple groups, this priority is used:
**Friends > Clan members > Clan members (Guest) > Guests in your clan > Chat channel (Friends Chat) members > Other players**

### NPC name exclusions
You can hide nametags for specific NPC names regardless of other toggles using a comma-separated list, for example:
`banker, man`

### Boss and minion filters
Optionally hide nametags for primary bosses and/or encounter minions and adds using dedicated toggles (`Hide Bosses` and `Hide Boss Minions`) to reduce screen clutter during PvM encounters without needing to manually type each name into the exclusion list.

### Other player name exclusions
You can hide nametags for specific other players regardless of other toggles using a comma-separated list, for example:
`zezima, bonecute`

### Entity hider compatibility
Optionally hide nametags when another render-hiding plugin suppresses the underlying actor, which makes this play nicely with plugins such as Dynamic Entity Hider.

### Hover-only
Optionally hide all nametags until you move your cursor over an entity. Your own nametag remains visible regardless if enabled.

### Distance-based culling
Limit the number of nametags shown at once. Nametags are sorted by distance, closest entities always shown first.

### WoW-style vertical stacking
When multiple nametags would overlap on screen, they are automatically shifted vertically so no two tags obscure each other - like in World of Warcraft. The closest entity keeps its default position; further entities stack above (or below, based on your anchor setting).

From this...
![Ewww](screenshots/2026-03-17_18-00-12.png)
...to this!
![Ahhh](screenshots/2026-03-17_18-00-09.png)

### Stacked player culling
Limit how many player nametags are displayed on the exact same tile (e.g. at crowded minigames or banks). Prioritizes your own character and the top visually rendered player.

### Combat level display
Optionally display combat levels next to players and NPCs.

### NPC max hit display
Optionally display the maximum damage hit for NPCs on their nametag.

![More info than ever!](screenshots\MaxHitnLvl.png)

### Colour & outline customisation
Every category has its own independently configurable:
- Label colour
- Font size
- Outline toggle, colour, and thickness

### Position control
- **Anchor above / below** — place the label above or below the entity
- **Vertical offset** — fine-tune the exact pixel distance from the entity

![Angry Chicken, but below](screenshots/2026-03-17_18-13-09.png)

---

<sub>Current version: 2.0</sub>