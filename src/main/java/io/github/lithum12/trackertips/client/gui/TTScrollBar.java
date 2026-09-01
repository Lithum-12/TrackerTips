package io.github.lithum12.trackertips.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * Minimal vertical scrollbar drawn alongside a {@link TTConfigTab}'s content. Mouse-wheel
 * scrolling (handled by {@link TTConfigTab#mouseScrolled}) is the primary way to scroll; this
 * widget mirrors the resulting position and additionally lets the thumb itself be dragged.
 */
final class TTScrollBar extends AbstractWidget {
    private final TTConfigTab tab;
    private boolean dragging;

    TTScrollBar(TTConfigTab tab) {
        super(0, 0, 6, 0, Component.empty());
        this.tab = tab;
        this.visible = false;
    }

    /** TTConfigTab owns layout for everything else; only this widget needs its height set directly. */
    void setTrackHeight(int trackHeight) {
        this.height = Math.max(0, trackHeight);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!tab.isScrollable()) return;
        graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x40FFFFFF);

        int thumbHeight = tab.thumbSize(getHeight());
        int thumbTop = getY() + tab.thumbOffset(getHeight());
        int color = dragging || isMouseOver(mouseX, mouseY) ? 0xFFE0E0E0 : 0xFFAAAAAA;
        graphics.fill(getX(), thumbTop, getX() + getWidth(), thumbTop + thumbHeight, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !tab.isScrollable() || !isMouseOver(mouseX, mouseY)) return false;
        dragging = true;
        dragTo(mouseY);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!dragging) return false;
        dragTo(mouseY);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void dragTo(double mouseY) {
        int thumbHeight = tab.thumbSize(getHeight());
        int travel = Math.max(1, getHeight() - thumbHeight);
        double relative = (mouseY - getY() - thumbHeight / 2.0) / travel;
        tab.scrollTo(relative);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        // Purely visual scroll affordance; the tab content itself carries the narration.
    }
}
