package io.github.lithum12.trackertips.client.gui;

import net.minecraft.client.gui.screens.Screen;

/** Entry point for ModMenu or another config-menu integration. */
public final class TTConfigScreenFactory {
    private TTConfigScreenFactory() { }

    public static Screen create(Screen parent) {
        return new TTConfigScreen(parent);
    }
}
