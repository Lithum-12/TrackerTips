package io.github.lithum12.trackertips.client.gui;

import io.github.lithum12.trackertips.client.gui.TTConfigScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(
        modid = "trackertips",
        bus = Mod.EventBusSubscriber.Bus.MOD
)
public class TTClient {

    public static final KeyMapping CONFIG_KEY = new KeyMapping(
            "key.trackertips.config",
            GLFW.GLFW_KEY_F8,
            "key.categories.trackertips"
    );

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(CONFIG_KEY);
    }

    @Mod.EventBusSubscriber(
            modid = "trackertips",
            bus = Mod.EventBusSubscriber.Bus.FORGE
    )
    public static class ForgeEvents {

        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            if (CONFIG_KEY.consumeClick()) {
                Minecraft.getInstance().setScreen(
                        new TTConfigScreen(Minecraft.getInstance().screen)
                );
            }
        }
    }
}