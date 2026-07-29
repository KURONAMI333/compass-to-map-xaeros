package com.kuronami.compasstomapxaeros;

import com.kuronami.compasstomapxaeros.event.CompassWatcher;
import com.kuronami.compasstomapxaeros.network.DiscoveryPayload;

import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.neoforged.fml.config.ModConfig;

/**
 * Compass to Map: Xaero's edition - Fabric 26.1.2 Mod entry (server / common 側)。
 *
 * <p>クライアント側の packet 受信ハンドラは {@link CompassToMapXaerosClientFabric}。
 */
public class CompassToMapXaerosFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        // Config 登録 (FCAP 経由)
        ConfigRegistry.INSTANCE.register(CompassToMapXaeros.MODID, ModConfig.Type.COMMON, Config.SPEC);

        // S2C payload 型登録 (両側で登録が必要)
        // 26.1.2: fabric-api の PayloadTypeRegistry#playS2C/playC2S は
        // clientboundPlay/serverboundPlay に改名された (javap 実測、fabric-api 0.155.2+26.1.2)。
        PayloadTypeRegistry.clientboundPlay().register(DiscoveryPayload.TYPE, DiscoveryPayload.STREAM_CODEC);

        // Server tick listener — 全 player iterate で発見検出
        ServerTickEvents.END_SERVER_TICK.register(CompassWatcher::onServerTick);

        // Logout で player の dedupe set をクリア
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                CompassWatcher.onPlayerDisconnect(handler.player.getUUID())
        );

        CompassToMapXaeros.LOGGER.info("Compass to Map: Xaero's edition (Fabric 26.1.2) initialized");
    }
}
