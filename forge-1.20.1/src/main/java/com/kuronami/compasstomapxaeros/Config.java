package com.kuronami.compasstomapxaeros;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Compass to Map: Xaero's edition の COMMON 設定 (Forge 1.20.1)。
 */
public final class Config {
    private static final ForgeConfigSpec.Builder B = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue ENABLED = B
            .comment("Master switch. If false, no waypoints are auto-registered.")
            .define("feature.enabled", true);

    public static final ForgeConfigSpec.BooleanValue ENABLE_STRUCTURE = B
            .comment("If true, register Explorer's Compass structure discoveries as waypoints.")
            .define("feature.enableStructure", true);

    public static final ForgeConfigSpec.BooleanValue ENABLE_BIOME = B
            .comment("If true, register Nature's Compass biome discoveries as waypoints.")
            .define("feature.enableBiome", true);

    public static final ForgeConfigSpec.BooleanValue NOTIFY_ON_FOUND = B
            .comment("Send a chat message to the player when a structure or biome is found and registered.")
            .define("notification.notifyOnFound", true);

    static final ForgeConfigSpec SPEC = B.build();

    private Config() {}
}
