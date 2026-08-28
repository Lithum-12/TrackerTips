package io.github.lithum12.trackertips.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.lithum12.trackertips.TrackerTips;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TrackerTips.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class TTKeyMappings {

    // 默认 H：关闭当前提示
    public static final KeyMapping DISMISS_HINT = new KeyMapping(
            "key.trackertips.dismiss",
            InputConstants.KEY_H,
            "key.categories.trackertips");

    // 默认 J：显示 / 隐藏全部提示
    public static final KeyMapping TOGGLE_HINT = new KeyMapping(
            "key.trackertips.toggle",
            InputConstants.KEY_J,
            "key.categories.trackertips");

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(DISMISS_HINT);
        event.register(TOGGLE_HINT);
    }
}