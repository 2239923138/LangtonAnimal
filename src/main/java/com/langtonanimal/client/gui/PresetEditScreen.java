package com.langtonanimal.client.gui;

import com.langtonanimal.config.AntAction;
import com.langtonanimal.config.AntConfig;
import com.langtonanimal.config.AntPreset;
import com.langtonanimal.config.BlockPalette;
import com.langtonanimal.config.ConfigValidator;
import com.langtonanimal.config.PresetColors;
import com.langtonanimal.config.RuleEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

/**
 * 单个预设的编辑界面：预设名、颜色、载体生物、规则行列表。
 */
public class PresetEditScreen extends BaseScreen {

    private static final int PANEL_W = 380;
    private static final int PANEL_H = 244;
    private static final int ROW_H = 26;
    private static final int LIST_TOP_OFFSET = 56;
    private static final int BOTTOM_BAR = 42;

    private static final int POPUP_NONE = 0;
    private static final int POPUP_COLOR = 1;
    private static final int POPUP_ACTION = 2;

    private static final int COLOR_COLUMNS = 4;
    private static final int COLOR_CELL = 18;
    private static final int ACTION_ROW_H = 16;
    private static final int ACTION_W = 92;

    private final Screen parent;
    private final AntPreset target;
    private final AntPreset preset;
    private final String defaultBlockId;

    private EditBox nameBox;
    private int scroll;
    private int listTop;
    private int listBottom;

    private String hoveredTooltip;
    private int popup = POPUP_NONE;
    private int popupRule = -1;
    private int popupX;
    private int popupY;
    private int popupW;
    private int popupH;

    public PresetEditScreen(Screen parent, AntPreset target, String defaultBlockId) {
        super(Component.translatable("langtonanimal.gui.edit_preset"));
        this.parent = parent;
        this.target = target;
        this.preset = target.copy();
        this.defaultBlockId = defaultBlockId;
    }

    private void commit() {
        this.preset.setName(this.nameBox.getValue());
        this.target.setName(this.preset.name());
        this.target.setEntityType(this.preset.entityType());
        this.target.setColor(this.preset.color());
        this.target.rules().clear();
        this.target.rules().addAll(this.preset.rules());
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.layoutPanel(PANEL_W, PANEL_H);
        this.listTop = this.panelTop + LIST_TOP_OFFSET;
        this.listBottom = this.panelTop + PANEL_H - BOTTOM_BAR - 4;
        this.nameBox = new EditBox(this.font, this.panelLeft + 62, this.panelTop + 24, 118, 18,
                Component.translatable("langtonanimal.gui.preset_name"));
        this.nameBox.setMaxLength(48);
        this.nameBox.setValue(this.preset.name());
        this.nameBox.setResponder(value -> this.preset.setName(value));
        this.addRenderableWidget(this.nameBox);
        this.scroll = Math.max(0, Math.min(this.scroll, this.maxScroll()));
        this.popup = POPUP_NONE;
    }

    private int maxScroll() {
        int visible = (this.listBottom - this.listTop) / ROW_H;
        return Math.max(0, this.preset.rules().size() - visible);
    }

    // ------------------------------------------------------------------ render

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        UiKit.panel(graphics, this.panelLeft, this.panelTop, PANEL_W, PANEL_H);
        graphics.drawString(this.font, this.title, this.panelLeft + 10, this.panelTop + 9, UiKit.ACCENT, false);

        graphics.drawString(this.font, Component.translatable("langtonanimal.gui.preset_name"),
                this.panelLeft + 10, this.panelTop + 29, UiKit.TEXT_DIM, false);

        Component entityName = ConfigValidator.displayEntityName(this.preset.entityType());
        String entityLabel = entityName == null ? this.preset.entityType() : entityName.getString();
        int colorX = this.panelLeft + 184;
        boolean colorHovered = UiKit.inside(mouseX, mouseY, colorX, this.panelTop + 24, 24, 18);
        graphics.fill(colorX, this.panelTop + 24, colorX + 24, this.panelTop + 42, UiKit.BUTTON_BORDER);
        graphics.fill(colorX + 1, this.panelTop + 25, colorX + 23, this.panelTop + 41, 0xFF000000 | this.preset.color());
        if (colorHovered || this.popup == POPUP_COLOR) {
            graphics.fill(colorX, this.panelTop + 24, colorX + 24, this.panelTop + 25, 0xFFFFFFFF);
            graphics.fill(colorX, this.panelTop + 41, colorX + 24, this.panelTop + 42, 0xFFFFFFFF);
            graphics.fill(colorX, this.panelTop + 24, colorX + 1, this.panelTop + 42, 0xFFFFFFFF);
            graphics.fill(colorX + 23, this.panelTop + 24, colorX + 24, this.panelTop + 42, 0xFFFFFFFF);
        }

        int entityButtonX = this.panelLeft + 212;
        boolean entityHovered = UiKit.inside(mouseX, mouseY, entityButtonX, this.panelTop + 24, 158, 18);
        UiKit.button(graphics, this.font, entityButtonX, this.panelTop + 24, 158, 18,
                Component.translatable("langtonanimal.gui.carrier", entityLabel), entityHovered, true);

        graphics.drawString(this.font, Component.translatable("langtonanimal.gui.col_condition"),
                this.panelLeft + 12, this.panelTop + 46, UiKit.TEXT_DIM, false);
        graphics.drawString(this.font, Component.translatable("langtonanimal.gui.col_action"),
                this.panelLeft + 144, this.panelTop + 46, UiKit.TEXT_DIM, false);
        graphics.drawString(this.font, Component.translatable("langtonanimal.gui.col_target"),
                this.panelLeft + 212, this.panelTop + 46, UiKit.TEXT_DIM, false);

        this.hoveredTooltip = null;
        graphics.enableScissor(this.panelLeft + 6, this.listTop, this.panelLeft + PANEL_W - 6, this.listBottom);
        int visible = (this.listBottom - this.listTop) / ROW_H + 1;
        for (int i = 0; i < visible && this.scroll + i < this.preset.rules().size(); i++) {
            int index = this.scroll + i;
            RuleEntry rule = this.preset.rules().get(index);
            int rowY = this.listTop + i * ROW_H;
            renderRuleRow(graphics, mouseX, mouseY, index, rule, rowY);
        }
        graphics.disableScissor();

        if (this.preset.rules().isEmpty()) {
            graphics.drawString(this.font, Component.translatable("langtonanimal.gui.no_rules"),
                    this.panelLeft + 12, this.listTop + 6, UiKit.TEXT_DIM, false);
        }

        int bottomY = this.panelTop + PANEL_H - 28;
        boolean addHovered = UiKit.inside(mouseX, mouseY, this.panelLeft + 10, bottomY, 96, 18);
        UiKit.button(graphics, this.font, this.panelLeft + 10, bottomY, 96, 18,
                Component.translatable("langtonanimal.gui.add_rule"), addHovered, true);
        boolean okHovered = UiKit.inside(mouseX, mouseY, this.panelLeft + PANEL_W - 194, bottomY, 88, 18);
        UiKit.button(graphics, this.font, this.panelLeft + PANEL_W - 194, bottomY, 88, 18,
                Component.translatable("langtonanimal.gui.ok"), okHovered, true);
        boolean cancelHovered = UiKit.inside(mouseX, mouseY, this.panelLeft + PANEL_W - 100, bottomY, 88, 18);
        UiKit.button(graphics, this.font, this.panelLeft + PANEL_W - 100, bottomY, 88, 18,
                Component.translatable("langtonanimal.gui.cancel"), cancelHovered, true);

        renderPopup(graphics, mouseX, mouseY);

        if (this.hoveredTooltip != null && this.popup == POPUP_NONE) {
            graphics.drawString(this.font, this.hoveredTooltip, mouseX + 8, mouseY - 12, UiKit.TEXT_DIM, false);
        }
    }

    private void renderRuleRow(GuiGraphics graphics, int mouseX, int mouseY, int index, RuleEntry rule, int rowY) {
        graphics.fill(this.panelLeft + 6, rowY, this.panelLeft + PANEL_W - 6, rowY + ROW_H - 2, UiKit.ROW_BG);

        Block conditionBlock = BlockPalette.resolveBlock(rule.condition(), this.defaultBlockId);
        Component conditionLabel = BlockPalette.displayName(rule.condition(), this.defaultBlockId);
        boolean conditionHovered = UiKit.inside(mouseX, mouseY, this.panelLeft + 10, rowY + 3, 120, 18);
        UiKit.buttonIcon(graphics, this.font, this.panelLeft + 10, rowY + 3, 120, 18,
                conditionBlock, conditionLabel, conditionHovered, true);

        boolean actionHovered = UiKit.inside(mouseX, mouseY, this.panelLeft + 134, rowY + 3, 62, 18)
                || (this.popup == POPUP_ACTION && this.popupRule == index);
        UiKit.button(graphics, this.font, this.panelLeft + 134, rowY + 3, 62, 18,
                Component.translatable("langtonanimal.action." + rule.action().id()), actionHovered, true);

        Block targetBlock = BlockPalette.resolveBlock(rule.target(), this.defaultBlockId);
        Component targetLabel = BlockPalette.displayName(rule.target(), this.defaultBlockId);
        boolean targetHovered = UiKit.inside(mouseX, mouseY, this.panelLeft + 200, rowY + 3, 120, 18);
        UiKit.buttonIcon(graphics, this.font, this.panelLeft + 200, rowY + 3, 120, 18,
                targetBlock, targetLabel, targetHovered, false);

        boolean deleteHovered = UiKit.inside(mouseX, mouseY, this.panelLeft + PANEL_W - 42, rowY + 3, 18, 18);
        UiKit.button(graphics, this.font, this.panelLeft + PANEL_W - 42, rowY + 3, 18, 18,
                Component.literal("x"), deleteHovered, true);

        if (conditionHovered) {
            this.hoveredTooltip = rule.condition();
        } else if (targetHovered) {
            this.hoveredTooltip = rule.target();
        }
    }

    private void renderPopup(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.popup == POPUP_NONE) {
            return;
        }
        graphics.fill(this.popupX, this.popupY, this.popupX + this.popupW, this.popupY + this.popupH, 0xF0101418);
        graphics.fill(this.popupX, this.popupY, this.popupX + this.popupW, this.popupY + 1, UiKit.PANEL_BORDER);
        graphics.fill(this.popupX, this.popupY + this.popupH - 1, this.popupX + this.popupW,
                this.popupY + this.popupH, UiKit.PANEL_BORDER);
        graphics.fill(this.popupX, this.popupY, this.popupX + 1, this.popupY + this.popupH, UiKit.PANEL_BORDER);
        graphics.fill(this.popupX + this.popupW - 1, this.popupY, this.popupX + this.popupW,
                this.popupY + this.popupH, UiKit.PANEL_BORDER);

        if (this.popup == POPUP_COLOR) {
            var tones = PresetColors.all();
            for (int i = 0; i < tones.size(); i++) {
                int column = i % COLOR_COLUMNS;
                int row = i / COLOR_COLUMNS;
                int cellX = this.popupX + 4 + column * COLOR_CELL;
                int cellY = this.popupY + 4 + row * COLOR_CELL;
                graphics.fill(cellX, cellY, cellX + 16, cellY + 16, 0xFF000000 | tones.get(i).rgb());
                if (tones.get(i).rgb() == (this.preset.color() & 0xFFFFFF)) {
                    graphics.fill(cellX - 1, cellY - 1, cellX + 17, cellY, 0xFFFFFFFF);
                    graphics.fill(cellX - 1, cellY + 16, cellX + 17, cellY + 17, 0xFFFFFFFF);
                    graphics.fill(cellX - 1, cellY, cellX, cellY + 16, 0xFFFFFFFF);
                    graphics.fill(cellX + 16, cellY, cellX + 17, cellY + 16, 0xFFFFFFFF);
                } else if (UiKit.inside(mouseX, mouseY, cellX, cellY, 16, 16)) {
                    graphics.fill(cellX, cellY, cellX + 16, cellY + 17, 0x40FFFFFF);
                }
            }
            return;
        }

        RuleEntry rule = this.ruleAt(this.popupRule);
        AntAction[] actions = AntAction.values();
        for (int i = 0; i < actions.length; i++) {
            int rowY = this.popupY + 2 + i * ACTION_ROW_H;
            boolean hovered = UiKit.inside(mouseX, mouseY, this.popupX + 2, rowY, this.popupW - 4, ACTION_ROW_H);
            if (hovered) {
                graphics.fill(this.popupX + 2, rowY, this.popupX + this.popupW - 2, rowY + ACTION_ROW_H,
                        UiKit.ROW_BG_HOVER);
            }
            boolean current = rule != null && rule.action() == actions[i];
            Component label = Component.translatable("langtonanimal.action." + actions[i].id());
            int textX = this.popupX + 8;
            if (current) {
                graphics.drawString(this.font, ">", textX, rowY + 4, UiKit.ACCENT, false);
            }
            graphics.drawString(this.font, label, textX + 10, rowY + 4,
                    current ? UiKit.ACCENT : UiKit.TEXT, false);
        }
    }

    private RuleEntry ruleAt(int index) {
        if (index < 0 || index >= this.preset.rules().size()) {
            return null;
        }
        return this.preset.rules().get(index);
    }

    // ------------------------------------------------------------------- input

    private void openColorPopup() {
        this.popup = POPUP_COLOR;
        this.popupW = COLOR_COLUMNS * COLOR_CELL + 8;
        this.popupH = (PresetColors.all().size() / COLOR_COLUMNS) * COLOR_CELL + 8;
        this.popupX = this.panelLeft + 184;
        this.popupY = this.panelTop + 43;
    }

    private void openActionPopup(int index, int rowY) {
        this.popup = POPUP_ACTION;
        this.popupRule = index;
        this.popupW = ACTION_W;
        this.popupH = AntAction.values().length * ACTION_ROW_H + 4;
        this.popupX = this.panelLeft + 134;
        int below = rowY + 22;
        this.popupY = below + this.popupH > this.panelTop + PANEL_H - 6 ? rowY - this.popupH - 1 : below;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (this.popup != POPUP_NONE) {
            handlePopupClick((int) mouseX, (int) mouseY);
            return true;
        }

        int bottomY = this.panelTop + PANEL_H - 28;
        if (UiKit.inside(mouseX, mouseY, this.panelLeft + 10, bottomY, 96, 18)) {
            this.preset.rules().add(new RuleEntry(AntConfig.DEFAULT_BLOCK, AntAction.RIGHT, AntConfig.KEEP));
            this.scroll = this.maxScroll();
            return true;
        }
        if (UiKit.inside(mouseX, mouseY, this.panelLeft + PANEL_W - 194, bottomY, 88, 18)) {
            this.commit();
            this.minecraft.setScreen(this.parent);
            return true;
        }
        if (UiKit.inside(mouseX, mouseY, this.panelLeft + PANEL_W - 100, bottomY, 88, 18)) {
            this.minecraft.setScreen(this.parent);
            return true;
        }
        if (UiKit.inside(mouseX, mouseY, this.panelLeft + 184, this.panelTop + 24, 24, 18)) {
            openColorPopup();
            return true;
        }
        if (UiKit.inside(mouseX, mouseY, this.panelLeft + 212, this.panelTop + 24, 158, 18)) {
            this.minecraft.setScreen(new EntityPickerScreen(this, this.preset::setEntityType));
            return true;
        }

        int visible = (this.listBottom - this.listTop) / ROW_H + 1;
        for (int i = 0; i < visible && this.scroll + i < this.preset.rules().size(); i++) {
            int index = this.scroll + i;
            int rowY = this.listTop + i * ROW_H;
            if (rowY + ROW_H < this.listTop || rowY > this.listBottom) {
                continue;
            }
            if (rowY + 3 > this.listBottom) {
                break;
            }
            RuleEntry rule = this.preset.rules().get(index);
            if (UiKit.inside(mouseX, mouseY, this.panelLeft + 10, rowY + 3, 120, 18)) {
                this.minecraft.setScreen(new BlockPickerScreen(this, false, this.defaultBlockId,
                        id -> this.preset.rules().set(index, rule.withCondition(id))));
                return true;
            }
            if (UiKit.inside(mouseX, mouseY, this.panelLeft + 134, rowY + 3, 62, 18)) {
                openActionPopup(index, rowY);
                return true;
            }
            if (UiKit.inside(mouseX, mouseY, this.panelLeft + 200, rowY + 3, 120, 18)) {
                this.minecraft.setScreen(new BlockPickerScreen(this, true, this.defaultBlockId,
                        id -> this.preset.rules().set(index, rule.withTarget(id))));
                return true;
            }
            if (UiKit.inside(mouseX, mouseY, this.panelLeft + PANEL_W - 42, rowY + 3, 18, 18)) {
                this.preset.rules().remove(index);
                this.scroll = Math.max(0, Math.min(this.scroll, this.maxScroll()));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void handlePopupClick(int mouseX, int mouseY) {
        if (this.popup == POPUP_COLOR) {
            var tones = PresetColors.all();
            for (int i = 0; i < tones.size(); i++) {
                int cellX = this.popupX + 4 + (i % COLOR_COLUMNS) * COLOR_CELL;
                int cellY = this.popupY + 4 + (i / COLOR_COLUMNS) * COLOR_CELL;
                if (UiKit.inside(mouseX, mouseY, cellX, cellY, 16, 16)) {
                    this.preset.setColor(tones.get(i).rgb());
                    this.popup = POPUP_NONE;
                    return;
                }
            }
        } else if (this.popup == POPUP_ACTION) {
            AntAction[] actions = AntAction.values();
            for (int i = 0; i < actions.length; i++) {
                int rowY = this.popupY + 2 + i * ACTION_ROW_H;
                if (UiKit.inside(mouseX, mouseY, this.popupX + 2, rowY, this.popupW - 4, ACTION_ROW_H)) {
                    RuleEntry rule = ruleAt(this.popupRule);
                    if (rule != null) {
                        this.preset.rules().set(this.popupRule, rule.withAction(actions[i]));
                    }
                    this.popup = POPUP_NONE;
                    return;
                }
            }
        }
        this.popup = POPUP_NONE;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.popup != POPUP_NONE) {
            return true;
        }
        if (scrollY != 0 && mouseY >= this.listTop && mouseY <= this.listBottom) {
            this.scroll = Math.max(0, Math.min(this.scroll - (int) Math.signum(scrollY), this.maxScroll()));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.nameBox != null && this.nameBox.isFocused()) {
            if (keyCode != 256 && this.nameBox.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        if (keyCode == 256) {
            if (this.popup != POPUP_NONE) {
                this.popup = POPUP_NONE;
                return true;
            }
            if (this.nameBox != null) {
                this.nameBox.setFocused(false);
            }
            this.minecraft.setScreen(this.parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.nameBox != null && this.nameBox.isFocused()) {
            return this.nameBox.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }
}
