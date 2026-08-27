package io.github.lithum12.trackertips.client.gui;

import com.google.gson.GsonBuilder;
import io.github.lithum12.trackertips.TrackerTips;
import io.github.lithum12.trackertips.config.TTConfigManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Small event-definition creator. Advanced JSON editing is intentionally delegated to the system editor. */
public class TTEventEditorScreen extends Screen {
    private final Screen parent;
    private final Path editingFile;
    private EditBox nameBox;
    private CycleButton<String> typeButton;

    private static final List<String> TYPES = List.of(
            "game_time", "potion_effect", "has_item", "advancement", "item_obtained",
            "in_dimension", "health_below", "kill_entity", "mine_block"
    );

    public TTEventEditorScreen(Screen parent, Path editingFile) {
        super(Component.translatable("trackertips.gui.event_editor.title"));
        this.parent = parent;
        this.editingFile = editingFile;
    }

    @Override
    protected void init() {
        int boxW = 280;
        int x = width / 2 - boxW / 2;
        nameBox = new EditBox(font, x, 116, boxW, 24, Component.translatable("trackertips.gui.event_editor.name"));
        nameBox.setMaxLength(64);
        nameBox.setFilter(s -> s.matches("[A-Za-z0-9_\\-]*"));
        if (editingFile != null) {
            String file = editingFile.getFileName().toString();
            nameBox.setValue(file.endsWith(".json") ? file.substring(0, file.length() - 5) : file);
        }
        addRenderableWidget(nameBox);

        typeButton = CycleButton.<String>builder(value -> Component.literal(value))
                .withValues(TYPES)
                .withInitialValue(TYPES.get(0))
                .create(x, 172, boxW, 24, Component.translatable("trackertips.gui.event_editor.type"), (b, value) -> {});
        addRenderableWidget(typeButton);

        addRenderableWidget(new ActionButton(x + 88, 226, 92, 24,
                Component.translatable("gui.cancel"), b -> minecraft.setScreen(parent)));
        addRenderableWidget(new ActionButton(x + 188, 226, 92, 24,
                Component.translatable("trackertips.gui.event_editor.create"), b -> create()));
    }

    private void create() {
        String name = nameBox.getValue().trim();
        if (name.isEmpty()) return;
        Path folder = TTConfigManager.globalFolder().resolve("hints");
        try {
            Files.createDirectories(folder);
            Path target = folder.resolve(name + ".json");
            if (editingFile == null && Files.exists(target)) {
                return;
            }
            String type = typeButton.getValue();
            String json = """
                    {
                      "id": "trackertips:%s",
                      "once": true,
                      "priority": 0,
                      "cooldown": 0,
                      "duration": 240,
                      "require": "all",
                      "accent": "F2C14E",
                      "sound": "",
                      "title": {
                        "text": "%s",
                        "color": "gold",
                        "bold": true
                      },
                      "text": [
                        {
                          "text": "Edit this hint in your JSON editor.",
                          "color": "gray"
                        }
                      ],
                      "triggers": [
                        { "type": "trackertips:%s" }
                      ]
                    }
                    """.formatted(name, name, type);
            Files.writeString(target, json, StandardCharsets.UTF_8);
            minecraft.setScreen(parent);
        } catch (Exception e) {
            TrackerTips.LOGGER.error("[TrackerTips] Failed to create event", e);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.fill(0, 0, width, height, 0xA0101115);
        int w = 360;
        int x = width / 2 - w / 2;
        g.fill(x, 62, x + w, 270, 0xF01B1D22);
        g.fill(x, 62, x + 3, 270, 0xFFE0A83A);
        g.drawCenteredString(font, title, width / 2, 78, 0xFFFFFF);
        g.drawString(font, Component.translatable("trackertips.gui.event_editor.name"), x + 40, 99, 0xA9ADB5, false);
        g.drawString(font, Component.translatable("trackertips.gui.event_editor.type"), x + 40, 155, 0xA9ADB5, false);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private static class ActionButton extends Button {
        ActionButton(int x, int y, int w, int h, Component text, OnPress press) {
            super(x, y, w, h, text, press, DEFAULT_NARRATION);
        }
        @Override protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            int bg = isHoveredOrFocused() ? 0xFF383B42 : 0xFF2A2C31;
            g.fill(getX(), getY(), getX() + width, getY() + height, bg);
            g.fill(getX(), getY(), getX() + width, getY() + 1, 0xFFE0A83A);
            g.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font, getMessage(),
                    getX() + width / 2, getY() + 8, 0xFFE6E7E9);
        }
    }
}
