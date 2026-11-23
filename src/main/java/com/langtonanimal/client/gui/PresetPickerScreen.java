package com.langtonanimal.client.gui;

import com.langtonanimal.client.ClientState;
import com.langtonanimal.client.RodState;
import com.langtonanimal.config.AntPreset;
import com.langtonanimal.config.ConfigValidator;
import com.langtonanimal.network.AssignPresetPayload;
import com.langtonanimal.network.SetRunningPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 烈焰棒编辑模式下的预设选择窗口：给兰顿生物换预设，或把普通生物转化成兰顿生物。
 */
public class PresetPickerScreen extends BaseScreen {

    private static final int PANEL_W = 320;
    private static final int PANEL_H = 200;
    private static final int ROW_H = 18;

    private final UUID entity;
    private final boolean converting;
    private final List<AntPreset> presets = new ArrayList<>();
    private int scroll;
    private int listTop;
    private int listBottom;

    public PresetPickerScreen(UUID entity, boolean converting) {
        super(Component.translatable(converting
                ? "langtonanimal.gui.convert_title" : "langtonanimal.gui.rebind_title"));
        this.entity = entity;
        this.converting = converting;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.layoutPanel(PANEL_W, PANEL_H);
        this.listTop = this.panelTop + 26;
        this.listBottom = this.panelTop + PANEL_H - 28;
        this.presets.clear();
        this.presets.addAll(ClientState.synced().presets());
        this.scroll = Math.max(0, Math.min(this.scroll, this.maxScroll()));
    }

    private int maxScroll() {
        int visible = Math.max(1, (this.listBottom - this.listTop) / ROW_H);
        return Math.max(0, this.presets.size() - visible);
    }

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        UiKit.panel(graphics, this.panelLeft, this.panelTop, PANEL_W, PANEL_H);
        graphics.drawString(this.font, this.title, this.panelLeft + 12, this.panelTop + 9, UiKit.ACCENT, false);

        graphics.enableScissor(this.panelLeft + 6, this.listTop, this.panelLeft + PANEL_W - 6, this.listBottom);
        int visible = Math.max(1, (this.listBottom - this.listTop) / ROW_H);
        for (int i = 0; i <= visible && this.scroll + i < this.presets.size(); i++) {
            AntPreset preset = this.presets.get(this.scroll + i);
            int rowY = this.listTop + i * ROW_H;
            boolean hovered = mouseX >= this.panelLeft + 6 && mouseX < this.panelLeft + PANEL_W - 6
                    && mouseY >= rowY && mouseY < rowY + ROW_H;
            if (hovered) {
                graphics.fill(this.panelLeft + 6, rowY, this.panelLeft + PANEL_W - 6, rowY + ROW_H,
                        UiKit.ROW_BG_HOVER);
            }
            int color = 0xFF000000 | preset.color();
            graphics.fill(this.panelLeft + 10, rowY + 4, this.panelLeft + 20, rowY + 14, color);
            graphics.drawString(this.font, Component.literal(preset.name()).withStyle(Style.EMPTY.withColor(preset.color())),
                    this.panelLeft + 26, rowY + 5, UiKit.TEXT, false);
            Component entityName = ConfigValidator.displayEntityName(preset.entityType());
            String detail = entityName == null ? preset.entityType() : entityName.getString();
            graphics.drawString(this.font, detail,
                    this.panelLeft + PANEL_W - 12 - this.font.width(detail), rowY + 5, UiKit.TEXT_DIM, false);
        }
        graphics.disableScissor();

        if (this.presets.isEmpty()) {
            graphics.drawString(this.font, Component.translatable("langtonanimal.gui.empty_list"),
                    this.panelLeft + 12, this.listTop + 4, UiKit.TEXT_DIM, false);
        }

        int bottomY = this.panelTop + PANEL_H - 24;
        boolean cancelHovered = UiKit.inside(mouseX, mouseY, this.panelLeft + 12, bottomY, 80, 16);
        UiKit.button(graphics, this.font, this.panelLeft + 12, bottomY, 80, 16,
                Component.translatable("langtonanimal.gui.cancel"), cancelHovered, true);
        boolean configHovered = UiKit.inside(mouseX, mouseY, this.panelLeft + PANEL_W - 132, bottomY, 120, 16);
        UiKit.button(graphics, this.font, this.panelLeft + PANEL_W - 132, bottomY, 120, 16,
                Component.translatable("langtonanimal.gui.open_config"), configHovered, true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        int bottomY = this.panelTop + PANEL_H - 24;
        if (UiKit.inside(mouseX, mouseY, this.panelLeft + 12, bottomY, 80, 16)) {
            cancel();
            return true;
        }
        if (UiKit.inside(mouseX, mouseY, this.panelLeft + PANEL_W - 132, bottomY, 120, 16)) {
            RodState.endEdit();
            this.minecraft.setScreen(new PresetListScreen());
            return true;
        }
        if (mouseX >= this.panelLeft + 6 && mouseX < this.panelLeft + PANEL_W - 6
                && mouseY >= this.listTop && mouseY < this.listBottom) {
            int index = this.scroll + (int) ((mouseY - this.listTop) / ROW_H);
            if (index >= 0 && index < this.presets.size()) {
                PacketDistributor.sendToServer(new AssignPresetPayload(this.entity, this.presets.get(index).name()));
                RodState.endEdit();
                this.minecraft.setScreen(null);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void cancel() {
        String name = RodState.editingName();
        if (name != null && RodState.editingWasRunning()) {
            PacketDistributor.sendToServer(new SetRunningPayload(name, true));
        }
        RodState.endEdit();
        this.minecraft.setScreen(null);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0 && mouseY >= this.listTop && mouseY <= this.listBottom) {
            this.scroll = Math.max(0, Math.min(this.scroll - (int) Math.signum(scrollY), this.maxScroll()));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        cancel();
    }
}
