Auto-registers the structures and biomes you find with Explorer's Compass and Nature's Compass as permanent Xaero's Minimap waypoints — no prompt, no edit screen, the waypoint just appears.

Find a structure or biome with Explorer's Compass / Nature's Compass and you normally have to add the coordinates to Xaero's by hand. This addon does it silently, the instant you find it.

**Features**

- Auto-waypoints structures (Explorer's Compass) and biomes (Nature's Compass), or both
- A brand-purple waypoint that doesn't clash with your other Xaero colours
- Operators get a clickable `/tp` suggestion in the discovery message; survival players just see the coordinates
- Duplicate-proof — re-discovering the same structure won't add a second waypoint
- No items, blocks, or textures, and it won't crash if Xaero's is absent (it has zero compile-time reference to Xaero's and reaches it by reflection that no-ops on absence or an API change)

**Config** (`config/compasstomapxaeros-common.toml`, or the Mod Settings GUI)

- `feature.enableStructure` / `feature.enableBiome` — toggle each compass
- `notification.notifyOnFound` — chat notification on discovery (waypoint registration is independent)

**Dependencies**

- At least one of [Explorer's Compass](https://modrinth.com/mod/explorers-compass) or [Nature's Compass](https://modrinth.com/mod/natures-compass) — required for detection
- [Xaero's Minimap](https://modrinth.com/mod/xaeros-minimap) and/or [Xaero's World Map](https://modrinth.com/mod/xaeros-world-map) (client) — the waypoint target
- Fabric only: [Forge Config API Port](https://modrinth.com/mod/forge-config-api-port)

For the JourneyMap version, see the sister mod Compass to Map.

Bugs and questions: comment on the CurseForge page, or DM @kuronami333 on X.

All Rights Reserved. Modpack inclusion is allowed without permission or credit. Source: https://github.com/KURONAMI333/compass-to-map-xaeros
