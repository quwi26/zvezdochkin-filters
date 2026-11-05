/*
 * Порт Fokuso (https://github.com/QazCetelic/Fokuso), © QazCetelic, LGPL-3.0.
 * Адаптация для Minecraft 1.21.1 - zvezdochkin.
 */
package ru.zvezdochkin.filters.client;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/**
 * Клиентская точка входа: первичная загрузка фильтров и команды.
 */
@Environment(EnvType.CLIENT)
public final class ZvezdochkinFiltersClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ChatFilterSystem.reload();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            var root = literal("chatfilters")
                    .then(literal("list").executes(context -> {
                        List<ChatFilterGroup> groups = ChatFilterSystem.getGroups();
                        if (groups.isEmpty()) {
                            context.getSource().sendFeedback(
                                    Component.translatable("command.zvezdochkin-filters.list.empty"));
                        } else {
                            String text = groups.stream()
                                    .map(ChatFilterGroup::toString)
                                    .collect(Collectors.joining(", "));
                            context.getSource().sendFeedback(Component.literal(text));
                        }
                        return 1;
                    }))
                    .then(literal("reload").executes(context -> {
                        ChatFilterSystem.reload();
                        context.getSource().sendFeedback(Component.translatable(
                                "command.zvezdochkin-filters.reload",
                                ChatFilterSystem.getGroups().size()));
                        return 1;
                    }))
                    .then(literal("toggle")
                            .then(argument("list", StringArgumentType.word())
                                    .suggests((context, builder) -> suggestGroups(builder))
                                    .then(argument("enabled", BoolArgumentType.bool())
                                            .executes(context -> {
                                                String listName = StringArgumentType.getString(context, "list");
                                                boolean enabled = BoolArgumentType.getBool(context, "enabled");
                                                Optional<ChatFilterGroup> group = ChatFilterSystem.findGroup(listName);

                                                if (group.isEmpty()) {
                                                    context.getSource().sendError(Component.translatable(
                                                            "command.zvezdochkin-filters.toggle.unknown",
                                                            listName));
                                                    return 0;
                                                }

                                                group.get().setEnabled(enabled);
                                                context.getSource().sendFeedback(Component.translatable(
                                                        enabled
                                                                ? "command.zvezdochkin-filters.toggle.on"
                                                                : "command.zvezdochkin-filters.toggle.off",
                                                        listName));
                                                return 1;
                                            }))));

            dispatcher.register(root);
        });
    }

    private static CompletableFuture<Suggestions> suggestGroups(SuggestionsBuilder builder) {
        ChatFilterSystem.getGroups().forEach(group -> builder.suggest(group.getName()));
        return builder.buildFuture();
    }
}
