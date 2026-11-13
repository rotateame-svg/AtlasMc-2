package com.rotatesmp.kasa;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class CrateManager {

    public static final String ADMIN_PERM = "rotatekasa.admin";
    public static final MiniMessage MM = MiniMessage.miniMessage();
    public static final Component PREFIX = MM.deserialize("<dark_gray>[<gold>Kasa<dark_gray>] ");

    private final KasaPlugin plugin;
    private final NamespacedKey keyTag;
    private final NamespacedKey holoTag;
    private final File file;
    private final Set<String> crates = new HashSet<>();
    private final Map<UUID, CrateRoll> rolls = new LinkedHashMap<>();

    public CrateManager(KasaPlugin plugin) {
        this.plugin = plugin;
        this.keyTag = new NamespacedKey(plugin, "kasa_anahtar");
        this.holoTag = new NamespacedKey(plugin, "kasa_yazi");
        this.file = new File(plugin.getDataFolder(), "crates.yml");
    }

    public KasaPlugin plugin() {
        return plugin;
    }

    // ---------- mesaj ----------

    public static void msg(CommandSender to, String mini, TagResolver... resolvers) {
        to.sendMessage(PREFIX.append(MM.deserialize(mini, resolvers)));
    }

    // ---------- kasa kaydi ----------

    public static String keyOf(Location l) {
        return l.getWorld().getName() + ";" + l.getBlockX() + ";" + l.getBlockY() + ";" + l.getBlockZ();
    }

    public boolean isCrate(Block b) {
        if (b == null || !Tag.SHULKER_BOXES.isTagged(b.getType())) {
            return false;
        }
        return crates.contains(keyOf(b.getLocation()));
    }

    public void register(Block b) {
        crates.add(keyOf(b.getLocation()));
        save();
        removeHologram(b);
        spawnHologram(b);
    }

    public void unregister(Block b) {
        crates.remove(keyOf(b.getLocation()));
        save();
        removeHologram(b);
    }

    // ---------- kasa ustu yazi (hologram) ----------

    private static final String HOLO_TEXT =
            "<gold><bold>Atlas Mc Kasalar<newline>"
                    + "<gray>Önizleme İçin <yellow>Sol Tıklayın<newline>"
                    + "<gray>Açmak İçin <green>Sağ Tık";

    private Location holoLocation(Block b) {
        return b.getLocation().add(0.5, 1.6, 0.5);
    }

    private List<Entity> findHolograms(Block b) {
        List<Entity> found = new ArrayList<>();
        Location center = holoLocation(b);
        World world = center.getWorld();
        if (world == null) {
            return found;
        }
        for (Entity e : world.getNearbyEntities(center, 1.0, 2.0, 1.0)) {
            if (e instanceof TextDisplay
                    && e.getPersistentDataContainer().has(holoTag, PersistentDataType.BYTE)) {
                found.add(e);
            }
        }
        return found;
    }

    public void spawnHologram(Block b) {
        Location loc = holoLocation(b);
        if (loc.getWorld() == null) {
            return;
        }
        loc.getWorld().spawn(loc, TextDisplay.class, td -> {
            td.text(MM.deserialize(HOLO_TEXT));
            td.setBillboard(Display.Billboard.CENTER);
            td.setAlignment(TextDisplay.TextAlignment.CENTER);
            td.setShadowed(true);
            td.setSeeThrough(false);
            td.setBackgroundColor(Color.fromARGB(90, 0, 0, 0));
            td.setPersistent(true);
            td.getPersistentDataContainer().set(holoTag, PersistentDataType.BYTE, (byte) 1);
        });
    }

    public void removeHologram(Block b) {
        for (Entity e : findHolograms(b)) {
            e.remove();
        }
    }

    /** Sunucu acilinca eksik yazilari tamamlar (zaten varsa dokunmaz). */
    public void ensureAllHolograms() {
        for (String key : new ArrayList<>(crates)) {
            String[] parts = key.split(";");
            if (parts.length != 4) {
                continue;
            }
            World world = Bukkit.getWorld(parts[0]);
            if (world == null) {
                continue;
            }
            try {
                Block b = world.getBlockAt(Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
                world.getChunkAt(b);
                if (Tag.SHULKER_BOXES.isTagged(b.getType()) && findHolograms(b).isEmpty()) {
                    spawnHologram(b);
                }
            } catch (NumberFormatException ignored) {
                // bozuk kayit, atla
            }
        }
    }

    public int count() {
        return crates.size();
    }

    public void load() {
        crates.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        crates.addAll(yml.getStringList("crates"));
    }

    public void save() {
        try {
            plugin.getDataFolder().mkdirs();
            YamlConfiguration yml = new YamlConfiguration();
            yml.set("crates", new ArrayList<>(crates));
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("crates.yml kaydedilemedi: " + e.getMessage());
        }
    }

    // ---------- anahtar ----------

    public ItemStack createKey(int amount) {
        ItemStack item = new ItemStack(Material.TRIPWIRE_HOOK, amount);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MM.deserialize("<!italic><gold><bold>Kasa Anahtarı"));
        meta.lore(List.of(
                MM.deserialize("<!italic><gray>Spawn'daki kasayı açmak için"),
                MM.deserialize("<!italic><gray>kasaya sağ tıkla.")
        ));
        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(keyTag, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isKey(ItemStack item) {
        if (item == null || item.getType() != Material.TRIPWIRE_HOOK || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(keyTag, PersistentDataType.BYTE);
    }

    // ---------- animasyon takibi ----------

    public boolean isRolling(Player p) {
        return rolls.containsKey(p.getUniqueId());
    }

    public void startRoll(Player p, List<ItemStack> pool) {
        CrateRoll roll = new CrateRoll(this, p, pool);
        rolls.put(p.getUniqueId(), roll);
        roll.start();
    }

    void removeRoll(UUID id) {
        rolls.remove(id);
    }

    public CrateRoll getRoll(UUID id) {
        return rolls.get(id);
    }

    public void finishAllRolls() {
        for (CrateRoll r : new ArrayList<>(rolls.values())) {
            r.finishNow();
        }
        rolls.clear();
    }
}
