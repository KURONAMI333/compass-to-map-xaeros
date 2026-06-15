package com.kuronami.compasstomapxaeros;

import com.kuronami.compasstomapxaeros.event.CompassWatcher;
import com.kuronami.compasstomapxaeros.network.DiscoveryPayload;

import fuzs.forgeconfigapiport.fabric.api.forge.v4.ForgeConfigRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraftforge.fml.config.ModConfig;

/**
 * Compass to Map: Xaero's edition - Fabric 1.21.1 Mod entry (server / common 側)。
 *
 * <p>クライアント側の packet 受信ハンドラは {@link CompassToMapXaerosClientFabric}。
 */
public class CompassToMapXaerosFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        // Config 登録 (FCAP 経由)
        ForgeConfigRegistry.INSTANCE.register(CompassToMapXaeros.MODID, ModConfig.Type.COMMON, Config.SPEC);

        // S2C payload 型登録 (両側で登録が必要)
        PayloadTypeRegistry.playS2C().register(DiscoveryPayload.TYPE, DiscoveryPayload.STREAM_CODEC);

        // Server tick listener — 全 player iterate で発見検出
        ServerTickEvents.END_SERVER_TICK.register(CompassWatcher::onServerTick);

        // Logout で player の dedupe set をクリア
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                CompassWatcher.onPlayerDisconnect(handler.player.getUUID())
        );

        CompassToMapXaeros.LOGGER.info("Compass to Map: Xaero's edition (Fabric 1.21.1) initialized");
    }
}
