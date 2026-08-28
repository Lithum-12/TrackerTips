package io.github.lithum12.trackertips.client.gui;

import io.github.lithum12.trackertips.config.TTClientConfig;
import io.github.lithum12.trackertips.config.TTConfigManager;
import io.github.lithum12.trackertips.config.TTSettings;
import io.github.lithum12.trackertips.theme.TTThemeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.TabButton;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * TrackerTips configuration hub using Minecraft 1.20.1's native TabNavigationBar.
 */
public class TTConfigScreen extends Screen {
    private final Screen parent;
    private final HeaderAndFooterLayout layout;
    private final TabManager tabManager;
    private TabNavigationBar tabNavigationBar;
    private List<TTConfigTab> tabs = List.of();

    private Button quitButton;
    private Button saveButton;
    private boolean changed;

    private boolean pendingEnable;
    private boolean pendingDebug;

    private net.minecraft.client.gui.components.EditBox offsetXBox;
    private net.minecraft.client.gui.components.EditBox offsetYBox;
    private net.minecraft.client.gui.components.EditBox maxWidthBox;
    private net.minecraft.client.gui.components.EditBox maxHintsBox;
    private net.minecraft.client.gui.components.EditBox fadeInBox;
    private net.minecraft.client.gui.components.EditBox fadeOutBox;
    private net.minecraft.client.gui.components.EditBox checkIntervalBox;
    private net.minecraft.client.gui.components.EditBox defaultDurationBox;

    private static final Component CANCEL_LABEL = CommonComponents.GUI_CANCEL;
    private static final Component QUIT_CONFIRM_TITLE =
            Component.translatable("trackertips.gui.quit.confirm.title");
    private static final Component QUIT_CONFIRM_WARNING =
            Component.translatable("trackertips.gui.quit.confirm.warning");

    public TTConfigScreen() {
        this(null);
    }

    public TTConfigScreen(Screen parent) {
        super(Component.translatable("trackertips.gui.title"));
        this.parent = parent;
        this.layout = new HeaderAndFooterLayout(this, 24, 33);
        this.tabManager = new TabManager(this::addRenderableWidget, this::removeWidget);

        pendingEnable = TTClientConfig.ENABLE.get();
        pendingDebug = TTConfigManager.readGlobalSettings().debug;
        TTThemeManager.ensureDefaults();
    }

    @Override
    protected void init() {
        clearWidgets();
        this.tabs = new ArrayList<>();

        TabNavigationBar.Builder builder = TabNavigationBar.builder(tabManager, this.width);

        addTab(builder, TTConfigTab.Kind.GENERAL);
        addTab(builder, TTConfigTab.Kind.EVENTS);
        addTab(builder, TTConfigTab.Kind.THEMES);
        addTab(builder, TTConfigTab.Kind.USAGE);

        tabNavigationBar = builder.build();
        initTabs(tabNavigationBar);
        addRenderableWidget(tabNavigationBar);

        LinearLayout footer = layout.addToFooter(new LinearLayout(0, 0, LinearLayout.Orientation.HORIZONTAL));
        footer.defaultChildLayoutSetting().paddingHorizontal(4);
        quitButton = footer.addChild(Button.builder(getQuitLabel(), b -> onClose()).width(200).build());
        saveButton = footer.addChild(Button.builder(getSaveLabel(), b -> saveAndClose()).width(200).build());

        layout.visitWidgets(widget -> {
            widget.setTabOrderGroup(1);
            addRenderableWidget(widget);
        });

        tabNavigationBar.selectTab(0, false);
        repositionElements();
    }

    private void addTab(TabNavigationBar.Builder builder, TTConfigTab.Kind kind) {
        TTConfigTab tab = new TTConfigTab(this, kind);
        tabs.add(tab);
        builder.addTabs(tab);
    }

    private void initTabs(TabNavigationBar bar) {
        int i = 0;
        for (var child : bar.children()) {
            if (child instanceof TabButton button && i < tabs.size()) {
                i++;
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void repositionElements() {
        refresh();

        if (tabNavigationBar != null) {
            tabNavigationBar.setWidth(this.width);
            tabNavigationBar.arrangeElements();
            int top = tabNavigationBar.getRectangle().bottom();

            ScreenRectangle area = new ScreenRectangle(
                    0,
                    top,
                    this.width,
                    this.height - layout.getFooterHeight() - top
            );
            tabManager.setTabArea(area);
            layout.setHeaderHeight(top);
            layout.arrangeElements();
        }
    }

    public void reopenTabs() {
        Minecraft.getInstance().setScreen(new TTConfigScreen(parent));
    }

    public void markChanged() {
        changed = true;
        refresh();
    }

    void setPendingEnable(boolean value) {
        pendingEnable = value;
        markChanged();
    }

    void setPendingDebug(boolean value) {
        pendingDebug = value;
        markChanged();
    }

    void setOffsetXBox(net.minecraft.client.gui.components.EditBox box) { offsetXBox = box; }
    void setOffsetYBox(net.minecraft.client.gui.components.EditBox box) { offsetYBox = box; }
    void setMaxWidthBox(net.minecraft.client.gui.components.EditBox box) { maxWidthBox = box; }
    void setMaxHintsBox(net.minecraft.client.gui.components.EditBox box) { maxHintsBox = box; }
    void setFadeInBox(net.minecraft.client.gui.components.EditBox box) { fadeInBox = box; }
    void setFadeOutBox(net.minecraft.client.gui.components.EditBox box) { fadeOutBox = box; }
    void setCheckIntervalBox(net.minecraft.client.gui.components.EditBox box) { checkIntervalBox = box; }
    void setDefaultDurationBox(net.minecraft.client.gui.components.EditBox box) { defaultDurationBox = box; }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (tabManager.getCurrentTab() instanceof TTConfigTab tab) {
            // Let focused child widgets consume the key first.
        }
        return tabNavigationBar.keyPressed(keyCode) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (!changed) {
            Minecraft.getInstance().setScreen(parent);
            return;
        }

        Minecraft.getInstance().setScreen(new ConfirmScreen(
                confirmed -> Minecraft.getInstance().setScreen(confirmed ? parent : this),
                QUIT_CONFIRM_TITLE,
                QUIT_CONFIRM_WARNING,
                CommonComponents.GUI_YES,
                CANCEL_LABEL
        ));
    }

    private int read(net.minecraft.client.gui.components.EditBox box, int fallback, int min, int max) {
        if (box == null) return fallback;
        try {
            int value = Integer.parseInt(box.getValue().trim());
            return Math.max(min, Math.min(max, value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private void saveAndClose() {
        int oldOffsetX = TTClientConfig.OFFSET_X.get();
        int oldOffsetY = TTClientConfig.OFFSET_Y.get();
        int oldMaxWidth = TTClientConfig.MAX_WIDTH.get();
        int oldMaxHints = TTClientConfig.MAX_HINTS.get();
        int oldFadeIn = TTClientConfig.FADE_IN.get();
        int oldFadeOut = TTClientConfig.FADE_OUT.get();

        int offsetX = read(offsetXBox, oldOffsetX, 0, 1000);
        int offsetY = read(offsetYBox, oldOffsetY, 0, 1000);
        int maxWidth = read(maxWidthBox, oldMaxWidth, 120, 600);
        int maxHints = read(maxHintsBox, oldMaxHints, 1, 10);
        int fadeIn = read(fadeInBox, oldFadeIn, 1, 100);
        int fadeOut = read(fadeOutBox, oldFadeOut, 1, 100);

        TTClientConfig.ENABLE.set(pendingEnable);
        TTClientConfig.OFFSET_X.set(offsetX);
        TTClientConfig.OFFSET_Y.set(offsetY);
        TTClientConfig.MAX_WIDTH.set(maxWidth);
        TTClientConfig.MAX_HINTS.set(maxHints);
        TTClientConfig.FADE_IN.set(fadeIn);
        TTClientConfig.FADE_OUT.set(fadeOut);
        TTClientConfig.SPEC.save();

        TTSettings settings = TTConfigManager.readGlobalSettings();
        settings.debug = pendingDebug;
        settings.checkInterval = read(checkIntervalBox, settings.checkInterval, 1, 72000);
        settings.maxActiveHints = maxHints;
        settings.defaultDuration = read(defaultDurationBox, settings.defaultDuration, -1, 72000);
        TTConfigManager.saveGlobalSettings(settings);

        changed = false;
        Minecraft.getInstance().setScreen(parent);
    }

    public void refresh() {
        if (saveButton == null || quitButton == null) return;
        saveButton.active = changed;
        saveButton.setMessage(getSaveLabel());
        quitButton.setMessage(getQuitLabel());
    }

    private Component getQuitLabel() {
        return changed
                ? Component.translatable("trackertips.gui.quit.unsaved")
                : CANCEL_LABEL;
    }

    private Component getSaveLabel() {
        return changed
                ? Component.translatable("trackertips.gui.save")
                : Component.translatable("trackertips.gui.saved");
    }

    public Minecraft getMinecraft() {
        return Objects.requireNonNull(minecraft);
    }

    public int getHeaderHeight() {
        return layout.getHeaderHeight();
    }

    public int getFooterHeight() {
        return layout.getFooterHeight();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
