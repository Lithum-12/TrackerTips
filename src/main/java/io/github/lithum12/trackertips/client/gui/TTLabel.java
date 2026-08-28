package io.github.lithum12.trackertips.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** Small layout-aware text label for the native 1.20.1 tab system. */
final class TTLabel extends AbstractWidget {
    TTLabel(Component message) {
        super(0, 0, 180, 20, message);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.drawString(MinecraftFontAccess.font(), getMessage(), getX(), getY() + 6, 0xFFFFFF);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, getMessage());
    }

    private static final class MinecraftFontAccess {
        private static net.minecraft.client.gui.Font font() {
            return net.minecraft.client.Minecraft.getInstance().font;
        }
    }
}
