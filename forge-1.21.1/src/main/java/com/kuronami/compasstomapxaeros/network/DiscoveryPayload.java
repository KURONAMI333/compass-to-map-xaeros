package com.kuronami.compasstomapxaeros.network;

import net.minecraft.network.FriendlyByteBuf;

/**
 * サーバ → クライアント の発見通知 payload (Forge 1.21.1, SimpleChannel 用)。
 *
 * <p>Forge {@code SimpleChannel} はクラス + encode/decode method ペアで wire format を定義する
 * （NeoForge の {@code StreamCodec} 構成とは別）。
 *
 * <p>{@code isBiome} を運ぶのは、<b>クライアント側の既存 waypoint の照合を種別で分ける</b>ため。
 * バイオームは検索のたびに座標がぶれるので名前だけで照合し、構造物は座標が決定的なので
 * 名前と x/z で照合する (理由は {@code DedupeKeys} の javadoc)。prettify 済みの表示名からは
 * どちらか判別できないので、サーバ側で分かっている種別をそのまま運ぶ。
 */
public final class DiscoveryPayload {

    public final String name;
    public final int x;
    public final int y;
    public final int z;
    public final boolean isBiome;

    public DiscoveryPayload(String name, int x, int y, int z, boolean isBiome) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.isBiome = isBiome;
    }

    /** SimpleChannel encoder. */
    public static void encode(DiscoveryPayload p, FriendlyByteBuf buf) {
        buf.writeUtf(p.name);
        buf.writeInt(p.x);
        buf.writeInt(p.y);
        buf.writeInt(p.z);
        buf.writeBoolean(p.isBiome);
    }

    /** SimpleChannel decoder. */
    public static DiscoveryPayload decode(FriendlyByteBuf buf) {
        String name = buf.readUtf();
        int x = buf.readInt();
        int y = buf.readInt();
        int z = buf.readInt();
        boolean isBiome = buf.readBoolean();
        return new DiscoveryPayload(name, x, y, z, isBiome);
    }
}
