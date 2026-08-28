package io.github.lithum12.trackertips.theme;

import com.google.gson.JsonObject;
import net.minecraft.util.Mth;

/** A serializable TrackerTips notification theme. */
public final class TTTheme {
    private final String id;
    private String name;
    private int background;
    private int border;
    private int titleColor;
    private int textColor;
    private int borderWidth;
    private int cornerRadius;
    private int padding;
    private TTAnimation cardAnimation;
    private TTAnimation textAnimation;

    public TTTheme(String id, String name, int background, int border, int titleColor, int textColor,
                   int borderWidth, int cornerRadius, int padding,
                   TTAnimation cardAnimation, TTAnimation textAnimation) {
        this.id = id;
        this.name = name;
        this.background = background;
        this.border = border;
        this.titleColor = titleColor;
        this.textColor = textColor;
        this.borderWidth = borderWidth;
        this.cornerRadius = cornerRadius;
        this.padding = padding;
        this.cardAnimation = cardAnimation;
        this.textAnimation = textAnimation;
    }

    public static TTTheme defaults(String id) {
        return new TTTheme(id, "Default", 0x14171C, 0xF2C14E, 0xF5F7FA, 0xDADCE0,
                1, 4, 8, TTAnimation.defaults("fade"), TTAnimation.defaults("fade"));
    }

    public static TTTheme fromJson(JsonObject json) {
        String id = json.has("id") ? json.get("id").getAsString() : "trackertips:custom";
        TTTheme theme = defaults(id);
        theme.name = json.has("name") ? json.get("name").getAsString() : id;

        JsonObject card = json.has("card") && json.get("card").isJsonObject()
                ? json.getAsJsonObject("card") : new JsonObject();
        theme.background = parseColor(card, "background", theme.background);
        theme.border = parseColor(card, "border", theme.border);
        theme.borderWidth = getInt(card, "border_width", theme.borderWidth);
        theme.cornerRadius = getInt(card, "corner_radius", theme.cornerRadius);
        theme.padding = getInt(card, "padding", theme.padding);

        JsonObject title = json.has("title") && json.get("title").isJsonObject()
                ? json.getAsJsonObject("title") : new JsonObject();
        JsonObject text = json.has("text") && json.get("text").isJsonObject()
                ? json.getAsJsonObject("text") : new JsonObject();
        theme.titleColor = parseColor(title, "color", theme.titleColor);
        theme.textColor = parseColor(text, "color", theme.textColor);

        JsonObject animation = json.has("animation") && json.get("animation").isJsonObject()
                ? json.getAsJsonObject("animation") : new JsonObject();
        theme.cardAnimation = parseAnimation(animation, "card", theme.cardAnimation);
        theme.textAnimation = parseAnimation(animation, "text", theme.textAnimation);
        return theme;
    }

    private static TTAnimation parseAnimation(JsonObject parent, String key, TTAnimation fallback) {
        if (!parent.has(key) || !parent.get(key).isJsonObject()) return fallback;
        JsonObject json = parent.getAsJsonObject(key);
        return new TTAnimation(
                json.has("type") ? json.get("type").getAsString() : fallback.type(),
                getInt(json, "duration", fallback.duration()),
                getInt(json, "delay", fallback.delay())
        );
    }

    private static int getInt(JsonObject json, String key, int fallback) {
        return json.has(key) ? json.get(key).getAsInt() : fallback;
    }

    private static int parseColor(JsonObject json, String key, int fallback) {
        if (!json.has(key)) return fallback;
        try {
            String value = json.get(key).getAsString().trim();
            if (value.startsWith("#")) value = value.substring(1);
            return (int) Long.parseLong(value, 16) & 0xFFFFFF;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("id", id);
        root.addProperty("name", name);

        JsonObject card = new JsonObject();
        card.addProperty("background", hex(background));
        card.addProperty("border", hex(border));
        card.addProperty("border_width", borderWidth);
        card.addProperty("corner_radius", cornerRadius);
        card.addProperty("padding", padding);
        root.add("card", card);

        JsonObject title = new JsonObject();
        title.addProperty("color", hex(titleColor));
        root.add("title", title);

        JsonObject text = new JsonObject();
        text.addProperty("color", hex(textColor));
        root.add("text", text);

        JsonObject animation = new JsonObject();
        animation.add("card", animationJson(cardAnimation));
        animation.add("text", animationJson(textAnimation));
        root.add("animation", animation);
        return root;
    }

    private static JsonObject animationJson(TTAnimation animation) {
        JsonObject json = new JsonObject();
        json.addProperty("type", animation.type());
        json.addProperty("duration", animation.duration());
        json.addProperty("delay", animation.delay());
        return json;
    }

    private static String hex(int color) {
        return String.format("%06X", color & 0xFFFFFF);
    }

    public String id() { return id; }
    public String name() { return name; }
    public void setName(String name) { this.name = name; }
    public int background() { return background; }
    public void setBackground(int value) { background = value & 0xFFFFFF; }
    public int border() { return border; }
    public void setBorder(int value) { border = value & 0xFFFFFF; }
    public int titleColor() { return titleColor; }
    public void setTitleColor(int value) { titleColor = value & 0xFFFFFF; }
    public int textColor() { return textColor; }
    public void setTextColor(int value) { textColor = value & 0xFFFFFF; }
    public int borderWidth() { return Mth.clamp(borderWidth, 0, 8); }
    public void setBorderWidth(int value) { borderWidth = Mth.clamp(value, 0, 8); }
    public int cornerRadius() { return Mth.clamp(cornerRadius, 0, 16); }
    public void setCornerRadius(int value) { cornerRadius = Mth.clamp(value, 0, 16); }
    public int padding() { return Mth.clamp(padding, 0, 32); }
    public void setPadding(int value) { padding = Mth.clamp(value, 0, 32); }
    public TTAnimation cardAnimation() { return cardAnimation; }
    public void setCardAnimation(TTAnimation value) { cardAnimation = value; }
    public TTAnimation textAnimation() { return textAnimation; }
    public void setTextAnimation(TTAnimation value) { textAnimation = value; }
}
