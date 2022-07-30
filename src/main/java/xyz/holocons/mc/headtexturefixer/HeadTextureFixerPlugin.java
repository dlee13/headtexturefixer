package xyz.holocons.mc.headtexturefixer;

import java.io.File;
import java.util.HexFormat;

import com.destroystokyo.paper.profile.ProfileProperty;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

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
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("fixhead") && sender instanceof final Player player) {
            return fixHead(player, args);
        }

        return false;
    }

    private static boolean fixHead(Player player, String[] args) {
        final var item = player.getInventory().getItemInMainHand();

        if (item.getItemMeta() instanceof final SkullMeta meta) {
            final var profile = meta.getPlayerProfile();
            if (profile == null) {
                return true;
            }

            if (args.length > 0 && args[0].charAt(0) == '#') {
                final var joinedArgs = String.join(" ", args);
                String name;
                int color;
                try {
                    name = joinedArgs.substring(7);
                    color = HexFormat.fromHexDigits(joinedArgs, 1, 7);
                } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
                    return false;
                }
                final var component = Component.text(name)
                    .color(TextColor.color(color))
                    .decoration(TextDecoration.ITALIC, false);
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
                    return true;
                }
            }
        } else {
            player.sendMessage("Put the head in your main hand!");
        }
        return true;
    }
}
