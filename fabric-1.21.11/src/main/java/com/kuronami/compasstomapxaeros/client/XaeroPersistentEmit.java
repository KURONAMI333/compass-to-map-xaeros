package com.kuronami.compasstomapxaeros.client;

import com.kuronami.compasstomapxaeros.util.CompassNames;

/**
 * Compass 発見の S2C を受け取って、Xaero's Minimap に <b>永続 waypoint</b> を追加する。
 *
 * <p>P2MX (一時 waypoint) と違って:
 * <ul>
 *   <li>{@code temporary=false} で構築 — Xaero がディスクに保存し、再起動後も残る</li>
 *   <li>寿命管理なし (再訪用に永続)</li>
 *   <li>同一 (name, x, y, z) の waypoint がすでにあればスキップ (再検索で重複登録防止)</li>
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
     * @param name 表示名 (例: "Village", "Cherry Grove" — サーバで prettify 済み)
     * @param x    world X
     * @param y    world Y
     * @param z    world Z
     */
    public static void emit(String name, int x, int y, int z) {
        Object minimapWorld = XaeroReflect.getCurrentMinimapWorld();
        if (minimapWorld == null) return; // Xaero 不在 or セッション未起動
        Object waypointSet = XaeroReflect.getCurrentWaypointSet(minimapWorld);
        if (waypointSet == null) return;

        // 重複チェック: 同じ (name, x, y, z) があればスキップ
        if (XaeroReflect.hasWaypoint(waypointSet, name, x, y, z)) return;

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
