package io.github.lithum12.trackertips.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.github.lithum12.trackertips.TrackerTips;
import io.github.lithum12.trackertips.config.TTConfigManager;
import io.github.lithum12.trackertips.engine.HintEngine;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.versions.forge.ForgeVersion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Registers and implements {@code /trackertips} (and its optional {@code /tt} shorthand).
 *
 * <p>Every message this class sends is a {@link Component#translatable} lookup - see the
 * {@code trackertips.command.*} lang keys - so the whole command surface follows the invoking
 * player's client language like the rest of the mod, rather than being hard-coded to English.
 */
public class TTCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(buildRoot("trackertips"));

        // Feature: optional "/tt" shorthand. The subtree is always registered (Brigadier has no
        // clean way to register/unregister a literal after RegisterCommandsEvent has already
        // fired), but .requires() is re-evaluated on every parse/suggestion attempt, so the
        // "shortcut command" toggle in the mod's UI takes effect immediately without a restart:
        // when it's off, "/tt" behaves as if it doesn't exist (no tab-complete, "unknown command").
        dispatcher.register(buildRoot("tt")
                .requires(source -> TTConfigManager.settings().shortcutCommand));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildRoot(String name) {
        return Commands.literal(name)
                // /<root> reload
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(2)) // Requires OP permission
                        .executes(TTCommands::executeReload))
                // /<root> test <id>
                .then(Commands.literal("test")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("id", ResourceLocationArgument.id())
                                .suggests(suggestIds())
                                .executes(TTCommands::executeTest)))
                // /<root> about
                .then(Commands.literal("about")
                        .executes(TTCommands::executeAbout))
                // /<root> list [global|saves]  (defaults to "saves" with no argument)
                .then(Commands.literal("list")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> executeList(ctx, ListScope.SAVES))
                        .then(Commands.literal("global")
                                .executes(ctx -> executeList(ctx, ListScope.GLOBAL)))
                        .then(Commands.literal("saves")
                                .executes(ctx -> executeList(ctx, ListScope.SAVES))));
    }

    // Provides ID auto-complete suggestions
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

    private static int executeAbout(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        String displayName = "TrackerTips";
        String version = null;
        try {
            IModInfo info = ModList.get().getModContainerById(TrackerTips.MODID)
                    .map(container -> container.getModInfo())
                    .orElse(null);
            if (info != null) {
                displayName = info.getDisplayName();
                version = info.getVersion().toString();
            }
        } catch (Exception e) {
            TrackerTips.LOGGER.warn("Failed to resolve TrackerTips mod info for /trackertips about", e);
        }

        String mcVersion = null;
        String forgeVersion = null;
        try {
            mcVersion = SharedConstants.getCurrentVersion().getName();
        } catch (Exception ignored) {}
        try {
            forgeVersion = ForgeVersion.getVersion();
        } catch (Exception ignored) {}

        final String finalDisplayName = displayName;
        final Component versionComponent = version != null
                ? Component.literal(version)
                : Component.translatable("trackertips.command.about.unknown");
        final Component mcVersionComponent = mcVersion != null
                ? Component.literal(mcVersion)
                : Component.translatable("trackertips.command.about.unknown");
        final Component forgeVersionComponent = forgeVersion != null
                ? Component.literal(forgeVersion)
                : Component.translatable("trackertips.command.about.unknown");

        source.sendSuccess(() -> Component.translatable("trackertips.command.about.header", finalDisplayName, versionComponent)
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        source.sendSuccess(() -> Component.translatable("trackertips.command.about.versions", mcVersionComponent, forgeVersionComponent)
                .withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.translatable("trackertips.command.about.hints", TTConfigManager.hints().size())
                .withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.translatable("trackertips.command.about.modid", TrackerTips.MODID)
                .withStyle(ChatFormatting.DARK_GRAY), false);
        return 1;
    }

    private enum ListScope { GLOBAL, SAVES }

    private static int executeList(CommandContext<CommandSourceStack> context, ListScope scope) {
        CommandSourceStack source = context.getSource();

        Path folder = scope == ListScope.GLOBAL
                ? TTConfigManager.globalFolder()
                : TTConfigManager.worldFolder(source.getServer());
        Path settingsFile = folder.resolve(scope == ListScope.GLOBAL ? "global_config.json" : "world_config.json");
        Path hintsFolder = folder.resolve("hints");
        Component scopeLabel = Component.translatable(scope == ListScope.GLOBAL
                ? "trackertips.command.list.scope.global"
                : "trackertips.command.list.scope.saves");

        source.sendSuccess(() -> Component.translatable("trackertips.command.list.header", scopeLabel)
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        source.sendSuccess(() -> Component.translatable("trackertips.command.list.path", folder.toString())
                .withStyle(ChatFormatting.DARK_GRAY), false);

        boolean settingsExists = Files.exists(settingsFile);
        String settingsName = settingsFile.getFileName().toString();
        source.sendSuccess(() -> Component.translatable(
                        settingsExists ? "trackertips.command.list.settings_found" : "trackertips.command.list.settings_missing",
                        settingsName)
                .withStyle(settingsExists ? ChatFormatting.GREEN : ChatFormatting.RED), false);

        List<String> hintFiles;
        try (Stream<Path> list = Files.isDirectory(hintsFolder) ? Files.list(hintsFolder) : Stream.empty()) {
            hintFiles = list.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .toList();
        } catch (IOException e) {
            source.sendFailure(Component.translatable("trackertips.command.list.read_failed", hintsFolder.toString())
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        if (hintFiles.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("trackertips.command.list.hints_empty").withStyle(ChatFormatting.GRAY), false);
        } else {
            source.sendSuccess(() -> Component.translatable("trackertips.command.list.hints_header", hintFiles.size())
                    .withStyle(ChatFormatting.GRAY), false);
            for (String name : hintFiles) {
                source.sendSuccess(() -> Component.translatable("trackertips.command.list.hint_entry", name)
                        .withStyle(ChatFormatting.WHITE), false);
            }
        }
        return 1;
    }
}
