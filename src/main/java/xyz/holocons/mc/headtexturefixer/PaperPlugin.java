package xyz.holocons.mc.headtexturefixer;

import com.destroystokyo.paper.profile.ProfileProperty;
import java.io.File;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

public final class PaperPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        this.saveResource(Native.LIBRARY_NAME, false);
        this.getLogger().info("Loading " + Native.LIBRARY_NAME);
        try {
            System.load(this.getDataFolder().getAbsolutePath() + File.separator + Native.LIBRARY_NAME);
        } catch (SecurityException | UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String args[]) {
        if (command.getName().equalsIgnoreCase("fixhead") && sender instanceof final Player player) {
            fixHead(player);
            return true;
        }

        return false;
    }

    private static void fixHead(Player player) {
        final var item = player.getInventory().getItemInMainHand();

        if (item.getItemMeta() instanceof final SkullMeta meta) {
            final var profile = meta.getPlayerProfile();
            if (profile == null) {
                return;
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
                    return;
                }
            }
        } else {
            player.sendMessage("Put the head in your main hand!");
        }
    }
}
