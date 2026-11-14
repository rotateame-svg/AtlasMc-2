package com.rotatesmp.kasa;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** /evler : 5 slotluk ev (home) menusu. */
public final class HomeModule implements CommandExecutor, Listener {

    private static final int HOME_COUNT = 5;
    private static final int[] SLOTS = {11, 12, 13, 14, 15};

    private record Home(String world, double x, double y, double z, float yaw, float pitch) {
        Location toLocation() {
            World w = Bukkit.getWorld(world);
            return w == null ? null : new Location(w, x, y, z, yaw, pitch);
        }
    }

    private static final class HomeMenu implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private final KasaPlugin plugin;
    private final File file;
    private final Map<UUID, Home[]> homes = new HashMap<>();

    public HomeModule(KasaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "homes.yml");
    }

    // ---------- kayit ----------

    public void load() {
        homes.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yml.getConfigurationSection("homes");
        if (root == null) {
            return;
        }
        for (String uuidStr : root.getKeys(false)) {
            UUID id;
            try {
                id = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                continue;
            }
            ConfigurationSection player = root.getConfigurationSection(uuidStr);
            if (player == null) {
                continue;
            }
            Home[] arr = new Home[HOME_COUNT];
            for (int i = 0; i < HOME_COUNT; i++) {
                ConfigurationSection h = player.getConfigurationSection(String.valueOf(i + 1));
                if (h == null || h.getString("world") == null) {
                    continue;
                }
                arr[i] = new Home(h.getString("world"), h.getDouble("x"), h.getDouble("y"), h.getDouble("z"),
                        (float) h.getDouble("yaw"), (float) h.getDouble("pitch"));
            }
            homes.put(id, arr);
        }
    }

    private void save() {
        YamlConfiguration yml = new YamlConfiguration();
        for (Map.Entry<UUID, Home[]> entry : homes.entrySet()) {
            for (int i = 0; i < HOME_COUNT; i++) {
                Home h = entry.getValue()[i];
                if (h == null) {
                    continue;
                }
                String path = "homes." + entry.getKey() + "." + (i + 1) + ".";
                yml.set(path + "world", h.world());
                yml.set(path + "x", h.x());
                yml.set(path + "y", h.y());
                yml.set(path + "z", h.z());
                yml.set(path + "yaw", h.yaw());
                yml.set(path + "pitch", h.pitch());
            }
        }
        try {
            plugin.getDataFolder().mkdirs();
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("homes.yml kaydedilemedi: " + e.getMessage());
        }
    }

    private Home[] homesOf(Player p) {
        return homes.computeIfAbsent(p.getUniqueId(), k -> new Home[HOME_COUNT]);
    }

    // ---------- komut ----------

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            CrateManager.msg(sender, "<red>Bu komutu sadece oyuncular kullanabilir.");
            return true;
        }
        openMenu(p);
        return true;
    }

    // ---------- menu ----------

    private static Component line(String mini, TagResolver... resolvers) {
        return CrateManager.MM.deserialize("<!italic>" + mini, resolvers);
    }

    private static ItemStack item(Material material, Component name, List<Component> lore) {
        ItemStack it = new ItemStack(material);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(name);
        if (lore != null) {
            meta.lore(lore);
        }
        it.setItemMeta(meta);
        return it;
    }

    private static String worldLabel(World w) {
        if (w == null) {
            return "Bilinmeyen dünya";
        }
        return switch (w.getEnvironment()) {
            case NORMAL -> "Dünya";
            case NETHER -> "Nether";
            case THE_END -> "End";
            default -> w.getName();
        };
    }

    private void render(Inventory inv, Player p) {
        ItemStack pane = item(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "), null);
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, pane);
        }
        Home[] arr = homesOf(p);
        for (int i = 0; i < HOME_COUNT; i++) {
            TagResolver n = Placeholder.unparsed("n", String.valueOf(i + 1));
            Home h = arr[i];
            if (h == null) {
                List<Component> lore = new ArrayList<>();
                lore.add(line("<gray>Bu slot boş."));
                lore.add(Component.empty());
                lore.add(line("<yellow>Tıkla: <white>Evi bulunduğun yere kur"));
                inv.setItem(SLOTS[i], item(Material.GRAY_BED, line("<gray><bold>Ev <n> <dark_gray>(Boş)", n), lore));
            } else {
                List<Component> lore = new ArrayList<>();
                lore.add(line("<gray>Konum: <white><w>", Placeholder.unparsed("w", worldLabel(Bukkit.getWorld(h.world())))));
                lore.add(line("<gray>X: <white><x> <gray>Y: <white><y> <gray>Z: <white><z>",
                        Placeholder.unparsed("x", String.valueOf((int) Math.floor(h.x()))),
                        Placeholder.unparsed("y", String.valueOf((int) Math.floor(h.y()))),
                        Placeholder.unparsed("z", String.valueOf((int) Math.floor(h.z())))));
                lore.add(Component.empty());
                lore.add(line("<yellow>Sol tık: <white>Işınlan"));
                lore.add(line("<yellow>Sağ tık: <white>Evi buraya taşı"));
                lore.add(line("<red>Shift + sağ tık: <white>Evi sil"));
                inv.setItem(SLOTS[i], item(Material.RED_BED, line("<green><bold>Ev <n>", n), lore));
            }
        }
    }

    public void openMenu(Player p) {
        HomeMenu holder = new HomeMenu();
        Inventory inv = Bukkit.createInventory(holder, 27, CrateManager.MM.deserialize("<gold><bold>Evlerin"));
        holder.inventory = inv;
        render(inv, p);
        p.openInventory(inv);
    }

    private static int indexOf(int slot) {
        for (int i = 0; i < SLOTS.length; i++) {
            if (SLOTS[i] == slot) {
                return i;
            }
        }
        return -1;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder(false) instanceof HomeMenu menu)) {
            return;
        }
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getClickedInventory() != e.getView().getTopInventory()) {
            return;
        }
        int idx = indexOf(e.getSlot());
        if (idx < 0) {
            return;
        }

        Home[] arr = homesOf(p);
        Home h = arr[idx];
        ClickType click = e.getClick();

        if (h == null) {
            setHome(p, arr, idx);
            render(menu.inventory, p);
            return;
        }

        switch (click) {
            case SHIFT_RIGHT -> {
                arr[idx] = null;
                save();
                p.playSound(p.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                CrateManager.msg(p, "<yellow>Ev <n> silindi.", Placeholder.unparsed("n", String.valueOf(idx + 1)));
                render(menu.inventory, p);
            }
            case RIGHT -> {
                setHome(p, arr, idx);
                render(menu.inventory, p);
            }
            case LEFT, SHIFT_LEFT -> {
                Location loc = h.toLocation();
                if (loc == null) {
                    CrateManager.msg(p, "<red>Bu evin bulunduğu dünya şu an yok.");
                    return;
                }
                p.closeInventory();
                p.teleportAsync(loc);
                p.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                CrateManager.msg(p, "<green>Ev <n>'e ışınlandın!", Placeholder.unparsed("n", String.valueOf(idx + 1)));
            }
            default -> {
            }
        }
    }

    private void setHome(Player p, Home[] arr, int idx) {
        Location l = p.getLocation();
        arr[idx] = new Home(l.getWorld().getName(), l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch());
        save();
        p.playSound(l, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
        CrateManager.msg(p, "<green>Ev <n> bulunduğun yere kuruldu!", Placeholder.unparsed("n", String.valueOf(idx + 1)));
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof HomeMenu) {
            e.setCancelled(true);
        }
    }
}
