package io.github.p1k0chu.criteria_sync;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
//? >=26.1
import net.minecraft.server.permissions.Permissions;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;

import static io.github.p1k0chu.criteria_sync.constants.AdvancementIDConstants.DEFAULT_BLOCKLIST;

public class CriteriaSync implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("criteria-sync");
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            //? >=26.1 {
            .setStrictness(com.google.gson.Strictness.LENIENT)
            //? } else
            //.setLenient()
            .create();

    @NonNull
    private static Set<String> advancementsBlocked = DEFAULT_BLOCKLIST;

    @Override
    public void onInitialize() throws RuntimeException {
        try {
            reloadBlockedAdvancements();
        } catch (IOException e) {
            LOGGER.error("Error while reloading blocklist", e);
        }

        CommandRegistrationCallback.EVENT.register((dispatcher, _, _) -> {
            dispatcher.register(Commands.literal("criteriasync")
                    .then(Commands.literal("reloadconfig")
                            //? >=26.1 {
                            .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                            //? } else
                            //.requires(source -> source.hasPermission(3))
                            .executes(ctx -> {
                                try {
                                    int n = reloadBlockedAdvancements();
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            String.format("Reloaded criteria-sync blocklist: %d entries", n)
                                    ), true);
                                    return 0;
                                } catch (IOException e) {
                                    ctx.getSource().sendFailure(Component.literal(
                                            String.format("Error while reloading blocklist: %s", e.getMessage())
                                    ));
                                    return 1;
                                }
                            })));
        });
    }

    private static int reloadBlockedAdvancements() throws IOException {
        Path configDir = FabricLoader.getInstance().getConfigDir();

        Path blockListPath = configDir.resolve("criteriasyncblocklist.json");
        File blockList = blockListPath.toFile();
        if (blockList.isDirectory()) {
            throw new IOException(String.format("\"%s\" is a directory!", blockList.getAbsolutePath()));
        }
        if (blockList.exists()) {
            var typeToken = new TypeToken<Collection<String>>() {
            };
            Collection<String> strings;
            try (BufferedReader reader = new BufferedReader(new FileReader(blockList))) {
                strings = GSON.fromJson(reader, typeToken);
            }
            strings.removeIf(Objects::isNull);
            advancementsBlocked = Set.copyOf(strings);
            return strings.size();
        } else {
            try (FileWriter writer = new FileWriter(blockList)) {
                GSON.toJson(DEFAULT_BLOCKLIST, writer);
            } catch (IOException e) {
                throw new IOException(String.format("Couldn't write to file \"%s\"", blockList.getAbsolutePath()), e);
            }

            advancementsBlocked = DEFAULT_BLOCKLIST;
            return DEFAULT_BLOCKLIST.size();
        }
    }

    public static boolean isAdvancementBlocked(String id) {
        return advancementsBlocked.contains(id);
    }
}
