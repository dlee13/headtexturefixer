package xyz.holocons.mc.headtexturefixer;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.io.File;
import java.lang.reflect.Field;
import java.util.Collection;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class PaperPlugin extends JavaPlugin {

    private static Field metaProfileField;

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
            final Player player = (Player) sender;
            final ItemStack mainHandItem = player.getInventory().getItemInMainHand();

            if (!(mainHandItem.getItemMeta() instanceof SkullMeta)) {
                player.sendMessage("Put the head in your main hand!");
                return false;
            }

            SkullMeta meta = (SkullMeta) mainHandItem.getItemMeta();
            mutateSkullMeta(meta);
            mainHandItem.setItemMeta(meta);

            return true;
        }

        return false;
    }

    private static void mutateSkullMeta(SkullMeta meta) {
        try {
            if (metaProfileField == null) {
                metaProfileField = meta.getClass().getDeclaredField("profile");
                metaProfileField.setAccessible(true);
            }

            GameProfile profile = (GameProfile) metaProfileField.get(meta);

            final Collection<Property> textures = profile.getProperties().get("textures");
            if (!textures.isEmpty()) {
                String base64 = textures.iterator().next().getValue();
                textures.clear();
                profile.getProperties().put("textures", new Property("textures", Native.normalizeTexture(base64)));
            }
        } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException e) {
            e.printStackTrace();
        }
    }
}
