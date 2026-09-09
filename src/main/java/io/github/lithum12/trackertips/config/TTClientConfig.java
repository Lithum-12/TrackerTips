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
    /** Feature: configurable popup anchor. See {@link HintAnchor}. */
    public static final ForgeConfigSpec.EnumValue<HintAnchor> ANCHOR;
    /** Feature: chat-overlap safeguard. See {@code HintRenderer}'s use of this value. */
    public static final ForgeConfigSpec.BooleanValue AVOID_CHAT_OVERLAP;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        ENABLE = builder.comment("Enable the hint overlay")
                .translation("trackertips.config.enable")
                .define("enable", true);
        ANCHOR = builder.comment("Where the hint popup stack is anchored on screen")
                .translation("trackertips.config.anchor")
                .defineEnum("anchor", HintAnchor.BOTTOM_LEFT);
        OFFSET_X = builder.comment("Horizontal offset from whichever edge the anchor touches (ignored for centered anchors: top/bottom)")
                .translation("trackertips.config.offset_x")
                .defineInRange("offset_x", 6, 0, 1000);
        OFFSET_Y = builder.comment("Vertical offset from whichever edge the anchor touches (ignored for the middle-anchored left/right)")
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
        AVOID_CHAT_OVERLAP = builder.comment(
                        "Reserve space for the chat log so left-anchored hints never render underneath it")
                .translation("trackertips.config.avoid_chat_overlap")
                .define("avoid_chat_overlap", true);

        SPEC = builder.build();
    }
}
