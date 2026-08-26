package io.github.lithum12.trackertips.client;

import io.github.lithum12.trackertips.TrackerTips;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TrackerTips.MODID, value = Dist.CLIENT)
public class TTClientEvents {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ClientHintManager.tick();
        }
    }
}