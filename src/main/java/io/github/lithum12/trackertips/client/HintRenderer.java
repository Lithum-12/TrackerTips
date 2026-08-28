package io.github.lithum12.trackertips.client;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.lithum12.trackertips.config.TTClientConfig;
import io.github.lithum12.trackertips.theme.TTAnimation;
import io.github.lithum12.trackertips.theme.TTTheme;
import io.github.lithum12.trackertips.theme.TTThemeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class HintRenderer {
    private static final int DEFAULT_PADDING = 8;

    public static void render(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !TTClientConfig.ENABLE.get()) return;
        if (!ClientHintManager.isVisible() || ClientHintManager.ACTIVE.isEmpty()) return;

        Font font = minecraft.font;
        int lineHeight = font.lineHeight + 2;
        int x = TTClientConfig.OFFSET_X.get();
        int bottom = screenHeight - TTClientConfig.OFFSET_Y.get();
        int configuredMaxWidth = TTClientConfig.MAX_WIDTH.get();
        int safeMaxWidth = Math.min(configuredMaxWidth, (screenWidth / 2) - 100);
        int maxWidth = Math.max(120, safeMaxWidth);
        int maxHints = TTClientConfig.MAX_HINTS.get();
        int drawn = 0;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        for (ClientHintManager.ActiveHint hint : ClientHintManager.ACTIVE) {
            if (drawn >= maxHints) break;

            TTTheme theme = TTThemeManager.get(hint.theme());
            int padding = theme.padding() > 0 ? theme.padding() : DEFAULT_PADDING;

            int iconOffset = hint.icon().isEmpty() ? 0 : 20;
            int availableWidth = maxWidth - padding * 2 - iconOffset;
            List<FormattedCharSequence> titleLines =
                    hint.title() != null ? font.split(hint.title(), availableWidth) : List.of();
            List<FormattedCharSequence> textLines = font.split(hint.text(), availableWidth);
            int textHeight = titleLines.size() * lineHeight
                    + (titleLines.isEmpty() ? 0 : 4)
                    + textLines.size() * lineHeight;
            int contentHeight = Math.max(textHeight, hint.icon().isEmpty() ? 0 : 16);
            int height = contentHeight + padding * 2;

            float alphaProgress = hint.alpha(TTClientConfig.FADE_IN.get(), TTClientConfig.FADE_OUT.get());
            float animationProgress = animationProgress(alphaProgress, theme.cardAnimation());
            int animationOffset = animationOffset(alphaProgress, theme.cardAnimation());

            int y = bottom - height + animationOffset;
            int a = Math.max(24, (int) (animationProgress * 255));

            drawPanel(guiGraphics, x, y, maxWidth, height, a, theme);

            int contentX = x + padding;
            int contentY = y + padding;

            if (!hint.icon().isEmpty()) {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, animationProgress);
                guiGraphics.renderItem(hint.icon(), contentX, contentY);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }

            int textX = contentX + iconOffset;
            int currentY = contentY;
            int titleColor = withAlpha(a, theme.titleColor());
            int textColor = withAlpha(a, theme.textColor());
            float textProgress = animationProgress(alphaProgress, theme.textAnimation());

            // The text animation is deliberately kept as a rendering hook. Fade is implemented now;
            // other animation types can be added without changing event JSON or the theme format.
            int textAlpha = Math.max(0, Math.min(255, (int) (textProgress * 255)));
            titleColor = withAlpha(textAlpha, theme.titleColor());
            textColor = withAlpha(textAlpha, theme.textColor());

            for (FormattedCharSequence line : titleLines) {
                guiGraphics.drawString(font, line, textX, currentY, titleColor, false);
                currentY += lineHeight;
            }
            if (!titleLines.isEmpty()) currentY += 4;

            for (FormattedCharSequence line : textLines) {
                guiGraphics.drawString(font, line, textX, currentY, textColor, false);
                currentY += lineHeight;
            }

            bottom = y - 4;
            drawn++;
        }

        RenderSystem.disableBlend();
    }

    private static float animationProgress(float alpha, TTAnimation animation) {
        String type = animation.normalizedType();
        if ("none".equals(type)) return 1.0F;
        // fade, slide_up and scale currently share the fade curve; this keeps the data format
        // forward-compatible while avoiding a hard-coded renderer contract.
        return Mth.clamp(alpha, 0.0F, 1.0F);
    }

    private static int animationOffset(float alpha, TTAnimation animation) {
        return switch (animation.normalizedType()) {
            case "slide", "slide_up" -> Math.round((1.0F - alpha) * 12.0F);
            default -> 0;
        };
    }

    private static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height,
                                  int alpha, TTTheme theme) {
        int x2 = x + width;
        int y2 = y + height;
        int backgroundAlpha = Mth.clamp((int) (alpha * 0.92F), 0, 255);
        int background = withAlpha(backgroundAlpha, theme.background());

        // Keep the renderer compatible with vanilla GuiGraphics primitives.
        // corner_radius is retained in the theme schema for a future rounded-panel renderer.
        graphics.fill(x, y, x2, y2, background);

        int border = withAlpha(alpha, theme.border());
        int borderWidth = theme.borderWidth();
        for (int i = 0; i < borderWidth; i++) {
            graphics.fill(x + i, y + i, x2 - i, y + i + 1, border);
            graphics.fill(x + i, y2 - i - 1, x2 - i, y2 - i, border);
            graphics.fill(x + i, y + i, x + i + 1, y2 - i, border);
            graphics.fill(x2 - i - 1, y + i, x2 - i, y2 - i, border);
        }

        // Accent remains useful as a per-event visual cue, but no longer controls the panel palette.
        graphics.fill(x + borderWidth + 1, y + borderWidth + 1,
                x + borderWidth + 3, y2 - borderWidth - 1,
                withAlpha(alpha, theme.border()));
    }

    private static int withAlpha(int alpha, int rgb) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (rgb & 0xFFFFFF);
    }
}
