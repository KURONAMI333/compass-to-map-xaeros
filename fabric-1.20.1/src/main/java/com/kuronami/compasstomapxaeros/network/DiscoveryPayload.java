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
 */
public record DiscoveryPayload(String name, int x, int y, int z) implements FabricPacket {

    public static final PacketType<DiscoveryPayload> TYPE = PacketType.create(
            new ResourceLocation(CompassToMapXaeros.MODID, "discovery"),
            DiscoveryPayload::new
    );

    public DiscoveryPayload(FriendlyByteBuf buf) {
        this(buf.readUtf(), buf.readInt(), buf.readInt(), buf.readInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(name);
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }
}
