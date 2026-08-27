package io.github.lithum12.trackertips.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class TTClientConfig {

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLE;
    public static final ForgeConfigSpec.IntValue OFFSET_X;
    public static final ForgeConfigSpec.IntValue OFFSET_Y;
    public static final ForgeConfigSpec.IntValue MAX_WIDTH;
    public static final ForgeConfigSpec.IntValue MAX_HINTS;
    public static final ForgeConfigSpec.IntValue FADE_IN;
    public static final ForgeConfigSpec.IntValue FADE_OUT;
    public static final ForgeConfigSpec.BooleanValue DEBUG;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        ENABLE = builder.comment("Enable the hint overlay")
                .translation("trackertips.config.enable")
                .define("enable", true);
        OFFSET_X = builder.comment("Left-bottom X offset")
                .translation("trackertips.config.offset_x")
                .defineInRange("offset_x", 6, 0, 1000);
        OFFSET_Y = builder.comment("Left-bottom Y offset from bottom")
                .translation("trackertips.config.offset_y")
                .defineInRange("offset_y", 45, 0, 1000);
        MAX_WIDTH = builder.comment("Max hint panel width")
                .translation("trackertips.config.max_width")
                .defineInRange("max_width", 260, 120, 600);
        MAX_HINTS = builder.comment("Max hints shown at once")
                .translation("trackertips.config.max_hints")
                .defineInRange("max_hints", 3, 1, 10);
        FADE_IN = builder.comment("Fade-in ticks")
                .translation("trackertips.config.fade_in")
                .defineInRange("fade_in", 6, 1, 100);
        FADE_OUT = builder.comment("Fade-out ticks")
                .translation("trackertips.config.fade_out")
                .defineInRange("fade_out", 10, 1, 100);
        DEBUG = builder.comment("Show debug info (hitbox / packet log)")
                .translation("trackertips.config.debug")
                .define("debug", false);

        SPEC = builder.build();
    }
}