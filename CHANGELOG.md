# Changelog

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