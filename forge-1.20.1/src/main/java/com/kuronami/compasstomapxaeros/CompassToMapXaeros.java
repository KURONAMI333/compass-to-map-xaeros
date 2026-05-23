package com.kuronami.compasstomapxaeros;

import com.kuronami.compasstomapxaeros.network.Channel;
import com.mojang.logging.LogUtils;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import org.slf4j.Logger;

/**
 * Compass to Map: Xaero's edition (Forge 1.20.1).
 *
 * Explorer's Compass / Nature's Compass の発見イベントを検出し、Xaero's Minimap /
 * World Map に永続 waypoint を立てる。chat-share 経由ではなく custom packet (server→client)
 * + Reflection で Xaero internal API 直叩きの構成。
 */
@Mod(CompassToMapXaeros.MODID)
public class CompassToMapXaeros {

    public static final String MODID = "compasstomapxaeros";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CompassToMapXaeros() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::commonSetup);

        // CompassWatcher は @Mod.EventBusSubscriber(modid = MODID) で
        // MinecraftForge.EVENT_BUS に自動登録される。ここで再登録すると
        // onPlayerTick が 1 tick あたり 2 回呼ばれるので明示登録はしない。

        // Config 登録
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        LOGGER.info("Compass to Map: Xaero's edition (Forge 1.20.1) initialized");
    }

    private void commonSetup(FMLCommonSetupEvent evt) {
        // SimpleChannel 登録は commonSetup の enqueueWork 内が安全
        evt.enqueueWork(Channel::register);
    }
}
