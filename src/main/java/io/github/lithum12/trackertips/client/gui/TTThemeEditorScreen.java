package io.github.lithum12.trackertips.client.gui;

import com.google.gson.JsonParseException;
import io.github.lithum12.trackertips.theme.TTAnimation;
import io.github.lithum12.trackertips.theme.TTTheme;
import io.github.lithum12.trackertips.theme.TTThemeManager;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.concurrent.atomic.AtomicReference;

/** Cloth Config editor for a single JSON theme. */
public final class TTThemeEditorScreen {
    private TTThemeEditorScreen() {}

    public static Screen create(Screen parent, TTTheme original) {
        TTTheme base = original == null
                ? TTTheme.defaults("trackertips:theme_" + Long.toHexString(System.currentTimeMillis()))
                : original;

        AtomicReference<TTTheme> working = new AtomicReference<>(copy(base));

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("trackertips.gui.theme.editor", base.name()));

        ConfigEntryBuilder entry = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(
                Component.translatable("trackertips.gui.theme.general"));
        general.addEntry(entry.startStrField(
                Component.translatable("trackertips.gui.theme.id"), base.id())
                .setDefaultValue(base.id())
                .setSaveConsumer(v -> {
                    // IDs are immutable for an existing theme. For new themes this is
                    // accepted and used when the file is saved.
                    working.get().setName(working.get().name());
                }).build());

        general.addEntry(entry.startStrField(
                Component.translatable("trackertips.gui.theme.name"), base.name())
                .setDefaultValue(base.name())
                .setSaveConsumer(v -> working.get().setName(v)).build());

        ConfigCategory card = builder.getOrCreateCategory(
                Component.translatable("trackertips.gui.theme.card"));
        addColor(entry, card, "trackertips.gui.theme.background", base.background(),
                v -> working.get().setBackground(parseColor(v, base.background())));
        addColor(entry, card, "trackertips.gui.theme.border", base.border(),
                v -> working.get().setBorder(parseColor(v, base.border())));
        addColor(entry, card, "trackertips.gui.theme.title_color", base.titleColor(),
                v -> working.get().setTitleColor(parseColor(v, base.titleColor())));
        addColor(entry, card, "trackertips.gui.theme.text_color", base.textColor(),
                v -> working.get().setTextColor(parseColor(v, base.textColor())));

        card.addEntry(entry.startIntField(
                Component.translatable("trackertips.gui.theme.border_width"), base.borderWidth())
                .setDefaultValue(1)
                .setMin(0).setMax(8)
                .setSaveConsumer(working.get()::setBorderWidth).build());

        card.addEntry(entry.startIntField(
                Component.translatable("trackertips.gui.theme.corner_radius"), base.cornerRadius())
                .setDefaultValue(4)
                .setMin(0).setMax(16)
                .setSaveConsumer(working.get()::setCornerRadius).build());

        card.addEntry(entry.startIntField(
                Component.translatable("trackertips.gui.theme.padding"), base.padding())
                .setDefaultValue(8)
                .setMin(0).setMax(32)
                .setSaveConsumer(working.get()::setPadding).build());

        ConfigCategory animation = builder.getOrCreateCategory(
                Component.translatable("trackertips.gui.theme.animation"));

        addAnimation(entry, animation, "trackertips.gui.theme.card_animation",
                base.cardAnimation(), true, working);
        addAnimation(entry, animation, "trackertips.gui.theme.text_animation",
                base.textAnimation(), false, working);

        builder.setSavingRunnable(() -> {
            TTTheme value = working.get();
            TTThemeManager.save(value);
        });

        return builder.build();
    }

    private static void addColor(ConfigEntryBuilder entry, ConfigCategory category, String key,
                                 int value, java.util.function.Consumer<String> consumer) {
        String hex = String.format("%06X", value & 0xFFFFFF);
        category.addEntry(entry.startStrField(Component.translatable(key), hex)
                .setDefaultValue(hex)
                .setSaveConsumer(consumer)
                .build());
    }

    private static void addAnimation(ConfigEntryBuilder entry, ConfigCategory category, String key,
                                     TTAnimation animation, boolean card, AtomicReference<TTTheme> working) {
        category.addEntry(entry.startStrField(
                Component.translatable(key + ".type"), animation.type())
                .setDefaultValue(animation.type())
                .setSaveConsumer(value -> {
                    TTAnimation old = card ? working.get().cardAnimation() : working.get().textAnimation();
                    TTAnimation next = new TTAnimation(value, old.duration(), old.delay());
                    if (card) working.get().setCardAnimation(next);
                    else working.get().setTextAnimation(next);
                }).build());

        category.addEntry(entry.startIntField(
                Component.translatable(key + ".duration"), animation.duration())
                .setDefaultValue(animation.duration())
                .setMin(0).setMax(200)
                .setSaveConsumer(value -> {
                    TTAnimation old = card ? working.get().cardAnimation() : working.get().textAnimation();
                    TTAnimation next = new TTAnimation(old.type(), value, old.delay());
                    if (card) working.get().setCardAnimation(next);
                    else working.get().setTextAnimation(next);
                }).build());

        category.addEntry(entry.startIntField(
                Component.translatable(key + ".delay"), animation.delay())
                .setDefaultValue(animation.delay())
                .setMin(0).setMax(200)
                .setSaveConsumer(value -> {
                    TTAnimation old = card ? working.get().cardAnimation() : working.get().textAnimation();
                    TTAnimation next = new TTAnimation(old.type(), old.duration(), value);
                    if (card) working.get().setCardAnimation(next);
                    else working.get().setTextAnimation(next);
                }).build());
    }

    private static int parseColor(String value, int fallback) {
        try {
            String clean = value.trim();
            if (clean.startsWith("#")) clean = clean.substring(1);
            if (clean.length() != 6) throw new NumberFormatException();
            return Integer.parseInt(clean, 16) & 0xFFFFFF;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static TTTheme copy(TTTheme source) {
        return TTTheme.fromJson(source.toJson());
    }
}
