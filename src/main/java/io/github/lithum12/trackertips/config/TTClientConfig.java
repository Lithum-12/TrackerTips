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

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        ENABLE = builder.comment("是否显示提示框").define("enable", true);
        OFFSET_X = builder.comment("左下角 X 偏移").defineInRange("offset_x", 6, 0, 1000);
        OFFSET_Y = builder.comment("左下角 Y 偏移（距底部）").defineInRange("offset_y", 4, 0, 1000);
        MAX_WIDTH = builder.comment("提示框最大宽度").defineInRange("max_width", 260, 120, 600);
        MAX_HINTS = builder.comment("最多同时显示几个提示").defineInRange("max_hints", 3, 1, 10);
        FADE_IN = builder.comment("淡入 tick").defineInRange("fade_in", 6, 1, 100);
        FADE_OUT = builder.comment("淡出 tick").defineInRange("fade_out", 10, 1, 100);

        SPEC = builder.build();
    }
}