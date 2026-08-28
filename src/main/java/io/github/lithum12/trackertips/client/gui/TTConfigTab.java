package io.github.lithum12.trackertips.client.gui;

import io.github.lithum12.trackertips.config.TTClientConfig;
import io.github.lithum12.trackertips.config.TTConfigManager;
import io.github.lithum12.trackertips.config.TTSettings;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** One native Minecraft 1.20.1 configuration tab. */
final class TTConfigTab extends GridLayoutTab {
    private final TTConfigScreen screen;
    private final Kind kind;

    private final List<AbstractWidget> widgets = new ArrayList<>();

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

    private void label(String key, int row) {
        add(new TTLabel(Component.translatable(key)), row, 0);
    }

    private void buildGeneral() {
        TTSettings settings = TTConfigManager.readGlobalSettings();
        int row = 0;

        label("trackertips.gui.enable", row);
        add(CycleButton.booleanBuilder(
                Component.translatable("trackertips.value.yes"),
                Component.translatable("trackertips.value.no"))
                .withInitialValue(TTClientConfig.ENABLE.get())
                .create(0, 0, 150, 20, Component.empty(),
                        (button, value) -> screen.setPendingEnable(value)), row++, 1);

        label("trackertips.gui.offset_x", row);
        add(numberBox(TTClientConfig.OFFSET_X.get(), 0, 1000), row, 1);
        screen.setOffsetXBox((EditBox) widgets.get(widgets.size() - 1));
        row++;

        label("trackertips.gui.offset_y", row);
        add(numberBox(TTClientConfig.OFFSET_Y.get(), 0, 1000), row, 1);
        screen.setOffsetYBox((EditBox) widgets.get(widgets.size() - 1));
        row++;

        label("trackertips.gui.max_width", row);
        add(numberBox(TTClientConfig.MAX_WIDTH.get(), 120, 600), row, 1);
        screen.setMaxWidthBox((EditBox) widgets.get(widgets.size() - 1));
        row++;

        label("trackertips.gui.max_hints", row);
        add(numberBox(TTClientConfig.MAX_HINTS.get(), 1, 10), row, 1);
        screen.setMaxHintsBox((EditBox) widgets.get(widgets.size() - 1));
        row++;

        label("trackertips.gui.fade_in", row);
        add(numberBox(TTClientConfig.FADE_IN.get(), 1, 100), row, 1);
        screen.setFadeInBox((EditBox) widgets.get(widgets.size() - 1));
        row++;

        label("trackertips.gui.fade_out", row);
        add(numberBox(TTClientConfig.FADE_OUT.get(), 1, 100), row, 1);
        screen.setFadeOutBox((EditBox) widgets.get(widgets.size() - 1));
        row++;

        label("trackertips.gui.check_interval", row);
        add(numberBox(settings.checkInterval, 1, 72000), row, 1);
        screen.setCheckIntervalBox((EditBox) widgets.get(widgets.size() - 1));
        row++;

        label("trackertips.gui.debug", row);
        add(CycleButton.booleanBuilder(
                Component.translatable("trackertips.value.yes"),
                Component.translatable("trackertips.value.no"))
                .withInitialValue(settings.debug)
                .create(0, 0, 150, 20, Component.empty(),
                        (button, value) -> screen.setPendingDebug(value)), row++, 1);

        label("trackertips.gui.default_duration", row);
        add(numberBox(settings.defaultDuration, -1, 72000), row, 1);
        screen.setDefaultDurationBox((EditBox) widgets.get(widgets.size() - 1));
        row++;

        add(new TTLabel(Component.translatable("trackertips.gui.general_hint")), row, 0);
    }

    private EditBox numberBox(int value, int min, int max) {
        EditBox box = new EditBox(Minecraft.getInstance().font, 0, 0, 150, 20, Component.empty());
        box.setValue(Integer.toString(value));
        box.setFilter(s -> s.matches("-?\\d*"));
        box.setResponder(valueString -> screen.markChanged());
        box.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("trackertips.gui.range", min, max)));
        return box;
    }

    private void buildEvents() {
        Path folder = TTConfigManager.globalFolder().resolve("hints");
        List<Path> files = new ArrayList<>();
        try {
            if (Files.isDirectory(folder)) {
                try (var stream = Files.list(folder)) {
                    stream.filter(p -> p.getFileName().toString().endsWith(".json"))
                            .sorted().forEach(files::add);
                }
            }
        } catch (Exception ignored) {}

        if (files.isEmpty()) {
            Button add = Button.builder(Component.translatable("trackertips.gui.add_event"),
                    b -> Minecraft.getInstance().setScreen(TTNewEventScreen.create(screen)))
                    .width(220).build();
            add(add, 0, 0);
            return;
        }

        int row = 0;
        for (Path file : files) {
            Button open = Button.builder(Component.literal(file.getFileName().toString()),
                    b -> Minecraft.getInstance().setScreen(TTClothEventEditor.create(screen, file)))
                    .width(220).build();
            Button edit = Button.builder(Component.translatable("trackertips.gui.edit"),
                    b -> Minecraft.getInstance().setScreen(TTClothEventEditor.create(screen, file)))
                    .width(70).build();
            Button delete = Button.builder(Component.translatable("trackertips.gui.delete"),
                    b -> Minecraft.getInstance().setScreen(TTConfirmScreen.create(screen,
                            Component.translatable("trackertips.gui.delete_confirm", file.getFileName()),
                            () -> { try { Files.deleteIfExists(file); } catch (Exception ignored) {} })))
                    .width(70).build();
            add(open, row, 0);
            add(edit, row, 1);
            add(delete, row, 2);
            row++;
        }

        add(Button.builder(Component.translatable("trackertips.gui.add_event"),
                b -> Minecraft.getInstance().setScreen(TTNewEventScreen.create(screen))).width(160).build(), row, 0);
        add(Button.builder(Component.translatable("trackertips.gui.refresh"),
                b -> screen.reopenTabs()).width(100).build(), row, 1);
    }

    private void buildThemes() {
        List<TTTheme> themes = new ArrayList<>(TTThemeManager.all());
        if (themes.isEmpty()) {
            add(Button.builder(Component.translatable("trackertips.gui.theme.create"),
                    b -> Minecraft.getInstance().setScreen(TTThemeEditorScreen.create(screen, null)))
                    .width(180).build(), 0, 0);
            return;
        }

        int row = 0;
        for (TTTheme theme : themes) {
            Button open = Button.builder(Component.literal(theme.name()),
                    b -> Minecraft.getInstance().setScreen(TTThemeEditorScreen.create(screen, theme)))
                    .width(220).build();
            Button edit = Button.builder(Component.translatable("trackertips.gui.edit"),
                    b -> Minecraft.getInstance().setScreen(TTThemeEditorScreen.create(screen, theme)))
                    .width(70).build();

            add(open, row, 0);
            add(edit, row, 1);

            if (!"trackertips:default".equals(theme.id())) {
                Button delete = Button.builder(Component.translatable("trackertips.gui.delete"),
                        b -> Minecraft.getInstance().setScreen(TTConfirmScreen.create(screen,
                                Component.translatable("trackertips.gui.theme.delete_confirm", theme.name()),
                                () -> TTThemeManager.delete(theme))))
                        .width(70).build();
                add(delete, row, 2);
            }
            row++;
        }

        add(Button.builder(Component.translatable("trackertips.gui.theme.create"),
                b -> Minecraft.getInstance().setScreen(TTThemeEditorScreen.create(screen, null)))
                .width(160).build(), row, 0);
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
    }

    @Override
    public void doLayout(ScreenRectangle rectangle) {
        layout.setX(rectangle.left() + 20);
        layout.setY(rectangle.top() + 12);
        layout.arrangeElements();
    }

    Kind kind() {
        return kind;
    }
}
