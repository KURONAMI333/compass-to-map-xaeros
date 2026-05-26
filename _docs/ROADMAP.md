# Compass to Map: Xaero's edition — ROADMAP

## ✅ Phase 1: コア機能 (v0.1 MVP)

- [x] Explorer's Compass の DataComponent (1.21+) / instance method (1.20.1) 監視
- [x] Nature's Compass の DataComponent (1.21+) / instance method (1.20.1) 監視
- [x] Loader 別 server tick で発見検出 (`PlayerTickEvent.Post` for Forge/NeoForge, `ServerTickEvents.END_SERVER_TICK` for Fabric)
- [x] **Custom S2C packet** で発見プレイヤー 1 名にのみ name + x + y + z を送信 (broadcast しない)
- [x] **Reflection** で Xaero internal API を直叩き、永続 Waypoint 構築 → `WaypointSet.add(wp, true)` で先頭挿入
- [x] **Server-side dedupe**: SEEN_KEYS (dimension + ID + x + z) で同じ構造物の再検索を抑制、LRU 512 件上限、logout でクリア
- [x] **Client-side dedupe**: 同一 (name, x, y, z) waypoint が既に WaypointSet にあればスキップ
- [x] **Y 座標 fallback**: チャンク未ロード時の dimension/種別ごとの推定値
- [x] **OP 限定 `/tp` 提案**: チャット通知の座標で OP のみクリック可能 TP 提案
- [x] **EC / NC への参照を Inner class** (ECInner / NCInner) に分離 → `NoClassDefFoundError` 回避
- [x] **EC / NC API 不一致時に永久サスペンドフラグ** (`ecApiBroken` / `ncApiBroken`)
- [x] **silent fail**: Xaero 不在 / API 不一致は全 catch で吸収、crash しない
- [x] Config: `feature.enabled` / `feature.enableStructure` / `feature.enableBiome` / `notification.notifyOnFound`
- [x] 22 言語 lang ファイル (zh_cn/zh_tw は `%1$s`/`%2$s` 位置指定で引数順正しく)
- [x] LICENSE (MIT) を jar 同梱
- [x] **マルチローダー対応**: NeoForge 1.21.1 / Forge 1.21.1 / Forge 1.20.1 / Fabric 1.21.1 / Fabric 1.20.1

## 🔮 Phase 2: UX 改善

- [ ] **構造物別の有効/無効化 Config** (例: 村は登録しない、要塞だけ登録)
- [ ] **waypoint 名のカスタムフォーマット** (Config で `%name%` `%coords%` 等)
- [ ] **チャンク強制ロード or async 高さ取得** (Y 座標を構造物の実位置に近づける)
- [ ] **namespace prefix** (modded 構造物の名前衝突対策)
- [ ] **Reflection cache** (起動時 lazy init)
- [x] ~~**silent fail の初回 1 回 LOGGER.warn** (公開後の issue triage コスト削減)~~ → **v0.1.1 で前倒し実装済** (`XaeroReflect#warnApiDriftOnce`、`isXaeroPresent` で Xaero 不在時は黙る、`AtomicBoolean` で once-only、5 catch 全てで呼出)

## 🚀 Phase 3: 公開・コミュニティ

- [ ] スクリーンショット / GIF（発見 → 即 waypoint シーン）
- [ ] Modrinth / CurseForge 公開
- [ ] Discord / Wiki

---

## 設計判断の記録

| 判断 | 理由 |
|---|---|
| Server-side 監視 + custom packet | EC / NC は ItemStack DataComponent / NBT に検出結果を保持、サーバから観測可。Mixin/AT 不要 |
| Reflection で Xaero internal API を直叩き | Xaero に公式 Java API なし、chat-share 経路は「プロンプト → ワンクリック」UX なので「副次的に立つ永続 waypoint」仕様には合わない。Reflection で内部 API を呼ぶしか手段がない |
| EC / NC を Inner class isolation | EC / NC 不在環境でも CompassWatcher 本体クラスロードを失敗させない |
| dedupe (dimension + ID + x + z) を server + client 二重 | 別 dim の同座標構造物の誤マッチ防止 + クライアントセッション再起動後の重複防止 |
| ecApiBroken / ncApiBroken 永久サスペンド | API 不一致時に毎 tick × 全プレイヤーのログ汚染を防ぐ |
| OP 限定 TP 提案 | サバイバルプレイヤーに「TP できる」誤期待を与えない |
| 単色 (Xaero PURPLE) | Xaero's の既存色と被らない識別マーカー。Phase 2 でカテゴリ別色分け予定 |
| サーバ・クライアント両側必須 | custom packet 経路のため。Xaero's 自体は CLIENT のみで OK |
| MIT ライセンス | modpack 採用しやすい |
