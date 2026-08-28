package io.github.lithum12.trackertips.theme;

/** Animation settings are intentionally data-driven so more renderers can be added later. */
public record TTAnimation(String type, int duration, int delay) {
    public static TTAnimation defaults(String type) {
        return new TTAnimation(type, 8, 0);
    }

    public String normalizedType() {
        return type == null || type.isBlank() ? "none" : type.toLowerCase(java.util.Locale.ROOT);
    }
}
