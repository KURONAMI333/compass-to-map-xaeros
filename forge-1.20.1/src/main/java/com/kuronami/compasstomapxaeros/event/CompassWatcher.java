package com.kuronami.compasstomapxaeros.event;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.kuronami.compasstomapxaeros.CompassToMapXaeros;
import com.kuronami.compasstomapxaeros.Config;
import com.kuronami.compasstomapxaeros.util.CompassNames;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/**
 * Compass to Map detection (Forge 1.20.1, NBT-based EC/NC API).
 *
 * 1.21.1 との差分：
 *  - DataComponents 未導入 → instance method 経由でアクセス
 *    EC: ExplorersCompass.explorersCompass.getStructureKey/getFoundStructureX/Y(stack)
 *    NC: NaturesCompass.naturesCompass.getBiomeKey/getFoundBiomeX/Y(stack)
 *  - getState(stack) は CompassState 型を直接返す (1.21 は Integer)
 */
@Mod.EventBusSubscriber(modid = CompassToMapXaeros.MODID)
public final class CompassWatcher {

    private static final Map<UUID, Set<String>> SEEN_KEYS = new ConcurrentHashMap<>();
    private static final int MAX_SEEN_PER_PLAYER = 512;

    private static volatile boolean ecApiBroken = false;
    private static volatile boolean ncApiBroken = false;

    private CompassWatcher() {}

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        SEEN_KEYS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!Config.ENABLED.get()) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        if (!ecApiBroken && Config.ENABLE_STRUCTURE.get() && ModList.get().isLoaded("explorerscompass")) {
            try {
                ECInner.tickCheck(player, serverLevel);
            } catch (LinkageError | RuntimeException t) {
                ecApiBroken = true;
                CompassToMapXaeros.LOGGER.warn(
                        "Explorer's Compass API mismatch or class missing. Structure detection disabled until restart.", t);
            }
        }

        if (!ncApiBroken && Config.ENABLE_BIOME.get() && ModList.get().isLoaded("naturescompass")) {
            try {
                NCInner.tickCheck(player, serverLevel);
            } catch (LinkageError | RuntimeException t) {
                ncApiBroken = true;
                CompassToMapXaeros.LOGGER.warn(
                        "Nature's Compass API mismatch or class missing. Biome detection disabled until restart.", t);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Explorer's Compass (NBT-based)
    // ─────────────────────────────────────────────────────────────
    private static final class ECInner {
        static void tickCheck(ServerPlayer player, ServerLevel serverLevel) {
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

            // EC 1.20.1: instance method 経由 (DataComponents 未導入)
            ResourceLocation structureKey = com.chaosthedude.explorerscompass.ExplorersCompass.explorersCompass.getStructureKey(found);
            if (structureKey == null) return;
            int x = com.chaosthedude.explorerscompass.ExplorersCompass.explorersCompass.getFoundStructureX(found);
            int z = com.chaosthedude.explorerscompass.ExplorersCompass.explorersCompass.getFoundStructureZ(found);
            String structureId = structureKey.toString();

            String dimKey = serverLevel.dimension().location().toString();
            String key = "s|" + dimKey + "|" + structureId + "|" + x + "|" + z;
            if (!recordSeen(player.getUUID(), key)) return;

            int y = estimateY(serverLevel, x, z, structureId, false);
            BlockPos pos = new BlockPos(x, y, z);

            String prettyName = CompassNames.prettify(structureId);
            ServerDispatch.send(player, prettyName, pos);

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
                    com.chaosthedude.explorerscompass.ExplorersCompass.explorersCompass.getState(stack);
            return state == com.chaosthedude.explorerscompass.util.CompassState.FOUND;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Nature's Compass (NBT-based)
    // ─────────────────────────────────────────────────────────────
    private static final class NCInner {
        static void tickCheck(ServerPlayer player, ServerLevel serverLevel) {
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

            ResourceLocation biomeKey = com.chaosthedude.naturescompass.NaturesCompass.naturesCompass.getBiomeKey(found);
            if (biomeKey == null) return;
            int x = com.chaosthedude.naturescompass.NaturesCompass.naturesCompass.getFoundBiomeX(found);
            int z = com.chaosthedude.naturescompass.NaturesCompass.naturesCompass.getFoundBiomeZ(found);
            String biomeId = biomeKey.toString();

            String dimKey = serverLevel.dimension().location().toString();
            String key = "b|" + dimKey + "|" + biomeId + "|" + x + "|" + z;
            if (!recordSeen(player.getUUID(), key)) return;

            int y = estimateY(serverLevel, x, z, biomeId, true);
            BlockPos pos = new BlockPos(x, y, z);

            String prettyName = CompassNames.prettify(biomeId);
            ServerDispatch.send(player, prettyName, pos);

            if (Config.NOTIFY_ON_FOUND.get()) {
                sendChatNotification(player, "message.compasstomapxaeros.biome_found", prettyName, x, y, z);
            }

            CompassToMapXaeros.LOGGER.info("Biome found by {}: {} @ ({}, ~{}, {})",
                    player.getName().getString(), biomeId, x, y, z);
        }

        private static boolean isFound(ItemStack stack) {
            if (stack.isEmpty()) return false;
            if (!(stack.getItem() instanceof com.chaosthedude.naturescompass.items.NaturesCompassItem)) return false;
            com.chaosthedude.naturescompass.util.CompassState state =
                    com.chaosthedude.naturescompass.NaturesCompass.naturesCompass.getState(stack);
            return state == com.chaosthedude.naturescompass.util.CompassState.FOUND;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 共通
    // ─────────────────────────────────────────────────────────────
    private static boolean recordSeen(UUID uuid, String key) {
        Set<String> seen = SEEN_KEYS.computeIfAbsent(uuid,
                k -> Collections.synchronizedSet(new LinkedHashSet<>()));
        synchronized (seen) {
            if (!seen.add(key)) return false;
            if (seen.size() > MAX_SEEN_PER_PLAYER) {
                Iterator<String> it = seen.iterator();
                it.next(); it.remove();
            }
            return true;
        }
    }

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
