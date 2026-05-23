package com.kuronami.compasstomapxaeros.network;

import com.kuronami.compasstomapxaeros.CompassToMapXaeros;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

/**
 * Forge 1.21.1 SimpleChannel ベースのカスタムパケット登録。
 *
 * <p>Channel ID = "{@code compasstomapxaeros:main}", protocol version 1 (optional)。
 * 方向は PLAY_TO_CLIENT のみ。本 MOD は 1 種類の payload ({@link DiscoveryPayload}) しか使わない
 * (構造物・バイオームを区別しない、サーバが prettyName で両方を表す)。
 *
 * <p>クライアント受信ハンドラは {@link DistExecutor#unsafeRunWhenOn} で CLIENT-side 限定実行
 * → 専用サーバで Xaero クラス参照を避ける (Xaero は CLIENT のみのため)。
 */
public final class Channel {

    public static final SimpleChannel CHANNEL = ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(CompassToMapXaeros.MODID, "main"))
            .networkProtocolVersion(1)
            .optional()
            .simpleChannel();

    private Channel() {}

    /**
     * Mod entry の commonSetup から呼ばれる。
     */
    public static void register() {
        CHANNEL.messageBuilder(DiscoveryPayload.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DiscoveryPayload::encode)
                .decoder(DiscoveryPayload::decode)
                .consumerMainThread(Channel::handleDiscovery)
                .add();
    }

    private static void handleDiscovery(DiscoveryPayload payload, CustomPayloadEvent.Context ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.kuronami.compasstomapxaeros.client.ClientDiscoveryHandler.handle(payload));
        ctx.setPacketHandled(true);
    }

    /** サーバ側 dispatch ヘルパー。 */
    public static void sendToPlayer(ServerPlayer player, DiscoveryPayload payload) {
        CHANNEL.send(payload, PacketDistributor.PLAYER.with(player));
    }
}
