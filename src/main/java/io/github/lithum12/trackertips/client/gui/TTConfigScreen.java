package io.github.lithum12.trackertips.client.gui;

import io.github.lithum12.trackertips.config.TTClientConfig;
import io.github.lithum12.trackertips.config.TTConfigManager;
import io.github.lithum12.trackertips.config.TTSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Main TrackerTips configuration screen.
 *
 * Vanilla 1.19.4 / Create World style top navigation.
 */
public class TTConfigScreen extends Screen {

    private final Screen parent;

    private enum Tab {
        GENERAL,
        EVENTS,
        COMMANDS,
        USAGE
    }

    private Tab currentTab = Tab.GENERAL;

    // Pending values
    private boolean pendingEnable;
    private int pendingMaxHints;
    private boolean pendingDebug;
    private int pendingCheckInterval;
    private int pendingDefaultDuration;

    // Input boxes
    private EditBox maxHintsBox;
    private EditBox checkIntervalBox;
    private EditBox defaultDurationBox;

    public TTConfigScreen() {
        this(null);
    }

    public TTConfigScreen(Screen parent) {
        super(Component.translatable("trackertips.gui.title"));
        this.parent = parent;

        pendingEnable = TTClientConfig.ENABLE.get();
        pendingMaxHints = TTClientConfig.MAX_HINTS.get();

        TTSettings settings = TTConfigManager.readGlobalSettings();

        pendingDebug = settings.debug;
        pendingCheckInterval = settings.checkInterval;
        pendingDefaultDuration = settings.defaultDuration;
    }

    @Override
    protected void init() {
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        pendingLabels.clear();

        maxHintsBox = null;
        checkIntervalBox = null;
        defaultDurationBox = null;

        buildTabs();

        switch (currentTab) {
            case GENERAL -> buildGeneral();
            case EVENTS -> buildEvents();
            case COMMANDS -> buildCommands();
            case USAGE -> {
            }
        }

        buildBottomButtons();
    }

    /* ------------------------------------------------------------
     * Top navigation
     * ------------------------------------------------------------ */

    private void buildTabs() {
        int left = 20;
        int width = this.width - 40;
        int tabWidth = width / 4;

        Tab[] tabs = Tab.values();

        for (int i = 0; i < tabs.length; i++) {
            final Tab tab = tabs[i];

            Button button = Button.builder(
                    Component.empty(),
                    b -> {
                        if (currentTab != tab) {
                            currentTab = tab;
                            rebuild();
                        }
                    }
            ).bounds(
                    left + i * tabWidth,
                    25,
                    tabWidth - 2,
                    24
            ).build();

            // Hide the vanilla button appearance.
            button.setAlpha(0.0F);

            addRenderableWidget(button);
        }
    }

    private Component getTabTitle(Tab tab) {
        return switch (tab) {
            case GENERAL ->
                    Component.translatable("trackertips.gui.tab.general");

            case EVENTS ->
                    Component.translatable("trackertips.gui.tab.events");

            case COMMANDS ->
                    Component.translatable("trackertips.gui.tab.commands");

            case USAGE ->
                    Component.translatable("trackertips.gui.tab.usage");
        };
    }

    private void renderTabs(GuiGraphics graphics) {
        int left = 20;
        int width = this.width - 40;
        int tabWidth = width / 4;

        for (int i = 0; i < Tab.values().length; i++) {
            Tab tab = Tab.values()[i];

            int x = left + i * tabWidth;
            int right = x + tabWidth - 2;

            boolean selected = currentTab == tab;

            // Base tab area.
            if (selected) {
                graphics.fill(
                        x,
                        25,
                        right,
                        49,
                        0xFF3F3F3F
                );

                // Vanilla-style selected underline.
                graphics.fill(
                        x,
                        47,
                        right,
                        49,
                        0xFFFFFFFF
                );
            } else {
                graphics.fill(
                        x,
                        25,
                        right,
                        49,
                        0x80202020
                );
            }

            Component title = getTabTitle(tab);

            graphics.drawCenteredString(
                    font,
                    title,
                    x + tabWidth / 2,
                    32,
                    selected ? 0xFFFFFF : 0xAAAAAA
            );
        }
    }

    /* ------------------------------------------------------------
     * General
     * ------------------------------------------------------------ */

    private void buildGeneral() {
        int labelX = this.width / 2 - 180;
        int controlX = this.width / 2 + 25;
        int controlWidth = 155;

        int y = 78;
        int rowHeight = 38;

        // Enable
        addRenderableWidget(
                CycleButton.booleanBuilder(
                                Component.translatable("trackertips.value.yes"),
                                Component.translatable("trackertips.value.no")
                        )
                        .withInitialValue(pendingEnable)
                        .create(
                                controlX,
                                y,
                                controlWidth,
                                20,
                                Component.empty(),
                                (button, value) -> pendingEnable = value
                        )
        );

        drawLabelLater(
                Component.translatable("trackertips.gui.enable"),
                labelX,
                y + 6
        );

        // Max hints
        maxHintsBox = createIntegerBox(
                controlX,
                y + rowHeight,
                controlWidth,
                pendingMaxHints
        );

        drawLabelLater(
                Component.translatable("trackertips.gui.max_hints"),
                labelX,
                y + rowHeight + 6
        );

        // Check interval
        checkIntervalBox = createIntegerBox(
                controlX,
                y + rowHeight * 2,
                controlWidth,
                pendingCheckInterval
        );

        drawLabelLater(
                Component.translatable("trackertips.gui.check_interval"),
                labelX,
                y + rowHeight * 2 + 6
        );

        // Debug
        addRenderableWidget(
                CycleButton.booleanBuilder(
                                Component.translatable("trackertips.value.yes"),
                                Component.translatable("trackertips.value.no")
                        )
                        .withInitialValue(pendingDebug)
                        .create(
                                controlX,
                                y + rowHeight * 3,
                                controlWidth,
                                20,
                                Component.empty(),
                                (button, value) -> pendingDebug = value
                        )
        );

        drawLabelLater(
                Component.translatable("trackertips.gui.debug"),
                labelX,
                y + rowHeight * 3 + 6
        );

        // Default duration
        defaultDurationBox = createIntegerBox(
                controlX,
                y + rowHeight * 4,
                controlWidth,
                pendingDefaultDuration
        );

        drawLabelLater(
                Component.translatable("trackertips.gui.default_duration"),
                labelX,
                y + rowHeight * 4 + 6
        );
    }

    private EditBox createIntegerBox(
            int x,
            int y,
            int width,
            int value
    ) {
        EditBox box = new EditBox(
                font,
                x,
                y,
                width,
                20,
                Component.empty()
        );

        box.setValue(Integer.toString(value));
        box.setFilter(s -> s.matches("-?\\d*"));
        box.setResponder(valueString -> {
            // Don't immediately mutate configuration.
            // The actual value is validated when saving.
        });

        addRenderableWidget(box);

        return box;
    }

    /*
     * Labels are rendered manually in render().
     * This avoids the duplicated Yes/No / label problem.
     */
    private final List<LabelEntry> pendingLabels = new ArrayList<>();

    private void drawLabelLater(Component text, int x, int y) {
        pendingLabels.add(new LabelEntry(text, x, y));
    }

    private record LabelEntry(Component text, int x, int y) {
    }

    /* ------------------------------------------------------------
     * Events
     * ------------------------------------------------------------ */

    private void buildEvents() {
        Path folder = TTConfigManager.globalFolder().resolve("hints");

        List<Path> files = new ArrayList<>();

        try {
            if (Files.isDirectory(folder)) {
                try (var stream = Files.list(folder)) {
                    stream.filter(
                                    p -> p.getFileName()
                                            .toString()
                                            .endsWith(".json")
                            )
                            .sorted()
                            .forEach(files::add);
                }
            }
        } catch (Exception ignored) {
        }

        int x = this.width / 2 - 190;
        int y = 64;

        int rowHeight = 28;
        int maxRows = Math.max(
                1,
                (this.height - 150) / rowHeight
        );

        if (files.isEmpty()) {
            addRenderableWidget(
                    Button.builder(
                                    Component.translatable(
                                            "trackertips.gui.add_event"
                                    ),
                                    b -> Minecraft.getInstance().setScreen(
                                            TTNewEventScreen.create(this)
                                    )
                            )
                            .bounds(
                                    this.width / 2 - 100,
                                    85,
                                    200,
                                    20
                            )
                            .build()
            );
        }

        for (int i = 0; i < Math.min(files.size(), maxRows); i++) {
            Path file = files.get(i);

            int rowY = y + i * rowHeight;

            addRenderableWidget(
                    Button.builder(
                                    Component.literal(
                                            file.getFileName().toString()
                                    ),
                                    b -> openEvent(file)
                            )
                            .bounds(x, rowY, 220, 20)
                            .build()
            );

            addRenderableWidget(
                    Button.builder(
                                    Component.translatable(
                                            "trackertips.gui.edit"
                                    ),
                                    b -> openEvent(file)
                            )
                            .bounds(x + 225, rowY, 70, 20)
                            .build()
            );

            addRenderableWidget(
                    Button.builder(
                                    Component.translatable(
                                            "trackertips.gui.delete"
                                    ),
                                    b -> confirmDelete(file)
                            )
                            .bounds(x + 300, rowY, 70, 20)
                            .build()
            );
        }

        if (!files.isEmpty()) {
            int actionsY = Math.min(
                    y + maxRows * rowHeight + 5,
                    this.height - 62
            );

            addRenderableWidget(
                    Button.builder(
                                    Component.translatable(
                                            "trackertips.gui.add_event"
                                    ),
                                    b -> Minecraft.getInstance().setScreen(
                                            TTNewEventScreen.create(this)
                                    )
                            )
                            .bounds(
                                    this.width / 2 - 105,
                                    actionsY,
                                    100,
                                    20
                            )
                            .build()
            );

            addRenderableWidget(
                    Button.builder(
                                    Component.translatable(
                                            "trackertips.gui.refresh"
                                    ),
                                    b -> rebuild()
                            )
                            .bounds(
                                    this.width / 2 + 5,
                                    actionsY,
                                    100,
                                    20
                            )
                            .build()
            );
        }
    }

    /* ------------------------------------------------------------
     * Commands
     * ------------------------------------------------------------ */

    private void buildCommands() {
        int y = 78;

        addRenderableWidget(
                Button.builder(
                                Component.translatable(
                                        "trackertips.gui.reload_command"
                                ),
                                b -> {
                                    if (Minecraft.getInstance().player != null) {
                                        Minecraft.getInstance()
                                                .player
                                                .connection
                                                .sendCommand(
                                                        "trackertips reload"
                                                );
                                    }
                                }
                        )
                        .bounds(
                                this.width / 2 - 90,
                                y,
                                180,
                                20
                        )
                        .build()
        );
    }

    /* ------------------------------------------------------------
     * Bottom buttons
     * ------------------------------------------------------------ */

    private void buildBottomButtons() {
        int bottom = this.height - 32;

        addRenderableWidget(
                Button.builder(
                                Component.translatable("gui.cancel"),
                                b -> onClose()
                        )
                        .bounds(
                                this.width / 2 - 155,
                                bottom,
                                100,
                                20
                        )
                        .build()
        );

        addRenderableWidget(
                Button.builder(
                                Component.translatable("gui.done"),
                                b -> saveAndClose()
                        )
                        .bounds(
                                this.width / 2 - 50,
                                bottom,
                                100,
                                20
                        )
                        .build()
        );

        addRenderableWidget(
                Button.builder(
                                Component.translatable(
                                        "trackertips.gui.restore_defaults"
                                ),
                                b -> restoreDefaults()
                        )
                        .bounds(
                                this.width / 2 + 55,
                                bottom,
                                100,
                                20
                        )
                        .build()
        );
    }

    /* ------------------------------------------------------------
     * Event operations
     * ------------------------------------------------------------ */

    private void openEvent(Path file) {
        Minecraft.getInstance().setScreen(
                TTClothEventEditor.create(this, file)
        );
    }

    private void confirmDelete(Path file) {
        Minecraft.getInstance().setScreen(
                TTConfirmScreen.create(
                        this,
                        Component.translatable(
                                "trackertips.gui.delete_confirm",
                                file.getFileName()
                        ),
                        () -> {
                            try {
                                Files.deleteIfExists(file);
                            } catch (Exception ignored) {
                            }

                            rebuild();
                        }
                )
        );
    }

    /* ------------------------------------------------------------
     * Defaults / saving
     * ------------------------------------------------------------ */

    private void restoreDefaults() {
        pendingEnable = true;
        pendingMaxHints = 3;
        pendingDebug = false;
        pendingCheckInterval = 20;
        pendingDefaultDuration = 240;

        rebuild();
    }

    private int readInteger(EditBox box, int fallback) {
        if (box == null) {
            return fallback;
        }

        try {
            return Integer.parseInt(box.getValue().trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void saveAndClose() {
        pendingMaxHints = Math.max(
                1,
                readInteger(maxHintsBox, pendingMaxHints)
        );

        pendingCheckInterval = Math.max(
                1,
                readInteger(checkIntervalBox, pendingCheckInterval)
        );

        pendingDefaultDuration = readInteger(
                defaultDurationBox,
                pendingDefaultDuration
        );

        TTClientConfig.ENABLE.set(pendingEnable);
        TTClientConfig.MAX_HINTS.set(pendingMaxHints);
        TTClientConfig.SPEC.save();

        TTSettings settings =
                TTConfigManager.readGlobalSettings();

        settings.debug = pendingDebug;
        settings.checkInterval = pendingCheckInterval;
        settings.maxActiveHints = pendingMaxHints;
        settings.defaultDuration = pendingDefaultDuration;

        TTConfigManager.saveGlobalSettings(settings);

        onClose();
    }

    /* ------------------------------------------------------------
     * Rendering
     * ------------------------------------------------------------ */

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderBackground(graphics);

        graphics.drawCenteredString(
                font,
                title,
                width / 2,
                8,
                0xFFFFFF
        );

        renderTabs(graphics);

        /*
         * Draw labels after background but before widgets.
         */
        for (LabelEntry label : pendingLabels) {
            graphics.drawString(
                    font,
                    label.text(),
                    label.x(),
                    label.y(),
                    0xFFFFFF
            );
        }

        if (currentTab == Tab.COMMANDS) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable(
                            "trackertips.gui.commands.description"
                    ),
                    width / 2,
                    55,
                    0xAAAAAA
            );
        }

        if (currentTab == Tab.USAGE) {
            int y = 65;

            graphics.drawCenteredString(
                    font,
                    Component.translatable(
                            "trackertips.gui.usage.title"
                    ),
                    width / 2,
                    y,
                    0xFFFFFF
            );

            graphics.drawCenteredString(
                    font,
                    Component.translatable(
                            "trackertips.gui.usage.line1"
                    ),
                    width / 2,
                    y + 24,
                    0xAAAAAA
            );

            graphics.drawCenteredString(
                    font,
                    Component.translatable(
                            "trackertips.gui.usage.line2"
                    ),
                    width / 2,
                    y + 42,
                    0xAAAAAA
            );

            graphics.drawCenteredString(
                    font,
                    Component.translatable(
                            "trackertips.gui.usage.line3"
                    ),
                    width / 2,
                    y + 60,
                    0xAAAAAA
            );
        }

        super.render(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        } else {
            super.onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}