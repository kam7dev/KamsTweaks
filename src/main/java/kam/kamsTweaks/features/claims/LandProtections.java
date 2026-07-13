package kam.kamsTweaks.features.claims;

import com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent;
import kam.kamsTweaks.managers.KTItems;
import kam.kamsTweaks.managers.KTStrings;
import kam.kamsTweaks.utils.Config;
import kam.kamsTweaks.utils.Logger;
import kam.kamsTweaks.features.gameplay.SeedDispenser;
import kam.kamsTweaks.utils.LocationUtils;
import kam.kamsTweaks.features.claims.LandClaims.*;

import net.kyori.adventure.text.Component;

import net.minecraft.world.attribute.EnvironmentAttributes;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.*;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.entity.*;
import org.bukkit.entity.minecart.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.BlockProjectileSource;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.*;

import io.papermc.paper.event.player.*;
import com.destroystokyo.paper.event.block.*;
import org.bukkit.event.world.*;

import java.util.*;

import static kam.kamsTweaks.features.claims.EntityProtections.getTrueHitter;

public class LandProtections implements Listener {
    private LandClaims claims;

    public void setup(LandClaims landClaims) {
        this.claims = landClaims;
    }

    // shortcut
    void message(Entity player, Component component) {
        claims.instance.message(player, component);
    }

    private void applyCooldowns(Player p) {
        Set<Material> mats = new HashSet<>();

        for (ItemStack it : p.getInventory().getContents()) {
            if (it != null && (it.getType().isBlock() || it.getType().name().toLowerCase().contains("bucket"))) {
                mats.add(it.getType());
            }
        }
        for (ItemStack it : p.getInventory().getArmorContents()) {
            if (it != null && (it.getType().isBlock() || it.getType().name().toLowerCase().contains("bucket"))) {
                mats.add(it.getType());
            }
        }
        ItemStack off = p.getInventory().getItemInOffHand();
        if (off.getType().isBlock() || off.getType().name().toLowerCase().contains("bucket")) {
            mats.add(off.getType());
        }

        for (Material m : mats) {
            p.setCooldown(m, 20);
        }
    }

    @EventHandler
    public void onLecternTake(PlayerTakeLecternBookEvent e) {
        if (!Config.getBool("land-claims.enabled", true)) return;

        Player player = e.getPlayer();
        LandClaim claim = claims.getClaim(e.getLectern().getLocation());
        if (claim == null) return;
        var res = claim.hasPermissions(player, LandPermission.BLOCK_INTERACT, AdvancedLandPermission.LECTERN_TAKE);
        if (!res.result()) {
            message(player, KTStrings.getFor(KTStrings.LC_NO_PERM, res.message(), claim.getOwnerName()));
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onLecternInsert(PlayerInsertLecternBookEvent e) {
        if (!Config.getBool("land-claims.enabled", true)) return;

        Player player = e.getPlayer();
        LandClaim claim = claims.getClaim(e.getLectern().getLocation());
        if (claim == null) return;
        var res = claim.hasPermissions(player, LandPermission.BLOCK_INTERACT, AdvancedLandPermission.LECTERN_INSERT);
        if (!res.result()) {
            message(player, KTStrings.getFor(KTStrings.LC_NO_PERM, res.message(), claim.getOwnerName()));
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrainCauldron(CauldronLevelChangeEvent e) {
        if (!Config.getBool("land-claims.enabled", true)) return;
        Player who = e.getEntity() instanceof Player ? (Player) e.getEntity() : null;
        LandClaim claim = claims.getClaim(e.getBlock().getLocation());
        if (claim == null) return;
        var res = claim.hasPermissions(who, LandPermission.BLOCK_INTERACT, AdvancedLandPermission.DRAIN_CAULDRON);
        if (!res.result()) {
            if (who != null) message(who, KTStrings.getFor(KTStrings.LC_NO_PERM, res.message(), claim.getOwnerName()));
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onAnvil(AnvilDamagedEvent e) {
        if (!Config.getBool("land-claims.enabled", true)) return;
        e.getInventory().getViewers().stream()
                .filter(h -> h instanceof Player)
                .map(h -> (Player) h)
                .findFirst()
                .ifPresent(player -> {
                    LandClaim claim = claims.getClaim(e.getInventory().getLocation());
                    if (claim == null) return;
                    var res = claim.hasPermissions(player, LandPermission.BLOCK_INTERACT, AdvancedLandPermission.DAMAGE_ANVIL);
                    if (!res.result()) {
                        message(player, KTStrings.getFor(KTStrings.LC_NO_PERM, res.message(), claim.getOwnerName()));
                        e.setCancelled(true);
                    }
                });

    }

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        if (!Config.getBool("land-claims.enabled", true))
            return;
        Player player = e.getPlayer();
        LandClaim claim = claims.getClaim(e.getBlock().getLocation());
        if (claim == null) return;
        var res = claim.hasPermissions(player, LandPermission.BLOCK_PLACE, AdvancedLandPermission.EMPTY_BUCKETS);
        if (!res.result()) {
            e.setCancelled(true);
            message(player, KTStrings.getFor(KTStrings.LC_NO_PERM, res.message(), claim.getOwnerName()));
            applyCooldowns(player);
        }
    }

    @EventHandler
    public void onBucketPickup(PlayerBucketFillEvent e) {
        if (!Config.getBool("land-claims.enabled", true))
            return;
        Player player = e.getPlayer();
        LandClaim claim = claims.getClaim(e.getBlock().getLocation());
        if (claim == null) return;
        var res = claim.hasPermissions(player, LandPermission.BLOCK_BREAK, AdvancedLandPermission.FILL_BUCKETS);
        if (!res.result()) {
            e.setCancelled(true);
            message(player, KTStrings.getFor(KTStrings.LC_NO_PERM, res.message(), claim.getOwnerName()));
        }
    }

    static List<Material> interactExceptions = List.of(
            Material.CRAFTING_TABLE, Material.SMITHING_TABLE, Material.STONECUTTER,
            Material.CARTOGRAPHY_TABLE, Material.GRINDSTONE, Material.FLETCHING_TABLE, // This one's important
            Material.LOOM, Material.ENCHANTING_TABLE, Material.ENDER_CHEST,
            Material.LECTERN, Material.ANVIL, Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL
    );

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (!Config.getBool("land-claims.enabled", true))
            return;
        if (e.getItem() != null && KTItems.getType(e.getItem()) == KTItems.ItemType.CLAIM_TOOL) {
            if (claims.instance.useClaimTool(e)) return;
        }

        if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                assert e.getClickedBlock() != null;
                if (interactExceptions.contains(e.getClickedBlock().getType())) {
                    return;
                }
            }
            assert e.getClickedBlock() != null;
            //noinspection deprecation
            if (e.getClickedBlock().getType().isInteractable()) {
                Player player = e.getPlayer();
                assert e.getClickedBlock() != null;
                LandClaim claim = claims.getClaim(e.getClickedBlock().getLocation());
                if (claim == null) return;
                var nmsLevel = ((CraftWorld) e.getClickedBlock().getWorld()).getHandle();
                if (e.getClickedBlock().getType().toString().contains("DOOR")) {
                    var res = claim.hasPermissions(player, LandPermission.DOOR_INTERACT, LandPermission.BLOCK_INTERACT);
                    if (!res.result()) {
                        message(player, KTStrings.getFor(KTStrings.LC_NO_PERM, res.message(), claim.getOwnerName()));
                        e.setCancelled(true);
                    }
                } else if ((e.getClickedBlock().getType().equals(Material.RESPAWN_ANCHOR)
                        && !e.getClickedBlock().getWorld().isRespawnAnchorWorks())
                        || (e.getClickedBlock().getType().name().contains("BED")
                        && nmsLevel.environmentAttributes().getValue(EnvironmentAttributes.BED_RULE, CraftLocation.toBlockPos(e.getClickedBlock().getLocation())).explodes())){
                    var perm = LandPermission.BLOCK_BREAK;
                    if (!claim.hasPermission(player, perm)) {
                        message(player, KTStrings.getFor(KTStrings.LC_NO_PERM, perm.label, claim.getOwnerName()));
                        e.setCancelled(true);
                    }
                } else {
                    var perm = LandPermission.BLOCK_INTERACT;
                    if (!claim.hasPermission(player, LandPermission.BLOCK_INTERACT)) {
                        message(player, KTStrings.getFor(KTStrings.LC_NO_PERM, perm.label, claim.getOwnerName()));
                        e.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (!Config.getBool("land-claims.enabled", true))
            return;
        Player player = e.getPlayer();
        LandClaim claim = claims.getClaim(e.getBlock().getLocation());
        if (claim == null) return;
        var perm = LandPermission.BLOCK_BREAK;
        if (!claim.hasPermission(player, perm)) {
            message(player, KTStrings.getFor(KTStrings.LC_NO_PERM, perm.label, claim.getOwnerName()));
            e.setCancelled(true);
        }
    }

    Map<Player, ItemStack> openBoxes = new HashMap<>();

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        if (!Config.getBool("land-claims.enabled", true))
            return;
        if (KTItems.ItemType.CLAIM_TOOL.equals(KTItems.getType(e.getItemInHand()))) {
            e.setCancelled(true);
            return;
        }
        Player player = e.getPlayer();
        LandClaim claim = claims.getClaim(e.getBlock().getLocation());
        if (claim == null) return;
        var perm = LandPermission.BLOCK_PLACE;
        if (!claim.hasPermission(player, perm)) {
            e.setCancelled(true);
            applyCooldowns(player);
            ItemStack item = e.getItemInHand();
            if (item.getType() == Material.SHULKER_BOX || item.getType().name().endsWith("_SHULKER_BOX")) {
                ItemMeta meta = item.getItemMeta();
                if (!(meta instanceof BlockStateMeta bsm)) return;
                BlockState state = bsm.getBlockState();
                if (!(state instanceof ShulkerBox box)) return;
                Inventory inv = box.getInventory();
                e.getPlayer().openInventory(inv);
                e.getPlayer().getInventory().setItem(e.getHand(), new ItemStack(Material.AIR));
                openBoxes.put(e.getPlayer(), item);
            } else if (item.getType() == Material.ENDER_CHEST) {
                e.getPlayer().openInventory(e.getPlayer().getEnderChest());
            } else {
                message(player, KTStrings.getFor(KTStrings.LC_NO_PERM, perm.label, claim.getOwnerName()));
            }
        }
    }

    @EventHandler
    void onShulkerBoxGuiClose(InventoryCloseEvent e) {
        Player plr = (Player) e.getPlayer();
        ItemStack item = openBoxes.remove(plr);
        if (item == null) return;
        if (!(item.getItemMeta() instanceof BlockStateMeta meta)) return;
        if (!(meta.getBlockState() instanceof ShulkerBox box)) return;
        Inventory shulkerInv = box.getInventory();
        Inventory guiInv = e.getInventory();
        shulkerInv.setContents(guiInv.getContents());
        meta.setBlockState(box);
        item.setItemMeta(meta);
        Map<Integer, ItemStack> rip = plr.getInventory().addItem(item);
        if (!rip.isEmpty()) {
            for (ItemStack drop : rip.values()) {
                plr.getWorld().dropItem(plr.getLocation(), drop);
            }
        }
    }

    @EventHandler
    public void onDispenserDispense(BlockDispenseEvent e) {
        if (!Config.getBool("land-claims.enabled", true))
            return;
        LandClaim to = claims.getClaim(e.getBlock().getRelative(((Directional) e.getBlock().getBlockData()).getFacing()).getLocation());
        LandClaim in = claims.getClaim(e.getBlock().getLocation());
        if (to == null) return;
        if (!to.hasPermission(in != null ? in.owner : null, LandPermission.BLOCK_PLACE)) {
            String lowered = e.getItem().getType().toString().toLowerCase();
            if (lowered.contains("bucket") || lowered.contains("shulker") || SeedDispenser.matForSeed(e.getItem()) != null) {
                e.setCancelled(true);
            } else {
                switch (e.getItem().getType()) {
                    case FLINT_AND_STEEL, BONE_MEAL, CARVED_PUMPKIN, WITHER_SKELETON_SKULL, SHEARS, TNT, FIRE, ARMOR_STAND:
                        e.setCancelled(true);
                    default: break;
                }
            }
        } else {
            // claim in claims check
            if (in != null) {
                var placeTo = to.hasPermission(null, LandPermission.BLOCK_PLACE);
                var placeIn = in.hasPermission(null, LandPermission.BLOCK_PLACE);
                if (placeIn != placeTo && !placeTo) {
                    String lowered = e.getItem().getType().toString().toLowerCase();
                    if (lowered.contains("bucket") || lowered.contains("shulker") || SeedDispenser.matForSeed(e.getItem()) != null) {
                        e.setCancelled(true);
                    } else {
                        switch (e.getItem().getType()) {
                            case FLINT_AND_STEEL, BONE_MEAL, CARVED_PUMPKIN, WITHER_SKELETON_SKULL, SHEARS, TNT, FIRE,
                                 ARMOR_STAND:
                                e.setCancelled(true);
                            default:
                                break;
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onTreeGrow(StructureGrowEvent event) {
        if (!Config.getBool("land-claims.enabled", true))
            return;
        Block tree = event.getLocation().getBlock();
        LandClaim in = claims.getClaim(tree.getLocation());
        for (BlockState state : event.getBlocks()) {
            var block = state.getBlock();
            LandClaim to = claims.getClaim(block.getLocation());
            if (to == null) continue;
            if (!to.hasPermission(in == null ? null : in.owner, LandPermission.BLOCK_PLACE)) {
                event.setCancelled(true);
                return;
            }

            // claim in claims check
            if (in != null) {
                var placeTo = to.hasPermission(null, LandPermission.BLOCK_PLACE);
                var placeIn = in.hasPermission(null, LandPermission.BLOCK_PLACE);
                if (placeIn != placeTo && !placeTo) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onBonemeal(BlockFertilizeEvent e) {
        if (!Config.getBool("land-claims.enabled", true))
            return;
        Player player = e.getPlayer();
        LandClaim in = claims.getClaim(e.getBlock().getLocation());
        List<Component> who = new ArrayList<>();
        var perm = LandPermission.BLOCK_PLACE;
        if (in != null && !in.hasPermission(player, perm)) {
            who.add(in.getOwnerName());
            e.setCancelled(true);
            e.getBlock().getState().update(true, false);
        }

        // funnily enough, setCancelled doesn't affect blocks in this list
        List<BlockState> toProtect = new ArrayList<>();
        for (BlockState blockstate : e.getBlocks()) {
            Block block = blockstate.getBlock();
            LandClaim to = claims.getClaim(block.getLocation());
            if (to == null)
                continue;
            if (!to.hasPermission(player, perm)) {
                Component name = to.getOwnerName();
                if (!who.contains(name))
                    who.add(name);
                toProtect.add(blockstate);
            }
        }

        Component plrs = Component.empty();
        for (var plr : who) {
            if (!plrs.children().isEmpty())
                plrs = plrs.append(Component.text(", "));
            plrs = plrs.append(plr);
        }
        if (player != null && !plrs.children().isEmpty())
            message(player, KTStrings.getFor(KTStrings.LC_NO_PERM, perm.label, plrs));
        e.getBlocks().removeAll(toProtect);
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (!Config.getBool("land-claims.enabled", true))
            return;

        InventoryHolder entity = e.getInventory().getHolder();
        if (entity instanceof ChestBoat || entity instanceof StorageMinecart) {
            Player player = (Player) e.getPlayer();
            LandClaim claim = claims.getClaim(((Entity) entity).getLocation());
            if (claim == null) return;
            var perm = LandPermission.BLOCK_INTERACT;
            if (!claim.hasPermission(player, perm)) {
                message(player, KTStrings.getFor(KTStrings.LC_NO_PERM, perm.label, claim.getOwnerName()));
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent e) {
        if (!Config.getBool("land-claims.enabled", true))
            return;
        Entity entity = e.getRightClicked();
        if (!(entity instanceof ItemFrame) && !(entity instanceof LeashHitch)) return;
        Player player = e.getPlayer();
        LandClaim claim = claims.getClaim(entity.getLocation());
        if (claim == null) return;
        if (entity instanceof LeashHitch) {
            var perm = LandPermission.BLOCK_INTERACT;
            if (!claim.hasPermission(player, perm)) {
                message(player, KTStrings.getFor(KTStrings.LC_NO_PERM, perm.label, claim.getOwnerName()));
                e.setCancelled(true);
            }
        } else {
            ItemFrame frame = (ItemFrame) entity;
            boolean spin = !frame.getItem().isEmpty();
            LandClaim.MPRes res;
            if (spin) {
                res = claim.hasPermissions(player, LandPermission.BLOCK_INTERACT, AdvancedLandPermission.ITEM_FRAME_ITEM_ROTATE);
            } else {
                res = claim.hasPermissions(player, LandPermission.BLOCK_PLACE, AdvancedLandPermission.ITEM_FRAME_ITEM_PLACE);
            }
            if (!res.result()) {
                message(player, KTStrings.getFor(KTStrings.LC_NO_PERM, res.message(), claim.getOwnerName()));
                e.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onEntityInteractAt(PlayerInteractAtEntityEvent e) {
        onEntityInteract(e);
    }


    @EventHandler
    public void onArmorStandInteract(PlayerArmorStandManipulateEvent e) {
        if (!Config.getBool("land-claims.enabled", true))
            return;
        ArmorStand stand = e.getRightClicked();
        Player player = e.getPlayer();
        LandClaim claim = claims.getClaim(stand.getLocation());

        // So I don't spend time trying to remember next time
        // this is for if a stand was placed in one claim and moved to another claim
        LandClaim origin = null;
        if (stand.getPersistentDataContainer().has(new NamespacedKey("kamstweaks", "origin"))) {
            Location loc = LocationUtils.deserializeBlockPos(Objects.requireNonNull(stand.getPersistentDataContainer().get(new NamespacedKey("kamstweaks", "origin"), PersistentDataType.STRING)));
            origin = claims.getClaim(loc);
        }

        List<AdvancedLandPermission> actions = new ArrayList<>();
        if (!e.getArmorStandItem().isEmpty()) actions.add(AdvancedLandPermission.ARMOR_STAND_ITEM_TAKE);
        if (!e.getPlayerItem().isEmpty())  actions.add(AdvancedLandPermission.ARMOR_STAND_ITEM_PLACE);

        if (claim != null) {
            var res = claim.hasPermissions(player, LandPermission.BLOCK_INTERACT, actions.toArray(new AdvancedLandPermission[0]));
            if (!res.result()) {
                // Shouldn't need to keep message for the origin one.
                if (origin == null || !origin.hasPermissions(player, LandPermission.BLOCK_INTERACT, actions.toArray(new AdvancedLandPermission[0])).result()) {
                    message(player, KTStrings.getFor(KTStrings.LC_NO_PERM, res.message(), claim.getOwnerName()));
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onHangPlace(HangingPlaceEvent e) {
        if (!Config.getBool("land-claims.enabled", true))
            return;
        if (e.getPlayer() instanceof Player player) {
            LandClaim claim = claims.getClaim(e.getEntity().getLocation());
            if (claim == null) return;
            var perm = LandPermission.BLOCK_PLACE;
            if (!claim.hasPermission(player, perm)) {
                message(player, KTStrings.getFor(KTStrings.LC_NO_PERM, perm.label, claim.getOwnerName()));
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onHangDestroy(HangingBreakByEntityEvent e) {
        if (!Config.getBool("land-claims.enabled", true))
            return;
        LandClaim claim = claims.getClaim(e.getEntity().getLocation());
        var remover = e.getRemover();
        if (claim == null) return;
        var perm = LandPermission.BLOCK_BREAK;
        if (!claim.hasPermission(remover, perm)) {
            message(remover, KTStrings.getFor(KTStrings.LC_NO_PERM, perm.label, claim.getOwnerName()));
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onKnockback(EntityKnockbackByEntityEvent e) {
        if (!Config.getBool("land-claims.enabled", true))
            return;
        var entity = e.getEntity();
        if (!(entity instanceof Hangable || entity instanceof Hanging || entity instanceof ArmorStand
                || entity instanceof Boat || entity instanceof Minecart)) return;
        LandClaim claim = claims.getClaim(entity.getLocation());
        var attacker = getTrueHitter(e.getHitBy());
        if (claim != null && !claim.hasPermission(attacker, LandPermission.BLOCK_BREAK)) {
            e.setCancelled(true);
            message(attacker, KTStrings.getFor(KTStrings.LC_NO_PERM, KTStrings.getFor(KTStrings.LC_BLOCK_BREAK), claim.getOwnerName()));
        }
    }

    @EventHandler
    public void onEntityPlace(EntityPlaceEvent e) {
        if (!Config.getBool("land-claims.enabled", true))
            return;
        if (e.getPlayer() instanceof Player player) {
            LandClaim claim = claims.getClaim(e.getEntity().getLocation());
            if (claim == null) return;
            boolean res;
            Component msg;
            if (e.getEntity() instanceof Vehicle) {
                var r = claim.hasPermissions(player, LandPermission.BLOCK_PLACE, AdvancedLandPermission.VEHICLE_PLACE);
                msg = r.message();
                res = r.result();
            } else {
                var perm = LandPermission.BLOCK_PLACE;
                res = claim.hasPermission(player, perm);
                msg = perm.label;
            }
            if (!res) {
                message(player, KTStrings.getFor(KTStrings.LC_NO_PERM, msg, claim.getOwnerName()));
                e.setCancelled(true);
            } else {
                e.getEntity().getPersistentDataContainer().set(new NamespacedKey("kamstweaks", "origin"), PersistentDataType.STRING, LocationUtils.serializeBlockPos(e.getEntity().getLocation()));
            }
        }
    }

    @EventHandler
    public void onVehicleDestroy(VehicleDestroyEvent e) {
        if (!Config.getBool("land-claims.enabled", true))
            return;
        Entity entity = e.getVehicle();
        var attacker = e.getAttacker();
        LandClaim claim = claims.getClaim(e.getVehicle().getLocation());
        if (claim == null) return;
        var res = claim.hasPermissions(attacker, LandPermission.BLOCK_BREAK, AdvancedLandPermission.VEHICLE_BREAK);
        if (!res.result()) {
            message(attacker, KTStrings.getFor(KTStrings.LC_NO_PERM, res.message(), claim.getOwnerName()));
            e.setCancelled(true);
        }

    }

    @EventHandler
    public void onEntityHit(EntityDamageByEntityEvent e) {
        if (!Config.getBool("land-claims.enabled", true))
            return;
        Entity entity = e.getEntity();
        Entity damager = e.getDamageSource().getCausingEntity();
        if (damager != null) {
            var claim = Claims.get().entityClaims.getClaim(damager);
            if (claim != null && !claim.config.canAggro) {
                e.setCancelled(true);
                return;
            }
        }
        if (!(entity instanceof Hangable || entity instanceof Hanging || entity instanceof ArmorStand
                || entity instanceof Boat || entity instanceof Minecart))
            return;
        LandClaim claim = claims.getClaim(e.getEntity().getLocation());
        if (claim == null) return;
        LandClaim origin = null;
        if (entity.getPersistentDataContainer().has(new NamespacedKey("kamstweaks", "origin"))) {
            Location loc = LocationUtils.deserializeBlockPos(Objects.requireNonNull(entity.getPersistentDataContainer().get(new NamespacedKey("kamstweaks", "origin"), PersistentDataType.STRING)));
            origin = claims.getClaim(loc);
        }
        if (entity instanceof ItemFrame frame && !frame.getItem().isEmpty()) {
            var res = claim.hasPermissions(damager, LandPermission.BLOCK_BREAK, AdvancedLandPermission.ITEM_FRAME_ITEM_TAKE);
            if (!res.result()) {
                if (origin == null || !origin.hasPermissions(entity, LandPermission.BLOCK_BREAK, AdvancedLandPermission.ITEM_FRAME_ITEM_TAKE).result())  {
                    message(entity, KTStrings.getFor(KTStrings.LC_NO_PERM, res.message(), claim.getOwnerName()));
                    e.setCancelled(true);
                }
            }
        } else {
            var perm = LandPermission.BLOCK_BREAK;
            if (!claim.hasPermission(damager, perm)) {
                if (origin == null || !origin.hasPermission(entity, perm)) {
                    message(entity, KTStrings.getFor(KTStrings.LC_NO_PERM, perm.label, claim.getOwnerName()));
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onEntityKaboom(EntityExplodeEvent e) {
        if (!Config.getBool("land-claims.enabled", true))
            return;
        if (e.getEntity() instanceof TNTPrimed tnt) {
            Entity source;
            if (tnt.getSource() instanceof Entity entity) source = entity;
            else source = null;

            List<Component> who = new ArrayList<>();
            List<Block> toProtect = new ArrayList<>();
            var perm = LandPermission.BLOCK_BREAK;
            for (Block block : e.blockList()) {
                LandClaim to = claims.getClaim(block.getLocation());
                if (to == null)
                    continue;
                if (!to.hasPermission(source, perm)) {
                    Component name = to.getOwnerName();
                    if (!who.contains(name))
                        who.add(name);
                    toProtect.add(block);
                }
            }
            Component plrs = Component.empty();
            for (var plr : who) {
                if (!plrs.children().isEmpty())
                    plrs = plrs.append(Component.text(", "));
                plrs = plrs.append(plr);
            }
            if (!plrs.children().isEmpty())
                message(source, KTStrings.getFor(KTStrings.LC_NO_PERM, perm.label, plrs));
            e.blockList().removeAll(toProtect);
            for (Block block : toProtect) {
                block.getState().update(true, false);
            }
        } else if (e.getEntity() instanceof WindCharge charge) {
            Entity source;
            if (charge.getShooter() instanceof Entity entity) source = entity;
            else source = null;

            List<Component> who = new ArrayList<>();
            List<Block> toProtect = new ArrayList<>();
            var perm = LandPermission.BLOCK_INTERACT;
            for (Block block : e.blockList()) {
                LandClaim to = claims.getClaim(block.getLocation());
                if (to == null)
                    continue;
                if (!to.hasPermission(source, perm) && (block.getType().toString().contains("DOOR") && !to.hasPermission(source, LandPermission.DOOR_INTERACT))) {
                    Component name = to.getOwnerName();
                    if (!who.contains(name))
                        who.add(name);
                    toProtect.add(block);
                }
            }
            Component plrs = Component.empty();
            for (var plr : who) {
                if (!plrs.children().isEmpty())
                    plrs = plrs.append(Component.text(", "));
                plrs = plrs.append(plr);
            }
            if (!plrs.children().isEmpty())
                message(source, KTStrings.getFor(KTStrings.LC_NO_PERM, perm.label, plrs));
            e.blockList().removeAll(toProtect);
            for (Block block : toProtect) {
                block.getState().update(true, false);
            }
        } else {
            List<Block> toProtect = new ArrayList<>();
            for (Block block : e.blockList()) {
                LandClaim to = claims.getClaim(block.getLocation());
                if (to != null && !to.hasPermission(null, LandPermission.BLOCK_BREAK)) {
                    toProtect.add(block);
                }
            }
            e.blockList().removeAll(toProtect);
            for (Block block : toProtect) {
                block.getState().update(true, false);
            }
        }
    }

    @EventHandler
    public void onBlockKaboom(BlockExplodeEvent e) {
        if (!Config.getBool("land-claims.enabled", true))
            return;
        List<Block> toProtect = new ArrayList<>();
        LandClaim in = claims.getClaim(e.getBlock().getLocation());
        for (Block block : e.blockList()) {
            LandClaim to = claims.getClaim(block.getLocation());
            if (to == null) continue;
            if (!to.hasPermission(in != null ? in.owner : null, LandPermission.BLOCK_BREAK)) {
                toProtect.add(block);
                continue;
            }

            // claim in claims check
            if (in != null) {
                var placeTo = to.hasPermission(null, LandPermission.BLOCK_PLACE);
                var placeIn = in.hasPermission(null, LandPermission.BLOCK_PLACE);
                if (placeIn != placeTo && !placeTo) {
                    toProtect.add(block);
                }
            }
        }
        e.blockList().removeAll(toProtect);
        for (Block block : toProtect) {
            block.getState().update(true, false);
        }
    }

    @EventHandler
    public void onIgnite(BlockIgniteEvent event) {
        if (!Config.getBool("land-claims.enabled", true))
            return;
        LandClaim to = claims.getClaim(event.getBlock().getLocation());
        switch (event.getCause()) {
            case LAVA, SPREAD -> {
                LandClaim in = event.getIgnitingBlock() != null ? claims.getClaim(event.getIgnitingBlock().getLocation()) : null;
                if (to == null) return;
                if (!to.hasPermission(in == null ? null : in.owner, LandPermission.BLOCK_PLACE)) {
                    event.setCancelled(true);
                    return;
                }

                // claim in claims check
                if (in != null) {
                    var placeTo = to.hasPermission(null, LandPermission.BLOCK_PLACE);
                    var placeIn = in.hasPermission(null, LandPermission.BLOCK_PLACE);
                    if (placeIn != placeTo && !placeTo) {
                        event.setCancelled(true);
                    }
                }
            }
            case LIGHTNING, ENDER_CRYSTAL, EXPLOSION -> {
                if (to != null && !to.hasPermission(null, LandPermission.BLOCK_PLACE)) {
                    event.setCancelled(true);
                }
            }
            case FIREBALL, ARROW -> {
                if (event.getIgnitingEntity() instanceof Projectile projectile) {
                    var shooter = projectile.getShooter();
                    var perm = LandPermission.BLOCK_PLACE;
                    if (shooter instanceof Entity entity) {
                        if (to != null && !to.hasPermission(entity, perm)) {
                            message(entity, KTStrings.getFor(KTStrings.LC_NO_PERM, perm.label, to.getOwnerName()));
                            event.setCancelled(true);
                        }
                    } else if (shooter instanceof BlockProjectileSource block) {
                        var in = claims.getClaim(block.getBlock().getLocation());
                        if (to != null && !to.hasPermission(in == null ? null : in.owner, perm)) {
                            event.setCancelled(true);
                        }
                    } else {
                        if (to != null && !to.hasPermission(null, perm)) {
                            event.setCancelled(true);
                        }
                    }
                } else {
                    LandClaim claim = claims.getClaim(event.getBlock().getLocation());
                    if (event.getIgnitingEntity() instanceof Entity entity) {
                        var perm = LandPermission.BLOCK_PLACE;
                        if (claim != null && !claim.hasPermission(entity, perm)) {
                            message(entity, KTStrings.getFor(KTStrings.LC_NO_PERM, perm.label, claim.getOwnerName()));
                            event.setCancelled(true);
                        }
                    } else {
                        if (claim != null && !claim.hasPermission(null, LandPermission.BLOCK_PLACE)) {
                            event.setCancelled(true);
                        }
                    }
                }
            }
            case FLINT_AND_STEEL -> {
                if (event.getIgnitingEntity() instanceof Entity entity) {
                    var perm = LandPermission.BLOCK_PLACE;
                    if (to != null && !to.hasPermission(entity, perm)) {
                        message(entity, KTStrings.getFor(KTStrings.LC_NO_PERM, perm.label, to.getOwnerName()));
                        event.setCancelled(true);
                    }
                } else {
                    if (to == null) return;
                    LandClaim in = event.getIgnitingBlock() != null ? claims.getClaim(event.getIgnitingBlock().getLocation()) : null;

                    if (!to.hasPermission(in == null ? null : in.owner, LandPermission.BLOCK_PLACE)) {
                        event.setCancelled(true);
                        return;
                    }

                    // claim in claims check
                    if (in != null) {
                        var placeTo = to.hasPermission(null, LandPermission.BLOCK_PLACE);
                        var placeIn = in.hasPermission(null, LandPermission.BLOCK_PLACE);
                        if (placeIn != placeTo && !placeTo) {
                            event.setCancelled(true);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onSponge(SpongeAbsorbEvent e) {
        if (!Config.getBool("land-claims.enabled", true))
            return;
        List<BlockState> toProtect = new ArrayList<>();
        LandClaim in = claims.getClaim(e.getBlock().getLocation());
        for (BlockState blockstate : e.getBlocks()) {
            Block block = blockstate.getBlock();
            LandClaim to = claims.getClaim(block.getLocation());
            if (to == null) continue;
            if (!to.hasPermission(in != null ? in.owner : null, LandPermission.BLOCK_BREAK)) {
                toProtect.add(blockstate);
                continue;
            }

            // claim in claims check
            if (in != null) {
                var placeTo = to.hasPermission(null, LandPermission.BLOCK_PLACE);
                var placeIn = in.hasPermission(null, LandPermission.BLOCK_PLACE);
                if (placeIn != placeTo && !placeTo) {
                    toProtect.add(blockstate);
                }
            }
        }
        e.getBlocks().removeAll(toProtect);
    }

    @EventHandler
    public void onFlow(BlockFromToEvent event) {
        if (!Config.getBool("land-claims.enabled", true))
            return;
        LandClaim to = claims.getClaim(event.getToBlock().getLocation());
        LandClaim in = claims.getClaim(event.getBlock().getLocation());
        if (to == null) return;
        if (!to.hasPermission(in == null ? null : in.owner, LandPermission.BLOCK_PLACE)) {
            event.setCancelled(true);
            return;
        }

        // claim in claims check
        if (in != null) {
            var placeTo = to.hasPermission(null, LandPermission.BLOCK_PLACE);
            var placeIn = in.hasPermission(null, LandPermission.BLOCK_PLACE);
            if (placeIn != placeTo && !placeTo) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onSpread(BlockSpreadEvent event) {
        if (!Config.getBool("land-claims.enabled", true))
            return;
        LandClaim in = claims.getClaim(event.getSource().getLocation());
        LandClaim to = claims.getClaim(event.getBlock().getLocation());
        if (to == null) return;
        if (!to.hasPermission(in == null ? null : in.owner, LandPermission.BLOCK_PLACE)) {
            event.setCancelled(true);
            return;
        }

        // claim in claims check
        if (in != null) {
            var placeTo = to.hasPermission(null, LandPermission.BLOCK_PLACE);
            var placeIn = in.hasPermission(null, LandPermission.BLOCK_PLACE);
            if (placeIn != placeTo && !placeTo) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBurn(BlockBurnEvent event) {
        if (!Config.getBool("land-claims.enabled", true))
            return;
        LandClaim in = null;
        if (event.getIgnitingBlock() != null) {
            in = claims.getClaim(event.getIgnitingBlock().getLocation());
        }
        LandClaim to = claims.getClaim(event.getBlock().getLocation());
        if (to == null) return;
        if (!to.hasPermission(in == null ? null : in.owner, LandPermission.BLOCK_PLACE)) {
            event.setCancelled(true);
            return;
        }

        // claim in claims check
        if (in != null) {
            var placeTo = to.hasPermission(null, LandPermission.BLOCK_PLACE);
            var placeIn = in.hasPermission(null, LandPermission.BLOCK_PLACE);
            if (placeIn != placeTo && !placeTo) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPistonExtend(org.bukkit.event.block.BlockPistonExtendEvent event) {
        if (!Config.getBool("land-claims.enabled", true))
            return;
        Block piston = event.getBlock();
        Directional dir = (Directional) piston.getBlockData();
        BlockFace face = dir.getFacing();
        LandClaim in = claims.getClaim(piston.getLocation());

        Block front = piston.getRelative(face);
        LandClaim to = claims.getClaim(front.getLocation());
        if (to != null) {
            if (!to.hasPermission(in == null ? null : in.owner, LandPermission.BLOCK_PLACE)) {
                event.setCancelled(true);
                return;
            }

            if (in != null) {
                var placeTo = to.hasPermission(null, LandPermission.BLOCK_PLACE);
                var placeIn = in.hasPermission(null, LandPermission.BLOCK_PLACE);
                if (placeIn != placeTo && !placeTo) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
        for (Block block : event.getBlocks()) {
            Block target = block.getRelative(face);
            LandClaim bIn = claims.getClaim(target.getLocation());
            if (bIn != null) {
                if (!bIn.hasPermission(in == null ? null : in.owner, LandPermission.BLOCK_PLACE)) {
                    event.setCancelled(true);
                    return;
                }
                if (in != null) {
                    var placeTo = bIn.hasPermission(null, LandPermission.BLOCK_PLACE);
                    var placeIn = in.hasPermission(null, LandPermission.BLOCK_PLACE);
                    if (placeIn != placeTo && !placeTo) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
            LandClaim bTo = claims.getClaim(target.getLocation());
            if (bTo != null) {
                if (!bTo.hasPermission(in == null ? null : in.owner, LandPermission.BLOCK_PLACE)) {
                    event.setCancelled(true);
                    return;
                }
                if (in != null) {
                    var placeTo = bTo.hasPermission(null, LandPermission.BLOCK_PLACE);
                    var placeIn = in.hasPermission(null, LandPermission.BLOCK_PLACE);
                    if (placeIn != placeTo && !placeTo) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPistonRetract(org.bukkit.event.block.BlockPistonRetractEvent event) {
        if (!Config.getBool("land-claims.enabled", true))
            return;
        Block piston = event.getBlock();
        Directional dir = (Directional) piston.getBlockData();
        BlockFace face = dir.getFacing().getOppositeFace();

        LandClaim in = claims.getClaim(piston.getLocation());
        Block front = piston.getRelative(face);
        LandClaim to = claims.getClaim(front.getLocation());
        if (to != null) {
            if (!to.hasPermission(in == null ? null : in.owner, LandPermission.BLOCK_PLACE)) {
                event.setCancelled(true);
                return;
            }

            if (in != null) {
                var placeTo = to.hasPermission(null, LandPermission.BLOCK_PLACE);
                var placeIn = in.hasPermission(null, LandPermission.BLOCK_PLACE);
                if (placeIn != placeTo && !placeTo) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
        for (Block block : event.getBlocks()) {
            Block target = block.getRelative(face);
            LandClaim bIn = claims.getClaim(block.getLocation());
            if (bIn != null) {
                if (!bIn.hasPermission(in == null ? null : in.owner, LandPermission.BLOCK_PLACE)) {
                    event.setCancelled(true);
                    return;
                }
                if (in != null) {
                    var placeTo = bIn.hasPermission(null, LandPermission.BLOCK_PLACE);
                    var placeIn = in.hasPermission(null, LandPermission.BLOCK_PLACE);
                    if (placeIn != placeTo && !placeTo) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
            LandClaim bTo = claims.getClaim(target.getLocation());
            if (bTo != null) {
                if (!bTo.hasPermission(in == null ? null : in.owner, LandPermission.BLOCK_PLACE)) {
                    event.setCancelled(true);
                    return;
                }
                if (in != null) {
                    var placeTo = bTo.hasPermission(null, LandPermission.BLOCK_PLACE);
                    var placeIn = in.hasPermission(null, LandPermission.BLOCK_PLACE);
                    if (placeIn != placeTo && !placeTo) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onItemDamage(EntityDamageByEntityEvent event) {
        if (!Config.getBool("land-claims.enabled", true))
            return;
        if (!(event.getEntity() instanceof Item)) return;
        Entity damager = event.getDamager();
        LandClaim claim = claims.getClaim(event.getEntity().getLocation());
        if (damager instanceof TNTPrimed tnt) {
            if (tnt.getSource() instanceof Entity entity) {
                if (claim != null && !claim.hasPermission(entity, LandPermission.BLOCK_BREAK)) {
                    event.setCancelled(true);
                }
            } else {
                if (claim != null && !claim.hasPermission(null, LandPermission.BLOCK_BREAK)) {
                    event.setCancelled(true);
                }
            }
        } else if (damager instanceof EnderCrystal || damager instanceof Creeper || damager instanceof ExplosiveMinecart) {
            if (claim != null && !claim.hasPermission(damager, LandPermission.BLOCK_BREAK)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onMobInteract(EntityInteractEvent event) {
        if (!Config.getBool("land-claims.enabled", true))
            return;
        if (event.getEntity() instanceof Player)
            return;
        LandClaim claim = claims.getClaim(event.getBlock().getLocation());
        if (event.getBlock().getType() == Material.FARMLAND) {
            if (claim != null && !claim.hasPermission(event.getEntity(), LandPermission.BLOCK_BREAK)) {
                event.setCancelled(true);
            }
        } else if (event.getEntity() instanceof ArmorStand stand && stand.getPersistentDataContainer().has(new NamespacedKey("kamstweaks", "grave"))) {
            event.setCancelled(true);
        } else {
            if (claim != null && !claim.hasPermission(event.getEntity(), LandPermission.BLOCK_INTERACT)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerTrample(PlayerInteractEvent event) {
        if (!Config.getBool("land-claims.enabled", true))
            return;
        if (event.getAction() != Action.PHYSICAL)
            return;
        if (event.getClickedBlock() == null)
            return;
        Player player = event.getPlayer();
        LandClaim claim = claims.getClaim(event.getClickedBlock().getLocation());
        if (event.getClickedBlock().getType() == Material.FARMLAND || event.getClickedBlock().getType() == Material.TURTLE_EGG) {
            var perm = LandPermission.BLOCK_BREAK;
            if (claim != null && !claim.hasPermission(player, perm)) {
                message(player, KTStrings.getFor(KTStrings.LC_NO_PERM, perm.label, claim.getOwnerName()));
                event.setCancelled(true);
            }
        } else {
            var perm = LandPermission.BLOCK_INTERACT;
            if (claim != null && !claim.hasPermission(player, perm)) {
                message(player, KTStrings.getFor(KTStrings.LC_NO_PERM, perm.label, claim.getOwnerName()));
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (!Config.getBool("land-claims.enabled", true))
            return;
        if (event.getEntityType() == EntityType.SHEEP || event.getEntityType() == EntityType.BEE)
            return;
        LandClaim claim = claims.getClaim(event.getBlock().getLocation());
        var perm = LandPermission.BLOCK_BREAK;
        switch (event.getEntity()) {
            case Player player -> {
                if (claim != null && !claim.hasPermission(player, perm)) {
                    message(player, KTStrings.getFor(KTStrings.LC_NO_PERM, perm.label, claim.getOwnerName()));
                    event.setCancelled(true);
                }
            }
            case ArmorStand stand when stand.getPersistentDataContainer().has(new NamespacedKey("kamstweaks", "grave")) -> event.setCancelled(true);
            case FallingBlock fb -> {
                if (event.getTo() == Material.AIR && !fb.getPersistentDataContainer().has(new NamespacedKey("kamstweaks", "startlocation"), PersistentDataType.STRING)) {
                    var newClaim = claims.getClaim(event.getBlock().getRelative(BlockFace.DOWN).getLocation());

                    if (claim != null && !claim.hasPermission(newClaim != null ? newClaim.owner : null, perm)) {
                        event.setCancelled(true);
                        fb.remove();
                    }

                    if (newClaim != null && !newClaim.hasPermission(claim != null ? claim.owner : null, perm)) {
                        event.setCancelled(true);
                        ItemStack drop = new ItemStack(event.getBlockData().getMaterial());
                        ItemMeta meta = drop.getItemMeta();
                        if (meta instanceof BlockDataMeta bdMeta) {
                            bdMeta.setBlockData(event.getBlockData());
                            drop.setItemMeta(bdMeta);
                        }
                        fb.getWorld().dropItemNaturally(fb.getLocation(), drop);
                        fb.remove();
                    }
                    fb.getPersistentDataContainer().set(new NamespacedKey("kamstweaks", "startlocation"), PersistentDataType.STRING, LocationUtils.serializeBlockPos(fb.getLocation()));
                } else {
                    LandClaim orig = null;
                    if (fb.getPersistentDataContainer().has(new NamespacedKey("kamstweaks", "startlocation"), PersistentDataType.STRING)) {
                        Location origin = LocationUtils.deserializeBlockPos(Objects.requireNonNull(fb.getPersistentDataContainer().get(new NamespacedKey("kamstweaks", "startlocation"), PersistentDataType.STRING)));
                        orig = claims.getClaim(origin);
                    }
                    if (claim != null && !claim.hasPermission(orig != null ? orig.owner : null, perm)) {
                        event.setCancelled(true);
                        ItemStack drop = new ItemStack(event.getBlockData().getMaterial());
                        ItemMeta meta = drop.getItemMeta();
                        if (meta instanceof BlockDataMeta bdMeta) {
                            bdMeta.setBlockData(event.getBlockData());
                            drop.setItemMeta(bdMeta);
                        }
                        fb.getWorld().dropItemNaturally(fb.getLocation(), drop);
                        fb.remove();
                    }
                }
            }
            default -> {
                if (claim != null && !claim.hasPermission(event.getEntity(), perm)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onEntityBlockForm(EntityBlockFormEvent event) {
        if (!Config.getBool("land-claims.enabled", true))
            return;
        LandClaim claim = claims.getClaim(event.getBlock().getLocation());
        var perm = LandPermission.BLOCK_PLACE;
        if (claim != null && !claim.hasPermission(event.getEntity(), perm)) {
            message(event.getEntity(), KTStrings.getFor(KTStrings.LC_NO_PERM, perm.label, claim.getOwnerName()));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onHopperPull(InventoryMoveItemEvent event) {
        if (!Config.getBool("land-claims.enabled", true))
            return;
        try {
            if (event.getSource().getHolder() == null || event.getDestination().getHolder() == null)
                return;
            LandClaim in;
            LandClaim to;
            if (event.getSource().getHolder() instanceof BlockInventoryHolder holder) {
                in = claims.getClaim(holder.getBlock().getLocation());
            } else if (event.getSource().getHolder() instanceof DoubleChest chest) {
                in = claims.getClaim(chest.getLocation());
            } else if (event.getSource().getHolder() instanceof HopperMinecart minecart) {
                in = claims.getClaim(minecart.getLocation());
            } else if (event.getSource().getHolder() instanceof StorageMinecart minecart) {
                in = claims.getClaim(minecart.getLocation());
            } else {
                return;
            }
            if (event.getDestination().getHolder() instanceof BlockInventoryHolder holder) {
                to = claims.getClaim(holder.getBlock().getLocation());
            } else if (event.getSource().getHolder() instanceof DoubleChest chest) {
                to = claims.getClaim(chest.getLocation());
            } else if (event.getDestination().getHolder() instanceof HopperMinecart minecart) {
                to = claims.getClaim(minecart.getLocation());
            } else if (event.getDestination().getHolder() instanceof StorageMinecart minecart) {
                to = claims.getClaim(minecart.getLocation());
            } else {
                return;
            }
            if (in != null && !in.hasPermission(to != null ? to.owner : null, LandPermission.BLOCK_INTERACT)) {
                event.setCancelled(true);
            }
            if (to != null && !to.hasPermission(in != null ? in.owner : null, LandPermission.BLOCK_INTERACT)) {
                event.setCancelled(true);
            }
        } catch (Exception e) {
            Logger.handleException(e);
        }
    }
}
