package io.github.lithum12.trackertips.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.github.lithum12.trackertips.config.TTConfigManager;
import io.github.lithum12.trackertips.engine.HintEngine;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class TTCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("trackertips")
                // /trackertips reload
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(2)) // 需要 OP 权限
                        .executes(TTCommands::executeReload))
                // /trackertips test <id>
                .then(Commands.literal("test")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("id", ResourceLocationArgument.id())
                                .suggests(suggestIds())
                                .executes(TTCommands::executeTest)))
        );
    }

    // 提供 ID 自动补全建议
    private static SuggestionProvider<CommandSourceStack> suggestIds() {
        return (context, builder) -> SharedSuggestionProvider.suggest(
                TTConfigManager.hints().stream().map(h -> h.id().toString()),
                builder
        );
    }

    private static int executeReload(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        TTConfigManager.reload(source.getServer());
        source.sendSuccess(() -> Component.translatable("trackertips.command.reload.success", TTConfigManager.hints().size())
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int executeTest(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        if (player == null) {
            source.sendFailure(Component.translatable("trackertips.command.test.player_only").withStyle(ChatFormatting.RED));
            return 0;
        }

        ResourceLocation id = ResourceLocationArgument.getId(context, "id");
        HintEngine.forceShow(player, id);
        return 1;
    }
}