package com.kuronami.compasstomapxaeros.network;

import com.kuronami.compasstomapxaeros.CompassToMapXaeros;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Fabric 1.20.1 用の発見通知 packet。
 *
 * <p>Fabric 1.20.1 は vanilla {@code CustomPacketPayload} を持たないため、
 * {@link FabricPacket} (旧 Fabric Networking API) で wire format を定義する。
 *
 * <p>{@code isBiome} を運ぶのは、<b>クライアント側の既存 waypoint の照合を種別で分ける</b>ため。
 * バイオームは検索のたびに座標がぶれるので名前だけで照合し、構造物は座標が決定的なので
 * 名前と x/z で照合する (理由は {@code DedupeKeys} の javadoc)。prettify 済みの表示名からは
 * どちらか判別できないので、サーバ側で分かっている種別をそのまま運ぶ。
 */
public record DiscoveryPayload(String name, int x, int y, int z, boolean isBiome) implements FabricPacket {

    public static final PacketType<DiscoveryPayload> TYPE = PacketType.create(
            new ResourceLocation(CompassToMapXaeros.MODID, "discovery"),
            DiscoveryPayload::new
    );

    public DiscoveryPayload(FriendlyByteBuf buf) {
        this(buf.readUtf(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readBoolean());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(name);
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeBoolean(isBiome);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }
}
