package com.kuronami.compasstomapxaeros;

import com.kuronami.compasstomapxaeros.client.ClientDiscoveryHandler;
import com.kuronami.compasstomapxaeros.network.DiscoveryPayload;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Compass to Map: Xaero's edition - Fabric 1.20.1 Client entry。
 *
 * <p>S2C で受け取る {@link DiscoveryPayload} ({@code FabricPacket}) の handler を登録する。
 */
public class CompassToMapXaerosClientFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(
                DiscoveryPayload.TYPE,
                ClientDiscoveryHandler::handle
        );
        CompassToMapXaeros.LOGGER.info("Compass to Map: Xaero's edition (Fabric 1.20.1 client) initialized");
    }
}
