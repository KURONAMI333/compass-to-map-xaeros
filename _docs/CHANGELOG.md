# Changelog

All notable changes to Compass to Map: Xaero's edition will be documented in this file.
Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) — [Semver](https://semver.org/)

## [0.1.2] - 2026-09-07

### Fixed
- **同じバイオームを再検索するとピンが増えていた問題を修正**。Nature's Compass はプレイヤーの現在地を起点に 64 ブロック刻みの格子でバイオームを探すため、同じバイオームでも検索のたびに違う座標を返す。重複判定が座標を見ていたので、再検索のたびに数十ブロック隣へ新しいピンが立っていた。バイオームの判定から座標を外した（構造物は座標が決定的なので従来どおり座標で判定し、別の構造物には別のピンが立つ）
- **前の検索結果を表示したままのコンパスを持って別の次元へ入ると、その座標が移動先の地図に登録されていた問題を修正**。コンパスは「どの次元で検索したか」を持っておらず、次元を移動しても結果が残る。重複判定から次元を外し、ログイン直後の 1.5 秒は既存の結果を記録するだけで登録しないようにした
- **再ログインのたびに、コンパスが表示したままの結果が再登録されていた問題を修正**
- **構造物の重複判定が Y 座標を見ていたため、すり抜けることがあった問題を修正**。Y はチャンクのロード状況で変わる推定値なので、判定に使わないようにした

### Notes
- 0.1.1 までの変更履歴はこのファイルに反映されていない（下の 0.1.0 の項が最後の記載）

## [0.1.0] - YYYY-MM-DD (未公開 / MVP)

### Added
- **Xaero's Minimap 連携**: Explorer's Compass / Nature's Compass の発見時に、Xaero internal API を Reflection 直叩きで永続 waypoint を構築・追加 (プロンプト / 編集画面なし)
- **Explorer's Compass 対応**: 構造物発見時の自動 waypoint 登録
- **Nature's Compass 対応**: バイオーム発見時の自動 waypoint 登録
- **Custom S2C packet** (`DiscoveryPayload`): サーバが該当プレイヤー 1 名にのみ name + x + y + z を送信。broadcast しない
- **Server-side dedupe**: `SEEN_KEYS` (dimension + ID + x + z) で同じ構造物の再検索を抑制、LRU 512 件上限、logout でクリア
- **Client-side dedupe**: 同一 (name, x, y, z) waypoint が既に WaypointSet にあればスキップ (Xaero 側に重複登録しない)
- **ブランド紫の固定色** (Xaero {@code WaypointColor.PURPLE})、`temporary=false` で永続 waypoint として保存
- **OP 限定の `/tp` 提案**: チャット通知の座標表示で OP のみクリック可能 TP 提案
- **Y 座標 fallback**: チャンク未ロードで Heightmap が world floor を返す場合の dimension/種別ごとの推定値
- **EC / NC 不在耐性**: Inner class isolation + ModList check + try-catch + 永久サスペンドフラグの多層防御
- **Config**: `feature.enabled` / `feature.enableStructure` / `feature.enableBiome` / `notification.notifyOnFound`
- **マルチローダー対応**: NeoForge 1.21.1 / Forge 1.21.1 / Forge 1.20.1 / Fabric 1.21.1 / Fabric 1.20.1

### Architecture notes
- Xaero's は closed-source で公式 Java API なし。本 MOD は **Reflection で internal API を直叩き** (compileOnly すら使わない)
- 全 Reflection paths で `catch (Throwable)` → null/false。Xaero's 不在 / API 不一致 / セッション未起動なら silent fail で何もしない
- サーバ → クライアントの custom packet 経路のため、**サーバ・クライアント両方に本 MOD を入れる必要がある**
- EC / NC は Inner class (`ECInner` / `NCInner`) に参照を閉じ込めて `NoClassDefFoundError` を避ける
- 1.20.1 系は DataComponent 未導入のため、EC/NC は instance method (`explorersCompass.getStructureKey(stack)` 等) 経由でアクセス
- Fabric は EC/NC の field 命名が違う (`_COMPONENT` サフィックス、`utils.CompassState`) ため CompassWatcher を loader 別に書く

### Compatibility
- Minecraft 1.21.1 (NeoForge / Forge / Fabric) または 1.20.1 (Forge / Fabric)
- **Optional**: Explorer's Compass (構造物検出、未導入時はバイオーム機能のみ)
- **Optional**: Nature's Compass (バイオーム検出、未導入時は構造物機能のみ)
- **Optional (client)**: Xaero's Minimap / Xaero's World Map (未導入時は chat 通知だけ、Reflection は silent fail)
- EC/NC のうち少なくとも 1 つあれば検出機能あり、両方無くても crash せず idle
- Fabric は Forge Config API Port (FCAP) 経由で Config を扱う

Sister mod: Ping to Map: Xaero's edition (Ping-Wheel × Xaero's addon)
