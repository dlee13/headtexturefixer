package xyz.holocons.mc.headtexturefixer;

import java.io.File;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import com.destroystokyo.paper.profile.ProfileProperty;
import com.mojang.brigadier.Command;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;

public final class HeadTextureFixerPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        final var config = getConfig();

        saveResource(Native.LIBRARY_NAME, config.getBoolean("replace-native"));
        getLogger().info("Loading " + Native.LIBRARY_NAME);
        try {
            System.load(getDataFolder().getAbsolutePath() + File.separator + Native.LIBRARY_NAME);
        } catch (SecurityException | UnsatisfiedLinkError e) {
            e.printStackTrace();
        }

        final var manager = getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> registerCommands(event.registrar()));
    }

    private static void registerCommands(final Commands registrar) {
        registrar.register(Commands.literal("fixhead")
                .executes(ctx -> fixHead(ctx.getSource().getSender(), null))
                .then(Commands.argument("component", ArgumentTypes.component()))
                .executes(ctx -> fixHead(ctx.getSource().getSender(), ctx.getArgument("component", Component.class)))
                .build(),
                "Usage: /fixhead [Component]");
    }

    private static int fixHead(final CommandSender sender, final Component component) {
        if (!(sender instanceof Player player)) {
            return 0;
        }

        final var item = player.getInventory().getItemInMainHand();
        if (item.getItemMeta() instanceof final SkullMeta meta) {
            final var profile = meta.getPlayerProfile();
            if (profile == null) {
                return Command.SINGLE_SUCCESS;
            }

            if (component != null) {
                meta.displayName(component);
            }

            final var properties = profile.getProperties();
            final var iterator = properties.iterator();
            while (iterator.hasNext()) {
                final var property = iterator.next();
                if (property.getName().matches("textures")) {
                    iterator.remove();
                    properties.add(new ProfileProperty("textures", Native.normalizeTexture(property.getValue())));
                    meta.setPlayerProfile(profile);
                    item.setItemMeta(meta);
                    break;
                }
            }
        } else {
            player.sendMessage("Put the head in your main hand!");
        }
        return Command.SINGLE_SUCCESS;
    }
}
