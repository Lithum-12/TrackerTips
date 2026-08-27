package io.github.lithum12.trackertips.client;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.lithum12.trackertips.config.TTClientConfig;
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
    private static final int PADDING = 8;

    public static void render(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !TTClientConfig.ENABLE.get()) return;
        if (!ClientHintManager.isVisible()) return;
        if (ClientHintManager.ACTIVE.isEmpty()) return;

        Font font = minecraft.font;
        int lineHeight = font.lineHeight + 2;
        int x = TTClientConfig.OFFSET_X.get();
        int bottom = screenHeight - TTClientConfig.OFFSET_Y.get();
        int configuredMaxWidth = TTClientConfig.MAX_WIDTH.get();
        int safeMaxWidth = Math.min(configuredMaxWidth, (screenWidth / 2) - 100);
        int maxWidth = Math.max(120, safeMaxWidth);
        int maxHints = TTClientConfig.MAX_HINTS.get();
        int drawn = 0;

        // 🚨【致命修复】必须在整个渲染循环的最外层开启 Blend！
        // 否则 GuiGraphics.fill 里的 Alpha 通道会失效，导致背景画不出来或者颜色错乱。
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        for (ClientHintManager.ActiveHint hint : ClientHintManager.ACTIVE) {
            if (drawn >= maxHints) break;

            int iconOffset = hint.icon().isEmpty() ? 0 : 20;
            int availableWidth = maxWidth - 12 - iconOffset;
            List<FormattedCharSequence> titleLines = hint.title() != null ? font.split(hint.title(), availableWidth) : List.of();
            List<FormattedCharSequence> textLines = font.split(hint.text(), availableWidth);
            int textHeight = titleLines.size() * lineHeight + (titleLines.isEmpty() ? 0 : 4) + textLines.size() * lineHeight;
            int contentHeight = Math.max(textHeight, hint.icon().isEmpty() ? 0 : 16);
            int height = contentHeight + PADDING * 2;
            int y = bottom - height;

            float alphaProgress = hint.alpha(TTClientConfig.FADE_IN.get(), TTClientConfig.FADE_OUT.get());
            int a = (int) (alphaProgress * 255);

            // 2. 绘制背景面板（现在 Blend 已经开启，透明度正常生效！）
            drawPanel(guiGraphics, x, y, maxWidth, height, a, hint.accent());

            int contentX = x + 8;
            int contentY = y + PADDING;

            // 3. 绘制物品图标
            if (!hint.icon().isEmpty()) {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alphaProgress);
                guiGraphics.renderItem(hint.icon(), contentX, contentY);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }

            int textX = contentX + iconOffset;
            int currentY = contentY;
            int textColor = withAlpha(a, 0xF5F7FA);

            for (FormattedCharSequence line : titleLines) {
                guiGraphics.drawString(font, line, textX, currentY, 0xFFFFFF, false);
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

        // 渲染结束，关闭 Blend 防止污染原版 UI
        RenderSystem.disableBlend();
    }

    private static void drawPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, int a, int accent) {
        int x2 = x + width;
        int y2 = y + height;
        int background = withAlpha((int) (a * 0.82F), 0x14171C);
        guiGraphics.fill(x + 1, y + 1, x2 - 1, y2 - 1, background);
        guiGraphics.fillGradient(x + 1, y + 1, x2 - 1, y2 - 1,
                withAlpha(Math.min(255, (int) (a * 0.82F) + 18), 0x232830), background);
        guiGraphics.fill(x, y, x2, y + 1, withAlpha(a, 0xFFFFFF));
        guiGraphics.fill(x, y2 - 1, x2, y2, withAlpha(Math.max(0, a - 36), 0xFFFFFF));
        guiGraphics.fill(x, y, x + 1, y2, withAlpha(a, 0xFFFFFF));
        guiGraphics.fill(x2 - 1, y, x2, y2, withAlpha(Math.max(0, a - 36), 0xFFFFFF));
        guiGraphics.fill(x + 2, y + 2, x + 4, y2 - 2, withAlpha(a, accent));
    }

    private static int withAlpha(int alpha, int rgb) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (rgb & 0xFFFFFF);
    }
}