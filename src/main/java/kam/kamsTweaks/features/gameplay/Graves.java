package kam.kamsTweaks.features.gameplay;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import kam.kamsTweaks.*;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.features.Feature;
import kam.kamsTweaks.managers.KTItems;
import kam.kamsTweaks.managers.KTPerms;
import kam.kamsTweaks.managers.KTStrings;
import kam.kamsTweaks.utils.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Graves extends Feature {
    public static final int MAIN_SIZE = 45;
    public static final int STATION_SIZE = 36;
    public static NamespacedKey guiKey = new NamespacedKey("kamstweaks", "gui");
    public static NamespacedKey graveKey = new NamespacedKey("kamstweaks", "grave");
    public static Map<Integer, Grave> graves = new HashMap<>();
    static int highest = 0;

    @Override
    public void setup() {
        Config.bool("graves.enabled", false).build().add();
        Config.integer("graves.time-limit", 600).build().add();
        Config.integer("graves.recovery-time-limit", 300).build().add();
        Config.integer("graves.recovery-limit", -1).build().add();
        Config.integer("graves.max-active-recoveries", 3).build().add();
        Bukkit.getScheduler().runTaskTimer(KamsTweaks.get(), new Runnable() {
            long lastTime = System.currentTimeMillis();

            @Override
            public void run() {
                long now = System.currentTimeMillis();
                long time = now - lastTime;
                graves.values().forEach(grave -> grave.tick(time));
                lastTime = now;
            }
        }, 20L, 20L); // 20L = 1 second in ticks
    }

    @Override
    public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {
        commands.registrar().register(Commands.literal("graves")
                .then(Commands.literal("list").executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (ctx.getSource().getExecutor() instanceof Player player) {
                        AtomicReference<Component> claimsMsg = new AtomicReference<>(Component.empty());
                        AtomicInteger i = new AtomicInteger();
                        graves.forEach((id, grave) -> {
                            if (grave.owner.getUniqueId().equals(player.getUniqueId())) {
                                i.getAndIncrement();
                                claimsMsg.set(claimsMsg.get().appendNewline().append(KTStrings.getFor(KTStrings.GRAVE_INFO,
                                        Component.text(id).color(NamedTextColor.GOLD),
                                        Component.text(grave.location.getBlockX() + ", " + grave.location.getBlockY() + ", " + grave.location.getBlockZ()).color(NamedTextColor.GREEN),
                                        Component.text(grave.location.getWorld().getName()).color(NamedTextColor.LIGHT_PURPLE),
                                        grave.msLeft < 0 ? KTStrings.getFor(KTStrings.GRAVE_EXPIRED).color(NamedTextColor.RED) : KTStrings.getFor(KTStrings.GRAVE_TIME_LEFT, Component.text((int) grave.msLeft / 1000)).color(NamedTextColor.YELLOW))
                                ));
                            }
                        });
                        player.sendMessage(KTStrings.getFor(KTStrings.GRAVE_COUNT, Component.text(i.get())).append(claimsMsg.get()));
                    } else {
                        sender.sendMessage(KTStrings.getFor(KTStrings.PLAYERS_ONLY));
                    }
                    return Command.SINGLE_SUCCESS;
                })).then(Commands.literal("recover")
                        .then(Commands.argument("id", IntegerArgumentType.integer()).suggests((ctx, builder) -> {
                            if (!(ctx.getSource().getSender() instanceof Player player))
                                return builder.buildFuture();
                            graves.forEach((id, grave) -> {
                                if (grave.owner.getUniqueId().equals(player.getUniqueId()) && grave.msLeft <= 0 /* && !grave.recovery */) {
                                    builder.suggest(id);
                                }
                            });
                            return builder.buildFuture();
                        }).executes(ctx -> {
                            if (!(ctx.getSource().getSender() instanceof Player player))
                                return Command.SINGLE_SUCCESS;
                            AtomicInteger recovCount = new AtomicInteger(0);
                            graves.forEach((id, grave) -> {
                                if (grave.owner.getUniqueId().equals(player.getUniqueId()) && grave.msLeft > 0 && grave.recovery) {
                                    recovCount.getAndAdd(1);
                                }
                            });
                            int maxActive = Config.getInt("graves.max-active-recoveries", 3);
                            if (recovCount.get() >= maxActive) {
                                ctx.getSource().getSender().sendMessage(KTStrings.getFor(KTStrings.GRAVE_RECOVERY_MAX, Component.text(maxActive)).color(NamedTextColor.RED));
                                return Command.SINGLE_SUCCESS;
                            }
                            int id = ctx.getArgument("id", Integer.class);
                            Grave grave = graves.getOrDefault(id, null);
                            if (grave != null && grave.owner.getUniqueId().equals(player.getUniqueId()) // && !grave.recovery
                                    && grave.msLeft <= 0) {
                                grave.recovery = true;
                                grave.msLeft = Config.getLong("graves.recovery-time-limit", 300);
                                grave.hasMessaged5 = false;
                                grave.hasMessaged1 = false;
                                grave.hasMessagedHalf = false;
                                grave.hasMessagedExpire = false;
                                grave.recoveries++;
                                int maxRecoveries = Config.getInt("graves.recovery-limit", -1);
                                if (maxRecoveries == -1) {
                                    ctx.getSource().getSender().sendMessage(KTStrings.getFor(KTStrings.GRAVE_RECOVERY, Component.text(maxActive)).color(NamedTextColor.AQUA));
                                } else {
                                    ctx.getSource().getSender().sendMessage(KTStrings.getFor(KTStrings.GRAVE_RECOVERY_LIMITED, Component.text(maxRecoveries - grave.recoveries), Component.text(maxActive)).color(NamedTextColor.AQUA));
                                }
                                grave.createStand();
                                return Command.SINGLE_SUCCESS;
                            }
                            ctx.getSource().getSender().sendMessage(KTStrings.getFor(KTStrings.GRAVE_NO_ID, Component.text(id)).color(NamedTextColor.RED));
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(Commands.literal("delete")
                        .then(Commands.argument("id", IntegerArgumentType.integer()).suggests((ctx, builder) -> {
                            if (!(ctx.getSource().getSender() instanceof Player player))
                                return builder.buildFuture();
                            graves.forEach((id, grave) -> {
                                if (grave.owner.getUniqueId().equals(player.getUniqueId())) {
                                    builder.suggest(id);
                                }
                            });
                            return builder.buildFuture();
                        }).executes(ctx -> {
                            if (!(ctx.getSource().getSender() instanceof Player player))
                                return Command.SINGLE_SUCCESS;
                            int id = ctx.getArgument("id", Integer.class);
                            Grave grave = graves.getOrDefault(id, null);
                            if (grave != null && grave.owner.getUniqueId().equals(player.getUniqueId())) {
                                if (grave.stand != null)
                                    grave.stand.remove();
                                graves.remove(grave.id);
                                ctx.getSource().getSender().sendMessage(KTStrings.getFor(KTStrings.GRAVE_DELETED).color(NamedTextColor.RED));
                                return Command.SINGLE_SUCCESS;
                            }
                            ctx.getSource().getSender().sendMessage(KTStrings.getFor(KTStrings.GRAVE_NO_ID, Component.text(id)).color(NamedTextColor.RED));
                            return Command.SINGLE_SUCCESS;
                        })))
                        .then(Commands.literal("toggle").executes(ctx -> {
                            CommandSender sender = ctx.getSource().getSender();
                            if (ctx.getSource().getExecutor() instanceof Player player) {
                                var newState = !UserDataManager.get(player.getUniqueId(), "graves.enabled", true);
                                UserDataManager.put(player.getUniqueId(), "graves.enabled", newState, Boolean.class);
                                player.sendMessage(KTStrings.getFor(KTStrings.GRAVE_TOGGLED, KTStrings.getFor(newState ? KTStrings.ON : KTStrings.OFF)));
                            } else {
                                sender.sendMessage(KTStrings.getFor(KTStrings.PLAYERS_ONLY));
                            }
                            return Command.SINGLE_SUCCESS;
                        })).executes(ctx -> {
                            CommandSender sender = ctx.getSource().getSender();
                            if (ctx.getSource().getExecutor() instanceof Player player) {
                                player.sendMessage(KTStrings.getFor(KTStrings.GRAVE_STATUS, KTStrings.getFor(UserDataManager.get(player.getUniqueId(), "graves.enabled", true) ? KTStrings.ON : KTStrings.OFF)));
                            } else {
                                sender.sendMessage(KTStrings.getFor(KTStrings.PLAYERS_ONLY));
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                .build());
    }

    static protected int getLavaTop(Location loc, int off) {
        if (loc.getBlock().getType() == Material.LAVA) {
            return getLavaTop(loc.add(0, 1, 0), off + 1);
        } else {
            if (loc.getBlock().getType().isAir()) return off;
            else if (loc.add(0, 1, 0).getBlock().getType().isAir()) return off + 1;
            else if (loc.add(0, 1, 0).getBlock().getType().isAir()) return off + 2;
            else if (loc.add(0, 1, 0).getBlock().getType().isAir()) return off + 3;
            loc.subtract(0, 3, 0);
            return 0;
        }
    }

    public static Location checkLocation(Location loc) {
        if (loc.getWorld() == null) return null;
        var wb = loc.getWorld().getWorldBorder();
        if (!wb.isInside(loc)) {
            loc.setX(Math.clamp(loc.getX(), wb.getCenter().getX() - wb.getSize() / 2, wb.getCenter().getX() + wb.getSize() / 2));
            loc.setZ(Math.clamp(loc.getZ(), wb.getCenter().getZ() - wb.getSize() / 2, wb.getCenter().getZ() + wb.getSize() / 2));
        }
        if (loc.getBlock().getType() == Material.LAVA) {
            var ret = getLavaTop(loc, 0);
            if (ret > 0) {
                var down = loc.getBlock().getRelative(BlockFace.DOWN);
                switch (loc.getWorld().getEnvironment()) {
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
                    switch (loc.getWorld().getEnvironment()) {
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
                return loc;
            }
        }
        if (loc.getBlockY() < loc.getWorld().getMinHeight()) {
            var block = loc.getWorld().getHighestBlockAt(loc);
            if (block.getType().isAir()) {
                loc.setYaw(0);
                loc.set(loc.getBlockX() + .5f, 63, loc.getBlockZ() + .25f);
                var down = loc.getBlock().getRelative(BlockFace.DOWN);
                switch (loc.getWorld().getEnvironment()) {
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
                    switch (loc.getWorld().getEnvironment()) {
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
    public void onRespawn(PlayerPostRespawnEvent e) {
        e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 15, 100));
    }

    @EventHandler
    public void onDie(PlayerDeathEvent event) {
        if (!Config.getBool("graves.enabled", false)) return;
        if (!KTPerms.hasPermission(event.getPlayer(), KTPerms.GRAVES)) return;

        Player player = event.getPlayer();
        if (!UserDataManager.get(player.getUniqueId(), "graves.enabled", true)) return;
        if (player.getInventory().isEmpty() && player.getTotalExperience() == 0) return;
        var loc = checkLocation(player.getLocation());
        if (loc == null) loc = player.getLocation();
        Grave grave = new Grave(player, loc);
        graves.put(grave.id, grave);
        event.getDrops().clear();
        event.setDroppedExp(0);
        player.sendMessage(KTStrings.getFor(KTStrings.GRAVE_NEW, Component.text(String.format("(%s, %s, %s)", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())).color(NamedTextColor.RED), Component.text(loc.getWorld().getName()).color(NamedTextColor.RED)).color(NamedTextColor.GOLD));
    }

    void tp() {
        Bukkit.getScheduler().runTaskLater(KamsTweaks.get(), () -> graves.forEach((id, grave) -> {
            if (grave.stand != null && !grave.stand.getLocation().equals(grave.location)) {
                grave.stand.teleport(grave.location.clone().addRotation(90, 0).subtract(0, 1.4375, 0));
            }
        }), 3L);
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
            if (isLocationInChunk(e.getChunk(), grave.location) && grave.owner.isOnline()) {
                if (grave.stand != null) {
                    grave.stand.remove();
                    grave.stand = null;
                }
                if (grave.msLeft > 0) {
                    grave.createStand();
                }
            }
        });
    }

    PlainTextComponentSerializer plainSerializer = PlainTextComponentSerializer.plainText();

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        var clickedInv = e.getClickedInventory();
        var item = e.getCurrentItem();
        if (item != null && !item.isEmpty() && item.getPersistentDataContainer().has(guiKey)) {
            switch (item.getPersistentDataContainer().get(guiKey, PersistentDataType.STRING)) {
                case "main": {
                    var id = item.getPersistentDataContainer().get(graveKey, PersistentDataType.INTEGER);
                    e.getWhoClicked().closeInventory();
                    Bukkit.getScheduler().scheduleSyncDelayedTask(KamsTweaks.get(), () -> {
                        var grave = graves.get(id);
                        if (grave == null) return;
                        e.getWhoClicked().openInventory(grave.getMainInv());
                    }, 0);
                    break;
                }
                case "station": {
                    var id = item.getPersistentDataContainer().get(graveKey, PersistentDataType.INTEGER);
                    e.getWhoClicked().closeInventory();
                    Bukkit.getScheduler().scheduleSyncDelayedTask(KamsTweaks.get(), () -> {
                        var grave = graves.get(id);
                        if (grave == null || grave.getStationInv() == null) return;
                        e.getWhoClicked().openInventory(grave.getStationInv());
                    }, 0);
                    break;
                }
                case null, default:
                    break;
            }
        }
        if (e.getView().title().equals(KTStrings.getFor(KTStrings.GRAVE_TITLE)) || e.getView().title().equals(KTStrings.getFor(KTStrings.GRAVE_BLOCK_TITLE))) {
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
        if (e.getView().title().equals(KTStrings.getFor(KTStrings.GRAVE_TITLE)) || e.getView().title().equals(KTStrings.getFor(KTStrings.GRAVE_BLOCK_TITLE))) {
            for (int rawSlot : e.getRawSlots()) {
                if (rawSlot < e.getView().getTopInventory().getSize()) {
                    e.setCancelled(true);
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        graves.forEach((id, grave) -> {
            if (grave.owner.getUniqueId().equals(e.getPlayer().getUniqueId())) {
                if (grave.stand != null) {
                    grave.stand.remove();
                    grave.stand = null;
                }
            }
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        AtomicInteger expired = new AtomicInteger();
        AtomicInteger unexpired = new AtomicInteger();
        graves.forEach((id, grave) -> {
            if (grave.owner.getUniqueId().equals(e.getPlayer().getUniqueId())) {
                if (grave.stand != null) {
                    grave.stand.remove();
                    grave.stand = null;
                }
                if (grave.msLeft > 0) {
                    grave.createStand();
                    unexpired.addAndGet(1);
                } else {
                    expired.addAndGet(1);
                }
            }
        });
        if (expired.get() > 0 || unexpired.get() > 0) {
            e.getPlayer().sendMessage(KTStrings.getFor(KTStrings.GRAVE_WELCOME, Component.text(unexpired.get()), Component.text(expired.get())).color(NamedTextColor.GOLD));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCloseInv(InventoryCloseEvent e) {
        AtomicInteger rem = new AtomicInteger(-1);
        graves.forEach((id, grave) -> {
            if (e.getInventory().equals(grave.getMainInv())) {
                var st = grave.getStationInv();
                if (st != null) {
                    var i = 0;
                    for (var stack : st.getContents()) {
                        if (stack != null && !stack.isEmpty()) i++;
                    }
                    if (i == 1) {
                        grave.stationInv = null;
                        grave.mainInv.setItem(MAIN_SIZE - 1, ItemStack.empty());
                    }
                }
                if (grave.getMainInv().isEmpty()) {
                    if (grave.stand != null) grave.stand.remove();
                    rem.set(id);
                    KamsTweaks.get().save();
                }
            }
        });
        if (rem.get() != -1) graves.remove(rem.get());
    }

    public void openGrave(PlayerInteractEntityEvent e) {
        Entity entity = e.getRightClicked();
        Player player = e.getPlayer();
        if (entity instanceof ArmorStand stand) {
            if (!stand.getPersistentDataContainer().has(graveKey, PersistentDataType.INTEGER)) return;
            e.setCancelled(true);
            @SuppressWarnings("DataFlowIssue")
            int id = stand.getPersistentDataContainer().get(graveKey, PersistentDataType.INTEGER);
            if (!graves.containsKey(id)) return;
            var grave = graves.get(id);
            if (grave.getOwner().getUniqueId().equals(player.getUniqueId())) {
                PlayerInventory inv = player.getInventory();
                Inventory inventory = grave.getMainInv();
                for (int i = 0; i < 36; i++) {
                    if (inv.getItem(i) != null && !Objects.requireNonNull(inv.getItem(i)).isEmpty()) continue;
                    ItemStack item = inventory.getItem(i);
                    if (item != null && !item.isEmpty()) {
                        inv.setItem(i, item);
                        inventory.setItem(i, null);
                    }
                }
                if (inv.getHelmet().isEmpty()) {
                    inv.setHelmet(inventory.getItem(36));
                    inventory.setItem(36, null);
                }
                if (inv.getChestplate().isEmpty()) {
                    inv.setChestplate(inventory.getItem(37));
                    inventory.setItem(37, null);
                }
                if (inv.getLeggings().isEmpty()) {
                    inv.setLeggings(inventory.getItem(38));
                    inventory.setItem(38, null);
                }
                if (inv.getBoots().isEmpty()) {
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
            if (!stand.getPersistentDataContainer().has(graveKey, PersistentDataType.INTEGER)) return;
            e.setCancelled(true);
            @SuppressWarnings("DataFlowIssue")
            int id = stand.getPersistentDataContainer().get(graveKey, PersistentDataType.INTEGER);
            if (!graves.containsKey(id)) return;
            var grave = graves.get(id);
            if (grave.getOwner().getUniqueId().equals(player.getUniqueId())) {
                player.openInventory(grave.getMainInv());
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

    public static class Grave {
        OfflinePlayer owner;
        Inventory mainInv;
        Inventory stationInv;
        Location location;
        ArmorStand stand;
        int experience;
        int id;
        long msLeft = Config.getLong("graves.time-limit", 600);
        boolean hasMessaged5 = false;
        boolean hasMessaged1 = false;
        boolean hasMessagedHalf = false;
        boolean hasMessagedExpire = false;
        boolean recovery = false;
        int recoveries = 0;

        public void tick(long ms) {
            Player player = Bukkit.getPlayer(owner.getUniqueId());
            if (player != null) {
                this.msLeft -= ms;
                if (msLeft <= 0) {
                    if (this.stand != null) {
                        this.stand.remove();
                        this.stand = null;
                    }
                    if (!hasMessagedExpire) {
                        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, .6f, 1.0f);
                        player.sendMessage(KTStrings.getFor(KTStrings.GRAVE_EXPIRE_NOW, Component.text(id)).color(NamedTextColor.RED));
                        hasMessagedExpire = true;
                    }
                    int maxRecoveries = Config.getInt("graves.recovery-limit", -1);
                    if (recovery && maxRecoveries >= 0 && recoveries >= maxRecoveries) {
                        Bukkit.getScheduler().runTask(KamsTweaks.get(), () -> graves.remove(this.id));
                    }
                } else {
                    if (this.msLeft <= 1000 * 60 * 5 && !hasMessaged5) {
                        hasMessaged5 = true;
                        player.sendMessage(KTStrings.getFor(KTStrings.GRAVE_5_MINS, Component.text(id)).color(NamedTextColor.RED));
                        player.playSound(player.getLocation(), Sound.BLOCK_BELL_USE, 1.0f, 1.0f);
                    }
                    if (this.msLeft <= 1000 * 60 && !hasMessaged1) {
                        hasMessaged1 = true;
                        player.sendMessage(KTStrings.getFor(KTStrings.GRAVE_1_MIN, Component.text(id)).color(NamedTextColor.RED));
                        player.playSound(player.getLocation(), Sound.BLOCK_BELL_USE, 1.0f, 1.2f);
                    }
                    if (this.msLeft <= 1000 * 30 && !hasMessagedHalf) {
                        hasMessagedHalf = true;
                        player.sendMessage(KTStrings.getFor(KTStrings.GRAVE_30_SEC, Component.text(id)).color(NamedTextColor.RED));
                        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, .5f, .5f);
                    }
                }
            }
        }

        public Grave(OfflinePlayer owner, Inventory inventoryA, Inventory inventoryB, Location location, int experience, long msLeft) {
            this.owner = owner;
            this.mainInv = inventoryA;
            this.stationInv = inventoryB;
            this.location = location;
            this.experience = experience;
            this.msLeft = msLeft;
        }

        private static boolean slotIsOutput(InventoryType type, int slot) {
            switch (type) {
                case LOOM, SMITHING -> {
                    return slot == 3;
                }
                case CARTOGRAPHY, GRINDSTONE, ANVIL, MERCHANT -> {
                    return slot == 2;
                }
                case STONECUTTER -> {
                    return slot == 1;
                }
                case WORKBENCH, CRAFTING -> {
                    return slot == 0;
                }
            }
            return false;
        }

        private static boolean holdsItems(InventoryType type) {
            return switch (type) {
                case CHEST, DISPENSER, DROPPER, FURNACE, BREWING, ENDER_CHEST, BEACON, HOPPER, SHULKER_BOX, BARREL,
                     BLAST_FURNACE, SMOKER, CRAFTER -> true;
                default -> false;
            };
        }

        public Grave(Player owner, Location location) {
            this.owner = owner;
            this.location = location;
            PlayerInventory inv = owner.getInventory();
            this.mainInv = Bukkit.createInventory(null, MAIN_SIZE, KTStrings.getFor(KTStrings.GRAVE_TITLE));
            for (int i = 0; i < 36; i++) {
                ItemStack item = inv.getItem(i);
                if (item != null && !item.containsEnchantment(Enchantment.VANISHING_CURSE)) {
                    mainInv.setItem(i, item);
                    inv.setItem(i, null);
                }
            }
            {
                var item = inv.getHelmet();
                if (!item.containsEnchantment(Enchantment.VANISHING_CURSE)) {
                    mainInv.setItem(36, item);
                    inv.setHelmet(null);
                }
            }
            {
                var item = inv.getChestplate();
                if (!item.containsEnchantment(Enchantment.VANISHING_CURSE)) {
                    mainInv.setItem(37, item);
                    inv.setChestplate(null);
                }
            }
            {
                var item = inv.getLeggings();
                if (!item.containsEnchantment(Enchantment.VANISHING_CURSE)) {
                    mainInv.setItem(38, item);
                    inv.setLeggings(null);
                }
            }
            {
                var item = inv.getBoots();
                if (!item.containsEnchantment(Enchantment.VANISHING_CURSE)) {
                    mainInv.setItem(39, item);
                    inv.setBoots(null);
                }
            }

            {
                var item = inv.getItemInOffHand();
                if (!item.isEmpty() && !item.containsEnchantment(Enchantment.VANISHING_CURSE)) {
                    mainInv.setItem(40, item);
                    inv.setItemInOffHand(null);
                }
            }
            {
                var item = owner.getItemOnCursor();
                if (!item.isEmpty() && !item.containsEnchantment(Enchantment.VANISHING_CURSE)) {
                    mainInv.setItem(41, item);
                    owner.setItemOnCursor(null);
                }
            }
            this.id = highest;
            highest++;

            Inventory topInv = owner.getOpenInventory().getTopInventory();
            if (!holdsItems(topInv.getType())) {
                this.stationInv = Bukkit.createInventory(null, STATION_SIZE, KTStrings.getFor(KTStrings.GRAVE_BLOCK_TITLE));
                for (int i = 0; i < topInv.getSize(); i++) {
                    if (i > STATION_SIZE) {
                        Logger.error("Attempted to put an item past max inventory space in an inventory! Type: " + topInv.getType());
                        Plugin dsPlugin = Bukkit.getPluginManager().getPlugin("DiscordSRV");
                        if (dsPlugin != null && dsPlugin.isEnabled()) {
                            try {
                                Class<?> dsClass = Class.forName("github.scarsz.discordsrv.DiscordSRV");
                                Object dsInstance = dsClass.getMethod("getPlugin").invoke(null);

                                Method getChannel = dsClass.getMethod("getDestinationTextChannelForGameChannelName", String.class);
                                Object channel = getChannel.invoke(dsInstance, "global");

                                if (channel != null) {
                                    Method sendMessage = channel.getClass().getMethod("sendMessage", CharSequence.class);
                                    Object action = sendMessage.invoke(channel, "<@!1254538148755537971> An inventory type appears to be a storage inventory but not listed! Check " + topInv.getType());
                                    action.getClass().getMethod("queue").invoke(action);
                                }
                            } catch (Exception e) {
                                Logger.handleException(e);
                            }
                        }
                        break;
                    }
                    ItemStack item = topInv.getItem(i);
                    if (item != null && !slotIsOutput(topInv.getType(), i)) {
                        stationInv.setItem(i, item);
                        topInv.setItem(i, null);
                    }
                }
                if (stationInv.isEmpty()) {
                    stationInv = null;
                } else {
                    var toStation = new ItemStack(Material.ARROW);
                    ItemMeta toMeta = toStation.getItemMeta();
                    if (toMeta != null) {
                        toMeta.displayName(KTStrings.getFor(KTStrings.GRAVE_TO_BLOCK).decoration(TextDecoration.ITALIC, false));
                        toMeta.getPersistentDataContainer().set(guiKey, PersistentDataType.STRING, "station");
                        toMeta.getPersistentDataContainer().set(graveKey, PersistentDataType.INTEGER, id);
                        toStation.setItemMeta(toMeta);
                    }
                    mainInv.setItem(MAIN_SIZE - 1, toStation);
                    var fromStation = new ItemStack(Material.ARROW);
                    ItemMeta fromMeta = fromStation.getItemMeta();
                    if (fromMeta != null) {
                        fromMeta.displayName(KTStrings.getFor(KTStrings.GRAVE_FROM_BLOCK).decoration(TextDecoration.ITALIC, false));
                        fromMeta.getPersistentDataContainer().set(guiKey, PersistentDataType.STRING, "main");
                        fromMeta.getPersistentDataContainer().set(graveKey, PersistentDataType.INTEGER, id);
                        fromStation.setItemMeta(fromMeta);
                    }
                    stationInv.setItem(STATION_SIZE - 1, fromStation);
                }
            }

            this.experience = getPlayerExp(owner);
            if (owner.isOnline()) {
                if (msLeft > 0) {
                    createStand();
                }
            }
        }

        public OfflinePlayer getOwner() {
            return owner;
        }

        public Inventory getMainInv() {
            return mainInv;
        }

        public Inventory getStationInv() {
            return stationInv;
        }

        public Location getLocation() {
            return location;
        }

        public void createStand() {
            stand = location.getWorld().spawn(location.clone().addRotation(90, 0).subtract(0, 1.4375, 0), ArmorStand.class);
            stand.setGravity(false);
            stand.setItem(EquipmentSlot.HEAD, KTItems.createItem(KTItems.ItemType.GRAVE_HEAD));
            stand.setCustomNameVisible(true);
            stand.setBasePlate(false);
            stand.setPersistent(false);
            stand.setRemoveWhenFarAway(true);
            stand.setInvisible(true);
            stand.setInvulnerable(true);
            stand.setArms(false);
            stand.customName(KTStrings.getFor(KTStrings.GRAVE_NAME, Component.text(owner.getName() == null ? "Unknown" : owner.getName()).color(NamedTextColor.GOLD)));
            stand.getPersistentDataContainer().set(graveKey, PersistentDataType.INTEGER, this.id);
        }
    }

    @Override
    public void saveData() {
        FileConfiguration config = KamsTweaks.get().getDataConfig();
        config.set("graves", null);
        graves.forEach((id, grave) -> {
            Inventories.saveInventory(grave.getMainInv(), config, "graves." + id);
            Inventories.saveInventory(grave.getStationInv(), config, "graves." + id + ".station");
            config.set("graves." + id + ".location", LocationUtils.serializeLocation(grave.location));
            config.set("graves." + id + ".owner", grave.owner.getUniqueId().toString());
            config.set("graves." + id + ".xp", grave.experience);
            config.set("graves." + id + ".timeleft", grave.msLeft);
            config.set("graves." + id + ".m5", grave.hasMessaged5);
            config.set("graves." + id + ".m1", grave.hasMessaged1);
            config.set("graves." + id + ".m30", grave.hasMessagedHalf);
            config.set("graves." + id + ".me", grave.hasMessagedExpire);
        });
    }

    @Override
    public void loadData() {
        graves.clear();
        highest = 0;
        FileConfiguration config = KamsTweaks.get().getDataConfig();
        if (config.contains("graves")) {
            for (String key : Objects.requireNonNull(config.getConfigurationSection("graves")).getKeys(false)) {
                try {
                    String ownerStr = config.getString("graves." + key + ".owner");
                    UUID owner = ownerStr == null ? null : UUID.fromString(ownerStr);
                    String locationStr = config.getString("graves." + key + ".location");
                    int xp = (int) config.get("graves." + key + ".xp", 0);
                    assert locationStr != null;
                    Location location = checkLocation(LocationUtils.deserializeLocation(locationStr));
                    if (location == null || location.getWorld() == null) continue;
                    Inventory mainInv = Inventories.loadInventory(KTStrings.getFor(KTStrings.GRAVE_TITLE), MAIN_SIZE, config, "graves." + key);
                    Inventory stationInv = Inventories.loadInventory(KTStrings.getFor(KTStrings.GRAVE_BLOCK_TITLE), MAIN_SIZE, config, "graves." + key + ".station");
                    long timeLeft = config.getLong("graves." + key + ".timeleft", 1000 * 60 * 20);
                    Grave grave = new Grave(owner == null ? null : Bukkit.getServer().getOfflinePlayer(owner), mainInv, stationInv, location, xp, timeLeft);
                    grave.hasMessaged5 = config.getBoolean("graves." + key + ".m5", false);
                    grave.hasMessaged1 = config.getBoolean("graves." + key + ".m1", false);
                    grave.hasMessagedHalf = config.getBoolean("graves." + key + ".m30", false);
                    grave.hasMessagedExpire = config.getBoolean("graves." + key + ".me", false);
                    int id = Integer.parseInt(key);
                    if (highest < id) highest = id + 1;
                    grave.id = id;
                    graves.put(id, grave);
                    if (grave.owner.isOnline()) {
                        if (grave.msLeft > 0) {
                            grave.createStand();
                        }
                    }
                } catch (Exception e) {
                    Logger.handleException(e);
                }
            }
        }
    }

    // From essentials

    // Calculate amount of EXP needed to level up
    public static int getExpToLevelUp(int level) {
        if (level <= 15) {
            return 2 * level + 7;
        } else if (level <= 30) {
            return 5 * level - 38;
        } else {
            return 9 * level - 158;
        }
    }

    // Calculate total experience up to a level
    public static int getExpAtLevel(int level) {
        if (level <= 16) {
            return (int) (Math.pow(level, 2) + 6 * level);
        } else if (level <= 31) {
            return (int) (2.5 * Math.pow(level, 2) - 40.5 * level + 360.0);
        } else {
            return (int) (4.5 * Math.pow(level, 2) - 162.5 * level + 2220.0);
        }
    }

    // Calculate player's current EXP amount
    public static int getPlayerExp(Player player) {
        int exp = 0;
        int level = player.getLevel();

        // Get the amount of XP in past levels
        exp += getExpAtLevel(level);

        // Get amount of XP towards next level
        exp += Math.round(getExpToLevelUp(level) * player.getExp());

        return exp;
    }

    // Give or take EXP
    @SuppressWarnings("UnusedReturnValue")
    public static int changePlayerExp(Player player, int exp) {
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
