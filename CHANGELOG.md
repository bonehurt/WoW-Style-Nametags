# Changelog

## [1.4] - 2026-04-13

### Added
- Added a dedicated `Shopkeepers` NPC category for any NPC with a `Trade` menu option, with the usual toggle, label colour, and outline controls.
- Added per-category nametag font size controls.

## [1.3.1] - 2026-04-10

### Fixed
- Fixed passive NPCs becoming marked aggressive after being player-provoked and respawning.
- Various minor aggression / detection optimisations.

## [1.3] - 2026-04-09

### Added
- Added a dedicated `Friendly non-talkers` NPC category (e.g. Catch/Shear/Pet interactions like sheep, Master Farmers, and geese) with independent toggle, label colour, and full outline controls like other categories. These were previously lumped in with Passive NPCs or undetected, but now have their own distinct pink styling to differentiate them from Talk-to NPCs.
- Added session learning for always-aggressive NPC types: once a type is observed actively targeting you, it is treated as Aggressive pre-aggro for the rest of the session.

### Fixed
- Fixed non-aggressive attackable NPCs incorrectly showing as red by applying the OSRS 2× combat threshold to attack-only classification.
- Improved live NPC tracking so nametags recover more reliably after distance changes or missed spawn/despawn updates, including follower edge cases.
- Various nametag filtering and categorisation optimisations and improvements to code.
- Attack/friendly classification now prefers live menu and transformed composition data over base composition, preventing stale or hidden action arrays from producing wrong colours.

### Known Limitations
- Pre-aggro aggression cannot be predetermined from RuneLite API data alone for every NPC type; always-aggressive behavior is learned per session after the NPC type is first observed actively targeting the player.
- Due to the above & now using the correct 2x combat level threshold, some ALWAYS aggressive NPCs that were previously incorrectly classified as aggressive may now show as passive until you engage them and they are learned as aggressive for the session.

## [1.2.1] - 2026-04-05

### Added
- Added configuration option for the overhead icon vertical offset fix.

### Fixed
- Nameplates now appear above overhead prayers/icons instead of being hidden below them.
- Stacked players unculled by entity hiders now show nametags when "Respect entity hiders" is enabled.

## [1.2] - 2026-03-17

### Added
- Added optional compatibility with render-hiding plugins so nametags stay hidden when the underlying actor is culled, such as with Dynamic Entity Hider.
- Added update notification ping on first launch after update.

### Fixed
- Fixed NPC nametags that included inline formatting prefixes (e.g. `<cox=00ffff>Glowing crystal`) rendering the raw tag text instead of the visible name.
- Fixed issue with follower nametag remaining on screen on world hop or level change.

### Changes
- Updated README screenshots to more of a vanilla look.

## [1.1] - 2026-03-16

### Added
- Added player relationship style overrides for friends, clan members, chat channel members, clan members in a guest clan channel, and guests in your clan channel.
- Added full outline controls for each player relationship style group (enable toggle, colour override, thickness).
- Added guest-clan relationship detection in both directions:
    - Members of the clan channel you joined as a guest.
    - Guests inside your own clan channel.
- Added `Excluded NPC names` text filter (comma-separated) to always hide matching NPC nametags.
- Added `Excluded other player names` text filter (comma-separated) to always hide matching other-player nametags.
- Added normalized name-set parsing for exclusion filters (case-insensitive, trims whitespace, supports comma/semicolon/newline separators).

### Fixed
- Fixed inaccurate code comments to match actual behavior and grouping.
- Corrected minor config description typo for aggressive NPC toggle text.

## [1.0]

### Initial release
- Initial release with support for:
    - NPC nametags (aggressive, neutral, passive)
    - Player nametags (self, others, followers)
    - WoW-style vertical stacking to prevent overlapping nametags
    - Customisable colours and outlines
    - Anchor and offset controls