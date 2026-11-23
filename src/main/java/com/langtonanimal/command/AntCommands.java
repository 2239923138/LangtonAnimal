package com.langtonanimal.command;

import com.langtonanimal.config.AntConfig;
import com.langtonanimal.config.AntPreset;
import com.langtonanimal.config.ConfigManager;
import com.langtonanimal.config.ConfigValidator;
import com.langtonanimal.data.AntEntityData;
import com.langtonanimal.data.AntRegistry;
import com.langtonanimal.data.ModAttachments;
import com.langtonanimal.engine.AntActions;
import com.langtonanimal.engine.AntEngine;
import com.langtonanimal.network.ModNetwork;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * /langtonanimal 与简写 /langtona 的命令树。
 */
public final class AntCommands {

    private static final SuggestionProvider<CommandSourceStack> PRESET_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(presetNames(), builder);

    private static final SuggestionProvider<CommandSourceStack> TARGET_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(targetNames(context), builder);

    private static final SuggestionProvider<CommandSourceStack> FACING_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(
                    List.of("north", "south", "east", "west"), builder);

    private AntCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(build("langtonanimal"));
        dispatcher.register(build("langtona"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> build(String name) {
        return Commands.literal(name)
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("summon")
                        .then(Commands.argument("preset", StringArgumentType.word())
                                .suggests(PRESET_SUGGESTIONS)
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(ctx -> summon(ctx, Direction.NORTH, AntEntityData.DEFAULT_SPEED))
                                        .then(Commands.argument("facing", StringArgumentType.word())
                                                .suggests(FACING_SUGGESTIONS)
                                                .executes(ctx -> summon(ctx,
                                                        parseFacing(StringArgumentType.getString(ctx, "facing")),
                                                        AntEntityData.DEFAULT_SPEED))
                                                .then(Commands.argument("speed",
                                                                IntegerArgumentType.integer(1, AntEntityData.MAX_SPEED))
                                                        .executes(ctx -> summon(ctx,
                                                                parseFacing(StringArgumentType.getString(ctx, "facing")),
                                                                IntegerArgumentType.getInteger(ctx, "speed"))))))))
                .then(Commands.literal("start")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(TARGET_SUGGESTIONS)
                                .executes(ctx -> setRunning(ctx, true))))
                .then(Commands.literal("stop")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(TARGET_SUGGESTIONS)
                                .executes(ctx -> setRunning(ctx, false))))
                .then(Commands.literal("speed")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(TARGET_SUGGESTIONS)
                                .then(Commands.argument("speed",
                                                IntegerArgumentType.integer(1, AntEntityData.MAX_SPEED))
                                        .executes(AntCommands::setSpeed))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(TARGET_SUGGESTIONS)
                                .executes(AntCommands::remove)))
                .then(Commands.literal("restore")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(TARGET_SUGGESTIONS)
                                .executes(AntCommands::restore)))
                .then(Commands.literal("list").executes(AntCommands::list));
    }

    // ------------------------------------------------------------------ summon

    private static int summon(CommandContext<CommandSourceStack> ctx, Direction facing, int speed) {
        CommandSourceStack source = ctx.getSource();
        String presetName = StringArgumentType.getString(ctx, "preset");
        String entityName = StringArgumentType.getString(ctx, "name");
        AntConfig config = ConfigManager.get();
        AntPreset preset = config.findPreset(presetName);
        if (preset == null) {
            source.sendFailure(Component.translatable("langtonanimal.msg.preset_missing", presetName));
            return 0;
        }
        if (AntConfig.ALL.equalsIgnoreCase(entityName)) {
            source.sendFailure(Component.translatable("langtonanimal.val.name_reserved", entityName));
            return 0;
        }
        MinecraftServer server = source.getServer();
        AntRegistry registry = AntRegistry.get(server);
        if (registry.containsName(entityName) || config.findPreset(entityName) != null) {
            source.sendFailure(Component.translatable("langtonanimal.msg.name_taken", entityName));
            return 0;
        }
        if (!ConfigValidator.isEntityAllowed(preset.entityType())) {
            source.sendFailure(Component.translatable("langtonanimal.msg.entity_invalid", preset.entityType()));
            return 0;
        }
        ServerLevel level = source.getLevel();
        ResourceLocation typeId = ResourceLocation.tryParse(preset.entityType());
        EntityType<?> type = typeId == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(typeId).orElse(null);
        if (type == null) {
            source.sendFailure(Component.translatable("langtonanimal.msg.entity_invalid", preset.entityType()));
            return 0;
        }
        Entity created = type.create(level);
        if (!(created instanceof Mob mob)) {
            source.sendFailure(Component.translatable("langtonanimal.msg.entity_invalid", preset.entityType()));
            return 0;
        }
        BlockPos pos = BlockPos.containing(source.getPosition());
        mob.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, facing.toYRot(), 0.0F);

        AntEntityData data = new AntEntityData();
        data.setEntityName(entityName);
        data.setPresetName(preset.name());
        data.setFacing(facing);
        data.setSpeed(speed);
        data.setRunning(false);
        mob.setData(ModAttachments.ANT_DATA, data);
        mob.setCustomName(Component.literal(entityName));
        mob.setCustomNameVisible(true);

        if (!level.addFreshEntity(mob)) {
            source.sendFailure(Component.translatable("langtonanimal.msg.entity_invalid", preset.entityType()));
            return 0;
        }
        AntEngine.snapToGrid(mob, facing);
        AntEngine.applyGuards(mob, data);
        mob.syncData(ModAttachments.ANT_DATA);

        registry.add(new AntRegistry.AntRecord(mob.getUUID(), entityName, preset.name(), level.dimension(),
                pos, false, speed));
        ModNetwork.broadcast(server);
        final String finalName = entityName;
        final String finalPreset = preset.name();
        source.sendSuccess(() -> Component.translatable("langtonanimal.msg.summoned", finalName, finalPreset)
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    // ------------------------------------------------------------------- start

    private static int setRunning(CommandContext<CommandSourceStack> ctx, boolean running) {
        CommandSourceStack source = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "name");
        AntActions.Outcome outcome = AntActions.setRunning(source.getServer(), name, running);
        if (outcome.error() != null) {
            source.sendFailure(outcome.error());
            return 0;
        }
        String key = running ? "langtonanimal.msg.started_group" : "langtonanimal.msg.stopped_group";
        source.sendSuccess(() -> Component.translatable(key, name, outcome.affected())
                .withStyle(ChatFormatting.GREEN), true);
        return outcome.affected();
    }

    // ------------------------------------------------------------------- speed

    private static int setSpeed(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "name");
        int speed = IntegerArgumentType.getInteger(ctx, "speed");
        AntActions.Outcome outcome = AntActions.setSpeed(source.getServer(), name, speed);
        if (outcome.error() != null) {
            source.sendFailure(outcome.error());
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("langtonanimal.msg.speed_set", name, speed,
                outcome.affected()).withStyle(ChatFormatting.GREEN), true);
        return outcome.affected();
    }

    // ------------------------------------------------------------------ remove

    private static int remove(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "name");
        AntActions.Outcome outcome = AntActions.remove(source.getServer(), name);
        if (outcome.error() != null) {
            source.sendFailure(outcome.error());
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("langtonanimal.msg.removed", name, outcome.affected())
                .withStyle(ChatFormatting.GREEN), true);
        return outcome.affected();
    }

    // ----------------------------------------------------------------- restore

    private static int restore(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "name");
        AntActions.Outcome outcome = AntActions.restore(source.getServer(), name);
        if (outcome.error() != null) {
            source.sendFailure(outcome.error());
            return 0;
        }
        if (outcome.affected() == 0) {
            source.sendSuccess(() -> Component.translatable("langtonanimal.msg.restore_none", name)
                    .withStyle(ChatFormatting.GRAY), false);
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("langtonanimal.msg.restored", name, outcome.affected())
                .withStyle(ChatFormatting.GREEN), true);
        return outcome.affected();
    }

    // -------------------------------------------------------------------- list

    private static int list(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        MinecraftServer server = source.getServer();
        AntRegistry registry = AntRegistry.get(server);
        AntConfig config = ConfigManager.get();
        List<AntRegistry.AntRecord> records = new ArrayList<>(registry.all());
        if (records.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("langtonanimal.msg.list_empty")
                    .withStyle(ChatFormatting.GRAY), false);
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("langtonanimal.msg.list_header", records.size())
                .withStyle(ChatFormatting.GOLD), false);
        for (AntRegistry.AntRecord record : records) {
            ServerLevel level = server.getLevel(record.dimension);
            boolean loaded = level != null && level.isLoaded(record.pos);
            boolean alive = loaded && level.getEntity(record.uuid) != null;
            AntPreset preset = config.findPreset(record.presetName);
            int color = preset == null ? 0xFFFFFF : preset.color();
            Component line = Component.translatable("langtonanimal.msg.list_entry",
                            Component.literal(record.entityName).withStyle(ChatFormatting.WHITE),
                            Component.literal(record.presetName).withStyle(Style.EMPTY.withColor(color)),
                            Component.translatable(record.running
                                            ? "langtonanimal.state.running" : "langtonanimal.state.stopped")
                                    .withStyle(record.running ? ChatFormatting.GREEN : ChatFormatting.GRAY),
                            Component.literal(String.valueOf(record.speed)).withStyle(ChatFormatting.YELLOW),
                            Component.literal(record.pos.getX() + ", " + record.pos.getY() + ", " + record.pos.getZ())
                                    .withStyle(ChatFormatting.GRAY),
                            Component.translatable(loaded
                                            ? "langtonanimal.state.loaded" : "langtonanimal.state.unloaded")
                                    .withStyle(loaded ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED))
                    .withStyle(ChatFormatting.WHITE);
            if (!alive && loaded) {
                line = line.copy().append(Component.translatable("langtonanimal.state.missing")
                        .withStyle(ChatFormatting.DARK_RED));
            }
            final Component finalLine = line;
            source.sendSuccess(() -> finalLine, false);
        }
        return records.size();
    }

    // ----------------------------------------------------------------- helpers

    private static Direction parseFacing(String value) {
        return switch (value.toLowerCase()) {
            case "south" -> Direction.SOUTH;
            case "east" -> Direction.EAST;
            case "west" -> Direction.WEST;
            default -> Direction.NORTH;
        };
    }

    private static List<String> presetNames() {
        List<String> names = new ArrayList<>();
        for (AntPreset preset : ConfigManager.get().presets()) {
            names.add(preset.name());
        }
        return names;
    }

    private static List<String> targetNames(CommandContext<CommandSourceStack> context) {
        Set<String> names = new LinkedHashSet<>();
        names.add(AntConfig.ALL);
        names.addAll(presetNames());
        AntRegistry registry = AntRegistry.get(context.getSource().getServer());
        names.addAll(registry.names());
        return new ArrayList<>(names);
    }
}
