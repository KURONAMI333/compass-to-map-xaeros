package com.kuronami.compasstomapxaeros.client;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Xaero's Minimap の internal API への Reflection ラッパー（pure / 状態なし）。
 *
 * <p>Xaero's は公式 Java API を提供してないので、本 MOD はバイトコード解析で
 * 確定した public メソッド / public static field チェーンを Reflection で叩く。
 *
 * <p>叩く対象 (xaerominimap Fabric 1.21.1 build (Y6UMYlPY = 25.3.12) で内部クラスが同名であることを想定):
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

    private XaeroReflect() {}

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
            return false;
        }
    }
}
