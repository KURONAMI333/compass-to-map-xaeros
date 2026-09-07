package com.kuronami.compasstomapxaeros.client;

import com.kuronami.compasstomapxaeros.network.DiscoveryPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * S2C で受け取った {@link DiscoveryPayload} を Xaero に変換する (Fabric 26.1.2)。
 *
 * <p>{@code ClientPlayNetworking.Context} の {@code client().execute(...)} で
 * render thread (=Xaero state を触れる正規スレッド) に再ディスパッチする。
 */
public final class ClientDiscoveryHandler {

    private ClientDiscoveryHandler() {}

    public static void handle(DiscoveryPayload payload, ClientPlayNetworking.Context ctx) {
        ctx.client().execute(() ->
                XaeroPersistentEmit.emit(payload.name(), payload.x(), payload.y(), payload.z(),
                        payload.isBiome())
        );
    }
}
