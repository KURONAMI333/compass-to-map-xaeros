package com.kuronami.compasstomapxaeros.network;

import com.kuronami.compasstomapxaeros.CompassToMapXaeros;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Forge 1.20.1 SimpleChannel ベースのカスタムパケット登録。
 *
 * <p>1.21.1 と API が違う:
 *  - {@link NetworkRegistry#newSimpleChannel} でビルド
 *  - handler signature: {@code BiConsumer<MSG, Supplier<NetworkEvent.Context>>}
 *  - {@link SimpleChannel#send(PacketDistributor.PacketTarget, Object)} (target → msg の順)
 */
public final class Channel {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CompassToMapXaeros.MODID, "main"),
            () -> PROTOCOL_VERSION,
            v -> true,
            v -> true
    );

    private Channel() {}

    public static void register() {
        CHANNEL.registerMessage(
                0,
                DiscoveryPayload.class,
                DiscoveryPayload::encode,
                DiscoveryPayload::decode,
                Channel::handleDiscovery,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }

    private static void handleDiscovery(DiscoveryPayload payload, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        com.kuronami.compasstomapxaeros.client.ClientDiscoveryHandler.handle(payload)));
        ctx.setPacketHandled(true);
    }

    public static void sendToPlayer(ServerPlayer player, DiscoveryPayload payload) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
    }
}
