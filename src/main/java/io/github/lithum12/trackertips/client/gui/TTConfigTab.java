package io.github.lithum12.trackertips.client.gui;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.github.lithum12.trackertips.config.TTClientConfig;
import io.github.lithum12.trackertips.config.TTConfigManager;
import io.github.lithum12.trackertips.theme.TTAnimation;
import io.github.lithum12.trackertips.theme.TTTheme;
import io.github.lithum12.trackertips.theme.TTThemeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** One native Minecraft 1.20.1 configuration tab. */
final class TTConfigTab extends GridLayoutTab {
    private final TTConfigScreen screen;
    private final Kind kind;

    private final List<AbstractWidget> widgets = new ArrayList<>();
    private final TTScrollBar scrollBar = new TTScrollBar(this);
    private final Map<AbstractWidget, Integer> naturalY = new IdentityHashMap<>();

    // Bug fix / feature: the tab's own content had no scrolling, so a long Events or Themes
    // list would simply run off the bottom of the screen with no way to reach the rest. These
    // fields track the visible content window and how far the content is currently scrolled;
    // doLayout() recomputes them on every relayout (including window resizes and tab rebuilds).
    private double scrollAmount;
    private int viewTop;
    private int viewBottom;
    private int viewLeft;
    private int viewRight;
    private int contentHeight;

    private static final int SCROLL_SPEED = 16;

    // These mirror the hard-coded defaults in TTClientConfig / TTSettings and only drive the
    // "italicize when changed from default" indicator below -- keep them in sync with those.
    private static final boolean DEFAULT_ENABLE = true;
    private static final int DEFAULT_OFFSET_X = 6;
    private static final int DEFAULT_OFFSET_Y = 45;
    private static final int DEFAULT_MAX_WIDTH = 260;
    private static final int DEFAULT_MAX_HINTS = 3;
    private static final int DEFAULT_FADE_IN = 6;
    private static final int DEFAULT_FADE_OUT = 10;
    private static final int DEFAULT_CHECK_INTERVAL = 20;
    private static final boolean DEFAULT_DEBUG = false;
    private static final int DEFAULT_DEFAULT_DURATION = 240;
    private static final boolean DEFAULT_SHORTCUT_COMMAND = false;

    enum Kind {
        GENERAL, EVENTS, THEMES, USAGE;

        String translationKey() {
            return switch (this) {
                case GENERAL -> "trackertips.gui.tab.general";
                case EVENTS -> "trackertips.gui.tab.events";
                case THEMES -> "trackertips.gui.tab.themes";
                case USAGE -> "trackertips.gui.tab.usage";
            };
        }
    }

    TTConfigTab(TTConfigScreen screen, Kind kind) {
        super(Component.translatable(kind.translationKey()));
        this.screen = screen;
        this.kind = kind;
        layout.columnSpacing(16).rowSpacing(8);
        build();
    }

    private void build() {
        switch (kind) {
            case GENERAL -> buildGeneral();
            case EVENTS -> buildEvents();
            case THEMES -> buildThemes();
            case USAGE -> buildUsage();
        }
    }

    private void add(AbstractWidget widget, int row, int col) {
        widgets.add(widget);
        layout.addChild(widget, row, col);
    }

    private TTLabel label(String key, int row) {
        TTLabel l = new TTLabel(Component.translatable(key));
        add(l, row, 0);
        return l;
    }

    private void buildGeneral() {
        int row = 0;

        TTLabel enableLabel = label("trackertips.gui.enable", row);
        boolean initialEnable = screen.pendingEnable();
        enableLabel.setItalic(initialEnable != DEFAULT_ENABLE);
        add(CycleButton.booleanBuilder(
                Component.translatable("trackertips.value.yes"),
                Component.translatable("trackertips.value.no"))
                .withInitialValue(initialEnable)
                .create(0, 0, 150, 20, Component.empty(),
                        (button, value) -> {
                            screen.setPendingEnable(value);
                            enableLabel.setItalic(value != DEFAULT_ENABLE);
                        }), row++, 1);

        TTLabel offsetXLabel = label("trackertips.gui.offset_x", row);
        add(numberBox(offsetXLabel, screen.currentOffsetX(), 0, 1000, DEFAULT_OFFSET_X), row, 1);
        screen.setOffsetXBox((EditBox) widgets.get(widgets.size() - 1));
        row++;

        TTLabel offsetYLabel = label("trackertips.gui.offset_y", row);
        add(numberBox(offsetYLabel, screen.currentOffsetY(), 0, 1000, DEFAULT_OFFSET_Y), row, 1);
        screen.setOffsetYBox((EditBox) widgets.get(widgets.size() - 1));
        row++;

        TTLabel maxWidthLabel = label("trackertips.gui.max_width", row);
        add(numberBox(maxWidthLabel, screen.currentMaxWidth(), 120, 600, DEFAULT_MAX_WIDTH), row, 1);
        screen.setMaxWidthBox((EditBox) widgets.get(widgets.size() - 1));
        row++;

        TTLabel maxHintsLabel = label("trackertips.gui.max_hints", row);
        add(numberBox(maxHintsLabel, screen.currentMaxHints(), 1, 10, DEFAULT_MAX_HINTS), row, 1);
        screen.setMaxHintsBox((EditBox) widgets.get(widgets.size() - 1));
        row++;

        TTLabel fadeInLabel = label("trackertips.gui.fade_in", row);
        add(numberBox(fadeInLabel, screen.currentFadeIn(), 1, 100, DEFAULT_FADE_IN), row, 1);
        screen.setFadeInBox((EditBox) widgets.get(widgets.size() - 1));
        row++;

        TTLabel fadeOutLabel = label("trackertips.gui.fade_out", row);
        add(numberBox(fadeOutLabel, screen.currentFadeOut(), 1, 100, DEFAULT_FADE_OUT), row, 1);
        screen.setFadeOutBox((EditBox) widgets.get(widgets.size() - 1));
        row++;

        TTLabel checkIntervalLabel = label("trackertips.gui.check_interval", row);
        add(numberBox(checkIntervalLabel, screen.currentCheckInterval(), 1, 72000, DEFAULT_CHECK_INTERVAL), row, 1);
        screen.setCheckIntervalBox((EditBox) widgets.get(widgets.size() - 1));
        row++;

        TTLabel debugLabel = label("trackertips.gui.debug", row);
        boolean initialDebug = screen.pendingDebug();
        debugLabel.setItalic(initialDebug != DEFAULT_DEBUG);
        add(CycleButton.booleanBuilder(
                Component.translatable("trackertips.value.yes"),
                Component.translatable("trackertips.value.no"))
                .withInitialValue(initialDebug)
                .create(0, 0, 150, 20, Component.empty(),
                        (button, value) -> {
                            screen.setPendingDebug(value);
                            debugLabel.setItalic(value != DEFAULT_DEBUG);
                        }), row++, 1);

        TTLabel defaultDurationLabel = label("trackertips.gui.default_duration", row);
        add(numberBox(defaultDurationLabel, screen.currentDefaultDuration(), -1, 72000, DEFAULT_DEFAULT_DURATION), row, 1);
        screen.setDefaultDurationBox((EditBox) widgets.get(widgets.size() - 1));
        row++;

        TTLabel shortcutLabel = label("trackertips.gui.shortcut_command", row);
        boolean initialShortcut = screen.pendingShortcutCommand();
        shortcutLabel.setItalic(initialShortcut != DEFAULT_SHORTCUT_COMMAND);
        add(CycleButton.booleanBuilder(
                Component.translatable("trackertips.value.yes"),
                Component.translatable("trackertips.value.no"))
                .withInitialValue(initialShortcut)
                .create(0, 0, 150, 20, Component.empty(),
                        (button, value) -> {
                            screen.setPendingShortcutCommand(value);
                            shortcutLabel.setItalic(value != DEFAULT_SHORTCUT_COMMAND);
                        }), row++, 1);

        add(new TTLabel(Component.translatable("trackertips.gui.general_hint")), row, 0);
    }

    /** @param label the row's label, italicized live whenever the parsed value differs from defaultValue. */
    private EditBox numberBox(TTLabel label, int value, int min, int max, int defaultValue) {
        EditBox box = new EditBox(Minecraft.getInstance().font, 0, 0, 150, 20, Component.empty());
        box.setValue(Integer.toString(value));
        box.setFilter(s -> s.matches("-?\\d*"));
        box.setResponder(valueString -> {
            screen.markChanged();
            try {
                label.setItalic(Integer.parseInt(valueString.trim()) != defaultValue);
            } catch (Exception ignored) {
                // Leave the italic state as-is while the field holds an incomplete/invalid number.
            }
        });
        box.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("trackertips.gui.range", min, max)));
        label.setItalic(value != defaultValue);
        return box;
    }

    private void buildEvents() {
        Path folder = TTConfigManager.globalFolder().resolve("hints");
        List<Path> files = new ArrayList<>();
        try {
            if (Files.isDirectory(folder)) {
                try (var stream = Files.list(folder)) {
                    stream.filter(p -> p.getFileName().toString().endsWith(".json"))
                            .filter(p -> !screen.isEventPendingDeletion(p))
                            .sorted().forEach(files::add);
                }
            }
        } catch (Exception ignored) {}

        if (files.isEmpty()) {
            add(Button.builder(Component.translatable("trackertips.gui.add_event"),
                    b -> Minecraft.getInstance().setScreen(TTNewEventScreen.create(screen)))
                    .width(260).build(), 0, 0);
            return;
        }

        int row = 0;
        for (Path file : files) {
            Component displayName = eventDisplayName(file);
            Button open = Button.builder(displayName,
                    b -> {
                        screen.registerEventEdit(file);
                        Minecraft.getInstance().setScreen(TTClothEventEditor.create(screen, file));
                    })
                    .width(320).build();
            open.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                    Component.literal(file.getFileName().toString())));

            Button delete = Button.builder(Component.literal("×"),
                    b -> Minecraft.getInstance().setScreen(TTConfirmScreen.create(screen,
                            Component.translatable("trackertips.gui.delete_confirm", displayName),
                            () -> screen.stageDeleteEvent(file))))
                    .width(24).build();
            delete.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                    Component.translatable("trackertips.gui.delete")));

            add(open, row, 0);
            add(delete, row, 1);
            row++;
        }

        add(Button.builder(Component.translatable("trackertips.gui.add_event"),
                b -> Minecraft.getInstance().setScreen(TTNewEventScreen.create(screen)))
                .width(170).build(), row, 0);
        add(Button.builder(Component.translatable("trackertips.gui.refresh"),
                b -> screen.reopenTabs()).width(100).build(), row, 1);
    }

    private Component eventDisplayName(Path file) {
        try {
            JsonElement root = JsonParser.parseString(Files.readString(file));
            if (root.isJsonObject()) {
                var json = root.getAsJsonObject();
                if (json.has("title")) {
                    JsonElement title = json.get("title");
                    if (title.isJsonObject()) {
                        var object = title.getAsJsonObject();
                        if (object.has("translate")) {
                            String key = object.get("translate").getAsString().trim();
                            if (!key.isEmpty()) return Component.translatable(key);
                        }
                        if (object.has("text")) {
                            String text = object.get("text").getAsString().trim();
                            if (!text.isEmpty()) return Component.literal(text);
                        }
                    } else if (title.isJsonPrimitive() && title.getAsJsonPrimitive().isString()) {
                        String text = title.getAsString().trim();
                        if (!text.isEmpty()) return Component.literal(text);
                    }
                }
                if (json.has("id")) {
                    String id = json.get("id").getAsString();
                    int colon = id.indexOf(':');
                    return Component.literal(colon >= 0 ? id.substring(colon + 1) : id);
                }
            }
        } catch (Exception ignored) {}
        String name = file.getFileName().toString();
        return Component.literal(name.endsWith(".json") ? name.substring(0, name.length() - 5) : name);
    }

    private void buildThemes() {
        List<TTTheme> themes = new ArrayList<>(TTThemeManager.all());
        themes.removeIf(theme -> screen.isThemePendingDeletion(theme.id()));
        if (themes.isEmpty()) {
            add(Button.builder(Component.translatable("trackertips.gui.theme.create"),
                    b -> Minecraft.getInstance().setScreen(TTThemeEditorScreen.create(screen, null)))
                    .width(260).build(), 0, 0);
            return;
        }

        int row = 0;
        for (TTTheme theme : themes) {
            Button open = Button.builder(Component.literal(theme.name()),
                    b -> Minecraft.getInstance().setScreen(TTThemeEditorScreen.create(screen, theme)))
                    .width(320).build();
            open.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                    Component.literal(theme.id())));

            add(open, row, 0);

            if (!"trackertips:default".equals(theme.id())) {
                Button delete = Button.builder(Component.literal("×"),
                        b -> Minecraft.getInstance().setScreen(TTConfirmScreen.create(screen,
                                Component.translatable("trackertips.gui.theme.delete_confirm", theme.name()),
                                () -> {
                                    screen.stageDeleteTheme(theme.id());
                                })))
                        .width(24).build();
                add(delete, row, 1);
            }
            row++;
        }

        add(Button.builder(Component.translatable("trackertips.gui.theme.create"),
                b -> Minecraft.getInstance().setScreen(TTThemeEditorScreen.create(screen, null)))
                .width(170).build(), row, 0);
        add(Button.builder(Component.translatable("trackertips.gui.refresh"),
                b -> screen.reopenTabs()).width(100).build(), row, 1);
    }

    private void buildUsage() {
        add(new TTLabel(Component.translatable("trackertips.gui.usage.title")), 0, 0);
        add(new TTLabel(Component.translatable("trackertips.gui.usage.line1")), 1, 0);
        add(new TTLabel(Component.translatable("trackertips.gui.usage.line2")), 2, 0);
        add(new TTLabel(Component.translatable("trackertips.gui.usage.line3")), 3, 0);
        add(new TTLabel(Component.translatable("trackertips.gui.usage.theme_line")), 4, 0);
    }

    @Override
    public void visitChildren(java.util.function.Consumer<AbstractWidget> consumer) {
        super.visitChildren(consumer);
        consumer.accept(scrollBar);
    }

    @Override
    public void doLayout(ScreenRectangle rectangle) {
        viewTop = rectangle.top();
        viewBottom = rectangle.bottom();
        viewLeft = rectangle.left();
        viewRight = rectangle.right();

        layout.setX(rectangle.left() + 20);
        layout.setY(rectangle.top() + 12);
        layout.arrangeElements();

        // Bug fix: previously nothing tracked content taller than the visible tab area, so an
        // Events/Themes list with enough entries simply ran off the bottom of the screen with
        // no way to reach the rest. naturalY records each widget's un-scrolled position (as
        // just placed by the grid above) so applyScroll() can offset from a stable baseline.
        naturalY.clear();
        int bottomMost = rectangle.top() + 12;
        for (AbstractWidget widget : widgets) {
            naturalY.put(widget, widget.getY());
            bottomMost = Math.max(bottomMost, widget.getY() + widget.getHeight());
        }
        contentHeight = bottomMost - (rectangle.top() + 12);

        int maxScroll = Math.max(0, contentHeight - (viewBottom - viewTop));
        scrollAmount = Mth.clamp(scrollAmount, 0, maxScroll);

        scrollBar.setX(rectangle.right() - 10);
        scrollBar.setY(viewTop);
        scrollBar.setTrackHeight(Math.max(0, viewBottom - viewTop));
        scrollBar.visible = isScrollable();

        applyScroll();
    }

    /**
     * Renders this tab's own widgets, scissor-clipped to its content area. Called directly by
     * TTConfigScreen.render() since these widgets are registered for input only (see the
     * TabManager constructor in TTConfigScreen) and are no longer in the screen's auto-render list.
     */
    void renderContent(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (viewBottom <= viewTop || viewRight <= viewLeft) return;
        graphics.enableScissor(viewLeft, viewTop, viewRight, viewBottom);
        for (AbstractWidget widget : widgets) {
            widget.render(graphics, mouseX, mouseY, partialTick);
        }
        scrollBar.render(graphics, mouseX, mouseY, partialTick);
        graphics.disableScissor();
    }

    private void applyScroll() {
        for (AbstractWidget widget : widgets) {
            int baseY = naturalY.getOrDefault(widget, widget.getY());
            int y = baseY - (int) Math.round(scrollAmount);
            widget.setY(y);
            widget.visible = y + widget.getHeight() > viewTop && y < viewBottom;
        }
    }

    /** Called by TTConfigScreen when the mouse wheel is used over this tab's content area. */
    boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (mouseY < viewTop || mouseY > viewBottom) return false;
        int maxScroll = Math.max(0, contentHeight - (viewBottom - viewTop));
        if (maxScroll <= 0) return false;
        double newScroll = Mth.clamp(scrollAmount - scrollY * SCROLL_SPEED, 0, maxScroll);
        if (newScroll == scrollAmount) return false;
        scrollAmount = newScroll;
        applyScroll();
        return true;
    }

    /** Called by TTScrollBar while its thumb is being dragged. relativeFraction is 0..1 top..bottom. */
    void scrollTo(double relativeFraction) {
        int maxScroll = Math.max(0, contentHeight - (viewBottom - viewTop));
        scrollAmount = Mth.clamp(relativeFraction * maxScroll, 0, maxScroll);
        applyScroll();
    }

    boolean isScrollable() {
        return contentHeight > (viewBottom - viewTop);
    }

    int thumbSize(int trackHeight) {
        int viewport = Math.max(1, viewBottom - viewTop);
        double fraction = Math.min(1.0, (double) viewport / Math.max(1, contentHeight));
        return (int) Math.max(12, trackHeight * fraction);
    }

    int thumbOffset(int trackHeight) {
        int maxScroll = Math.max(0, contentHeight - (viewBottom - viewTop));
        if (maxScroll <= 0) return 0;
        int thumbSize = thumbSize(trackHeight);
        double fraction = scrollAmount / maxScroll;
        return (int) Math.round((trackHeight - thumbSize) * fraction);
    }

    Kind kind() {
        return kind;
    }
}
