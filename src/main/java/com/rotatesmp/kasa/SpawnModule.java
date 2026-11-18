package com.rotatesmp.kasa;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** /spawn, /setspawn ve olunce her zaman spawn'da dogma. */
public final class SpawnModule implements CommandExecutor, TabCompleter, Listener {

    private final KasaPlugin plugin;
    private final File file;
    private Location spawn;

    public SpawnModule(KasaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "spawn.yml");
    }

    public void load() {
        spawn = null;
        if (file.exists()) {
            spawn = YamlConfiguration.loadConfiguration(file).getLocation("spawn");
        }
    }

    private void save() {
        try {
            plugin.getDataFolder().mkdirs();
            YamlConfiguration yml = new YamlConfiguration();
            yml.set("spawn", spawn);
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("spawn.yml kaydedilemedi: " + e.getMessage());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String name = cmd.getName().toLowerCase(Locale.ROOT);

        if (name.equals("setspawn")) {
            if (!sender.hasPermission(CrateManager.ADMIN_PERM)) {
                CrateManager.msg(sender, "<red>Bu komutu kullanma yetkin yok.");
                return true;
            }
            if (!(sender instanceof Player p)) {
                CrateManager.msg(sender, "<red>Bu komutu sadece oyuncular kullanabilir.");
                return true;
            }
            spawn = p.getLocation();
            p.getWorld().setSpawnLocation(spawn);
            save();
            CrateManager.msg(p, "<green>Spawn noktası bulunduğun yer olarak ayarlandı.");
            return true;
        }

        if (name.equals("spawn")) {
            if (spawn == null || spawn.getWorld() == null) {
                CrateManager.msg(sender, "<red>Spawn henüz ayarlanmamış. <gray>(/setspawn)");
                return true;
            }

            Player target;
            if (args.length >= 1 && sender.hasPermission(CrateManager.ADMIN_PERM)) {
                target = sender.getServer().getPlayerExact(args[0]);
                if (target == null) {
                    CrateManager.msg(sender, "<red>Oyuncu bulunamadı: <white><p>",
                            Placeholder.unparsed("p", args[0]));
                    return true;
                }
            } else if (sender instanceof Player self) {
                target = self;
            } else {
                CrateManager.msg(sender, "<red>Konsoldan kullanırken oyuncu adı yazmalısın.");
                return true;
            }

            target.teleport(spawn);
            CrateManager.msg(target, "<green>Spawn'a ışınlandın!");
            if (target != sender) {
                CrateManager.msg(sender, "<white><p> <gray>spawn'a ışınlandı.",
                        Placeholder.unparsed("p", target.getName()));
            }
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (cmd.getName().equalsIgnoreCase("spawn") && args.length == 1
                && sender.hasPermission(CrateManager.ADMIN_PERM)) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (Player p : sender.getServer().getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    out.add(p.getName());
                }
            }
        }
        return out;
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        if (spawn != null && spawn.getWorld() != null) {
            e.setRespawnLocation(spawn);
        }
    }
}
