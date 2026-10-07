# Publishing Checklist — Compass to Map: Xaero's edition

Modrinth / CurseForge への公開・更新手順。

準備中の版: **v0.1.3**（既存15セル）。配布先での公開状態は公開直前に直接確認する。
姉妹 MOD `compass-to-map` (JourneyMap 版) とは別プロジェクトとして公開する。

---

## 公開前チェックリスト

### コード・ビルド

- [ ] `mod_version` を全サブプロジェクト（ルート含む）の `gradle.properties` で更新（semver）
- [ ] 全サブプロジェクト（ルート含む）で `./gradlew clean build --no-build-cache`
- [ ] 全 `build/libs/compass-to-map-xaero-<modversion>+<loader>-<minecraft>.jar` のサイズ確認（compileOnly が混入してないこと）
- [ ] 全 jar に `LICENSE_compasstomapxaeros` が同梱されてること (`unzip -l <jar> | grep LICENSE`)
- [ ] 所有者付きの既存runclientラッパー で各 loader/MC で実機確認
  - [ ] Explorer's Compass で構造物検索 → 発見 → Xaero's に紫の永続 waypoint が**プロンプト無しで即座に立つ**
  - [ ] Nature's Compass でバイオーム検索 → 同上
  - [ ] チャット通知（OP = `/tp` クリック可、非 OP = 装飾なし）
  - [ ] 同一構造物の再検索で waypoint が増えない（server SEEN_KEYS + client hasWaypoint の二重 dedupe）
  - [ ] Xaero's を入れずに起動 → 何も起きない (silent fail、crash なし、chat 通知だけ出る)

### ドキュメント

- [ ] `README.md` の機能 / 設定 / 互換 / インストールが最新
- [ ] `LICENSE` 存在 (jar 同梱も build.gradle で処理済み)
- [ ] `_docs/CHANGELOG.md` 更新 (日付プレースホルダ確定)
- [ ] バージョン番号が各所一致（全 `gradle.properties`・mods.toml/fabric.mod.json・README）

### メタデータ

- [ ] Sourceリンクを確認し、不具合窓口はCurseForgeコメントまたはXの @kuronami333 のDMとする
- [ ] `description` が実装と整合 (古い "chat-share" 記述が残ってないこと、特に root NeoForge の neoforge.mods.toml)
- [ ] `authors=KURONAMI` / `license=All Rights Reserved`

---

## ストア事実（説明文・タグの基準）

| 項目 | 値 |
|---|---|
| Project name | `Compass to Map: Xaero's Minimap & Explorer's Compass & Nature's Compass Addon` |
| Slug | `compass-to-map-xaeros` |
| Mod ID | `compasstomapxaeros` / package `com.kuronami.compasstomapxaeros` |
| Loaders × MC | neoforge: 1.21.1, 1.21.11, 1.21.4, 1.21.8, 26.1.2, 26.2 / forge: 1.20.1, 1.21.1 / fabric: 1.20.1, 1.21.1, 1.21.11, 1.21.4, 1.21.8, 26.1.2, 26.2 |
| Environment | **Client + Server** (custom packet 経路のため両側必須。Xaero's 自体はクライアントのみ) |
| 依存 | **EC または NC のうち最低 1 つ** / Xaero's Minimap（waypoint 作成に必要・CLIENT のみ。World Map は併用可だが単独では不可）/ Fabric は FCAP も |
| 既知制限 | NeoForge 1.20.1 は存在しない (NeoForge 自体が 1.21+) |
| License | All Rights Reserved |

> Description 本文は `_docs/STORE_BODY_EN.md` をそのままコピペ（Modrinth は Markdown 可、CurseForge は WYSIWYG → Markdown モードに切り替えてからペースト）。jar 実ファイル名は `compass-to-map-xaero-<modversion>+<loader>-<minecraft>.jar`（Minecraft・loader・MOD版をファイル名とストアのタグで区別）。

---

## 更新リリース手順

1. 全 `gradle.properties` の `mod_version` を semver で更新
2. `_docs/CHANGELOG.md` 更新
3. 全サブプロジェクト（ルート含む） `./gradlew clean build --no-build-cache`
4. 実機で動作確認（最低限 NeoForge 1.21.1 で EC + NC + Xaero's 入りで 1 パターン、Xaero's 抜きで 1 パターン）
5. Modrinth: Versions タブから各 loader/MC の jar をアップロード（タグ付与）
6. CurseForge: Files タブから同上
7. GitHub: `vX.Y.Z` タグ付きリリース作成

この版は正式ビルド・JAR検査・実ホストAPI照合まで実施済み。実ゲーム起動・GUI・全host patchの動作成功は今回未確認。JourneyMap版とXaero版のJARは同じ命名形式を使用し、MOD ID・namespace・版番号は変更しない。
