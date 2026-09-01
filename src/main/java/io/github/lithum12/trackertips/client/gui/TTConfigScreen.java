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
import net.minecraft.client.gui.components.EditBox;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** TrackerTips configuration hub using Minecraft 1.20.1's native TabNavigationBar. */
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
    private boolean pendingShortcutCommand;
    private int pendingOffsetX;
    private int pendingOffsetY;
    private int pendingMaxWidth;
    private int pendingMaxHints;
    private int pendingFadeIn;
    private int pendingFadeOut;
    private int pendingCheckInterval;
    private int pendingDefaultDuration;

    private final Set<Path> pendingDeletedEvents = new LinkedHashSet<>();
    private final Set<String> pendingDeletedThemes = new LinkedHashSet<>();

    /**
     * Bug fix: TTClothEventEditor writes its event JSON straight to disk as soon as its own
     * (Cloth Config) Save button is pressed, completely bypassing this screen's pending/undo
     * system. That meant there was no way to back out of an event edit from TrackerTips' own
     * config screen: the "Cancel"/quit-without-saving flow below only ever reverted the
     * settings and staged deletions tracked directly on this screen. This map snapshots each
     * event file's content the first time it is opened for editing during this screen's
     * lifetime, so a discarded session can restore it (or delete it, if it didn't exist
     * before) instead of silently keeping the Cloth Config write.
     */
    private final Map<Path, String> pendingEventBackups = new LinkedHashMap<>();

    private int selectedTabIndex;

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
        // Feature: tab content is rendered manually (see render() below) so it can be scissor-clipped
        // to the tab area; addWidget (not addRenderableWidget) still registers it for input dispatch
        // (clicks/keys/scroll) via the normal children() list, it just skips the automatic render pass.
        this.tabManager = new TabManager(this::addWidget, this::removeWidget);
        TTThemeManager.ensureDefaults();
        loadPendingValues();
    }

    private TTConfigScreen(Screen parent, TTConfigScreen state, int selectedTabIndex) {
        this(parent);
        this.changed = state.changed;
        this.pendingEnable = state.pendingEnable;
        this.pendingDebug = state.pendingDebug;
        this.pendingShortcutCommand = state.pendingShortcutCommand;
        this.pendingOffsetX = state.currentOffsetX();
        this.pendingOffsetY = state.currentOffsetY();
        this.pendingMaxWidth = state.currentMaxWidth();
        this.pendingMaxHints = state.currentMaxHints();
        this.pendingFadeIn = state.currentFadeIn();
        this.pendingFadeOut = state.currentFadeOut();
        this.pendingCheckInterval = state.currentCheckInterval();
        this.pendingDefaultDuration = state.currentDefaultDuration();
        this.pendingDeletedEvents.addAll(state.pendingDeletedEvents);
        this.pendingDeletedThemes.addAll(state.pendingDeletedThemes);
        this.pendingEventBackups.putAll(state.pendingEventBackups);
        this.selectedTabIndex = selectedTabIndex;
    }

    private void loadPendingValues() {
        pendingEnable = TTClientConfig.ENABLE.get();
        pendingOffsetX = TTClientConfig.OFFSET_X.get();
        pendingOffsetY = TTClientConfig.OFFSET_Y.get();
        pendingMaxWidth = TTClientConfig.MAX_WIDTH.get();
        pendingMaxHints = TTClientConfig.MAX_HINTS.get();
        pendingFadeIn = TTClientConfig.FADE_IN.get();
        pendingFadeOut = TTClientConfig.FADE_OUT.get();

        TTSettings settings = TTConfigManager.readGlobalSettings();
        pendingDebug = settings.debug;
        pendingCheckInterval = settings.checkInterval;
        pendingDefaultDuration = settings.defaultDuration;
        pendingShortcutCommand = settings.shortcutCommand;
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

        tabNavigationBar.selectTab(Math.max(0, Math.min(selectedTabIndex, tabs.size() - 1)), false);
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
        // Bug fix: content is scissor-clipped to the tab's own area here, before the rest of the
        // screen (nav bar, footer, and the deferred tooltip draw inside super.render()) so a row
        // that's only partially scrolled into view is cleanly cut off instead of visibly bleeding
        // past the header/footer boundary.
        if (tabManager.getCurrentTab() instanceof TTConfigTab tab) {
            tab.renderContent(graphics, mouseX, mouseY, partialTick);
        }
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

    /** Rebuilds the native tab bar while preserving the current tab and pending changes. */
    public void reopenTabs() {
        capturePendingValues();

        int currentTab = 0;
        if (tabManager.getCurrentTab() instanceof TTConfigTab tab) {
            currentTab = tab.kind().ordinal();
        }

        Minecraft.getInstance().setScreen(
                new TTConfigScreen(parent, this, currentTab)
        );
    }

    private void capturePendingValues() {
        pendingOffsetX = currentOffsetX();
        pendingOffsetY = currentOffsetY();
        pendingMaxWidth = currentMaxWidth();
        pendingMaxHints = currentMaxHints();
        pendingFadeIn = currentFadeIn();
        pendingFadeOut = currentFadeOut();
        pendingCheckInterval = currentCheckInterval();
        pendingDefaultDuration = currentDefaultDuration();
    }

    private int parse(EditBox box, int fallback) {
        if (box == null) return fallback;
        try {
            return Integer.parseInt(box.getValue().trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    int currentOffsetX() { return parse(offsetXBox, pendingOffsetX); }
    int currentOffsetY() { return parse(offsetYBox, pendingOffsetY); }
    int currentMaxWidth() { return parse(maxWidthBox, pendingMaxWidth); }
    int currentMaxHints() { return parse(maxHintsBox, pendingMaxHints); }
    int currentFadeIn() { return parse(fadeInBox, pendingFadeIn); }
    int currentFadeOut() { return parse(fadeOutBox, pendingFadeOut); }
    int currentCheckInterval() { return parse(checkIntervalBox, pendingCheckInterval); }
    int currentDefaultDuration() { return parse(defaultDurationBox, pendingDefaultDuration); }

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

    boolean pendingEnable() {
        return pendingEnable;
    }

    boolean pendingDebug() {
        return pendingDebug;
    }

    void setPendingShortcutCommand(boolean value) {
        pendingShortcutCommand = value;
        markChanged();
    }

    boolean pendingShortcutCommand() {
        return pendingShortcutCommand;
    }

    void setOffsetXBox(net.minecraft.client.gui.components.EditBox box) { offsetXBox = box; }
    void setOffsetYBox(net.minecraft.client.gui.components.EditBox box) { offsetYBox = box; }
    void setMaxWidthBox(net.minecraft.client.gui.components.EditBox box) { maxWidthBox = box; }
    void setMaxHintsBox(net.minecraft.client.gui.components.EditBox box) { maxHintsBox = box; }
    void setFadeInBox(net.minecraft.client.gui.components.EditBox box) { fadeInBox = box; }
    void setFadeOutBox(net.minecraft.client.gui.components.EditBox box) { fadeOutBox = box; }
    void setCheckIntervalBox(net.minecraft.client.gui.components.EditBox box) { checkIntervalBox = box; }
    void setDefaultDurationBox(net.minecraft.client.gui.components.EditBox box) { defaultDurationBox = box; }

    void stageDeleteEvent(Path file) {
        pendingDeletedEvents.add(file.toAbsolutePath().normalize());
        markChanged();
        reopenTabs();
    }

    boolean isEventPendingDeletion(Path file) {
        return pendingDeletedEvents.contains(file.toAbsolutePath().normalize());
    }

    void stageDeleteTheme(String id) {
        if ("trackertips:default".equals(id)) return;
        pendingDeletedThemes.add(id);
        markChanged();
        reopenTabs();
    }

    boolean isThemePendingDeletion(String id) {
        return pendingDeletedThemes.contains(id);
    }

    /**
     * Call this right before opening TTClothEventEditor (or before creating a brand-new
     * event file) for the given path. Snapshots the file's current content so it can be
     * restored if the user backs out of this whole config screen without saving. Only the
     * first snapshot per file/session is kept, so repeated edits in the same session still
     * revert all the way back to what was on disk when this screen was opened.
     */
    void registerEventEdit(Path file) {
        Path key = file.toAbsolutePath().normalize();
        if (pendingEventBackups.containsKey(key)) return;
        try {
            String content = Files.exists(key) ? Files.readString(key, StandardCharsets.UTF_8) : null;
            pendingEventBackups.put(key, content);
        } catch (Exception e) {
            io.github.lithum12.trackertips.TrackerTips.LOGGER.error(
                    "Failed to back up TrackerTips event before editing: {}", key, e);
        }
        markChanged();
    }

    private void restoreEventBackups() {
        for (Map.Entry<Path, String> backup : pendingEventBackups.entrySet()) {
            Path file = backup.getKey();
            String original = backup.getValue();
            try {
                if (original == null) {
                    Files.deleteIfExists(file);
                } else {
                    Files.createDirectories(file.getParent());
                    Files.writeString(file, original, StandardCharsets.UTF_8);
                }
            } catch (Exception e) {
                io.github.lithum12.trackertips.TrackerTips.LOGGER.error(
                        "Failed to restore TrackerTips event on discard: {}", file, e);
            }
        }
        pendingEventBackups.clear();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return tabNavigationBar != null && tabNavigationBar.keyPressed(keyCode)
                || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollAmount) {
        // Feature: forward wheel scroll to the active tab's own scroll handling (see
        // TTConfigTab.mouseScrolled) so long Events/Themes lists can be scrolled into view.
        if (tabManager.getCurrentTab() instanceof TTConfigTab tab && tab.mouseScrolled(mouseX, mouseY, scrollAmount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollAmount);
    }

    @Override
    public void onClose() {
        if (!changed) {
            Minecraft.getInstance().setScreen(parent);
            return;
        }

        Minecraft.getInstance().setScreen(new ConfirmScreen(
                confirmed -> {
                    // Nothing was physically deleted yet; simply leaving the screen discards staged deletions.
                    // Any event edited via the Cloth Config trigger editor during this session is restored
                    // (or, if it was newly created, deleted) from the backup taken in registerEventEdit().
                    if (confirmed) restoreEventBackups();
                    Minecraft.getInstance().setScreen(confirmed ? parent : this);
                },
                QUIT_CONFIRM_TITLE,
                QUIT_CONFIRM_WARNING,
                CommonComponents.GUI_YES,
                CANCEL_LABEL
        ));
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void saveAndClose() {
        capturePendingValues();

        TTClientConfig.ENABLE.set(pendingEnable);
        TTClientConfig.OFFSET_X.set(clamp(pendingOffsetX, 0, 1000));
        TTClientConfig.OFFSET_Y.set(clamp(pendingOffsetY, 0, 1000));
        TTClientConfig.MAX_WIDTH.set(clamp(pendingMaxWidth, 120, 600));
        TTClientConfig.MAX_HINTS.set(clamp(pendingMaxHints, 1, 10));
        TTClientConfig.FADE_IN.set(clamp(pendingFadeIn, 1, 100));
        TTClientConfig.FADE_OUT.set(clamp(pendingFadeOut, 1, 100));
        TTClientConfig.SPEC.save();

        TTSettings settings = TTConfigManager.readGlobalSettings();
        settings.debug = pendingDebug;
        settings.checkInterval = clamp(pendingCheckInterval, 1, 72000);
        settings.maxActiveHints = TTClientConfig.MAX_HINTS.get();
        settings.defaultDuration = clamp(pendingDefaultDuration, -1, 72000);
        settings.shortcutCommand = pendingShortcutCommand;
        TTConfigManager.saveGlobalSettings(settings);

        for (Path file : pendingDeletedEvents) {
            try {
                Files.deleteIfExists(file);
            } catch (Exception e) {
                io.github.lithum12.trackertips.TrackerTips.LOGGER.error(
                        "Failed to delete TrackerTips event: {}", file, e);
            }
        }

        for (String id : pendingDeletedThemes) {
            if (!"trackertips:default".equals(id)) {
                var theme = TTThemeManager.get(id);
                if (theme != null) TTThemeManager.delete(theme);
            }
        }

        pendingDeletedEvents.clear();
        pendingDeletedThemes.clear();
        // Event edits made via the Cloth Config trigger editor were already written straight
        // to disk when that screen's own Save button was pressed; committing here just means
        // dropping the backups instead of restoring them.
        pendingEventBackups.clear();
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
