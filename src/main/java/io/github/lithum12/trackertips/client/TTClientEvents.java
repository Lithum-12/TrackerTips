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

        // 打开任何界面（聊天框 / 菜单 / 背包）时不响应按键，防止打字误触
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }

        while (TTKeyMappings.OPEN_CONFIG.consumeClick()) {
            minecraft.setScreen(new io.github.lithum12.trackertips.client.gui.TTConfigScreen());
        }

        // 按 H：关闭当前正在显示的提示
        while (TTKeyMappings.DISMISS_HINT.consumeClick()) {
            ClientHintManager.dismissCurrent();
        }

        // 按 J：显示 / 隐藏全部提示
        while (TTKeyMappings.TOGGLE_HINT.consumeClick()) {
            boolean visible = ClientHintManager.toggleVisible();
            minecraft.player.sendSystemMessage(Component.translatable(
                    visible ? "trackertips.message.hints_on" : "trackertips.message.hints_off"));
        }
    }
}