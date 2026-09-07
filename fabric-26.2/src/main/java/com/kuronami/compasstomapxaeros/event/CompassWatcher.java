package com.kuronami.compasstomapxaeros.event;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.kuronami.compasstomapxaeros.CompassToMapXaeros;
import com.kuronami.compasstomapxaeros.Config;
import com.kuronami.compasstomapxaeros.util.CompassNames;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

/**
 * Explorer's Compass / Nature's Compass の DataComponent を監視して
 * 構造物・バイオーム発見を検出する。
 *
 * 設計:
 *  - **EC / NC とも optional**: どちらか片方、両方、いずれの構成でも起動可能
 *  - 各 MOD への参照は Inner class (ECInner / NCInner) に分離して NoClassDefFoundError 回避
 *  - サーバ側 PlayerTickEvent.Post で各 player の inventory を 1 回走査して両方検出
 *  - dedupe key は {@link DedupeKeys}（種別で座標の扱いが違う。理由はそちらの javadoc）
 *  - 各検出パスは独立 try-catch + 永久サスペンドフラグで他方の障害から隔離
 *  - EC / NC どちらも無くても起動するが、機能はしない (ログだけ出る)
 */
public final class CompassWatcher {

    /** 各 player の観測状態 (dedupe / ログイン時持ち越しの判定用)。ログアウト時に破棄。 */
    private static final Map<UUID, PlayerState> STATES = new ConcurrentHashMap<>();

    /** EC API 不一致時に EC 監視を止めるフラグ (再起動まで再開しない) */
    private static volatile boolean ecApiBroken = false;
    /** NC API 不一致時に NC 監視を止めるフラグ */
    private static volatile boolean ncApiBroken = false;

    private CompassWatcher() {}

    /** ServerPlayConnectionEvents.JOIN から呼ばれる (mod entry で登録)。 */
    public static void onPlayerJoin(java.util.UUID playerUuid) {
        STATES.put(playerUuid, new PlayerState());
    }

    /** ServerPlayConnectionEvents.DISCONNECT から呼ばれる (mod entry で登録)。 */
    public static void onPlayerDisconnect(java.util.UUID playerUuid) {
        STATES.remove(playerUuid);
    }

    /**
     * ServerTickEvents.END_SERVER_TICK から毎 tick 呼ばれる (mod entry で登録)。
     * Forge/NeoForge の per-player PlayerTickEvent 相当を、server 全 player iterate で再現する。
     */
    public static void onServerTick(MinecraftServer server) {
        if (!Config.ENABLED.get()) return;
        boolean ecLoaded = FabricLoader.getInstance().isModLoaded("explorerscompass");
        boolean ncLoaded = FabricLoader.getInstance().isModLoaded("naturescompass");

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!(player.level() instanceof ServerLevel serverLevel)) continue;

            // ログイン event を取りこぼした場合の受け皿。新規に作った state は必ず priming から始まる。
            PlayerState state = STATES.computeIfAbsent(player.getUUID(), k -> new PlayerState());
            boolean priming = state.consumePrimingTick();

            if (!ecApiBroken && ecLoaded && Config.ENABLE_STRUCTURE.get()) {
                try {
                    ECInner.tickCheck(player, serverLevel, state, priming);
                } catch (LinkageError | RuntimeException t) {
                    ecApiBroken = true;
                    CompassToMapXaeros.LOGGER.warn(
                            "Explorer's Compass API mismatch or class missing. Structure detection disabled until restart.",
                            t);
                }
            }

            if (!ncApiBroken && ncLoaded && Config.ENABLE_BIOME.get()) {
                try {
                    NCInner.tickCheck(player, serverLevel, state, priming);
                } catch (LinkageError | RuntimeException t) {
                    ncApiBroken = true;
                    CompassToMapXaeros.LOGGER.warn(
                            "Nature's Compass API mismatch or class missing. Biome detection disabled until restart.",
                            t);
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Explorer's Compass (構造物検出) - Inner class で isolation
    // ─────────────────────────────────────────────────────────────

    /**
     * EC API への参照を Inner class に閉じ込めることで、EC が classpath に無い時に
     * CompassWatcher 自体のクラスロードを失敗させない。
     */
    private static final class ECInner {

        static void tickCheck(ServerPlayer player, ServerLevel serverLevel,
                              PlayerState state, boolean priming) {
            Inventory inv = player.getInventory();
            ItemStack found = null;
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (isFound(stack)) {
                    found = stack;
                    break;
                }
            }
            if (found == null) return;

            String structureId = found.get(com.chaosthedude.explorerscompass.ExplorersCompass.STRUCTURE_ID);
            Integer x = found.get(com.chaosthedude.explorerscompass.ExplorersCompass.FOUND_X);
            Integer z = found.get(com.chaosthedude.explorerscompass.ExplorersCompass.FOUND_Z);
            if (structureId == null || x == null || z == null) return;

            String key = DedupeKeys.structure(structureId, x, z);
            if (!state.shouldRegister(key, x, z, priming)) return;

            int y = estimateY(serverLevel, x, z, structureId, /*isBiome=*/false);
            BlockPos pos = new BlockPos(x, y, z);

            String prettyName = CompassNames.prettify(structureId);
            ServerDispatch.send(player, prettyName, pos, /*isBiome=*/false);

            if (Config.NOTIFY_ON_FOUND.get()) {
                sendChatNotification(player, "message.compasstomapxaeros.structure_found", prettyName, x, y, z);
            }

            CompassToMapXaeros.LOGGER.info("Structure found by {}: {} @ ({}, ~{}, {})",
                    player.getName().getString(), structureId, x, y, z);
        }

        private static boolean isFound(ItemStack stack) {
            if (stack.isEmpty()) return false;
            if (!(stack.getItem() instanceof com.chaosthedude.explorerscompass.item.ExplorersCompassItem)) return false;
            Integer state = stack.get(com.chaosthedude.explorerscompass.ExplorersCompass.COMPASS_STATE);
            return state != null && state == com.chaosthedude.explorerscompass.util.CompassState.FOUND.getID();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Nature's Compass (バイオーム検出) - Inner class で isolation
    // ─────────────────────────────────────────────────────────────

    /**
     * NC API への参照を Inner class に閉じ込めることで、NC が classpath に無い時に
     * CompassWatcher 自体のクラスロードを失敗させない。
     */
    private static final class NCInner {

        static void tickCheck(ServerPlayer player, ServerLevel serverLevel,
                              PlayerState state, boolean priming) {
            Inventory inv = player.getInventory();
            ItemStack found = null;
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (isFound(stack)) {
                    found = stack;
                    break;
                }
            }
            if (found == null) return;

            String biomeId = found.get(com.chaosthedude.naturescompass.NaturesCompass.BIOME_ID);
            Integer x = found.get(com.chaosthedude.naturescompass.NaturesCompass.FOUND_X);
            Integer z = found.get(com.chaosthedude.naturescompass.NaturesCompass.FOUND_Z);
            if (biomeId == null || x == null || z == null) return;

            String key = DedupeKeys.biome(biomeId);
            if (!state.shouldRegister(key, x, z, priming)) return;

            int y = estimateY(serverLevel, x, z, biomeId, /*isBiome=*/true);
            BlockPos pos = new BlockPos(x, y, z);

            String prettyName = CompassNames.prettify(biomeId);
            ServerDispatch.send(player, prettyName, pos, /*isBiome=*/true);

            if (Config.NOTIFY_ON_FOUND.get()) {
                sendChatNotification(player, "message.compasstomapxaeros.biome_found", prettyName, x, y, z);
            }

            CompassToMapXaeros.LOGGER.info("Biome found by {}: {} @ ({}, ~{}, {})",
                    player.getName().getString(), biomeId, x, y, z);
        }

        private static boolean isFound(ItemStack stack) {
            if (stack.isEmpty()) return false;
            if (!(stack.getItem() instanceof com.chaosthedude.naturescompass.item.NaturesCompassItem)) return false;
            Integer state = stack.get(com.chaosthedude.naturescompass.NaturesCompass.COMPASS_STATE);
            return state != null && state == com.chaosthedude.naturescompass.util.CompassState.FOUND.getID();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 共通ヘルパー
    // ─────────────────────────────────────────────────────────────

    /**
     * Y 座標を Heightmap で推定。チャンク未ロード時は dimension/種別ごとの安全な Y を返す
     * (奈落落ち防止)。
     */
    private static int estimateY(ServerLevel level, int x, int z, String resourceId, boolean isBiome) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y <= level.getMinY() + 1) {
            String dim = level.dimension().identifier().toString();
            if ("minecraft:the_end".equals(dim)) return 64;
            if ("minecraft:the_nether".equals(dim)) return 96;
            if (isBiome) return 96;
            String lower = resourceId.toLowerCase();
            if (lower.contains("mineshaft") || lower.contains("dungeon")
                    || lower.contains("stronghold") || lower.contains("ancient_city")
                    || lower.contains("trial_chambers")) return 40;
            if (lower.contains("ocean_monument") || lower.contains("shipwreck")
                    || lower.contains("buried_treasure")) return 80;
            return 96;
        }
        return y;
    }

    /**
     * チャット通知 (構造物・バイオーム共通)。
     * 表示は X, Z のみ (Y は構造物の実位置とズレるため非表示)。
     * OP のみ /tp コマンド提案を有効化。
     */
    private static void sendChatNotification(ServerPlayer player, String translationKey,
                                              String prettyName, int x, int y, int z) {
        final boolean isOp = ((net.minecraft.server.level.ServerLevel) player.level()).getServer().getPlayerList().isOp(new net.minecraft.server.players.NameAndId(player.getGameProfile()));
        final String tpCmd = "/tp @s " + x + " " + y + " " + z;
        Component coord = Component.literal(x + ", " + z)
                .withStyle(s -> {
                    s = s.withColor(net.minecraft.ChatFormatting.LIGHT_PURPLE);
                    if (isOp) {
                        s = s.withUnderlined(true)
                                .withClickEvent(new ClickEvent.SuggestCommand(tpCmd))
                                .withHoverEvent(new HoverEvent.ShowText(
                                        Component.literal("Click to insert /tp command")));
                    }
                    return s;
                });
        // 26.1 で displayClientMessage(Component, boolean) は 2 分割された。
        // actionBar=false（チャット行）は Player#sendSystemMessage(Component) が等価。
        player.sendSystemMessage(
                Component.translatable(translationKey, prettyName, coord)
        );
    }
}
