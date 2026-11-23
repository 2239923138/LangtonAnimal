package com.langtonanimal.client.gui;

import com.langtonanimal.config.ConfigValidator;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 载体生物选择列表。只展示允许作为载体的生物（排除 Boss 与非生物实体）。
 */
public class EntityPickerScreen extends BaseScreen {

    private static final int PANEL_W = 320;
    private static final int PANEL_H = 220;
    private static final int ROW_H = 14;

    private final Screen parent;
    private final Consumer<String> onPick;
    private final List<String> entries = new ArrayList<>();

    private int scroll;
    private int listTop;
    private int listBottom;

    public EntityPickerScreen(Screen parent, Consumer<String> onPick) {
        super(Component.translatable("langtonanimal.gui.pick_entity"));
        this.parent = parent;
        this.onPick = onPick;
    }

    @Override
    protected void init() {
        this.layoutPanel(PANEL_W, PANEL_H);
        this.listTop = this.panelTop + 26;
        this.listBottom = this.panelTop + PANEL_H - 30;
        this.entries.clear();
        this.entries.addAll(ConfigValidator.allowedEntityIds());
        this.scroll = Math.max(0, Math.min(this.scroll, this.maxScroll()));
    }

    private int maxScroll() {
        int visible = (this.listBottom - this.listTop) / ROW_H;
        return Math.max(0, this.entries.size() - visible);
    }

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        UiKit.panel(graphics, this.panelLeft, this.panelTop, PANEL_W, PANEL_H);
        graphics.drawString(this.font, this.title, this.panelLeft + 12, this.panelTop + 9, UiKit.ACCENT, false);

        graphics.enableScissor(this.panelLeft + 6, this.listTop, this.panelLeft + PANEL_W - 6, this.listBottom);
        int visible = (this.listBottom - this.listTop) / ROW_H;
        for (int i = 0; i <= visible && this.scroll + i < this.entries.size(); i++) {
            String id = this.entries.get(this.scroll + i);
            int rowY = this.listTop + i * ROW_H;
            boolean hovered = mouseX >= this.panelLeft + 6 && mouseX < this.panelLeft + PANEL_W - 6
                    && mouseY >= rowY && mouseY < rowY + ROW_H;
            if (hovered) {
                graphics.fill(this.panelLeft + 6, rowY, this.panelLeft + PANEL_W - 6, rowY + ROW_H, UiKit.ROW_BG_HOVER);
            }
            Component name = ConfigValidator.displayEntityName(id);
            graphics.drawString(this.font, name == null ? Component.literal(id) : name,
                    this.panelLeft + 12, rowY + 3, UiKit.TEXT, false);
            graphics.drawString(this.font, id, this.panelLeft + PANEL_W - 12 - this.font.width(id),
                    rowY + 3, UiKit.TEXT_DIM, false);
        }
        graphics.disableScissor();

        Component cancel = Component.translatable("langtonanimal.gui.cancel");
        boolean cancelHovered = UiKit.inside(mouseX, mouseY, this.panelLeft + 12, this.panelTop + PANEL_H - 24, 80, 18);
        UiKit.button(graphics, this.font, this.panelLeft + 12, this.panelTop + PANEL_H - 24, 80, 18,
                cancel, cancelHovered, true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (UiKit.inside(mouseX, mouseY, this.panelLeft + 12, this.panelTop + PANEL_H - 24, 80, 18)) {
                this.minecraft.setScreen(this.parent);
                return true;
            }
            if (mouseX >= this.panelLeft + 6 && mouseX < this.panelLeft + PANEL_W - 6
                    && mouseY >= this.listTop && mouseY < this.listBottom) {
                int index = this.scroll + (int) ((mouseY - this.listTop) / ROW_H);
                if (index >= 0 && index < this.entries.size()) {
                    this.onPick.accept(this.entries.get(index));
                    this.minecraft.setScreen(this.parent);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0) {
            this.scroll = Math.max(0, Math.min(this.scroll - (int) Math.signum(scrollY), this.maxScroll()));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}
