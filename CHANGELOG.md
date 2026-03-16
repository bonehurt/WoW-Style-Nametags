# Changelog

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