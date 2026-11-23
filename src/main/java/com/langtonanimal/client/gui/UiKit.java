package com.langtonanimal.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * 极简界面绘制工具，统一配色与命中判定。
 */
public final class UiKit {

    public static final int PANEL_BG = 0xE60E1014;
    public static final int PANEL_BORDER = 0xFF3C4250;
    public static final int ROW_BG = 0x40FFFFFF;
    public static final int ROW_BG_HOVER = 0x663F7CBF;
    public static final int BUTTON_BG = 0xFF242A36;
    public static final int BUTTON_BG_HOVER = 0xFF3A4759;
    public static final int BUTTON_BG_DISABLED = 0xFF1A1D24;
    public static final int BUTTON_BORDER = 0xFF4A5364;
    public static final int TEXT = 0xFFE6E6E6;
    public static final int TEXT_DIM = 0xFF9AA3B2;
    public static final int TEXT_WARN = 0xFFFF6B6B;
    public static final int ACCENT = 0xFF6FB2FF;

    private UiKit() {
    }

    public static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, PANEL_BG);
        graphics.fill(x, y, x + width, y + 1, PANEL_BORDER);
        graphics.fill(x, y + height - 1, x + width, y + height, PANEL_BORDER);
        graphics.fill(x, y, x + 1, y + height, PANEL_BORDER);
        graphics.fill(x + width - 1, y, x + width, y + height, PANEL_BORDER);
    }

    public static void button(GuiGraphics graphics, Font font, int x, int y, int width, int height,
                              Component label, boolean hovered, boolean enabled) {
        int background = !enabled ? BUTTON_BG_DISABLED : (hovered ? BUTTON_BG_HOVER : BUTTON_BG);
        graphics.fill(x, y, x + width, y + height, background);
        graphics.fill(x, y, x + width, y + 1, BUTTON_BORDER);
        graphics.fill(x, y + height - 1, x + width, y + height, BUTTON_BORDER);
        graphics.fill(x, y, x + 1, y + height, BUTTON_BORDER);
        graphics.fill(x + width - 1, y, x + width, y + height, BUTTON_BORDER);
        int color = enabled ? TEXT : 0xFF6A7280;
        graphics.drawString(font, label, x + (width - font.width(label)) / 2,
                y + (height - 8) / 2 + 1, color, false);
    }

    public static void buttonIcon(GuiGraphics graphics, Font font, int x, int y, int width, int height,
                                  Block block, Component label, boolean hovered, boolean enabled) {
        int background = !enabled ? BUTTON_BG_DISABLED : (hovered ? BUTTON_BG_HOVER : BUTTON_BG);
        graphics.fill(x, y, x + width, y + height, background);
        graphics.fill(x, y, x + width, y + 1, BUTTON_BORDER);
        graphics.fill(x, y + height - 1, x + width, y + height, BUTTON_BORDER);
        graphics.fill(x, y, x + 1, y + height, BUTTON_BORDER);
        graphics.fill(x + width - 1, y, x + width, y + height, BUTTON_BORDER);
        if (block != null) {
            graphics.renderItem(new ItemStack(block), x + 2, y + (height - 16) / 2);
        }
        int textX = block != null ? x + 21 : x + 4;
        int available = width - (block != null ? 22 : 6);
        Component shown = label;
        if (font.width(shown) > available) {
            String plain = shown.getString();
            while (font.width(plain) > available && plain.length() > 1) {
                plain = plain.substring(0, plain.length() - 1);
            }
            shown = Component.literal(plain + "…");
        }
        int color = enabled ? TEXT : 0xFF6A7280;
        graphics.drawString(font, shown, textX, y + (height - 8) / 2 + 1, color, false);
    }

    public static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public static void tooltip(GuiGraphics graphics, Font font, Component text, int mouseX, int mouseY) {
        graphics.renderComponentTooltip(font, java.util.List.of(text), mouseX, mouseY);
    }

    /** 按可用宽度裁剪文本后绘制，避免超出面板。 */
    public static void drawClamped(GuiGraphics graphics, Font font, Component text, int x, int y,
                                   int color, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            graphics.drawString(font, text, x, y, color, false);
            return;
        }
        String plain = font.plainSubstrByWidth(text.getString(), Math.max(0, maxWidth - 6));
        graphics.drawString(font, plain + "…", x, y, color, false);
    }
}
