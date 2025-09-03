package kam.kamsTweaks.features;

import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.ConfigCommand;
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.Logger;
import kam.kamsTweaks.utils.Inventories;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.bukkit.Bukkit.getServer;

public class Graves implements Listener {
    public Map<Integer, Grave> graves = new HashMap<>();
    static int highest = 0;

    public void setup() {
        ConfigCommand.addConfig(new ConfigCommand.BoolConfig("graves.enabled", "graves.enabled", true, "kamstweaks.configure"));
        Bukkit.getScheduler().runTaskTimer(KamsTweaks.getInstance(), new Runnable() {
            long lastTime = System.currentTimeMillis();

            @Override
            public void run() {
                long now = System.currentTimeMillis();
                long time = now - lastTime;
                graves.values().forEach(grave -> grave.tick(time));
                lastTime = now;
            }
        }, 20L, 20L);  // 20L = 1 second in ticks
    }

    public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {
        commands.registrar().register(Commands.literal("graves")
                .then(Commands.literal("list").executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (ctx.getSource().getExecutor() instanceof Player player) {
                        StringBuilder claimsMsg = new StringBuilder();
                        AtomicInteger i = new AtomicInteger();
                        graves.forEach((id, grave) -> {
                            if (grave.owner.equals(player)) {
                                i.getAndIncrement();
                                claimsMsg
                                        .append("\nGrave ").append(i.get()).append(": ")
                                        .append(grave.location.getBlockX())
                                        .append(", ")
                                        .append(grave.location.getBlockY())
                                        .append(", ")
                                        .append(grave.location.getBlockZ())
                                        .append(" in ")
                                        .append(grave.location.getWorld().getName());
                            }
                        });
                        claimsMsg.insert(0, "You have " + i + " graves.");
                        player.sendMessage(claimsMsg.toString());
                    } else {
                        sender.sendMessage("Only players can run this.");
                    }
                    return Command.SINGLE_SUCCESS;
                }))
                .build());
    }

    public Location checkLocation(Location loc) {
        if (loc.getBlockY() < loc.getWorld().getMinHeight()) {
            var block = loc.getWorld().getHighestBlockAt(loc);
            if (block.getType().isAir()) {
                loc.setYaw(0);
                loc.set(loc.getBlockX() + .5f, 63, loc.getBlockZ() + .25f);
                var down = loc.getBlock().getRelative(BlockFace.DOWN);
                switch(loc.getWorld().getEnvironment()) {
                    case NORMAL: {
                        down.setType(Material.STONE);
                        break;
                    }
                    case NETHER: {
                        down.setType(Material.NETHERRACK);
                        break;
                    }
                    case THE_END: {
                        down.setType(Material.END_STONE);
                        break;
                    }
                }
                var back = down.getRelative(BlockFace.SOUTH);
                if (back.getType().equals(Material.AIR)) {
                    switch(loc.getWorld().getEnvironment()) {
                        case NORMAL: {
                            back.setType(Material.STONE);
                            break;
                        }
                        case NETHER: {
                            back.setType(Material.NETHERRACK);
                            break;
                        }
                        case THE_END: {
                            back.setType(Material.END_STONE);
                            break;
                        }
                    }
                }
            } else {
                loc.setY(block.getY() + 1);
            }
        }

        return loc;
    }

    @EventHandler
    public void onDie(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        if (player.getInventory().isEmpty() && player.getTotalExperience() == 0) return;
        var loc = checkLocation(player.getLocation());
        Grave grave = new Grave(player, loc);
        graves.put(grave.id, grave);
        event.getDrops().clear();
        event.setDroppedExp(0);
    }

    void tp() {
        Bukkit.getScheduler().runTaskLater(KamsTweaks.getInstance(), () -> {
            graves.forEach((id, grave) -> {
                if (!grave.stand.getLocation().equals(grave.location)) {
                    grave.stand.teleport(grave.location.clone().addRotation(90, 0).subtract(0, 1.4375, 0));
                }
            });
        }, 3L);
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        tp();
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        tp();
    }

    public boolean isLocationInChunk(Chunk chunk, Location location) {
        int chunkMinX = chunk.getX() << 4;
        int chunkMinZ = chunk.getZ() << 4;
        int chunkMaxX = chunkMinX + 15;
        int chunkMaxZ = chunkMinZ + 15;
        if (chunk.getWorld() != location.getWorld()) return false;
        return location.getBlockX() >= chunkMinX && location.getBlockX() <= chunkMaxX &&
                location.getBlockZ() >= chunkMinZ && location.getBlockZ() <= chunkMaxZ;
    }

    @EventHandler
    public void chunkLoad(ChunkLoadEvent e) {
        graves.forEach((id, grave) -> {
            if (List.of(e.getChunk().getEntities()).contains(grave.stand)) return;
            if (isLocationInChunk(e.getChunk(), grave.location)) {
                grave.createStand();
            }
        });
    }

    PlainTextComponentSerializer plainSerializer = PlainTextComponentSerializer.plainText();

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        var clickedInv = e.getClickedInventory();
        if (plainSerializer.serialize(e.getView().title()).equals("Grave")) {
            switch (e.getAction()) {
                case PLACE_ALL:
                case PLACE_ONE:
                case PLACE_SOME:
                case SWAP_WITH_CURSOR:
                case PLACE_FROM_BUNDLE:
                case PLACE_ALL_INTO_BUNDLE:
                case PLACE_SOME_INTO_BUNDLE:
                    if (clickedInv != null && clickedInv.equals(e.getView().getTopInventory())) {
                        e.setCancelled(true);
                    }
                    break;
                case HOTBAR_SWAP:
                    var clicked = e.getHotbarButton() == -1 ? e.getWhoClicked().getInventory().getItemInOffHand() : e.getWhoClicked().getInventory().getItem(e.getHotbarButton());
                    if (clickedInv != null && clickedInv.equals(e.getView().getTopInventory()) && clicked != null && !clicked.isEmpty()) {
                        e.setCancelled(true);
                    }
                    break;
                case MOVE_TO_OTHER_INVENTORY:
                    if (clickedInv != null && clickedInv.equals(e.getView().getBottomInventory())) {
                        e.setCancelled(true);
                    }
                    break;

                default:
                    break;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent e) {
        if (plainSerializer.serialize(e.getView().title()).equals("Grave")) {
            for (int rawSlot : e.getRawSlots()) {
                if (rawSlot < e.getView().getTopInventory().getSize()) {
                    e.setCancelled(true);
                    break;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCloseInv(InventoryCloseEvent e) {
        AtomicInteger rem = new AtomicInteger(-1);
        graves.forEach((id, grave) -> {
            if (e.getInventory().equals(grave.getInventory())) {
                if (grave.getInventory().isEmpty()) {
                    if (grave.stand != null) grave.stand.remove();
                    rem.set(id);
                }
            }
        });
        if (rem.get() != -1) graves.remove(rem.get());
    }

    public void openGrave(PlayerInteractEntityEvent e) {
        Entity entity = e.getRightClicked();
        Player player = e.getPlayer();
        if (entity instanceof ArmorStand stand) {
            NamespacedKey key = new NamespacedKey("kamstweaks", "grave");
            if (!stand.getPersistentDataContainer().has(key, PersistentDataType.INTEGER)) return;
            if (!KamsTweaks.getInstance().getConfig().getBoolean("graves.enabled", true))
                return;
            @SuppressWarnings("DataFlowIssue") int id = stand.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
            if (!graves.containsKey(id)) return;
            e.setCancelled(true);
            var grave = graves.get(id);
            if (grave.getOwner().getUniqueId().equals(player.getUniqueId())) {
                PlayerInventory inv = player.getInventory();
                Inventory inventory = grave.getInventory();
                for (int i = 0; i < 36; i++) {
                    if (inv.getItem(i) != null && !Objects.requireNonNull(inv.getItem(i)).isEmpty()) continue;
                    ItemStack item = inventory.getItem(i);
                    if (item != null && !item.isEmpty()) {
                        inv.setItem(i, item);
                        inventory.setItem(i, null);
                    }
                }
                if (inv.getHelmet() == null || Objects.requireNonNull(inv.getHelmet()).isEmpty()) {
                    inv.setHelmet(inventory.getItem(36));
                    inventory.setItem(36, null);
                }
                if (inv.getChestplate() == null || Objects.requireNonNull(inv.getChestplate()).isEmpty()) {
                    inv.setChestplate(inventory.getItem(37));
                    inventory.setItem(37, null);
                }
                if (inv.getLeggings() == null || Objects.requireNonNull(inv.getLeggings()).isEmpty()) {
                    inv.setLeggings(inventory.getItem(38));
                    inventory.setItem(38, null);
                }
                if (inv.getBoots() == null || Objects.requireNonNull(inv.getBoots()).isEmpty()) {
                    inv.setBoots(inventory.getItem(39));
                    inventory.setItem(39, null);
                }
                if (inv.getItemInOffHand().isEmpty()) {
                    inv.setItemInOffHand(inventory.getItem(40));
                    inventory.setItem(40, null);
                }
                if (grave.experience != 0) {
                    changePlayerExp(player, grave.experience);
                    grave.experience = 0;
                }
                if (inventory.isEmpty()) {
                    if (grave.stand != null) grave.stand.remove();
                    graves.remove(grave.id);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityInteract(PlayerInteractEntityEvent e) {
        Entity entity = e.getRightClicked();
        if (e.getPlayer().isSneaking()) {
            openGrave(e);
            return;
        }
        if (entity instanceof ArmorStand stand) {
            Player player = e.getPlayer();
            NamespacedKey key = new NamespacedKey("kamstweaks", "grave");
            if (!stand.getPersistentDataContainer().has(key, PersistentDataType.INTEGER)) return;
            e.setCancelled(true);
            if (!KamsTweaks.getInstance().getConfig().getBoolean("graves.enabled", true))
                return;
            @SuppressWarnings("DataFlowIssue") int id = stand.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
            if (!graves.containsKey(id)) return;
            var grave = graves.get(id);
            if (grave.getOwner().getUniqueId().equals(player.getUniqueId())) {
                player.openInventory(grave.getInventory());
                if (grave.experience != 0) {
                    changePlayerExp(player, grave.experience);
                    grave.experience = 0;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityInteractAt(PlayerInteractAtEntityEvent e) {
        onEntityInteract(e);
    }

    public class Grave {
        OfflinePlayer owner;
        Inventory inventory;
        Location location;
        ArmorStand stand;
        int experience;
        int id;
        long msLeft = 1000 * 60 * 20; // 20 mins
        long offlineMsLeft = 1000 * 60 * 60 * 24 * 7; // 7 days
        boolean wasOnlineLast = false;

        public void tick(long ms) {
            if (owner.isOnline()) {
                this.msLeft -= ms;
                if (msLeft <= 0) {
                    graves.remove(this.id);
                    this.stand.remove();
                }
                this.wasOnlineLast = true;
            } else {
                if (this.wasOnlineLast) {
                    offlineMsLeft = 1000 * 60 * 60 * 24 * 7; // 7 days
                } else {
                    this.offlineMsLeft -= ms;
                    if (offlineMsLeft <= 0) {
                        graves.remove(this.id);
                        this.stand.remove();
                    }
                }
                this.wasOnlineLast = false;
            }
        }

        public Grave(OfflinePlayer owner, Inventory inventory, Location location, int experience, long msLeft) {
            this.owner = owner;
            this.inventory = inventory;
            this.location = location;
            this.experience = experience;
            this.msLeft = msLeft;
        }

        public Grave(Player owner, Location location) {
            this.owner = owner;
            this.location = location;
            PlayerInventory inv = owner.getInventory();
            this.inventory = Bukkit.createInventory(null, 45, Component.text("Grave"));
            for (int i = 0; i < 36; i++) {
                ItemStack item = inv.getItem(i);
                if (item != null && !item.isEmpty()) {
                    inventory.setItem(i, item);
                    inv.setItem(i, null);
                }
            }
            inventory.setItem(36, inv.getHelmet());
            inv.setHelmet(null);
            inventory.setItem(37, inv.getChestplate());
            inv.setChestplate(null);
            inventory.setItem(38, inv.getLeggings());
            inv.setLeggings(null);
            inventory.setItem(39, inv.getBoots());
            inv.setBoots(null);
            inventory.setItem(40, inv.getItemInOffHand());
            inv.setItemInOffHand(null);
            inventory.setItem(41, owner.getItemOnCursor());
            owner.setItemOnCursor(null);
            this.experience = getPlayerExp(owner);
            this.id = highest;
            highest++;
            createStand();
        }

        public OfflinePlayer getOwner() {
            return owner;
        }

        public Inventory getInventory() {
            return inventory;
        }

        public Location getLocation() {
            return location;
        }

        public void createStand() {
            stand = location.getWorld().spawn(location.clone().addRotation(90, 0).subtract(0, 1.4375, 0), ArmorStand.class);
            stand.setGravity(false);
            stand.setItem(EquipmentSlot.HEAD, new ItemStack(Material.STONE_BRICK_WALL));
            stand.setCustomNameVisible(true);
            stand.setBasePlate(false);
            stand.setRemoveWhenFarAway(false);
            stand.setPersistent(false);
            stand.setInvisible(true);
            stand.setInvulnerable(true);
            stand.customName(Component.text(owner.getName() == null ? "Unknown" : owner.getName()).color(NamedTextColor.GOLD).append(Component.text("'s Grave").color(NamedTextColor.WHITE)));
            stand.getPersistentDataContainer().set(new NamespacedKey("kamstweaks", "grave"), PersistentDataType.INTEGER, this.id);
        }

        void setArmorStand(ArmorStand stand) {
            this.stand = stand;
        }

        ArmorStand getArmorStand() {
            return stand;
        }
    }

    public void saveGraves() {
        FileConfiguration config = KamsTweaks.getInstance().getGeneralConfig();
        config.set("graves", null);
        graves.forEach((id, grave) -> {
            Inventories.saveInventory(grave.getInventory(), config, "graves." + id);
            config.set("graves." + id + ".location", serializeLocation(grave.location));
            config.set("graves." + id + ".owner", grave.owner.getUniqueId().toString());
            config.set("graves." + id + ".xp", grave.experience);
            config.set("graves." + id + ".timeleft", grave.msLeft);
        });
    }

    public void loadGraves() {
        graves.clear();
        highest = 0;
        FileConfiguration config = KamsTweaks.getInstance().getGeneralConfig();
        if (config.contains("graves")) {
            for (String key : Objects.requireNonNull(config.getConfigurationSection("graves")).getKeys(false)) {
                try {
                    String ownerStr = config.getString("graves." + key + ".owner");
                    UUID owner = ownerStr == null ? null : UUID.fromString(ownerStr);
                    String locationStr = config.getString("graves." + key + ".location");
                    int xp = (int) config.get("graves." + key + ".xp", 0);
                    assert locationStr != null;
                    Location location = checkLocation(deserializeLocation(locationStr));
                    if (location.getWorld() == null) continue;
                    Inventory inv = Inventories.loadInventory(Component.text("Grave"), 45, config, "graves." + key);
                    long timeLeft = config.getLong("graves." + key + ".timeleft", 1000 * 60 * 20);
                    Grave grave = new Grave(owner == null ? null : getServer().getOfflinePlayer(owner), inv, location, xp, timeLeft);
                    int id = Integer.parseInt(key);
                    if (highest < id) highest = id;
                    grave.id = id;
                    graves.put(id, grave);
                    grave.createStand();
                } catch (Exception e) {
                    Logger.warn(e.getMessage());
                }
            }
        }
    }

    public static String serializeLocation(Location loc) {
        return loc.getWorld() == null ? "" : loc.getWorld().getUID() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ() + "," + loc.getYaw() + "," + loc.getPitch();
    }

    public static Location deserializeLocation(String s) {
        String[] parts = s.split(",");
        UUID worldUuid = UUID.fromString(parts[0]);
        return new Location(
                getServer().getWorld(worldUuid),
                Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2]),
                Double.parseDouble(parts[3]),
                Float.parseFloat(parts[4]),
                Float.parseFloat(parts[5])
        );
    }

    // From essentials

    // Calculate amount of EXP needed to level up
    public static int getExpToLevelUp(int level){
        if(level <= 15){
            return 2*level+7;
        } else if(level <= 30){
            return 5*level-38;
        } else {
            return 9*level-158;
        }
    }

    // Calculate total experience up to a level
    public static int getExpAtLevel(int level){
        if(level <= 16){
            return (int) (Math.pow(level,2) + 6*level);
        } else if(level <= 31){
            return (int) (2.5*Math.pow(level,2) - 40.5*level + 360.0);
        } else {
            return (int) (4.5*Math.pow(level,2) - 162.5*level + 2220.0);
        }
    }

    // Calculate player's current EXP amount
    public static int getPlayerExp(Player player){
        int exp = 0;
        int level = player.getLevel();

        // Get the amount of XP in past levels
        exp += getExpAtLevel(level);

        // Get amount of XP towards next level
        exp += Math.round(getExpToLevelUp(level) * player.getExp());

        return exp;
    }

    // Give or take EXP
    public static int changePlayerExp(Player player, int exp){
        // Get player's current exp
        int currentExp = getPlayerExp(player);

        // Reset player's current exp to 0
        player.setExp(0);
        player.setLevel(0);

        // Give the player their exp back, with the difference
        int newExp = currentExp + exp;
        player.giveExp(newExp);

        // Return the player's new exp amount
        return newExp;
    }

}
