package io.github.lithum12.trackertips.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class TTConfirmScreen extends Screen {
    private final Screen parent;
    private final Component message;
    private final Runnable confirmed;

    private TTConfirmScreen(Screen parent, Component message, Runnable confirmed) {
        super(Component.translatable("trackertips.gui.confirm"));
        this.parent = parent;
        this.message = message;
        this.confirmed = confirmed;
    }

    public static Screen create(Screen parent, Component message, Runnable confirmed) {
        return new TTConfirmScreen(parent, message, confirmed);
    }

    @Override protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> minecraft.setScreen(parent))
                .bounds(width / 2 - 105, height - 45, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.yes"), b -> {
            confirmed.run();
            // The callback may navigate to a freshly rebuilt screen. Only return to the parent
            // when the callback did not already change the current screen.
            if (minecraft.screen == this) {
                minecraft.setScreen(parent);
            }
        }).bounds(width / 2 + 5, height - 45, 100, 20).build());
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, message, width / 2, height / 2 - 10, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public boolean isPauseScreen() { return false; }
}
