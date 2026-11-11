package com.kuronami.compasstomapxaeros.network;

import com.kuronami.compasstomapxaeros.CompassToMapXaeros;
import com.kuronami.compasstomapxaeros.client.ClientDiscoveryHandler;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Custom packet handler の登録 (mod event bus, ロード時 1 回)。
 *
 * <p>{@code "1"} は protocol version: 将来 ペイロード形式変更時 に bump する。
 * クライアント側の handler は {@code playToClient} で登録 — サーバが本 MOD 未導入
 * (例: バニラサーバ) でも、本 packet は登録不要 (optional) なので接続できる挙動を
 * 取りたい場合は {@code optional()} を呼ぶ必要があるが、本 MOD はサーバ・クライアント
 * 両方への導入を必須とする想定なので省略。
 */
@EventBusSubscriber(modid = CompassToMapXaeros.MODID)
public final class Channel {

    private Channel() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(
                DiscoveryPayload.TYPE,
                DiscoveryPayload.STREAM_CODEC,
                ClientDiscoveryHandler::handle
        );
    }
}
