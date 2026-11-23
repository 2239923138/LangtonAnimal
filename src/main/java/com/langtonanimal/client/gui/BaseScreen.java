package com.langtonanimal.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 统一的界面基类：先铺背景，再画自绘内容，最后把原版控件（输入框、按钮）盖上去。
 */
public abstract class BaseScreen extends Screen {

    protected int panelLeft;
    protected int panelTop;
    protected int panelWidth;
    protected int panelHeight;

    protected BaseScreen(Component title) {
        super(title);
    }

    protected void layoutPanel(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        this.panelLeft = (this.width - width) / 2;
        this.panelTop = (this.height - height) / 2;
    }

    protected abstract void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        this.renderContent(graphics, mouseX, mouseY, partialTick);
        for (Renderable renderable : this.renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
