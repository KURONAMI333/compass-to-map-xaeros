package com.kuronami.compasstomapxaeros.client;

import com.kuronami.compasstomapxaeros.network.DiscoveryPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C で受け取った {@link DiscoveryPayload} を render thread で処理して
 * Xaero に waypoint を追加する。
 *
 * <p>NeoForge 1.21.x の {@code IPayloadContext#enqueueWork} は client 受信時に
 * render thread (=Xaero の state を触れる正規スレッド) でタスクを実行する。
 */
public final class ClientDiscoveryHandler {

    private ClientDiscoveryHandler() {}

    public static void handle(DiscoveryPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() ->
                XaeroPersistentEmit.emit(payload.name(), payload.x(), payload.y(), payload.z())
        );
    }
}
