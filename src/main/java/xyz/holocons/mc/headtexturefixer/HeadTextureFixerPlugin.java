package xyz.holocons.mc.headtexturefixer;

import java.nio.charset.StandardCharsets;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import com.destroystokyo.paper.profile.ProfileProperty;
import com.dylibso.chicory.runtime.ExportFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.wasm.Parser;
import com.mojang.brigadier.Command;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;

public final class HeadTextureFixerPlugin extends JavaPlugin {

    private Module module;

    @Override
    public void onEnable() {
        try {
            this.module = new Module(this);
        } catch (Exception e) {
            getLogger().severe(e.getMessage());
            return;
        }

        final var manager = getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> registerCommands(event.registrar()));
    }

    private void registerCommands(final Commands registrar) {
        registrar.register(Commands.literal("fixhead")
                .executes(ctx -> fixHead(ctx.getSource().getSender(), null))
                .then(Commands.argument("component", ArgumentTypes.component())
                        .executes(ctx -> fixHead(ctx.getSource().getSender(),
                                ctx.getArgument("component", Component.class))))
                .build(),
                "Usage: /fixhead [Component]");
    }

    private int fixHead(final CommandSender sender, final Component component) {
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
                if (property.getName().equals("textures")) {
                    iterator.remove();
                    final var textures = module.fixTexture(property.getValue());
                    properties.add(new ProfileProperty("textures", textures));
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

    private static final class Module {

        private static final String MODULE_NAME = "normalize_texture.wasm";

        private final ExportFunction alloc;
        private final ExportFunction dealloc;
        private final ExportFunction normalizeTexture;
        private final Memory memory;

        public Module(final HeadTextureFixerPlugin plugin) throws Exception {
            final var stream = plugin.getResource(MODULE_NAME);
            final var module = Parser.parse(stream);
            final var instance = Instance.builder(module).build();
            this.alloc = instance.export("alloc");
            this.dealloc = instance.export("dealloc");
            this.normalizeTexture = instance.export("normalize_texture");
            this.memory = instance.memory();
        }

        private int alloc(final int length) {
            return (int) alloc.apply(length)[0];
        }

        private void dealloc(final int pointer, final int length) {
            dealloc.apply(pointer, length);
        }

        private int normalizeTexture(final int pointer, final int length) {
            return (int) normalizeTexture.apply(pointer, length)[0];
        }

        public String fixTexture(final String inputBase64) {
            final var inputLength = inputBase64.getBytes(StandardCharsets.UTF_8).length;
            final var pointer = alloc(inputLength);
            memory.writeString(pointer, inputBase64, StandardCharsets.UTF_8);
            final var outputLength = normalizeTexture(pointer, inputLength);
            final var outputBase64 = memory.readString(pointer, outputLength, StandardCharsets.UTF_8);
            dealloc(pointer, inputLength);
            return outputBase64;
        }
    }
}
