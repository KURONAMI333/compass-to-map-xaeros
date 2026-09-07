package com.kuronami.compasstomapxaeros.client;

import com.kuronami.compasstomapxaeros.network.DiscoveryPayload;

/**
 * S2C で受け取った {@link DiscoveryPayload} を Xaero に変換する (Forge 1.21.1)。
 *
 * <p>SimpleChannel の {@code consumerMainThread} が render thread (=Xaero state を触れる
 * 正規スレッド) で呼び出すため、enqueueWork 不要 — そのまま emit していい。
 */
public final class ClientDiscoveryHandler {

    private ClientDiscoveryHandler() {}

    public static void handle(DiscoveryPayload payload) {
        XaeroPersistentEmit.emit(payload.name, payload.x, payload.y, payload.z, payload.isBiome);
    }
}
