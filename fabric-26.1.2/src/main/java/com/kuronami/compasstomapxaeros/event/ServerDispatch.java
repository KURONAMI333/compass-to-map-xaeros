package com.kuronami.compasstomapxaeros.event;

import com.kuronami.compasstomapxaeros.network.DiscoveryPayload;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/**
 * サーバ側から発見プレイヤーへ {@link DiscoveryPayload} を unicast 送信する薄ラッパー (Fabric 26.1.2)。
 *
 * <p>送信先は発見した本人 1 名のみ (broadcast しない)。Compass の発見は
 * 「持ってる本人にしか見えない情報」なので waypoint も本人にしか立てない。
 */
public final class ServerDispatch {

    private ServerDispatch() {}

    public static void send(ServerPlayer player, String prettyName, BlockPos pos, boolean isBiome) {
        ServerPlayNetworking.send(
                player,
                new DiscoveryPayload(prettyName, pos.getX(), pos.getY(), pos.getZ(), isBiome)
        );
    }
}
