package com.rotatesmp.kasa;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** /rtp, /artp (menu), /rtpnpcolustur, /rtpnpcsil ve RTP NPC'si. */
public final class RtpModule implements CommandExecutor, Listener {

    private static final int SLOT_OVERWORLD = 11;
    private static final int SLOT_NETHER = 13;
    private static final int SLOT_END = 15;

    private static final Set<Material> UNSAFE = EnumSet.of(
            Material.LAVA, Material.WATER, Material.MAGMA_BLOCK, Material.CACTUS,
            Material.FIRE, Material.SOUL_FIRE, Material.CAMPFIRE, Material.SOUL_CAMPFIRE,
            Material.SWEET_BERRY_BUSH, Material.POWDER_SNOW, Material.WITHER_ROSE,
            Material.POINTED_DRIPSTONE, Material.COBWEB, Material.BEDROCK);

    private final KasaPlugin plugin;
    private final NamespacedKey npcTag;
    private final Map<UUID, Long> lastUse = new HashMap<>();
    private final Set<UUID> searching = new HashSet<>();

    public RtpModule(KasaPlugin plugin) {
        this.plugin = plugin;
        this.npcTag = new NamespacedKey(plugin, "rtp_npc");
    }

    // ---------- menu ----------

    private static final class MenuHolder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private static ItemStack button(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(CrateManager.MM.deserialize("<!italic>" + name));
        if (lore.length > 0) {
            List<Component> lines = new java.util.ArrayList<>();
            for (String l : lore) {
                lines.add(CrateManager.MM.deserialize("<!italic>" + l));
            }
            meta.lore(lines);
        }
        item.setItemMeta(meta);
        return item;
    }

    public void openMenu(Player p) {
        MenuHolder holder = new MenuHolder();
        Inventory inv = Bukkit.createInventory(holder, 27, CrateManager.MM.deserialize("<gold><bold>Rastgele Işınlan"));
        holder.inventory = inv;

        ItemStack pane = button(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, pane);
        }
        inv.setItem(SLOT_OVERWORLD, button(Material.GRASS_BLOCK, "<green><bold>Dünya",
                "<gray>Dünyada rastgele bir yere", "<gray>ışınlan.", "", "<yellow>Tıkla!"));
        inv.setItem(SLOT_NETHER, button(Material.NETHERRACK, "<red><bold>Nether",
                "<gray>Nether'da rastgele bir yere", "<gray>ışınlan.", "", "<yellow>Tıkla!"));
        inv.setItem(SLOT_END, button(Material.END_STONE, "<light_purple><bold>End",
                "<gray>End'de rastgele bir yere", "<gray>ışınlan.", "", "<yellow>Tıkla!"));
        p.openInventory(inv);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder(false) instanceof MenuHolder)) {
            return;
        }
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getClickedInventory() != e.getView().getTopInventory()) {
            return;
        }
        World.Environment env;
        switch (e.getSlot()) {
            case SLOT_OVERWORLD -> env = World.Environment.NORMAL;
            case SLOT_NETHER -> env = World.Environment.NETHER;
            case SLOT_END -> env = World.Environment.THE_END;
            default -> {
                return;
            }
        }
        p.closeInventory();
        requestRtp(p, env);
    }

    @EventHandler
    public void onMenuDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof MenuHolder) {
            e.setCancelled(true);
        }
    }

    // ---------- komutlar ----------

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String name = cmd.getName().toLowerCase(Locale.ROOT);

        if (name.equals("rtp") || name.equals("artp")) {
            if (!(sender instanceof Player p)) {
                CrateManager.msg(sender, "<red>Bu komutu sadece oyuncular kullanabilir.");
                return true;
            }
            if (name.equals("artp")) {
                openMenu(p);
            } else {
                requestRtp(p, World.Environment.NORMAL);
            }
            return true;
        }

        if (!sender.hasPermission(CrateManager.ADMIN_PERM)) {
            CrateManager.msg(sender, "<red>Bu komutu kullanma yetkin yok.");
            return true;
        }
        if (!(sender instanceof Player p)) {
            CrateManager.msg(sender, "<red>Bu komutu sadece oyuncular kullanabilir.");
            return true;
        }

        if (name.equals("rtpnpcolustur")) {
            createNpc(p);
            return true;
        }
        if (name.equals("rtpnpcsil")) {
            removeNpc(p);
            return true;
        }
        return false;
    }

    // ---------- NPC ----------

    private boolean isNpc(Entity e) {
        return e instanceof Villager && e.getPersistentDataContainer().has(npcTag, PersistentDataType.BYTE);
    }

    private void createNpc(Player p) {
        RayTraceResult hit = p.rayTraceBlocks(6.0);
        if (hit == null || hit.getHitBlock() == null || hit.getHitBlockFace() == null) {
            CrateManager.msg(p, "<red>NPC'nin duracağı bir bloğa bak <gray>(en fazla 6 blok).");
            return;
        }

        Block spot = hit.getHitBlock().getRelative(hit.getHitBlockFace());
        Location loc = spot.getLocation().add(0.5, 0, 0.5);
        loc.setDirection(p.getLocation().toVector().subtract(loc.toVector()));
        loc.setPitch(0f);

        loc.getWorld().spawn(loc, Villager.class, v -> {
            v.customName(CrateManager.MM.deserialize("<aqua><bold>Rastgele Işınlan <gray>(Tıkla)"));
            v.setCustomNameVisible(true);
            v.setAI(false);
            v.setSilent(true);
            v.setGravity(false);
            v.setCollidable(false);
            v.setPersistent(true);
            v.setRemoveWhenFarAway(false);
            v.getPersistentDataContainer().set(npcTag, PersistentDataType.BYTE, (byte) 1);
        });

        CrateManager.msg(p, "<green>RTP NPC'si oluşturuldu! <gray>Kaldırmak için ona bakıp <white>/rtpnpcsil <gray>yaz.");
    }

    private void removeNpc(Player p) {
        RayTraceResult hit = p.getWorld().rayTraceEntities(
                p.getEyeLocation(), p.getEyeLocation().getDirection(), 6.0, 0.3, this::isNpc);
        if (hit == null || hit.getHitEntity() == null) {
            CrateManager.msg(p, "<red>Bir RTP NPC'sine bakmalısın <gray>(en fazla 6 blok).");
            return;
        }
        hit.getHitEntity().remove();
        CrateManager.msg(p, "<yellow>RTP NPC'si kaldırıldı.");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteractNpc(PlayerInteractEntityEvent e) {
        if (!isNpc(e.getRightClicked())) {
            return;
        }
        e.setCancelled(true); // ticaret penceresi acilmasin
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        openMenu(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onNpcDamage(EntityDamageEvent e) {
        if (isNpc(e.getEntity())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onNpcHit(EntityDamageByEntityEvent e) {
        if (!isNpc(e.getEntity())) {
            return;
        }
        e.setCancelled(true);
        if (e.getDamager() instanceof Player p && p.isSneaking() && p.hasPermission(CrateManager.ADMIN_PERM)) {
            e.getEntity().remove();
            CrateManager.msg(p, "<yellow>RTP NPC'si kaldırıldı.");
        }
    }

    // ---------- RTP ----------

    private static String envKey(World.Environment env) {
        return switch (env) {
            case NETHER -> "nether";
            case THE_END -> "end";
            default -> "overworld";
        };
    }

    private static String envName(World.Environment env) {
        return switch (env) {
            case NETHER -> "Nether";
            case THE_END -> "End";
            default -> "Dünya";
        };
    }

    public void requestRtp(Player p, World.Environment env) {
        UUID id = p.getUniqueId();
        if (searching.contains(id)) {
            return;
        }

        FileConfiguration cfg = plugin.getConfig();
        int cooldown = Math.max(0, cfg.getInt("rtp.cooldown-seconds", 10));
        if (!p.hasPermission(CrateManager.ADMIN_PERM)) {
            Long last = lastUse.get(id);
            long now = System.currentTimeMillis();
            if (last != null && now - last < cooldown * 1000L) {
                long left = (cooldown * 1000L - (now - last) + 999) / 1000;
                CrateManager.msg(p, "<red>Tekrar ışınlanmak için <white><s> saniye <red>beklemelisin.",
                        Placeholder.unparsed("s", String.valueOf(left)));
                return;
            }
        }

        World world = null;
        for (World w : Bukkit.getWorlds()) {
            if (w.getEnvironment() == env) {
                world = w;
                break;
            }
        }
        if (world == null) {
            CrateManager.msg(p, "<red><d> boyutu bu sunucuda yok.", Placeholder.unparsed("d", envName(env)));
            return;
        }

        String base = "rtp." + envKey(env) + ".";
        int defMin = env == World.Environment.THE_END ? 10 : env == World.Environment.NETHER ? 100 : 300;
        int defMax = env == World.Environment.THE_END ? 45 : env == World.Environment.NETHER ? 1500 : 5000;
        int min = Math.max(0, cfg.getInt(base + "min-distance", defMin));
        int max = Math.max(min + 10, cfg.getInt(base + "max-distance", defMax));
        int attempts = Math.max(1, cfg.getInt("rtp.max-attempts", 20));

        searching.add(id);
        CrateManager.msg(p, "<gray><d> için rastgele bir yer aranıyor...", Placeholder.unparsed("d", envName(env)));
        attempt(p, world, 1, attempts, min, max);
    }

    private void attempt(Player p, World world, int n, int maxAttempts, int min, int max) {
        UUID id = p.getUniqueId();
        if (!p.isOnline()) {
            searching.remove(id);
            return;
        }
        if (n > maxAttempts) {
            searching.remove(id);
            CrateManager.msg(p, "<red>Uygun bir yer bulunamadı, tekrar dene.");
            return;
        }

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int dx = rnd.nextInt(min, max + 1) * (rnd.nextBoolean() ? 1 : -1);
        int dz = rnd.nextInt(-max, max + 1);
        if (rnd.nextBoolean()) {
            int t = dx;
            dx = dz;
            dz = t;
        }
        int cx = 0;
        int cz = 0;
        if (world.getEnvironment() == World.Environment.NORMAL) {
            Location spawn = world.getSpawnLocation();
            cx = spawn.getBlockX();
            cz = spawn.getBlockZ();
        }
        int x = cx + dx;
        int z = cz + dz;

        if (!world.getWorldBorder().isInside(new Location(world, x, 64, z))) {
            attempt(p, world, n + 1, maxAttempts, min, max);
            return;
        }

        world.getChunkAtAsync(x >> 4, z >> 4).whenComplete((chunk, err) ->
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (err != null || !p.isOnline()) {
                        attempt(p, world, n + 1, maxAttempts, min, max);
                        return;
                    }
                    Location safe = findSafe(world, x, z);
                    if (safe == null) {
                        attempt(p, world, n + 1, maxAttempts, min, max);
                        return;
                    }
                    Location now = p.getLocation();
                    safe.setYaw(now.getYaw());
                    safe.setPitch(now.getPitch());
                    p.teleportAsync(safe).whenComplete((ok, err2) ->
                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                searching.remove(id);
                                if (err2 != null || !Boolean.TRUE.equals(ok)) {
                                    CrateManager.msg(p, "<red>Işınlanma başarısız oldu, tekrar dene.");
                                    return;
                                }
                                lastUse.put(id, System.currentTimeMillis());
                                p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                                CrateManager.msg(p, "<green>Işınlandın! <gray>X: <white><x> <gray>Y: <white><y> <gray>Z: <white><z>",
                                        Placeholder.unparsed("x", String.valueOf(safe.getBlockX())),
                                        Placeholder.unparsed("y", String.valueOf(safe.getBlockY())),
                                        Placeholder.unparsed("z", String.valueOf(safe.getBlockZ())));
                            }));
                }));
    }

    private boolean badGround(Block ground) {
        Material type = ground.getType();
        return !type.isSolid() || ground.isLiquid() || UNSAFE.contains(type)
                || Tag.LEAVES.isTagged(type) || Tag.ICE.isTagged(type);
    }

    private boolean badSpace(Block b) {
        return !b.isPassable() || b.isLiquid() || UNSAFE.contains(b.getType());
    }

    private Location findSafe(World world, int x, int z) {
        if (world.getEnvironment() == World.Environment.NETHER) {
            return findSafeNether(world, x, z);
        }
        Block ground = world.getHighestBlockAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        if (badGround(ground)) {
            return null;
        }
        Block feet = ground.getRelative(0, 1, 0);
        Block head = ground.getRelative(0, 2, 0);
        if (badSpace(feet) || badSpace(head)) {
            return null;
        }
        return new Location(world, x + 0.5, feet.getY(), z + 0.5);
    }

    /** Nether'da tavan oldugu icin rastgele bir yukseklikten asagi dogru tarar. */
    private Location findSafeNether(World world, int x, int z) {
        int start = ThreadLocalRandom.current().nextInt(40, 111);
        for (int y = start; y > 33; y--) {
            Block ground = world.getBlockAt(x, y, z);
            if (badGround(ground)) {
                continue;
            }
            Block feet = ground.getRelative(0, 1, 0);
            Block head = ground.getRelative(0, 2, 0);
            if (badSpace(feet) || badSpace(head)) {
                continue;
            }
            return new Location(world, x + 0.5, feet.getY(), z + 0.5);
        }
        return null;
    }
}
