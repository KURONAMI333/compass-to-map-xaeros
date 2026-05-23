# Compass to Map: Xaero's Minimap & Explorer's Compass & Nature's Compass Addon

> Auto-register found structures (Explorer's Compass) and biomes (Nature's Compass) as permanent Xaero's Minimap waypoints. No prompt, no edit screen — the waypoint just appears the instant you find it.

Found a structure or biome with Explorer's Compass or Nature's Compass, then had to **manually** punch the coordinates into Xaero's? This addon does it for you, the instant you find it. The waypoint appears silently on the minimap with zero UI interruption.

- ✨ **Auto-waypoint on discovery** — no prompt, no edit screen, no clicks
- 🧭 **Explorer's Compass + Nature's Compass supported** — structures, biomes, or both
- 🎨 **Brand-purple waypoint** — visually consistent, doesn't clash with vanilla Xaero colours
- 🛡️ **OP-only `/tp` suggestion** — operators get a clickable teleport suggestion in the chat notification; survival players just see the coordinates. Server-friendly.
- 🔁 **Duplicate-proof** — re-discovering the same structure won't add a second waypoint (server + client double dedupe)
- 💡 **Pure addon** — no items, no blocks, no textures. Just the automation.
- 🛟 **Safe without Xaero's** — silent fail if Xaero's isn't installed; no crash

## How it works

### Server side
Explorer's Compass / Nature's Compass store their discovery results on the compass item itself (DataComponent on 1.21+, NBT on 1.20.1). This mod watches each player's inventory in a server-side tick handler and detects fresh discoveries. When one fires, the server sends a single S2C custom packet (`DiscoveryPayload`: name + x + y + z) to that one player only — never broadcast.

### Client side
On packet receive, work is marshalled to the main client thread, then **reflection** is used to reach into Xaero's internal API:
```
xaero.hud.minimap.BuiltInHudModules.MINIMAP
  .getCurrentSession()
  .getWorldManager()
  .getCurrentWorld()
  .getCurrentWaypointSet()
  → Construct a permanent (non-temporary) Waypoint and add it directly
```

A client-side dedup pass skips the add if an identical (name, x, y, z) waypoint already exists in the set. Combined with the server-side `SEEN_KEYS` dedup, you won't see duplicates from re-scans or session restarts.

All reflection paths swallow exceptions silently, so an Xaero update / API rename / Xaero absence just yields a no-op (no crash, no console spam). This mod has zero compile-time reference to Xaero's classes.

### Both sides required
Because the architecture is server-side detect → custom packet → client-side reflect, **this mod must be installed on both server and client**. Xaero's Minimap itself is client-side only as usual.

## Supported loaders / versions

| Minecraft | NeoForge | Forge | Fabric |
|---|:---:|:---:|:---:|
| 1.21.1 | ✅ | ✅ | ✅ |
| 1.20.1 | — | ✅ | ✅ |

NeoForge has no 1.20.1 release.

## Configuration

`config/compasstomapxaeros-common.toml` or the Mod Settings GUI:

| Key | Default | Description |
|---|---|---|
| `feature.enabled` | true | Master switch |
| `feature.enableStructure` | true | Explorer's Compass structure waypoints |
| `feature.enableBiome` | true | Nature's Compass biome waypoints |
| `notification.notifyOnFound` | true | Chat notification on discovery (waypoint registration is independent) |

## Compatibility

| Mod | Support | Note |
|---|---|---|
| **Explorer's Compass** | optional | Structure-detection host |
| **Nature's Compass** | optional | Biome-detection host |
| **Xaero's Minimap** | optional (CLIENT only) | Waypoint target |
| **Xaero's World Map** | optional (CLIENT only) | Shares the same waypoint store with Minimap |
| JourneyMap | not targeted by this build | Use the JourneyMap sister mod instead |

At least one of Explorer's Compass / Nature's Compass is required for detection to do anything; with neither, the mod loads and stays idle.

## Install

1. Install **NeoForge / Forge / Fabric** for 1.21.1 or 1.20.1
2. Install at least one of [Explorer's Compass](https://modrinth.com/mod/explorers-compass) / [Nature's Compass](https://modrinth.com/mod/natures-compass) (both is fine)
3. Install [Xaero's Minimap](https://modrinth.com/mod/xaeros-minimap) and/or [Xaero's World Map](https://modrinth.com/mod/xaeros-world-map) on the **client** (recommended)
4. **Fabric only:** add [Forge Config API Port](https://modrinth.com/mod/forge-config-api-port)
5. Drop `compasstomapxaeros-<version>.jar` from the releases page into `mods/` on **both server and client**

## License

MIT — modpack use, modification and redistribution OK, credit not required (welcome).

Author: KURONAMI · Built on Explorer's Compass / Nature's Compass / Xaero's Minimap / Xaero's World Map. Sister mod: Ping to Map: Xaero's edition.
