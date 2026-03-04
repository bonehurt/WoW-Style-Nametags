# WoW-Style Nametags

Displays floating nametags above NPCs and players in a style inspired by World of Warcraft. Each entity type can be made visible or hidden and is colour-coded to make it immediately clear whether an NPC is aggressive, passive, friendly, or neutral, never right click EVER again (slight exaggeration).

![Ooo look at all the nametags](<screenshots/Screenshot 2026-03-03 181942.png>)

## Features

### Colour-coded NPC classification
NPCs are automatically classified and coloured by their type:

| Category | Default colour | Description |
|---|---|---|
| Aggressive | Red | NPCs that can only be attacked and are aggressive towards the player |
| Passive | Orange | Attackable NPCs whose combat level is below yours |
| Friendly | Green | NPCs with a Talk-to option (e.g. shopkeepers, quest NPCs) |
| Neutral | Yellow | NPCs that are both attackable and have talk-to options; (e.g. 'Man', 'Woman' entities in Lumbridge) |

![Angry Chicken](<screenshots/Screenshot 2026-03-03 182108.png>)

### Player & follower nametags
- **Your character**
- **Other players**
- **Your followers** (e.g. Pets, Cats)
- **Other players' followers**

### Hover-only
Optionally hide all nametags until you move your cursor over an entity. Your own nametag remains visible regardless if enabled.

### Distance-based culling
Limit the number of nametags shown at once. Nametags are sorted by distance, closest entities always shown first. Set to 0 for unlimited (default: 0).

### WoW-style vertical stacking
When multiple nametags would overlap on screen, they are automatically shifted vertically so no two tags obscure each other - like in World of Warcraft. The closest entity keeps its default position; further entities stack above (or below, based on your anchor setting).

From this...
![Ewww](<screenshots/Screenshot 2026-03-03 181841.png>)
...to this!
![Ahhh](<screenshots/Screenshot 2026-03-03 181853.png>)

### Colour & outline customisation
Every category has its own independently configurable:
- Label colour
- Outline toggle, colour, and thickness

### Position control
- **Anchor above / below** — place the label above or below the entity
- **Vertical offset** — fine-tune the exact pixel distance from the entity

![Angry Chicken, but below](<screenshots/Screenshot 2026-03-03 182125.png>)

---

<sub>Current version: 1.0</sub>