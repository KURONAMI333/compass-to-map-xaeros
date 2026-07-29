package com.kuronami.compasstomapxaeros.event;

import com.kuronami.compasstomapxaeros.network.DiscoveryPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * サーバ側から発見プレイヤーへ {@link DiscoveryPayload} を unicast 送信するだけの薄ラッパー。
 *
 * <p>送信先は発見した本人 1 名のみ (broadcast しない)。Compass の発見は
 * 「持ってる本人にしか見えない情報」なので waypoint も本人にしか立てない。
 *
 * <p>Xaero への登録ロジック (Reflection 直叩き) はクライアント側
 * {@link com.kuronami.compasstomapxaeros.client.XaeroPersistentEmit} に完全に閉じてる。
 */
public final class ServerDispatch {

    private ServerDispatch() {}

    /**
     * 構造物 / バイオーム発見を該当プレイヤーのクライアントに通知する。
     *
     * @param player     発見したプレイヤー
     * @param prettyName 表示名（サーバ側で {@code CompassNames.prettify} 済み）
     * @param pos        world 座標
     */
    public static void send(ServerPlayer player, String prettyName, BlockPos pos) {
        PacketDistributor.sendToPlayer(
                player,
                new DiscoveryPayload(prettyName, pos.getX(), pos.getY(), pos.getZ())
        );
    }
}
