package com.kuronami.compasstomapxaeros;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Compass to Map の COMMON 設定。
 */
public final class Config {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLED = B
            .comment("Master switch. If false, no waypoints are auto-registered.")
            .define("feature.enabled", true);

    public static final ModConfigSpec.BooleanValue ENABLE_STRUCTURE = B
            .comment("If true, register Explorer's Compass structure discoveries as waypoints.")
            .define("feature.enableStructure", true);

    public static final ModConfigSpec.BooleanValue ENABLE_BIOME = B
            .comment("If true, register Nature's Compass biome discoveries as waypoints.")
            .define("feature.enableBiome", true);

    public static final ModConfigSpec.BooleanValue NOTIFY_ON_FOUND = B
            .comment("Send a chat message to the player when a structure or biome is found and registered.")
            .define("notification.notifyOnFound", true);

    // NOTE: color category / persistent waypoint settings are not exposed in this build.
    // The Xaero's chat-share path uses a fixed brand-purple waypoint colour and lets
    // Xaero's own UI manage waypoint persistence (the "Add to waypoints?" prompt is
    // resolved by the player; once added, Xaero handles save/load).

    static final ModConfigSpec SPEC = B.build();

    private Config() {}
}
