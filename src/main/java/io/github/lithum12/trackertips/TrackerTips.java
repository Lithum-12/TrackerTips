package io.github.lithum12.trackertips;

import com.mojang.logging.LogUtils;
import io.github.lithum12.trackertips.config.TTClientConfig;
import io.github.lithum12.trackertips.network.TTNetwork;
import io.github.lithum12.trackertips.trigger.Triggers;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(TrackerTips.MODID)
public class TrackerTips {

    public static final String MODID = "trackertips";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TrackerTips() {
        TTNetwork.register();
        Triggers.init();
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, TTClientConfig.SPEC);
    }
}