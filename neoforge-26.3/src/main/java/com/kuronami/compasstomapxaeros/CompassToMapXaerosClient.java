package com.kuronami.compasstomapxaeros;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * クライアント限定の初期化。
 */
@Mod(value = CompassToMapXaeros.MODID, dist = Dist.CLIENT)
public class CompassToMapXaerosClient {
    public CompassToMapXaerosClient(ModContainer container) {
        // Mods 画面 → Compass to Map → Config で自動生成された設定画面を開く
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
