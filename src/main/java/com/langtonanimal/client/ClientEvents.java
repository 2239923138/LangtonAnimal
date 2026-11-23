package com.langtonanimal.client;

import com.langtonanimal.client.gui.PresetListScreen;
import com.langtonanimal.client.gui.PresetPickerScreen;
import com.langtonanimal.data.AntEntityData;
import com.langtonanimal.data.ModAttachments;
import com.langtonanimal.network.AdjustSpeedPayload;
import com.langtonanimal.network.SetRunningPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 客户端事件：快捷键、烈焰棒交互、名称显示、高亮与 HUD。
 */
public final class ClientEvents {

    private ClientEvents() {
    }

    /** 名称显示用：主手或副手都算。 */
    public static boolean holdingRod(Player player) {
        return player.getMainHandItem().is(Items.BLAZE_ROD)
                || player.getOffhandItem().is(Items.BLAZE_ROD);
    }

    /** 交互用：只有主手烈焰棒才触发模式操作。 */
    public static boolean holdingRodMain(Player player) {
        return player.getMainHandItem().is(Items.BLAZE_ROD);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (LangtonAnimalClient.OPEN_GUI.consumeClick()) {
            if (minecraft.screen == null && minecraft.player != null) {
                minecraft.setScreen(new PresetListScreen());
            }
        }
    }

    // --------------------------------------------------------------- 名称显示

    public static void onRenderNameTag(RenderNameTagEvent event) {
        Entity entity = event.getEntity();
        AntEntityData data = entity.getExistingDataOrNull(ModAttachments.ANT_DATA);
        if (data == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        if (!holdingRod(minecraft.player)) {
            event.setCanRender(TriState.FALSE);
            return;
        }
        int color = ClientState.colorOf(data.presetName());
        Component label = Component.literal(data.entityName() + " ")
                .append(Component.literal("[" + data.presetName() + "]").withStyle(Style.EMPTY.withColor(color)));
        event.setContent(label);
        event.setCanRender(TriState.TRUE);
    }

    // --------------------------------------------------------------- 滚轮交互

    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || minecraft.player == null) {
            return;
        }
        if (!holdingRodMain(minecraft.player)) {
            return;
        }
        double delta = event.getScrollDeltaY();
        if (delta == 0) {
            return;
        }
        int direction = delta > 0 ? 1 : -1;
        if (Screen.hasControlDown()) {
            RodState.cycleMode(direction);
            event.setCanceled(true);
            return;
        }
        if (Screen.hasAltDown()) {
            RodState.cycleScope(direction);
            event.setCanceled(true);
            return;
        }
        if (Screen.hasShiftDown() && RodState.mode() == RodState.MODE_SPEED) {
            String target = RodState.targetName();
            if (target != null) {
                PacketDistributor.sendToServer(new AdjustSpeedPayload(target, direction));
                event.setCanceled(true);
            }
        }
    }

    // --------------------------------------------------------------- 右键交互

    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || minecraft.player == null || minecraft.level == null) {
            return;
        }
        if (!holdingRodMain(minecraft.player)) {
            return;
        }
        Entity target = minecraft.crosshairPickEntity;
        if (target != null && target != minecraft.player) {
            if (handleEntityRightClick(minecraft, target)) {
                event.setCanceled(true);
            }
            return;
        }
        if (RodState.mode() == RodState.MODE_RUN && RodState.selectedData() != null) {
            toggleRunning();
            event.setCanceled(true);
        }
    }

    private static boolean handleEntityRightClick(Minecraft minecraft, Entity target) {
        AntEntityData data = target.getExistingDataOrNull(ModAttachments.ANT_DATA);
        if (RodState.mode() == RodState.MODE_EDIT) {
            String name = data == null ? null : data.entityName();
            boolean wasRunning = data != null && data.running();
            if (wasRunning && name != null) {
                PacketDistributor.sendToServer(new SetRunningPayload(name, false));
            }
            RodState.beginEdit(name, wasRunning);
            minecraft.setScreen(new PresetPickerScreen(target.getUUID(), data == null));
            return true;
        }
        if (data == null) {
            return false;
        }
        RodState.select(target);
        return true;
    }

    private static void toggleRunning() {
        AntEntityData data = RodState.selectedData();
        String target = RodState.targetName();
        if (data == null || target == null) {
            return;
        }
        PacketDistributor.sendToServer(new SetRunningPayload(target, !data.running()));
    }

    // --------------------------------------------------------------- 渲染

    public static void onRenderGui(RenderGuiEvent.Post event) {
        RodHud.onRenderGui(event);
    }

    public static void onRenderLevel(RenderLevelStageEvent event) {
        HighlightRenderer.onRenderLevel(event);
    }
}
