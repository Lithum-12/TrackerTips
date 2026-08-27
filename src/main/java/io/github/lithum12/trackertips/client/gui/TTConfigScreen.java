package io.github.lithum12.trackertips.client.gui;

import io.github.lithum12.trackertips.TrackerTips;
import io.github.lithum12.trackertips.config.TTClientConfig;
import io.github.lithum12.trackertips.config.TTConfigManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.config.ModConfig;

import java.awt.Desktop;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public class TTConfigScreen extends Screen {
    private enum Tab { GENERAL, EVENTS }

    private final Screen parent;
    private Tab currentTab = Tab.GENERAL;

    private boolean pendingEnable, pendingDebug;
    private int pendingOffsetX, pendingOffsetY, pendingMaxWidth, pendingMaxHints, pendingFadeIn, pendingFadeOut;

    private EditBox offsetX, offsetY, maxWidth, maxHints, fadeIn, fadeOut;
    private List<Path> eventFiles = List.of();

    private static final int SIDEBAR_W = 176;
    private static final int TOP_H = 58;
    private static final int BOTTOM_H = 54;

    public TTConfigScreen() { this(null); }

    public TTConfigScreen(Screen parent) {
        super(Component.translatable("trackertips.gui.title"));
        this.parent = parent;
        loadPending();
    }

    private void loadPending() {
        pendingEnable = TTClientConfig.ENABLE.get();
        pendingDebug = TTClientConfig.DEBUG.get();
        pendingOffsetX = TTClientConfig.OFFSET_X.get();
        pendingOffsetY = TTClientConfig.OFFSET_Y.get();
        pendingMaxWidth = TTClientConfig.MAX_WIDTH.get();
        pendingMaxHints = TTClientConfig.MAX_HINTS.get();
        pendingFadeIn = TTClientConfig.FADE_IN.get();
        pendingFadeOut = TTClientConfig.FADE_OUT.get();
    }

    @Override protected void init() { rebuildWidgets(); }

    private void rebuildWidgets() {
        clearWidgets();

        addRenderableWidget(new NavButton(12, 92, SIDEBAR_W - 24, 30,
                Component.translatable("trackertips.gui.tab.general"), b -> switchTab(Tab.GENERAL)));
        addRenderableWidget(new NavButton(12, 132, SIDEBAR_W - 24, 30,
                Component.translatable("trackertips.gui.tab.events"), b -> switchTab(Tab.EVENTS)));

        int x = SIDEBAR_W + 26;
        int w = Math.max(300, width - x - 26);
        if (currentTab == Tab.GENERAL) buildGeneralWidgets(x, w);
        else buildEventWidgets(x, w);

        int actionY = height - BOTTOM_H + 15;
        int actionW = 92, gap = 8;
        int startX = width - (actionW * 3 + gap * 2) - 24;
        addRenderableWidget(new FlatButton(startX, actionY, actionW, 24,
                Component.translatable("gui.cancel"), b -> cancel()));
        addRenderableWidget(new FlatButton(startX + actionW + gap, actionY, actionW, 24,
                Component.translatable("trackertips.gui.save"), b -> save()));
        addRenderableWidget(new FlatButton(startX + (actionW + gap) * 2, actionY, actionW, 24,
                Component.translatable("trackertips.gui.reset"), b -> reset()));
    }

    private void buildGeneralWidgets(int x, int w) {
        int y = TOP_H + 46;
        int rowH = 42;
        int controlW = Math.min(170, Math.max(130, w - 230));
        int controlX = x + w - controlW - 14;

        addRenderableWidget(CycleButton.onOffBuilder(pendingEnable).create(controlX, y + 8, controlW, 22,
                Component.empty(), (b, value) -> pendingEnable = value));
        y += rowH;
        offsetX = numericBox(controlX, y + 7, controlW, pendingOffsetX); y += rowH;
        offsetY = numericBox(controlX, y + 7, controlW, pendingOffsetY); y += rowH;
        maxWidth = numericBox(controlX, y + 7, controlW, pendingMaxWidth); y += rowH;
        maxHints = numericBox(controlX, y + 7, controlW, pendingMaxHints); y += rowH + 22;
        fadeIn = numericBox(controlX, y + 7, controlW, pendingFadeIn); y += rowH;
        fadeOut = numericBox(controlX, y + 7, controlW, pendingFadeOut); y += rowH + 22;
        addRenderableWidget(CycleButton.onOffBuilder(pendingDebug).create(controlX, y + 8, controlW, 22,
                Component.empty(), (b, value) -> pendingDebug = value));
    }

    private EditBox numericBox(int x, int y, int w, int value) {
        EditBox box = new EditBox(font, x, y, w, 22, Component.empty());
        box.setValue(Integer.toString(value));
        box.setFilter(s -> s.isEmpty() || s.matches("\\d{0,4}"));
        addRenderableWidget(box);
        return box;
    }

    private void buildEventWidgets(int x, int w) {
        eventFiles = listEventFiles();
        addRenderableWidget(new FlatButton(x, height - BOTTOM_H - 10, 110, 22,
                Component.translatable("trackertips.gui.add_event"), b ->
                minecraft.setScreen(new TTEventEditorScreen(this, null))));
        addRenderableWidget(new FlatButton(x + 118, height - BOTTOM_H - 10, 92, 22,
                Component.translatable("trackertips.gui.refresh"), b -> rebuildWidgets()));

        int rowY = TOP_H + 66;
        for (Path file : eventFiles) {
            if (rowY > height - BOTTOM_H - 30) break;
            addRenderableWidget(new FlatButton(x + w - 184, rowY + 9, 80, 22,
                    Component.translatable("trackertips.gui.edit"), b -> openFile(file)));
            addRenderableWidget(new FlatButton(x + w - 88, rowY + 9, 72, 22,
                    Component.translatable("trackertips.gui.delete"), b -> deleteFile(file)));
            rowY += 50;
        }
    }

    private List<Path> listEventFiles() {
        Path folder = TTConfigManager.globalFolder().resolve("hints");
        try {
            if (!Files.isDirectory(folder)) return List.of();
            try (var stream = Files.list(folder)) {
                return stream.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList();
            }
        } catch (Exception e) {
            TrackerTips.LOGGER.error("[TrackerTips] Failed to list hint files", e);
            return List.of();
        }
    }

    private void switchTab(Tab tab) {
        if (currentTab != tab) { currentTab = tab; rebuildWidgets(); }
    }

    private void cancel() { if (minecraft != null) minecraft.setScreen(parent); }

    private void reset() {
        pendingEnable = TTClientConfig.ENABLE.getDefault();
        pendingDebug = TTClientConfig.DEBUG.getDefault();
        pendingOffsetX = TTClientConfig.OFFSET_X.getDefault();
        pendingOffsetY = TTClientConfig.OFFSET_Y.getDefault();
        pendingMaxWidth = TTClientConfig.MAX_WIDTH.getDefault();
        pendingMaxHints = TTClientConfig.MAX_HINTS.getDefault();
        pendingFadeIn = TTClientConfig.FADE_IN.getDefault();
        pendingFadeOut = TTClientConfig.FADE_OUT.getDefault();
        rebuildWidgets();
    }

    private int read(EditBox box, int old) {
        try { return Integer.parseInt(box.getValue()); }
        catch (NumberFormatException ignored) { return old; }
    }

    private void save() {
        if (currentTab == Tab.GENERAL) {
            pendingOffsetX = clamp(read(offsetX, pendingOffsetX), 0, 1000);
            pendingOffsetY = clamp(read(offsetY, pendingOffsetY), 0, 1000);
            pendingMaxWidth = clamp(read(maxWidth, pendingMaxWidth), 120, 600);
            pendingMaxHints = clamp(read(maxHints, pendingMaxHints), 1, 10);
            pendingFadeIn = clamp(read(fadeIn, pendingFadeIn), 1, 100);
            pendingFadeOut = clamp(read(fadeOut, pendingFadeOut), 1, 100);
            TTClientConfig.ENABLE.set(pendingEnable);
            TTClientConfig.DEBUG.set(pendingDebug);
            TTClientConfig.OFFSET_X.set(pendingOffsetX);
            TTClientConfig.OFFSET_Y.set(pendingOffsetY);
            TTClientConfig.MAX_WIDTH.set(pendingMaxWidth);
            TTClientConfig.MAX_HINTS.set(pendingMaxHints);
            TTClientConfig.FADE_IN.set(pendingFadeIn);
            TTClientConfig.FADE_OUT.set(pendingFadeOut);
            try {
                Set<ModConfig> configs = net.minecraftforge.fml.config.ConfigTracker.INSTANCE.configSets().get(ModConfig.Type.CLIENT);
                if (configs != null) for (ModConfig config : configs) {
                    if (config.getModId().equals(TrackerTips.MODID)) { config.save(); break; }
                }
            } catch (Exception e) { TrackerTips.LOGGER.error("[TrackerTips] Failed to save client config", e); }
        }
        if (minecraft != null && minecraft.player != null)
            minecraft.player.displayClientMessage(Component.translatable("trackertips.gui.saved"), true);
        cancel();
    }

    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }

    private void openFile(Path file) {
        try { if (Files.exists(file) && Desktop.isDesktopSupported()) Desktop.getDesktop().open(file.toFile()); }
        catch (Exception e) { TrackerTips.LOGGER.error("[TrackerTips] Failed to open hint file: {}", file, e); }
    }

    private void deleteFile(Path file) {
        try { Files.deleteIfExists(file); rebuildWidgets(); }
        catch (Exception e) { TrackerTips.LOGGER.error("[TrackerTips] Failed to delete hint file: {}", file, e); }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        drawChrome(g);
        if (currentTab == Tab.GENERAL) drawGeneral(g);
        else drawEvents(g);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawChrome(GuiGraphics g) {
        g.fill(0, 0, width, height, 0xC0101115);
        g.fill(0, 0, SIDEBAR_W, height, 0xD00D0E11);
        g.fill(SIDEBAR_W, 0, width, TOP_H, 0xE018191D);
        g.fill(SIDEBAR_W, height - BOTTOM_H, width, height, 0xE00F1013);
        g.fill(SIDEBAR_W, TOP_H, width, TOP_H + 1, 0xFF303238);
        g.fill(0, height - BOTTOM_H, width, height - BOTTOM_H + 1, 0xFF303238);

        g.drawString(font, title, 24, 24, 0xFFFFFF, true);
        g.drawString(font, Component.translatable("trackertips.gui.subtitle"), 24, 39, 0x777B84, false);
        g.drawString(font, Component.translatable("trackertips.gui.section.settings"), 20, 78, 0x6F737D, false);

        navVisual(g, 12, 92, currentTab == Tab.GENERAL);
        navVisual(g, 12, 132, currentTab == Tab.EVENTS);
        navText(g, 12, 172, Component.translatable("trackertips.gui.tab.help"), false);
    }

    private void navVisual(GuiGraphics g, int x, int y, boolean selected) {
        if (selected) {
            g.fill(x, y, SIDEBAR_W - 12, y + 30, 0xFF292C33);
            g.fill(x, y, x + 3, y + 30, 0xFFE0A83A);
        }
    }

    private void navText(GuiGraphics g, int x, int y, Component text, boolean selected) {
        g.drawString(font, text, x + 13, y + 10, selected ? 0xFFFFFF : 0xA4A7AE, selected);
    }

    private void drawGeneral(GuiGraphics g) {
        int x = SIDEBAR_W + 26, w = width - x - 26, y = TOP_H + 12;
        panel(g, x, y, w, 36, Component.translatable("trackertips.gui.category.display")); y += 46;
        row(g, x, y, w, "trackertips.config.enable", "trackertips.config.enable.desc"); y += 42;
        row(g, x, y, w, "trackertips.config.offset_x", "trackertips.config.offset_x.desc"); y += 42;
        row(g, x, y, w, "trackertips.config.offset_y", "trackertips.config.offset_y.desc"); y += 42;
        row(g, x, y, w, "trackertips.config.max_width", "trackertips.config.max_width.desc"); y += 42;
        row(g, x, y, w, "trackertips.config.max_hints", "trackertips.config.max_hints.desc"); y += 64;
        panel(g, x, y, w, 36, Component.translatable("trackertips.gui.category.animation")); y += 46;
        row(g, x, y, w, "trackertips.config.fade_in", "trackertips.config.fade_in.desc"); y += 42;
        row(g, x, y, w, "trackertips.config.fade_out", "trackertips.config.fade_out.desc"); y += 64;
        panel(g, x, y, w, 36, Component.translatable("trackertips.gui.category.debug")); y += 46;
        row(g, x, y, w, "trackertips.config.debug", "trackertips.config.debug.desc");
    }

    private void row(GuiGraphics g, int x, int y, int w, String title, String desc) {
        g.fill(x, y, x + w, y + 41, 0xA516171B);
        g.drawString(font, Component.translatable(title), x + 14, y + 7, 0xF0F0F0, false);
        g.drawString(font, Component.translatable(desc), x + 14, y + 24, 0x747881, false);
    }

    private void panel(GuiGraphics g, int x, int y, int w, int h, Component title) {
        g.fill(x, y, x + w, y + h, 0xD91B1D22);
        g.fill(x, y, x + 3, y + h, 0xFF3A3D45);
        g.drawString(font, title, x + 14, y + 12, 0xE7E8EA, true);
    }

    private void drawEvents(GuiGraphics g) {
        int x = SIDEBAR_W + 26, w = width - x - 26;
        int y = TOP_H + 18;
        g.drawString(font, Component.translatable("trackertips.gui.events.title"), x, y, 0xFFFFFF, true);
        g.drawString(font, Component.translatable("trackertips.gui.events.subtitle"), x, y + 16, 0x777B84, false);
        int rowY = TOP_H + 66;
        if (eventFiles.isEmpty()) {
            g.fill(x, rowY, x + w, rowY + 54, 0xA516171B);
            g.drawString(font, Component.translatable("trackertips.gui.events.empty"), x + 14, rowY + 20, 0x888C94, false);
            return;
        }
        for (Path file : eventFiles) {
            if (rowY > height - BOTTOM_H - 30) break;
            g.fill(x, rowY, x + w, rowY + 42, 0xB51A1B20);
            g.fill(x, rowY, x + 3, rowY + 42, 0xFF5C6069);
            g.drawString(font, file.getFileName().toString(), x + 14, rowY + 8, 0xE7E8EA, false);
            g.drawString(font, file.toString(), x + 14, rowY + 24, 0x666B74, false);
            rowY += 50;
        }
    }

    private static class NavButton extends Button {
        NavButton(int x, int y, int w, int h, Component message, OnPress press) { super(x, y, w, h, message, press, DEFAULT_NARRATION); }
        @Override protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) { }
    }

    private static class FlatButton extends Button {
        FlatButton(int x, int y, int w, int h, Component message, OnPress press) { super(x, y, w, h, message, press, DEFAULT_NARRATION); }
        @Override protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            int bg = active ? (isHoveredOrFocused() ? 0xFF383B42 : 0xFF2A2C31) : 0xFF222328;
            g.fill(getX(), getY(), getX() + width, getY() + height, bg);
            g.fill(getX(), getY(), getX() + width, getY() + 1, isHoveredOrFocused() ? 0xFFE0A83A : 0xFF44474F);
            g.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font, getMessage(),
                    getX() + width / 2, getY() + 8, active ? 0xFFE6E7E9 : 0xFF6F737A);
        }
    }
}
