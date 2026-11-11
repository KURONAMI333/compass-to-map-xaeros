package com.kuronami.compasstomapxaeros.client;

import com.kuronami.compasstomapxaeros.util.CompassNames;

/**
 * Compass 発見の S2C を受け取って、Xaero's Minimap に <b>永続 waypoint</b> を追加する。
 *
 * <p>P2MX (一時 waypoint) と違って:
 * <ul>
 *   <li>{@code temporary=false} で構築 — Xaero がディスクに保存し、再起動後も残る</li>
 *   <li>寿命管理なし (再訪用に永続)</li>
 *   <li>既存の waypoint があればスキップ (再検索で重複登録防止)。<b>照合は種別で分ける</b>
 *       — バイオームは名前だけ、構造物は名前と x/z (下の {@link #emit})</li>
 * </ul>
 *
 * <p>呼び出しは render thread から (ClientDiscoveryHandler が enqueueWork で確保)。
 */
public final class XaeroPersistentEmit {

    /** ブランド色（Xaero {@code WaypointColor.PURPLE} = index 13）。 */
    private static final String BRAND_COLOR_ENUM = "PURPLE";

    private XaeroPersistentEmit() {}

    /**
     * 発見した構造物 / バイオームを Xaero waypoint として追加する。
     *
     * @param name    表示名 (例: "Village", "Cherry Grove" — サーバで prettify 済み)
     * @param x       world X
     * @param y       world Y
     * @param z       world Z
     * @param isBiome バイオーム発見なら true (照合を名前だけにする)
     */
    public static void emit(String name, int x, int y, int z, boolean isBiome) {
        Object minimapWorld = XaeroReflect.getCurrentMinimapWorld();
        if (minimapWorld == null) return; // Xaero 不在 or セッション未起動
        Object waypointSet = XaeroReflect.getCurrentWaypointSet(minimapWorld);
        if (waypointSet == null) return;

        // 重複チェック。**種別で見るものが違う**（理由は DedupeKeys の javadoc）。
        //  - バイオーム: 再検索のたびに座標がぶれるので、名前が一致すればもう立っているとみなす
        //  - 構造物: 座標が決定的なので名前と x/z で見る（別の村には別のピンが立つ）
        // Y はどちらでも見ない。Y は estimateY が Heightmap から推定する値で、チャンクの
        // ロード状況で変わる。Y を含めると同じ構造物でもすり抜けて重複する。
        // バイオームは名前だけで見るぶん、利用者の手作りピンと衝突しないよう色でも絞る
        // （WaypointSet は利用者のピンと共有。詳細は XaeroReflect#hasWaypointNamed）。
        boolean exists = isBiome
                ? XaeroReflect.hasWaypointNamed(waypointSet, name, BRAND_COLOR_ENUM)
                : XaeroReflect.hasWaypointAt(waypointSet, name, x, z);
        if (exists) return;

        String initials = CompassNames.initialsOf(name);
        Object waypoint = XaeroReflect.newWaypoint(
                x, y, z, name, initials, BRAND_COLOR_ENUM, "NORMAL",
                false, // temporary=false → 永続 (構造物発見は再訪したい)
                true   // yIncluded=true → Y 座標を持つ
        );
        if (waypoint == null) return;

        XaeroReflect.addWaypoint(waypointSet, waypoint, true);  // 先頭挿入 (新規発見が waypoint list の上に見える)
    }
}
