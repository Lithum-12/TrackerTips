package io.github.lithum12.trackertips.client;

import io.github.lithum12.trackertips.TrackerTips;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TrackerTips.MODID, value = Dist.CLIENT)
public class TTClientEvents {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientHintManager.tick();

        // Don't respond to keybinds while any screen (chat / menu / inventory) is open, to avoid misfires while typing
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }

        // Press H: dismiss the currently displayed hint
        while (TTKeyMappings.DISMISS_HINT.consumeClick()) {
            ClientHintManager.dismissCurrent();
        }

        // Press J: show / hide all hints
        while (TTKeyMappings.TOGGLE_HINT.consumeClick()) {
            boolean visible = ClientHintManager.toggleVisible();
            minecraft.player.sendSystemMessage(Component.translatable(
                    visible ? "trackertips.message.hints_on" : "trackertips.message.hints_off"));
        }
    }
}