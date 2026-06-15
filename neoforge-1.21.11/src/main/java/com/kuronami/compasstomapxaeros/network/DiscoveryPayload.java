package com.kuronami.compasstomapxaeros.network;

import com.kuronami.compasstomapxaeros.CompassToMapXaeros;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * サーバが構造物 / バイオーム発見を検出した時に該当プレイヤーへ送る S2C packet。
 *
 * <p>ペイロードはプレイヤー宛 unicast (broadcast しない)。受信側 (クライアント) は
 * {@link com.kuronami.compasstomapxaeros.client.ClientDiscoveryHandler} で
 * render thread に再ディスパッチして Xaero へ waypoint を追加する。
 *
 * <p>このペイロードは Xaero への chat-share 書式を使わない。代わりに発見情報
 * (name + coords) だけを運び、Xaero への登録 (Reflection 直叩き) は完全に
 * クライアント側で完結する。
 */
public record DiscoveryPayload(String name, int x, int y, int z) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<DiscoveryPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath(CompassToMapXaeros.MODID, "discovery"));

    public static final StreamCodec<FriendlyByteBuf, DiscoveryPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, DiscoveryPayload::name,
                    ByteBufCodecs.INT, DiscoveryPayload::x,
                    ByteBufCodecs.INT, DiscoveryPayload::y,
                    ByteBufCodecs.INT, DiscoveryPayload::z,
                    DiscoveryPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
