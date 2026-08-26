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
        for (ClientHintManager.ActiveHint hint : ClientHintManager.ACTIVE) {
            if (drawn >= maxHints) break;

            // 1. 计算布局尺寸
            int iconOffset = hint.icon().isEmpty() ? 0 : 20; // 16px 图标 + 4px 间距
            int availableWidth = maxWidth - 12 - iconOffset;

            List<FormattedCharSequence> titleLines = hint.title() != null ? font.split(hint.title(), availableWidth) : List.of();
            List<FormattedCharSequence> textLines = font.split(hint.text(), availableWidth);

            int textHeight = titleLines.size() * lineHeight + (titleLines.isEmpty() ? 0 : 4) + textLines.size() * lineHeight;
            int contentHeight = Math.max(textHeight, hint.icon().isEmpty() ? 0 : 16);
            int height = contentHeight + PADDING * 2;

            int y = bottom - height;
            float alphaProgress = hint.alpha(TTClientConfig.FADE_IN.get(), TTClientConfig.FADE_OUT.get());
            int a = (int) (alphaProgress * 255);

            // 2. 绘制背景面板
            drawPanel(guiGraphics, x, y, maxWidth, height, a, hint.accent());

            int contentX = x + 8;
            int contentY = y + PADDING;

            // 3. 绘制物品图标 (带透明度淡入淡出)
            if (!hint.icon().isEmpty()) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alphaProgress);
                guiGraphics.renderItem(hint.icon(), contentX, contentY);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F); // 恢复颜色，防止污染后续渲染
                RenderSystem.disableBlend();
            }

            int textX = contentX + iconOffset;
            int currentY = contentY;
            int textColor = withAlpha(a, 0xF5F7FA);

            // 4. 绘制标题
            for (FormattedCharSequence line : titleLines) {
                guiGraphics.drawString(font, line, textX, currentY, 0xFFFFFF, false);
                currentY += lineHeight;
            }
            if (!titleLines.isEmpty()) currentY += 4; // 标题与正文的间距

            // 5. 绘制正文
            for (FormattedCharSequence line : textLines) {
                guiGraphics.drawString(font, line, textX, currentY, textColor, false);
                currentY += lineHeight;
            }

            bottom = y - 4;
            drawn++;
        }
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