package io.github.lithum12.trackertips.player;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public class TTCapabilities {
    public static final Capability<PlayerHintData> HINT_DATA =
            CapabilityManager.get(new CapabilityToken<>() {});
}