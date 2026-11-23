package com.langtonanimal.client;

import com.langtonanimal.data.AntEntityData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * 手持烈焰棒时右下角的状态 HUD：模式与范围各列出全部选项，高亮当前项。
 */
public final class RodHud {

    private static final int BG = 0xC0101418;
    private static final int BORDER = 0xFF3C4250;
    private static final int TEXT = 0xFFE6E6E6;
    private static final int DIM = 0xFF9AA3B2;
    private static final int ACCENT = 0xFF6FB2FF;

    private static final int HEADER_Y = 5;
    private static final int ROW_START = 16;
    private static final int ROW_H = 10;
    private static final int LEFT_X = 9;
    private static final int RIGHT_X = 96;
    private static final int DIVIDER_Y = 56;
    private static final int TARGET_Y = 60;

    private RodHud() {
    }

    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || minecraft.screen != null) {
            return;
        }
        if (!ClientEvents.holdingRodMain(minecraft.player)) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int mode = RodState.mode();
        int scope = RodState.scope();
        AntEntityData data = RodState.selectedData();

        Component targetLine;
        if (data == null) {
            targetLine = Component.translatable("langtonanimal.hud.no_selection")
                    .withStyle(Style.EMPTY.withColor(DIM));
        } else {
            int color = ClientState.colorOf(data.presetName());
            Component name = Component.literal(data.entityName()).withStyle(Style.EMPTY.withColor(TEXT));
            Component preset = Component.literal(data.presetName()).withStyle(Style.EMPTY.withColor(color));
            Component speed = Component.literal("  " + data.speed() + "/s").withStyle(Style.EMPTY.withColor(DIM));
            targetLine = Component.empty().append(name).append(" · ").append(preset).append(speed);
        }
        int targetWidth = font.width(targetLine);

        int headerWidth = Math.max(font.width(Component.translatable("langtonanimal.hud.modes")),
                font.width(Component.translatable("langtonanimal.hud.scopes")));
        int width = Math.max(190, Math.max(targetWidth + LEFT_X + 9, RIGHT_X + headerWidth + 30));
        int height = TARGET_Y + 12;
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int x = screenWidth - width - 6;
        int y = screenHeight - height - 6;

        graphics.fill(x, y, x + width, y + height, BG);
        graphics.fill(x, y, x + width, y + 1, BORDER);
        graphics.fill(x, y + height - 1, x + width, y + height, BORDER);
        graphics.fill(x, y, x + 1, y + height, BORDER);
        graphics.fill(x + width - 1, y, x + width, y + height, BORDER);

        graphics.drawString(font, Component.translatable("langtonanimal.hud.modes"), x + LEFT_X, y + HEADER_Y,
                DIM, true);
        graphics.drawString(font, Component.translatable("langtonanimal.hud.scopes"), x + RIGHT_X, y + HEADER_Y,
                DIM, true);

        for (int i = 0; i < RodState.MODE_COUNT; i++) {
            boolean active = i == mode;
            graphics.drawString(font, active ? "> " : "  ", x + LEFT_X, y + ROW_START + i * ROW_H,
                    active ? ACCENT : DIM, true);
            graphics.drawString(font, Component.translatable("langtonanimal.mode." + i),
                    x + LEFT_X + 9, y + ROW_START + i * ROW_H, active ? ACCENT : DIM, true);
        }
        for (int i = 0; i < RodState.SCOPE_COUNT; i++) {
            boolean active = i == scope;
            graphics.drawString(font, active ? "> " : "  ", x + RIGHT_X, y + ROW_START + i * ROW_H,
                    active ? ACCENT : DIM, true);
            graphics.drawString(font, Component.translatable("langtonanimal.scope." + i),
                    x + RIGHT_X + 9, y + ROW_START + i * ROW_H, active ? ACCENT : DIM, true);
        }

        graphics.fill(x + LEFT_X, y + DIVIDER_Y, x + width - LEFT_X, y + DIVIDER_Y + 1, BORDER);
        graphics.drawString(font, targetLine, x + LEFT_X, y + TARGET_Y, TEXT, true);
    }
}
