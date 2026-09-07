package com.kuronami.compasstomapxaeros.client;

import com.kuronami.compasstomapxaeros.network.DiscoveryPayload;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * S2C で受け取った {@link DiscoveryPayload} を Xaero に変換する (Fabric 1.20.1)。
 *
 * <p>{@code ClientPlayNetworking.PlayPacketHandler} の handler シグネチャは
 * {@code (packet, player, responseSender)}。受信は network thread なので
 * {@link Minecraft#execute} で render thread に再ディスパッチする。
 */
public final class ClientDiscoveryHandler {

    private ClientDiscoveryHandler() {}

    public static void handle(DiscoveryPayload payload, LocalPlayer player, PacketSender responseSender) {
        Minecraft.getInstance().execute(() ->
                XaeroPersistentEmit.emit(payload.name(), payload.x(), payload.y(), payload.z(),
                        payload.isBiome())
        );
    }
}
