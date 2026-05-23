# Compass to Map: Xaero's Minimap & Explorer's Compass & Nature's Compass Addon

> Explorer's Compass / Nature's Compass で構造物・バイオームを発見した瞬間に、Xaero's Minimap に**永続 waypoint** を自動登録する。プロンプト無し、UI 介入ゼロ。

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Modrinth](https://img.shields.io/badge/Modrinth-compass--to--map--xaeros-00AF5C)](https://modrinth.com/mod/compass-to-map-xaeros)
[![CurseForge](https://img.shields.io/badge/CurseForge-compass--to--map--xaeros-F16436)](https://www.curseforge.com/minecraft/mc-mods/compass-to-map-xaeros)

---

## Supported Loaders / Versions

| Minecraft | NeoForge | Forge | Fabric |
|---|:---:|:---:|:---:|
| 1.21.1 | ✅ | ✅ | ✅ |
| 1.20.1 | — | ✅ | ✅ |

NeoForge は 1.20.1 リリース無し。

---

## なにをするやつ?

Explorer's Compass / Nature's Compass で構造物・バイオームを見つけても、座標を Xaero's Minimap に **手動で waypoint 登録**するのが面倒だった。
このアドオン MOD はその手間を **完全に自動化**する。発見した瞬間、ミニマップに紫の waypoint が静かに現れる。プロンプトも編集画面も出ない。

- ✨ **発見した瞬間に Xaero's Minimap に永続 waypoint** — プロンプト無し、UI 介入ゼロ
- 🧭 **Explorer's Compass + Nature's Compass 両対応**
- 🎨 **ブランド紫の固定色 waypoint** — Xaero's 既存色と被らない識別マーカー
- 🛡️ **OP 限定の `/tp` 提案** — チャット通知の座標が OP のみクリック可能 TP 提案になる
- 🔁 **重複防止** — 同じ構造物の再検索で waypoint が増殖しない (server + client 両方で dedupe)
- 💡 **シンプル**: テクスチャ・モデル無し、純粋な機能アドオン
- 🛟 **Xaero's 不在でも crash しない** — 何も起きないだけ (silent fail)

---

## How it works

### サーバサイド
Explorer's Compass / Nature's Compass は構造物・バイオーム発見時に Item の DataComponent (1.21+) または NBT (1.20.1) に結果を保持する。本 MOD は `PlayerTickEvent.Post` (NeoForge/Forge) または `ServerTickEvents.END_SERVER_TICK` (Fabric) で各プレイヤーの inventory を走査して発見を検出。

発見すると **custom S2C packet** (`DiscoveryPayload`: name + x + y + z) を該当プレイヤー 1 名にのみ送る (broadcast しない)。EC/NC の発見は持ち主にしか見えない情報なので waypoint も本人にだけ立てる。

### クライアントサイド
packet を受信したら main thread にディスパッチして **Reflection** で Xaero's の internal API を直叩き:
```
xaero.hud.minimap.BuiltInHudModules.MINIMAP
  .getCurrentSession()
  .getWorldManager()
  .getCurrentWorld()
  .getCurrentWaypointSet()
  → Waypoint オブジェクトを直接構築 (temporary=false で永続) して add
```

重複チェック: 同一 (name, x, y, z) waypoint が既に WaypointSet にあればスキップ。サーバ side の SEEN_KEYS と合わせて二重防御。

Xaero's に何の参照も持たない (compileOnly すらしない)。Reflection で全例外を catch するので Xaero's 不在 / API 不一致 / セッション未起動なら silent fail で何もしない。

### サーバ・クライアント両側に必須
custom packet 経路のため、**サーバ・クライアント両方に本 MOD を入れる必要がある**。Xaero's 自体はクライアント側のみで OK (サーバには不要)。

---

## Installation

1. **NeoForge / Forge / Fabric** いずれかの 1.21.1 or 1.20.1 を導入
2. 以下から少なくとも 1 つ導入 (両方でも OK):
   - [Explorer's Compass](https://modrinth.com/mod/explorers-compass) — 構造物検出
   - [Nature's Compass](https://modrinth.com/mod/natures-compass) — バイオーム検出
3. **クライアント側**に [Xaero's Minimap](https://modrinth.com/mod/xaeros-minimap) および/または [Xaero's World Map](https://modrinth.com/mod/xaeros-world-map) を導入（推奨）
4. **Fabric** のみ: [Forge Config API Port](https://modrinth.com/mod/forge-config-api-port) を追加導入
5. リリースページから `compasstomapxaeros-<version>.jar` を `mods/` に放り込む (**サーバ・クライアント両方**)

---

## Configuration

`config/compasstomapxaeros-common.toml` または NeoForge / Forge の Mod Settings GUI:

| キー | 既定 | 説明 |
|---|---|---|
| `feature.enabled` | true | マスタースイッチ |
| `feature.enableStructure` | true | Explorer's Compass の構造物 waypoint 登録 |
| `feature.enableBiome` | true | Nature's Compass のバイオーム waypoint 登録 |
| `notification.notifyOnFound` | true | 発見時のチャット通知 (waypoint 自体は通知 OFF でも立つ) |

---

## Compatibility

| MOD | サポート | 備考 |
|---|---|---|
| **Explorer's Compass** | optional | 構造物検出のホスト |
| **Nature's Compass** | optional | バイオーム検出のホスト |
| **Xaero's Minimap** | optional (CLIENT のみ) | waypoint 登録のターゲット |
| **Xaero's World Map** | optional (CLIENT のみ) | Minimap と同じ waypoint store を共有 |
| JourneyMap | 本ビルドの対象外 | JourneyMap 版の姉妹 MOD を使ってください |

注: EC / NC のうち少なくとも 1 つ無いと検出機能は動かない。両方無くても起動はする (idle 状態)。

---

## FAQ

**Q. 既存ワールドに途中から入れて壊れない？**
A. はい、安全です。ロード時から自動で動作します。

**Q. waypoint が「Add to waypoints?」プロンプト経由で立たないけど、これで合ってる？**
A. はい、本 MOD は Xaero's 内部 API を Reflection で直叩きして **プロンプト無しで永続 waypoint を直接追加** します。UI 介入ゼロが仕様です。

**Q. JourneyMap も使ってるけど対応してくれる？**
A. JourneyMap 版の姉妹 MOD を別途公開。本 MOD は Xaero's に特化。

**Q. Nature's Compass（バイオーム検索）も対応してる？**
A. 対応済み。NC を入れてれば自動でバイオーム waypoint も登録されます。NC が入ってなくても構造物機能だけで動作します。

**Q. 自分の作った modpack に入れていい？**
A. もちろん。MIT ライセンスなので modpack 利用 OK、許可・通知不要です。

**Q. シングルプレイとマルチプレイ両方で動く？**
A. 両方対応。マルチサーバではサーバ・クライアント両方に本 MOD を入れてください（Xaero's はクライアントのみで OK）。

**Q. waypoint がたまりすぎて困る**
A. Xaero's の waypoint 管理画面から手動削除できます。

**Q. Xaero's を入れずに使うと？**
A. Reflection が silent fail して何も起きません (crash はしません)。チャット通知は出ます (OP には `/tp` 提案付き)。

---

## Bug Reports / Feature Requests

GitHub Issues: [Issues](https://github.com/KURONAMI333/compass-to-map-xaeros/issues)

---

## License

[MIT License](LICENSE)

---

## Credits

- Author: KURONAMI
- Assist: Claude (Anthropic)
- Built on:
  - [Explorer's Compass](https://modrinth.com/mod/explorers-compass) by ChaosTheDude / MattCzyr
  - [Nature's Compass](https://modrinth.com/mod/natures-compass) by ChaosTheDude / MattCzyr
  - [Xaero's Minimap](https://modrinth.com/mod/xaeros-minimap) by xaero96
  - [Xaero's World Map](https://modrinth.com/mod/xaeros-world-map) by xaero96
- Sister mod: [Ping to Map: Xaero's edition](https://github.com/KURONAMI333/ping-to-map-xaeros)
