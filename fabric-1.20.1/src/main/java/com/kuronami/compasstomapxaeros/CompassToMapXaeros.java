package com.kuronami.compasstomapxaeros;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Compass to Map: Xaero's edition - 定数のみ (Fabric 1.20.1)。
 *
 * <p>Fabric 版の実エントリは {@link CompassToMapXaerosFabric} (ModInitializer) と
 * {@link CompassToMapXaerosClientFabric} (ClientModInitializer)。
 */
public final class CompassToMapXaeros {

    public static final String MODID = "compasstomapxaeros";
    public static final Logger LOGGER = LogUtils.getLogger();

    private CompassToMapXaeros() {}
}
