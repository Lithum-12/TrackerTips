package io.github.lithum12.trackertips.client.gui;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.github.lithum12.trackertips.TrackerTips;
import io.github.lithum12.trackertips.config.TTConfigManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TTNewEventScreen extends Screen {
    private final Screen parent;
    private EditBox nameBox;
    private EditBox typeBox;

    private TTNewEventScreen(Screen parent) {
        super(Component.translatable("trackertips.gui.add_event"));
        this.parent = parent;
    }

    public static Screen create(Screen parent) { return new TTNewEventScreen(parent); }

    @Override
    protected void init() {
        nameBox = new EditBox(font, width / 2 - 100, 78, 200, 20, Component.translatable("trackertips.gui.event.name"));
        nameBox.setValue("new_event.json");
        addRenderableWidget(nameBox);

        typeBox = new EditBox(font, width / 2 - 100, 120, 200, 20, Component.translatable("trackertips.gui.event.type"));
        typeBox.setValue("trackertips:game_time");
        addRenderableWidget(typeBox);

        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b ->
                        minecraft.setScreen(parent))
                .bounds(width / 2 - 105, height - 45, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("trackertips.gui.create"), b -> createFile())
                .bounds(width / 2 + 5, height - 45, 100, 20).build());
    }

    private void createFile() {
        String name = nameBox.getValue().trim();
        if (name.isEmpty()) return;
        if (!name.endsWith(".json")) name += ".json";
        Path file = TTConfigManager.globalFolder().resolve("hints").resolve(name);
        try {
            Files.createDirectories(file.getParent());
            if (!Files.exists(file)) {
                JsonObject json = new JsonObject();
                json.addProperty("id", TrackerTips.MODID + ":" + name.substring(0, name.length() - 5));
                json.addProperty("once", true);
                json.addProperty("priority", 0);
                json.addProperty("cooldown", 0);
                json.addProperty("duration", 240);
                json.addProperty("require", "any");
                json.addProperty("accent", "F2C14E");
                json.addProperty("theme", "trackertips:default");
                json.addProperty("text", "");
                var triggers = new com.google.gson.JsonArray();
                JsonObject trigger = new JsonObject();
                trigger.addProperty("type", typeBox.getValue().trim());
                triggers.add(trigger);
                json.add("triggers", triggers);
                Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(json), StandardCharsets.UTF_8);
            }
            minecraft.setScreen(TTClothEventEditor.create(parent, file));
        } catch (Exception e) {
            TrackerTips.LOGGER.error("Failed to create TrackerTips event: {}", file, e);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 35, 0xFFFFFF);
        graphics.drawString(font, Component.translatable("trackertips.gui.event.name"), width / 2 - 100, 64, 0xFFFFFF);
        graphics.drawString(font, Component.translatable("trackertips.gui.event.type"), width / 2 - 100, 106, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public boolean isPauseScreen() { return false; }
}
