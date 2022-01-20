package xyz.holocons.mc.headtexturefixer;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import java.io.File;
import java.util.Iterator;
import java.util.Set;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class PaperPlugin extends JavaPlugin {

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
        if (command.getName().equalsIgnoreCase("fixhead") && sender instanceof Player) {
            fixHead((Player) sender);
            return true;
        }

        return false;
    }

    private static void fixHead(Player player) {
        final ItemStack mainHandItem = player.getInventory().getItemInMainHand();

        if (mainHandItem.getItemMeta() instanceof SkullMeta) {
            final SkullMeta meta = (SkullMeta) mainHandItem.getItemMeta();
            final PlayerProfile profile = meta.getPlayerProfile();
            if (profile == null) {
                return;
            }

            final Set<ProfileProperty> properties = profile.getProperties();
            final Iterator<ProfileProperty> iterator = properties.iterator();
            while (iterator.hasNext()) {
                final ProfileProperty property = iterator.next();
                if (property.getName().matches("textures")) {
                    iterator.remove();
                    properties.add(new ProfileProperty("textures", Native.normalizeTexture(property.getValue())));
                    meta.setPlayerProfile(profile);
                    mainHandItem.setItemMeta(meta);
                    return;
                }
            }
        } else {
            player.sendMessage("Put the head in your main hand!");
        }
    }
}
