package com.kuronami.compasstomapxaeros.event;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.kuronami.compasstomapxaeros.CompassToMapXaeros;
import com.kuronami.compasstomapxaeros.Config;
import com.kuronami.compasstomapxaeros.util.CompassNames;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Fabric 1.20.1: NBT-based EC/NC API + chat 通知のみ (JM 統合 disable)。
 */
public final class CompassWatcher {

    /** 各 player の観測状態 (dedupe / ログイン時持ち越しの判定用)。ログアウト時に破棄。 */
    private static final Map<UUID, PlayerState> STATES = new ConcurrentHashMap<>();

    private static volatile boolean ecApiBroken = false;
    private static volatile boolean ncApiBroken = false;

    private CompassWatcher() {}

    /** ServerPlayConnectionEvents.JOIN から呼ばれる (mod entry で登録)。 */
    public static void onPlayerJoin(java.util.UUID playerUuid) {
        STATES.put(playerUuid, new PlayerState());
    }

    /** ServerPlayConnectionEvents.DISCONNECT から呼ばれる。 */
    public static void onPlayerDisconnect(java.util.UUID playerUuid) {
        STATES.remove(playerUuid);
    }

    public static void onServerTick(MinecraftServer server) {
        if (!Config.ENABLED.get()) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!(player.level() instanceof ServerLevel serverLevel)) continue;

            // ログイン event を取りこぼした場合の受け皿。新規に作った state は必ず priming から始まる。
            PlayerState state = STATES.computeIfAbsent(player.getUUID(), k -> new PlayerState());
            boolean priming = state.consumePrimingTick();

            if (!ecApiBroken && Config.ENABLE_STRUCTURE.get()
                    && FabricLoader.getInstance().isModLoaded("explorerscompass")) {
                try {
                    ECInner.tickCheck(player, serverLevel, state, priming);
                } catch (LinkageError | RuntimeException t) {
                    ecApiBroken = true;
                    CompassToMapXaeros.LOGGER.warn(
                            "Explorer's Compass API mismatch. Structure detection disabled until restart.", t);
                }
            }

            if (!ncApiBroken && Config.ENABLE_BIOME.get()
                    && FabricLoader.getInstance().isModLoaded("naturescompass")) {
                try {
                    NCInner.tickCheck(player, serverLevel, state, priming);
                } catch (LinkageError | RuntimeException t) {
                    ncApiBroken = true;
                    CompassToMapXaeros.LOGGER.warn(
                            "Nature's Compass API mismatch. Biome detection disabled until restart.", t);
                }
            }
        }
    }

    private static final class ECInner {
        static void tickCheck(ServerPlayer player, ServerLevel serverLevel,
                              PlayerState state, boolean priming) {
            Inventory inv = player.getInventory();
            ItemStack found = null;
            for (int i = 0; i < inv.items.size(); i++) {
                ItemStack stack = inv.items.get(i);
                if (isFound(stack)) { found = stack; break; }
            }
            if (found == null) {
                ItemStack off = inv.offhand.get(0);
                if (isFound(off)) found = off;
            }
            if (found == null) return;

            // EC Fabric 1.20.1: EXPLORERS_COMPASS_ITEM static field, getStructureID method
            ResourceLocation structureKey =
                    com.chaosthedude.explorerscompass.ExplorersCompass.EXPLORERS_COMPASS_ITEM.getStructureID(found);
            if (structureKey == null) return;
            int x = com.chaosthedude.explorerscompass.ExplorersCompass.EXPLORERS_COMPASS_ITEM.getFoundStructureX(found);
            int z = com.chaosthedude.explorerscompass.ExplorersCompass.EXPLORERS_COMPASS_ITEM.getFoundStructureZ(found);
            String structureId = structureKey.toString();

            String key = DedupeKeys.structure(structureId, x, z);
            if (!state.shouldRegister(key, x, z, priming)) return;

            int y = estimateY(serverLevel, x, z, structureId, false);
            String prettyName = CompassNames.prettify(structureId);

            ServerDispatch.send(player, prettyName, new net.minecraft.core.BlockPos(x, y, z), /*isBiome=*/false);

            if (Config.NOTIFY_ON_FOUND.get()) {
                sendChatNotification(player, "message.compasstomapxaeros.structure_found", prettyName, x, y, z);
            }

            CompassToMapXaeros.LOGGER.info("Structure found by {}: {} @ ({}, ~{}, {})",
                    player.getName().getString(), structureId, x, y, z);
        }

        private static boolean isFound(ItemStack stack) {
            if (stack.isEmpty()) return false;
            if (!(stack.getItem() instanceof com.chaosthedude.explorerscompass.items.ExplorersCompassItem)) return false;
            com.chaosthedude.explorerscompass.util.CompassState state =
                    com.chaosthedude.explorerscompass.ExplorersCompass.EXPLORERS_COMPASS_ITEM.getState(stack);
            return state == com.chaosthedude.explorerscompass.util.CompassState.FOUND;
        }
    }

    private static final class NCInner {
        static void tickCheck(ServerPlayer player, ServerLevel serverLevel,
                              PlayerState state, boolean priming) {
            Inventory inv = player.getInventory();
            ItemStack found = null;
            for (int i = 0; i < inv.items.size(); i++) {
                ItemStack stack = inv.items.get(i);
                if (isFound(stack)) { found = stack; break; }
            }
            if (found == null) {
                ItemStack off = inv.offhand.get(0);
                if (isFound(off)) found = off;
            }
            if (found == null) return;

            // NC Fabric 1.20.1: NATURES_COMPASS_ITEM, getBiomeID
            ResourceLocation biomeKey =
                    com.chaosthedude.naturescompass.NaturesCompass.NATURES_COMPASS_ITEM.getBiomeID(found);
            if (biomeKey == null) return;
            int x = com.chaosthedude.naturescompass.NaturesCompass.NATURES_COMPASS_ITEM.getFoundBiomeX(found);
            int z = com.chaosthedude.naturescompass.NaturesCompass.NATURES_COMPASS_ITEM.getFoundBiomeZ(found);
            String biomeId = biomeKey.toString();

            String key = DedupeKeys.biome(biomeId);
            if (!state.shouldRegister(key, x, z, priming)) return;

            int y = estimateY(serverLevel, x, z, biomeId, true);
            String prettyName = CompassNames.prettify(biomeId);

            ServerDispatch.send(player, prettyName, new net.minecraft.core.BlockPos(x, y, z), /*isBiome=*/true);

            if (Config.NOTIFY_ON_FOUND.get()) {
                sendChatNotification(player, "message.compasstomapxaeros.biome_found", prettyName, x, y, z);
            }

            CompassToMapXaeros.LOGGER.info("Biome found by {}: {} @ ({}, ~{}, {})",
                    player.getName().getString(), biomeId, x, y, z);
        }

        private static boolean isFound(ItemStack stack) {
            if (stack.isEmpty()) return false;
            if (!(stack.getItem() instanceof com.chaosthedude.naturescompass.items.NaturesCompassItem)) return false;
            // NC Fabric 1.20.1 は utils 単数 (1.21.1 と同じ)
            com.chaosthedude.naturescompass.utils.CompassState state =
                    com.chaosthedude.naturescompass.NaturesCompass.NATURES_COMPASS_ITEM.getState(stack);
            return state == com.chaosthedude.naturescompass.utils.CompassState.FOUND;
        }
    }

    // 共通
    private static int estimateY(ServerLevel level, int x, int z, String resourceId, boolean isBiome) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y <= level.getMinBuildHeight() + 1) {
            String dim = level.dimension().location().toString();
            if ("minecraft:the_end".equals(dim)) return 64;
            if ("minecraft:the_nether".equals(dim)) return 96;
            if (isBiome) return 96;
            String lower = resourceId.toLowerCase();
            if (lower.contains("mineshaft") || lower.contains("dungeon")
                    || lower.contains("stronghold") || lower.contains("ancient_city")) return 40;
            if (lower.contains("ocean_monument") || lower.contains("shipwreck")
                    || lower.contains("buried_treasure")) return 80;
            return 96;
        }
        return y;
    }

    private static String prettifyResourceName(String resourceId) {
        try {
            // 1.20.1 では ResourceLocation.parse() 未導入 (1.21+)、new ResourceLocation で代替
            ResourceLocation rl = new ResourceLocation(resourceId);
            String path = rl.getPath();
            String[] parts = path.split("_");
            StringBuilder sb = new StringBuilder();
            for (String part : parts) {
                if (part.isEmpty()) continue;
                if (sb.length() > 0) sb.append(" ");
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) sb.append(part.substring(1));
            }
            return sb.toString();
        } catch (Throwable t) {
            return resourceId;
        }
    }

    private static void sendChatNotification(ServerPlayer player, String translationKey,
                                              String prettyName, int x, int y, int z) {
        final boolean isOp = player.hasPermissions(2);
        final String tpCmd = "/tp @s " + x + " " + y + " " + z;
        Component coord = Component.literal(x + ", " + z)
                .withStyle(s -> {
                    s = s.withColor(ChatFormatting.LIGHT_PURPLE);
                    if (isOp) {
                        s = s.withUnderlined(true)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, tpCmd))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        Component.literal("Click to insert /tp command")));
                    }
                    return s;
                });
        player.displayClientMessage(
                Component.translatable(translationKey, prettyName, coord),
                false
        );
    }
}
