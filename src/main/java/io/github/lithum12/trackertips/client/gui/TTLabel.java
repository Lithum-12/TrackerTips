package io.github.lithum12.trackertips.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** Small layout-aware text label for the native 1.20.1 tab system. */
final class TTLabel extends AbstractWidget {
    private final Component baseMessage;
    private boolean italic;

    TTLabel(Component message) {
        super(0, 0, 180, 20, message);
        this.baseMessage = message;
    }

    /**
     * Marks this label's control as differing from its default value by rendering the label
     * text in italics, similar to vanilla's own "modified from default" option indicators.
     */
    void setItalic(boolean italic) {
        if (this.italic == italic) return;
        this.italic = italic;
        setMessage(italic ? baseMessage.copy().withStyle(ChatFormatting.ITALIC) : baseMessage);
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
