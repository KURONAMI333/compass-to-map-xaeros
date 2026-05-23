package com.kuronami.compasstomapxaeros.network;

import net.minecraft.network.FriendlyByteBuf;

/**
 * サーバ → クライアント の発見通知 payload (Forge 1.20.1, SimpleChannel 用)。
 *
 * <p>Forge {@code SimpleChannel} はクラス + encode/decode method ペアで wire format を定義する
 * （NeoForge の {@code StreamCodec} 構成とは別）。
 */
public final class DiscoveryPayload {

    public final String name;
    public final int x;
    public final int y;
    public final int z;

    public DiscoveryPayload(String name, int x, int y, int z) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /** SimpleChannel encoder. */
    public static void encode(DiscoveryPayload p, FriendlyByteBuf buf) {
        buf.writeUtf(p.name);
        buf.writeInt(p.x);
        buf.writeInt(p.y);
        buf.writeInt(p.z);
    }

    /** SimpleChannel decoder. */
    public static DiscoveryPayload decode(FriendlyByteBuf buf) {
        String name = buf.readUtf();
        int x = buf.readInt();
        int y = buf.readInt();
        int z = buf.readInt();
        return new DiscoveryPayload(name, x, y, z);
    }
}
