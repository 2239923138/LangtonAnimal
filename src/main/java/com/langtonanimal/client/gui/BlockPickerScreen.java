package com.langtonanimal.client.gui;

import com.langtonanimal.config.AntConfig;
import com.langtonanimal.config.BlockPalette;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * 方块选择弹窗：顶部置顶「默认初始方块」/「不变」，主体按 16 色羊毛 / 混凝土 / 陶瓦分类平铺。
 */
public class BlockPickerScreen extends BaseScreen {

    private static final int PANEL_W = 364;
    private static final int PANEL_H = 254;
    private static final int CELL_W = 42;
    private static final int CELL_H = 22;
    private static final int COLUMNS = 8;

    private final Screen parent;
    private final boolean allowKeep;
    private final String defaultBlockId;
    private final Consumer<String> onPick;

    private final List<Cell> cells = new ArrayList<>();

    private record Cell(int x, int y, int w, int h, String id, Block block) {
    }

    public BlockPickerScreen(Screen parent, boolean allowKeep, String defaultBlockId, Consumer<String> onPick) {
        super(Component.translatable("langtonanimal.gui.pick_block"));
        this.parent = parent;
        this.allowKeep = allowKeep;
        this.defaultBlockId = defaultBlockId;
        this.onPick = onPick;
    }

    @Override
    protected void init() {
        this.layoutPanel(PANEL_W, PANEL_H);
        this.cells.clear();

        if (this.allowKeep) {
            this.cells.add(new Cell(this.panelLeft + 14, this.panelTop + 24, 160, 18,
                    AntConfig.KEEP, null));
            this.cells.add(new Cell(this.panelLeft + 178, this.panelTop + 24, 172, 18,
                    AntConfig.DEFAULT_BLOCK, BlockPalette.blockById(BlockPalette.normalize(this.defaultBlockId))));
        } else {
            this.cells.add(new Cell(this.panelLeft + 14, this.panelTop + 24, 336, 18,
                    AntConfig.DEFAULT_BLOCK, BlockPalette.blockById(BlockPalette.normalize(this.defaultBlockId))));
        }

        int cursorY = this.panelTop + 52;
        for (BlockPalette.Family family : BlockPalette.Family.values()) {
            cursorY += 11;
            List<BlockPalette.Entry> entries = BlockPalette.byFamily(family);
            for (int i = 0; i < entries.size(); i++) {
                int column = i % COLUMNS;
                int row = i / COLUMNS;
                BlockPalette.Entry entry = entries.get(i);
                this.cells.add(new Cell(this.panelLeft + 14 + column * CELL_W, cursorY + row * CELL_H,
                        CELL_W - 2, CELL_H - 2, entry.idString(), entry.block()));
            }
            cursorY += 2 * CELL_H + 3;
        }
    }

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        UiKit.panel(graphics, this.panelLeft, this.panelTop, PANEL_W, PANEL_H);
        graphics.drawString(this.font, this.title, this.panelLeft + 14, this.panelTop + 9, UiKit.ACCENT, false);

        int headerY = this.panelTop + 52;
        for (BlockPalette.Family family : BlockPalette.Family.values()) {
            graphics.drawString(this.font,
                    Component.translatable("langtonanimal.gui.family." + family.key()),
                    this.panelLeft + 14, headerY, UiKit.TEXT_DIM, false);
            headerY += 11 + 2 * CELL_H + 3;
        }

        Component hoveredName = null;
        for (Cell cell : this.cells) {
            boolean hovered = UiKit.inside(mouseX, mouseY, cell.x(), cell.y(), cell.w(), cell.h());
            boolean pinned = cell.id().equals(AntConfig.KEEP) || cell.id().equals(AntConfig.DEFAULT_BLOCK);
            if (pinned) {
                Component label = BlockPalette.displayName(cell.id(), this.defaultBlockId);
                UiKit.buttonIcon(graphics, this.font, cell.x(), cell.y(), cell.w(), cell.h(),
                        cell.block(), label, hovered, true);
                if (hovered) {
                    hoveredName = label;
                }
            } else {
                if (hovered) {
                    graphics.fill(cell.x(), cell.y(), cell.x() + cell.w(), cell.y() + cell.h(), UiKit.ROW_BG_HOVER);
                }
                if (cell.block() != null) {
                    graphics.renderItem(new ItemStack(cell.block()),
                            cell.x() + (cell.w() - 16) / 2, cell.y() + (cell.h() - 16) / 2);
                }
                if (hovered) {
                    hoveredName = BlockPalette.displayName(cell.id(), this.defaultBlockId);
                }
            }
        }

        int cancelY = this.panelTop + PANEL_H - 26;
        boolean cancelHovered = UiKit.inside(mouseX, mouseY, this.panelLeft + 14, cancelY, 90, 18);
        UiKit.button(graphics, this.font, this.panelLeft + 14, cancelY, 90, 18,
                Component.translatable("langtonanimal.gui.cancel"), cancelHovered, true);

        if (hoveredName != null) {
            graphics.flush();
            UiKit.tooltip(graphics, this.font, hoveredName, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (UiKit.inside(mouseX, mouseY, this.panelLeft + 14, this.panelTop + PANEL_H - 26, 90, 18)) {
                this.minecraft.setScreen(this.parent);
                return true;
            }
            for (Cell cell : this.cells) {
                if (UiKit.inside(mouseX, mouseY, cell.x(), cell.y(), cell.w(), cell.h())) {
                    this.onPick.accept(cell.id());
                    this.minecraft.setScreen(this.parent);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}
