package com.kuronami.compasstomapxaeros;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

import org.slf4j.Logger;

/**
 * Compass to Map: Xaero's Minimap & Explorer's Compass & Nature's Compass Addon.
 *
 * Explorer's Compass / Nature's Compass で構造物・バイオームを発見した瞬間、
 * その座標を Xaero's Minimap / World Map の waypoint として登録するよう促す
 * チャット行 ({@code xaero-waypoint:...}) をプレイヤーに送る。
 *
 * 仕組み:
 *  - サーバ側 {@code PlayerTickEvent.Post} で各プレイヤーの inventory を走査
 *  - Explorer's Compass / Nature's Compass の DataComponent を監視
 *  - state = FOUND かつ前回値と異なれば「新規発見」と判定
 *  - Xaero's の chat-share 書式の {@code System chat} 行をプレイヤーに送る
 *    → Xaero's のクライアント Mixin がそれを拾って「Add to waypoints?」プロンプトを表示
 *
 * Mixin / リフレクション不要 (Explorer's Compass / Nature's Compass は public static
 * フィールドで API 提供。Xaero's への連携も chat-share 経由なので API 依存なし)
 */
@Mod(CompassToMapXaeros.MODID)
public class CompassToMapXaeros {

    public static final String MODID = "compasstomapxaeros";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CompassToMapXaeros(IEventBus modEventBus, ModContainer modContainer) {
        // CompassWatcher は @EventBusSubscriber(modid = MODID) で NeoForge.EVENT_BUS に
        // 自動登録される。ここで再登録すると onPlayerTick が 1 tick あたり 2 回呼ばれるので
        // 明示登録はしない。

        // Config 登録
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
