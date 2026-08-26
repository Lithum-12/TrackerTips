package io.github.lithum12.trackertips.client;

import io.github.lithum12.trackertips.TrackerTips;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TrackerTips.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class TTClientModEvents {

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(
                VanillaGuiOverlay.CHAT_PANEL.id(),
                "hints",
                (gui, guiGraphics, partialTick, screenWidth, screenHeight) ->
                        HintRenderer.render(guiGraphics, screenWidth, screenHeight)
        );
    }
}