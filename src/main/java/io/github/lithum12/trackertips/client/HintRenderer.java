package io.github.lithum12.trackertips.client;

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
        if (minecraft.player == null || !TTClientConfig.ENABLE.get()) {
            return;
        }
        if (ClientHintManager.ACTIVE.isEmpty()) {
            return;
        }

        Font font = minecraft.font;
        int lineHeight = font.lineHeight + 4;
        int x = TTClientConfig.OFFSET_X.get();
        int bottom = screenHeight - TTClientConfig.OFFSET_Y.get();
        int maxWidth = TTClientConfig.MAX_WIDTH.get();
        int maxHints = TTClientConfig.MAX_HINTS.get();

        int drawn = 0;
        for (ClientHintManager.ActiveHint hint : ClientHintManager.ACTIVE) {
            if (drawn >= maxHints) {
                break;
            }

            List<FormattedCharSequence> lines = font.split(hint.text(), maxWidth - PADDING * 2 - 6);
            int height = lines.size() * lineHeight + PADDING * 2;
            int y = bottom - height;

            int a = (int) (hint.alpha(TTClientConfig.FADE_IN.get(), TTClientConfig.FADE_OUT.get()) * 255);

            drawPanel(guiGraphics, x, y, maxWidth, height, a, hint.accent());

            int textY = y + PADDING;
            int textColor = withAlpha(a, 0xF5F7FA);
            for (FormattedCharSequence line : lines) {
                // 使用 GuiGraphics 的 drawString
                guiGraphics.drawString(font, line, x + PADDING + 4, textY, textColor, false);
                textY += lineHeight;
            }

            bottom = y - 4;
            drawn++;
        }
    }

    private static void drawPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, int a, int accent) {
        int x2 = x + width;
        int y2 = y + height;

        int background = withAlpha((int) (a * 0.82F), 0x14171C);

        // 使用 GuiGraphics 的 fill 和 fillGradient
        guiGraphics.fill(x + 1, y + 1, x2 - 1, y2 - 1, background);
        guiGraphics.fillGradient(x + 1, y + 1, x2 - 1, y2 - 1,
                withAlpha(Math.min(255, (int) (a * 0.82F) + 18), 0x232830),
                background);

        guiGraphics.fill(x, y, x2, y + 1, withAlpha(a, 0xFFFFFF));
        guiGraphics.fill(x, y2 - 1, x2, y2, withAlpha(Math.max(0, a - 36), 0xFFFFFF));
        guiGraphics.fill(x, y, x + 1, y2, withAlpha(a, 0xFFFFFF));
        guiGraphics.fill(x2 - 1, y, x2, y2, withAlpha(Math.max(0, a - 36), 0xFFFFFF));

        // 左侧强调条
        guiGraphics.fill(x + 2, y + 2, x + 4, y2 - 2, withAlpha(a, accent));
    }

    private static int withAlpha(int alpha, int rgb) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (rgb & 0xFFFFFF);
    }
}