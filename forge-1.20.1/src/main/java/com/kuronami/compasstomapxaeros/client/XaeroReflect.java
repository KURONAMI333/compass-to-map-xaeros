package com.kuronami.compasstomapxaeros.client;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Xaero's Minimap の internal API への Reflection ラッパー（pure / 状態なし）。
 *
 * <p>Xaero's は公式 Java API を提供してないので、本 MOD はバイトコード解析で
 * 確定した public メソッド / public static field チェーンを Reflection で叩く。
 *
 * <p>叩く対象 (xaerominimap NeoForge 1.21.1 25.3.13 で検証):
 * <ul>
 *   <li>{@code xaero.hud.minimap.BuiltInHudModules.MINIMAP} (public static field)</li>
 *   <li>{@code xaero.hud.module.HudModule#getCurrentSession()}</li>
 *   <li>{@code xaero.hud.minimap.module.MinimapSession#getWorldManager()}</li>
 *   <li>{@code xaero.hud.minimap.world.MinimapWorldManager#getCurrentWorld()}</li>
 *   <li>{@code xaero.hud.minimap.world.MinimapWorld#getCurrentWaypointSet()}</li>
 *   <li>{@code xaero.hud.minimap.waypoint.set.WaypointSet#add(Waypoint, boolean)}</li>
 *   <li>{@code xaero.hud.minimap.waypoint.set.WaypointSet#getWaypoints()} (重複チェック用)</li>
 *   <li>{@code xaero.common.minimap.waypoints.Waypoint#&lt;init&gt;(III, String, String, WaypointColor, WaypointPurpose, boolean, boolean)}</li>
 *   <li>{@code Waypoint#getX/getY/getZ/getName} (重複チェック用)</li>
 *   <li>{@code xaero.hud.minimap.waypoint.WaypointColor.PURPLE} (enum value)</li>
 *   <li>{@code xaero.hud.minimap.waypoint.WaypointPurpose.NORMAL} (enum value)</li>
 * </ul>
 *
 * <p>各メソッドは Xaero's 不在 / API 不一致 / セッション未起動の場合に
 * {@code null} (or {@code false}) を返す。呼び出し側は null チェック必須。
 */
public final class XaeroReflect {

    private static final Logger LOGGER = LoggerFactory.getLogger("compasstomapxaeros");

    /** Xaero's Minimap がクラスパスに居るかの一度限り判定キャッシュ。null=未判定 / true=居る / false=居ない。 */
    private static volatile Boolean xaeroPresent = null;

    /** Reflection 失敗 (API drift) の警告を 1 セッション 1 回だけ出すためのフラグ。 */
    private static final AtomicBoolean WARNED_API_DRIFT = new AtomicBoolean(false);

    private XaeroReflect() {}

    /**
     * Xaero's Minimap がクラスパスにロードされているかを判定 (結果はキャッシュ)。
     *
     * <p>不在＝Xaero を入れてないユーザーの正常状態 → silent fail で OK。
     * 居る＝API drift 検出時に warn する正当性がある状態。
     */
    private static boolean isXaeroPresent() {
        Boolean cached = xaeroPresent;
        if (cached != null) return cached;
        boolean present;
        try {
            Class.forName("xaero.hud.minimap.BuiltInHudModules");
            present = true;
        } catch (Throwable t) {
            present = false;
        }
        xaeroPresent = present;
        return present;
    }

    /**
     * Xaero 在中の状態で reflection 失敗 (API drift) を検出した時、初回 1 回だけ warn を吐く。
     *
     * <p>毎構造物発見ごとにログを吐かないため、`AtomicBoolean` で once-only ガード。
     * Xaero 不在環境では何も出さない (issue triage 時のノイズ削減)。
     */
    private static void warnApiDriftOnce(String operation, Throwable cause) {
        if (!isXaeroPresent()) return;
        if (WARNED_API_DRIFT.compareAndSet(false, true)) {
            LOGGER.warn("[compasstomapxaeros] Xaero's Minimap API drift detected at '{}' "
                    + "(Xaero is installed but reflection failed: {}). "
                    + "This addon's Xaero integration is disabled for this session. "
                    + "Please report at https://github.com/KURONAMI333/compass-to-map-xaeros/issues "
                    + "with your Xaero's Minimap version.",
                    operation, cause.toString());
        }
    }

    /** 現在の {@code MinimapWorld}。Xaero's 不在 / セッション未起動なら null。 */
    public static Object getCurrentMinimapWorld() {
        try {
            Class<?> builtInCls = Class.forName("xaero.hud.minimap.BuiltInHudModules");
            Object minimapModule = builtInCls.getField("MINIMAP").get(null);
            if (minimapModule == null) return null;

            Class<?> hudModuleCls = Class.forName("xaero.hud.module.HudModule");
            Object moduleSession = hudModuleCls.getMethod("getCurrentSession").invoke(minimapModule);
            if (moduleSession == null) return null;

            Class<?> minimapSessionCls = Class.forName("xaero.hud.minimap.module.MinimapSession");
            Object worldManager = minimapSessionCls.getMethod("getWorldManager").invoke(moduleSession);
            if (worldManager == null) return null;

            Class<?> worldManagerCls = Class.forName("xaero.hud.minimap.world.MinimapWorldManager");
            return worldManagerCls.getMethod("getCurrentWorld").invoke(worldManager);
        } catch (Throwable t) {
            warnApiDriftOnce("getCurrentMinimapWorld", t);
            return null;
        }
    }

    /** {@code MinimapWorld} から現在の {@code WaypointSet} を取り出す。 */
    public static Object getCurrentWaypointSet(Object minimapWorld) {
        if (minimapWorld == null) return null;
        try {
            Class<?> worldCls = Class.forName("xaero.hud.minimap.world.MinimapWorld");
            return worldCls.getMethod("getCurrentWaypointSet").invoke(minimapWorld);
        } catch (Throwable t) {
            warnApiDriftOnce("getCurrentWaypointSet", t);
            return null;
        }
    }

    /**
     * 同一 (name, x, y, z) の waypoint が既に WaypointSet 内にあるか調べる。
     * 構造物再検索で重複登録されないように呼び出し側で使う。
     */
    public static boolean hasWaypoint(Object waypointSet, String name, int x, int y, int z) {
        if (waypointSet == null) return false;
        try {
            Class<?> setCls = Class.forName("xaero.hud.minimap.waypoint.set.WaypointSet");
            Object iterable = setCls.getMethod("getWaypoints").invoke(waypointSet);
            if (!(iterable instanceof Iterable<?> it)) return false;

            Class<?> waypointCls = Class.forName("xaero.common.minimap.waypoints.Waypoint");
            Method getX = waypointCls.getMethod("getX");
            Method getY = waypointCls.getMethod("getY");
            Method getZ = waypointCls.getMethod("getZ");
            Method getName = waypointCls.getMethod("getName");

            for (Object wp : it) {
                if ((int) getX.invoke(wp) == x
                        && (int) getY.invoke(wp) == y
                        && (int) getZ.invoke(wp) == z
                        && name.equals(getName.invoke(wp))) {
                    return true;
                }
            }
            return false;
        } catch (Throwable t) {
            warnApiDriftOnce("hasWaypoint", t);
            return false;
        }
    }

    /**
     * {@code Waypoint} オブジェクトを構築する。
     * コンストラクタ: {@code Waypoint(int x, int y, int z, String name, String initials,
     * WaypointColor color, WaypointPurpose purpose, boolean temporary, boolean yIncluded)}
     */
    public static Object newWaypoint(int x, int y, int z, String name, String initials,
                                     String colorEnumName, String purposeEnumName,
                                     boolean temporary, boolean yIncluded) {
        try {
            Class<?> colorCls = Class.forName("xaero.hud.minimap.waypoint.WaypointColor");
            Object color = colorCls.getField(colorEnumName).get(null);

            Class<?> purposeCls = Class.forName("xaero.hud.minimap.waypoint.WaypointPurpose");
            Object purpose = purposeCls.getField(purposeEnumName).get(null);

            Class<?> waypointCls = Class.forName("xaero.common.minimap.waypoints.Waypoint");
            Constructor<?> ctor = waypointCls.getConstructor(
                    int.class, int.class, int.class,
                    String.class, String.class,
                    colorCls, purposeCls,
                    boolean.class, boolean.class
            );
            return ctor.newInstance(x, y, z, name, initials, color, purpose, temporary, yIncluded);
        } catch (Throwable t) {
            warnApiDriftOnce("newWaypoint", t);
            return null;
        }
    }

    /**
     * {@code WaypointSet.add(Waypoint, boolean)} を呼び出して waypoint を追加する。
     *
     * <p>Xaero の {@code WaypointSet.add(wp, flag)} のバイトコード解析:
     * {@code flag=true} なら {@code list.add(0, wp)} で先頭挿入、
     * {@code flag=false} なら {@code list.add(wp)} で末尾追加。
     */
    public static boolean addWaypoint(Object waypointSet, Object waypoint, boolean addToTop) {
        if (waypointSet == null || waypoint == null) return false;
        try {
            Class<?> setCls = Class.forName("xaero.hud.minimap.waypoint.set.WaypointSet");
            Class<?> waypointCls = Class.forName("xaero.common.minimap.waypoints.Waypoint");
            Method add = setCls.getMethod("add", waypointCls, boolean.class);
            add.invoke(waypointSet, waypoint, addToTop);
            return true;
        } catch (Throwable t) {
            warnApiDriftOnce("addWaypoint", t);
            return false;
        }
    }
}
