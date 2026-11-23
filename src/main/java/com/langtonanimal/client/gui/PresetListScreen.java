package com.langtonanimal.client.gui;

import com.langtonanimal.client.ClientState;
import com.langtonanimal.config.AntConfig;
import com.langtonanimal.config.AntPreset;
import com.langtonanimal.config.BlockPalette;
import com.langtonanimal.config.ConfigValidator;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

/**
 * 主配置界面：默认初始方块 + 预设列表。
 */
public class PresetListScreen extends BaseScreen {

    private static final int PANEL_W = 400;
    private static final int PANEL_H = 250;
    private static final int ROW_H = 30;
    private static final int LIST_TOP_OFFSET = 56;
    private static final int BOTTOM_BAR = 40;

    private EditBox defaultBlockBox;
    private int scroll;
    private int listTop;
    private int listBottom;
    private final List<Component> errors = new ArrayList<>();
    private int pendingDelete = -1;

    public PresetListScreen() {
        super(Component.translatable("langtonanimal.gui.title"));
        ClientState.beginSession();
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.layoutPanel(PANEL_W, PANEL_H);
        this.listTop = this.panelTop + LIST_TOP_OFFSET;
        this.listBottom = this.panelTop + PANEL_H - BOTTOM_BAR - 4;

        this.defaultBlockBox = new EditBox(this.font, this.panelLeft + 84, this.panelTop + 24, 190, 18,
                Component.translatable("langtonanimal.gui.default_block"));
        this.defaultBlockBox.setMaxLength(96);
        this.defaultBlockBox.setValue(ClientState.draft().defaultBlockId());
        this.addRenderableWidget(this.defaultBlockBox);
    }

    private List<AntPreset> presets() {
        return ClientState.draft().presets();
    }

    private int visibleRows() {
        return Math.max(1, (this.listBottom - this.listTop) / ROW_H);
    }

    private int maxScroll() {
        return Math.max(0, presets().size() - visibleRows());
    }

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        UiKit.panel(graphics, this.panelLeft, this.panelTop, PANEL_W, PANEL_H);
        graphics.drawString(this.font, this.title, this.panelLeft + 10, this.panelTop + 9, UiKit.ACCENT, false);

        graphics.drawString(this.font, Component.translatable("langtonanimal.gui.default_block"),
                this.panelLeft + 10, this.panelTop + 29, UiKit.TEXT_DIM, false);
        boolean resetHovered = UiKit.inside(mouseX, mouseY, this.panelLeft + 282, this.panelTop + 24, 108, 18);
        UiKit.button(graphics, this.font, this.panelLeft + 282, this.panelTop + 24, 108, 18,
                Component.translatable("langtonanimal.gui.reset_default"), resetHovered, true);

        graphics.enableScissor(this.panelLeft + 6, this.listTop, this.panelLeft + PANEL_W - 6, this.listBottom);
        for (int i = 0; i < visibleRows() && this.scroll + i < presets().size(); i++) {
            AntPreset preset = presets().get(this.scroll + i);
            int rowY = this.listTop + i * ROW_H;
            boolean rowHovered = mouseX >= this.panelLeft + 6 && mouseX < this.panelLeft + PANEL_W - 6
                    && mouseY >= rowY && mouseY < rowY + ROW_H - 2;
            graphics.fill(this.panelLeft + 6, rowY, this.panelLeft + PANEL_W - 6, rowY + ROW_H - 2,
                    rowHovered ? UiKit.ROW_BG_HOVER : UiKit.ROW_BG);
            graphics.fill(this.panelLeft + 6, rowY, this.panelLeft + 9, rowY + ROW_H - 2,
                    0xFF000000 | preset.color());
            UiKit.drawClamped(graphics, this.font,
                    Component.literal(preset.name().isEmpty() ? "?" : preset.name())
                            .withStyle(Style.EMPTY.withColor(preset.color())),
                    this.panelLeft + 14, rowY + 5, UiKit.TEXT, PANEL_W - 152);
            Component entityName = ConfigValidator.displayEntityName(preset.entityType());
            String detail = (entityName == null ? preset.entityType() : entityName.getString())
                    + "  ·  " + preset.rules().size() + " " + Component.translatable("langtonanimal.gui.rules").getString();
            UiKit.drawClamped(graphics, this.font, Component.literal(detail),
                    this.panelLeft + 14, rowY + 16, UiKit.TEXT_DIM, PANEL_W - 152);

            int editX = this.panelLeft + PANEL_W - 118;
            int deleteX = this.panelLeft + PANEL_W - 62;
            boolean editHovered = UiKit.inside(mouseX, mouseY, editX, rowY + 5, 50, 18);
            boolean deleteHovered = UiKit.inside(mouseX, mouseY, deleteX, rowY + 5, 50, 18);
            UiKit.button(graphics, this.font, editX, rowY + 5, 50, 18,
                    Component.translatable("langtonanimal.gui.edit"), editHovered, true);
            UiKit.button(graphics, this.font, deleteX, rowY + 5, 50, 18,
                    Component.translatable("langtonanimal.gui.delete"), deleteHovered, true);
        }
        graphics.disableScissor();

        if (presets().isEmpty()) {
            graphics.drawString(this.font, Component.translatable("langtonanimal.gui.empty_list"),
                    this.panelLeft + 12, this.listTop + 6, UiKit.TEXT_DIM, false);
        }

        if (!this.errors.isEmpty()) {
            int errorHeight = 12 + Math.min(3, this.errors.size()) * 10;
            int errorY = this.listBottom - errorHeight;
            graphics.fill(this.panelLeft + 8, errorY, this.panelLeft + PANEL_W - 8, errorY + errorHeight, 0xF02A0E14);
            graphics.fill(this.panelLeft + 8, errorY, this.panelLeft + PANEL_W - 8, errorY + 1, UiKit.TEXT_WARN);
            for (int i = 0; i < Math.min(3, this.errors.size()); i++) {
                UiKit.drawClamped(graphics, this.font, this.errors.get(i),
                        this.panelLeft + 12, errorY + 4 + i * 10, UiKit.TEXT_WARN, PANEL_W - 24);
            }
        }

        int bottomY = this.panelTop + PANEL_H - 28;
        boolean newHovered = UiKit.inside(mouseX, mouseY, this.panelLeft + 10, bottomY, 96, 18);
        UiKit.button(graphics, this.font, this.panelLeft + 10, bottomY, 96, 18,
                Component.translatable("langtonanimal.gui.new_preset"), newHovered, true);
        boolean saveHovered = UiKit.inside(mouseX, mouseY, this.panelLeft + PANEL_W - 130, bottomY, 120, 18);
        UiKit.button(graphics, this.font, this.panelLeft + PANEL_W - 130, bottomY, 120, 18,
                Component.translatable("langtonanimal.gui.save_close"), saveHovered, true);

        int hintY = bottomY + 22;
        if (hintY + 8 < this.panelTop + PANEL_H) {
            graphics.drawString(this.font, Component.translatable("langtonanimal.gui.hint"),
                    this.panelLeft + 10, hintY, 0xFF606060, false);
        }

        renderDeleteDialog(graphics, mouseX, mouseY);
    }

    private void renderDeleteDialog(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.pendingDelete < 0) {
            return;
        }
        String name = this.pendingDelete < presets().size()
                ? presets().get(this.pendingDelete).name() : "";
        graphics.fill(0, 0, this.width, this.height, 0x99000000);
        int dialogW = 260;
        int dialogH = 92;
        int x = (this.width - dialogW) / 2;
        int y = (this.height - dialogH) / 2;
        UiKit.panel(graphics, x, y, dialogW, dialogH);
        graphics.drawString(this.font, Component.translatable("langtonanimal.gui.confirm_delete"),
                x + 12, y + 10, 0xFFFF6B6B, false);
        UiKit.drawClamped(graphics, this.font,
                Component.translatable("langtonanimal.gui.confirm_delete_body", name),
                x + 12, y + 30, UiKit.TEXT, dialogW - 24);
        UiKit.drawClamped(graphics, this.font,
                Component.translatable("langtonanimal.gui.confirm_delete_hint"),
                x + 12, y + 44, UiKit.TEXT_DIM, dialogW - 24);
        int buttonY = y + dialogH - 26;
        boolean confirmHovered = UiKit.inside(mouseX, mouseY, x + dialogW - 138, buttonY, 62, 18);
        UiKit.button(graphics, this.font, x + dialogW - 138, buttonY, 62, 18,
                Component.translatable("langtonanimal.gui.delete"), confirmHovered, true);
        boolean cancelHovered = UiKit.inside(mouseX, mouseY, x + dialogW - 70, buttonY, 58, 18);
        UiKit.button(graphics, this.font, x + dialogW - 70, buttonY, 58, 18,
                Component.translatable("langtonanimal.gui.cancel"), cancelHovered, true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (this.pendingDelete >= 0) {
            int dialogW = 260;
            int dialogH = 92;
            int x = (this.width - dialogW) / 2;
            int y = (this.height - dialogH) / 2;
            int buttonY = y + dialogH - 26;
            if (UiKit.inside(mouseX, mouseY, x + dialogW - 138, buttonY, 62, 18)) {
                if (this.pendingDelete < presets().size()) {
                    presets().remove(this.pendingDelete);
                    this.scroll = Math.max(0, Math.min(this.scroll, this.maxScroll()));
                }
                this.pendingDelete = -1;
                return true;
            }
            if (UiKit.inside(mouseX, mouseY, x + dialogW - 70, buttonY, 58, 18)) {
                this.pendingDelete = -1;
                return true;
            }
            if (!UiKit.inside(mouseX, mouseY, x, y, dialogW, dialogH)) {
                this.pendingDelete = -1;
            }
            return true;
        }
        if (UiKit.inside(mouseX, mouseY, this.panelLeft + 282, this.panelTop + 24, 108, 18)) {
            this.defaultBlockBox.setValue(AntConfig.FALLBACK_DEFAULT_BLOCK);
            this.errors.clear();
            return true;
        }
        int bottomY = this.panelTop + PANEL_H - 28;
        if (UiKit.inside(mouseX, mouseY, this.panelLeft + 10, bottomY, 96, 18)) {
            String name = this.uniqueName();
            AntPreset preset = AntConfig.newTemplatePreset(name);
            ClientState.draft().presets().add(preset);
            this.scroll = this.maxScroll();
            this.minecraft.setScreen(new PresetEditScreen(this, preset, this.currentDefaultBlock()));
            return true;
        }
        if (UiKit.inside(mouseX, mouseY, this.panelLeft + PANEL_W - 130, bottomY, 120, 18)) {
            this.save();
            return true;
        }

        if (mouseX >= this.panelLeft + 6 && mouseX < this.panelLeft + PANEL_W - 6
                && mouseY >= this.listTop && mouseY < this.listBottom) {
            int index = this.scroll + (int) ((mouseY - this.listTop) / ROW_H);
            if (index >= 0 && index < presets().size()) {
                int rowY = this.listTop + (int) ((mouseY - this.listTop) / ROW_H) * ROW_H;
                int editX = this.panelLeft + PANEL_W - 118;
                int deleteX = this.panelLeft + PANEL_W - 62;
                if (UiKit.inside(mouseX, mouseY, editX, rowY + 5, 50, 18)) {
                    this.minecraft.setScreen(new PresetEditScreen(this, presets().get(index),
                            this.currentDefaultBlock()));
                    return true;
                }
                if (UiKit.inside(mouseX, mouseY, deleteX, rowY + 5, 50, 18)) {
                    this.pendingDelete = index;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void save() {
        AntConfig draft = ClientState.draft();
        draft.setDefaultBlockId(this.defaultBlockBox.getValue());
        this.errors.clear();
        this.errors.addAll(ConfigValidator.validate(draft, ClientState.liveNames()));
        if (!this.errors.isEmpty()) {
            return;
        }
        ClientState.submit(draft);
        this.minecraft.setScreen(null);
    }

    private String currentDefaultBlock() {
        return BlockPalette.normalize(this.defaultBlockBox == null
                ? ClientState.draft().defaultBlockId()
                : this.defaultBlockBox.getValue());
    }

    private String uniqueName() {
        int index = 1;
        while (true) {
            String candidate = "preset_" + index;
            boolean exists = ClientState.draft().findPreset(candidate) != null;
            if (!exists) {
                return candidate;
            }
            index++;
        }
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.defaultBlockBox != null && this.defaultBlockBox.isFocused()
                && this.defaultBlockBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (keyCode == 256) {
            if (this.pendingDelete >= 0) {
                this.pendingDelete = -1;
                return true;
            }
            if (this.defaultBlockBox != null) {
                this.defaultBlockBox.setFocused(false);
            }
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.defaultBlockBox != null && this.defaultBlockBox.isFocused()) {
            return this.defaultBlockBox.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void onClose() {
        ClientState.discardDraft();
        this.minecraft.setScreen(null);
    }
}
