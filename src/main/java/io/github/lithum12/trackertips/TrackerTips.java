package io.github.lithum12.trackertips;

import com.mojang.logging.LogUtils;
import io.github.lithum12.trackertips.config.TTClientConfig;
import io.github.lithum12.trackertips.network.TTNetwork;
import io.github.lithum12.trackertips.trigger.Triggers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * TrackerTips' main mod entry point.
 *
 * <p>Constructs shared, dist-agnostic state (networking, the {@link Triggers} registry, the
 * client config spec) and, on the client only, registers TrackerTips' config screen with Forge's
 * mod list "Config" button (see {@link #registerConfigScreen()}).
 *
 * <p><b>For addon mods:</b> this class has no public extension surface of its own. See instead:
 * <ul>
 *   <li>{@link Triggers#register} - add a custom trigger/condition type.</li>
 *   <li>{@code io.github.lithum12.trackertips.theme.TTThemeManager#registerBuiltIn} - ship a built-in theme.</li>
 *   <li>{@code io.github.lithum12.trackertips.config.TTConfigManager#registerHintProvider} - contribute hint definitions programmatically.</li>
 * </ul>
 * All three are safe to call from your own mod's {@code FMLCommonSetupEvent} handler, which is
 * guaranteed to run after every mod (including this one) has finished constructing.
 */
@Mod(TrackerTips.MODID)
public class TrackerTips {

    public static final String MODID = "trackertips";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TrackerTips() {
        TTNetwork.register();
        Triggers.init();
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, TTClientConfig.SPEC);

        // Feature: lets players open TrackerTips' own config screen from Forge's mod list
        // "Config" button, instead of needing a command. Wrapped in DistExecutor since
        // ConfigScreenHandler/Screen are client-only classes that must never be touched while
        // constructing this mod on a dedicated server.
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> TrackerTips::registerConfigScreen);
    }

    @OnlyIn(Dist.CLIENT)
    private static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(
                net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parentScreen) ->
                                new io.github.lithum12.trackertips.client.gui.TTConfigScreen(parentScreen)));
    }
}
