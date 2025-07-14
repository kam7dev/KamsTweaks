package kam.kamsTweaks.features.landclaims;

import com.destroystokyo.paper.profile.ProfileProperty;
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.utils.Pair;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

import static org.bukkit.Bukkit.*;

public class LandClaimGui implements Listener {

    Map<Player, GuiInventory> guis = new HashMap<>();

//    public final Inventory claimsInv = Bukkit.createInventory(null, 9, Component.text("Land Claims"));
//    public final ItemStack claimGuiItem = createGuiItem(Material.SHIELD, Component.text("Claim").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false), Component.text("Claim land.").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
//    public final ItemStack showClaimsItem = createGuiItem(Material.GLASS, Component.text("View Claims").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false), Component.text("Show claims with particles.").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
//    public final ItemStack editPermissionItem = createGuiItem(Material.IRON_DOOR, Component.text("Edit Permissions").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false), Component.text("Edit permissions for the claim you're currently in.").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false), Component.text("Only works if you own the claim you're in").color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));

//    public final Inventory editInv = Bukkit.createInventory(null, 9, Component.text("Land Claims"));
//    public final ItemStack defaultPermissionItem = createGuiItem(Material.LEVER, Component.text("Change Default Permission").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false), Component.text("Change what all players can do by default.").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
//    public final ItemStack managePlayersItem = createGuiItem(Material.PLAYER_HEAD, Component.text("Manage Player Permissions").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false), Component.text("Give different players specific permissions.").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
//    public final ItemStack deleteClaimItem = createGuiItem(Material.BARRIER, Component.text("Delete Claim").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false), Component.text("Delete this claim.").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
//
//    public final Inventory permInv = Bukkit.createInventory(null, 9, Component.text("Land Claims"));
//    public final ItemStack nonePermItem = createGuiItem(Material.RED_CONCRETE, Component.text("No block interaction").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
//    public final ItemStack doorPermItem = createGuiItem(Material.ORANGE_CONCRETE, Component.text("Doors only").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
//    public final ItemStack interactPermItem = createGuiItem(Material.PURPLE_CONCRETE, Component.text("Interact with blocks").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
//    public final ItemStack blockPermItem = createGuiItem(Material.CYAN_CONCRETE, Component.text("Break/place any blocks").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));

    LandClaims lc;

    public LandClaimGui(LandClaims lc) {
        this.lc = lc;
//        claimsInv.setItem(3, editPermissionItem);
//        claimsInv.setItem(4, claimGuiItem);
//        claimsInv.setItem(5, showClaimsItem);
//
//        editInv.setItem(3, defaultPermissionItem);
//        editInv.setItem(4, managePlayersItem);
//        editInv.setItem(5, deleteClaimItem);
//
//        permInv.setItem(1, nonePermItem);
//        permInv.setItem(3, doorPermItem);
//        permInv.setItem(5, interactPermItem);
//        permInv.setItem(7, blockPermItem);
    }

    protected static ItemStack createGuiItem(final Material material, final Component name, final Component... lore) {
        final ItemStack item = new ItemStack(material, 1);
        final ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        if (lore != null) meta.lore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }



    // Events
    @EventHandler
    public void onInventoryDrag(final InventoryDragEvent e) {
        if (guis.containsKey((Player) e.getWhoClicked())) {
            for (GuiInventory.Screen screen : guis.get((Player) e.getWhoClicked()).screens) {
                if (e.getInventory().equals(screen.inv)) {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onLeave(final PlayerQuitEvent e) {
        guis.remove(e.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(final InventoryClickEvent e) {
        if (guis.containsKey((Player) e.getWhoClicked())) {
            for (GuiInventory.Screen screen : guis.get((Player) e.getWhoClicked()).screens) {
                if (e.getInventory().equals(screen.inv)) {
                    e.setCancelled(true);
                    if (Objects.equals(e.getCurrentItem(), screen.leftArrow)) {
                        screen.previousPage();
                    } else if (Objects.equals(e.getCurrentItem(), screen.rightArrow)) {
                        screen.nextPage();
                    } else {
                        screen.items.forEach((slot, item) -> {
                            if (item.first.equals(e.getCurrentItem())) {
                                item.second.run((Player) e.getWhoClicked(), e.getInventory(), e.getCurrentItem());
                            }
                        });
                    }
                }
            }
        }
    }

    public static class GuiInventory {
        static class Screen {
            public Map<Integer, Pair<ItemStack, GuiRunnable>> items = new HashMap<>();
            public Inventory inv;
            private int limit;
            private int size;
            public Component title;
            int page = 0;
            int highest = 0;
            ItemStack leftArrow;
            ItemStack paper;
            ItemStack rightArrow;
            public Screen(int size, Component title) {
                this.title = title;
                this.size = Math.min(54, Math.max(9, (size / 9) * 9));

                leftArrow = new ItemStack(Material.ARROW);
                ItemMeta meta = leftArrow.getItemMeta();
                meta.displayName(Component.text("Previous Page").decoration(TextDecoration.ITALIC, false));
                leftArrow.setItemMeta(meta);

                paper = new ItemStack(Material.PAPER);

                rightArrow = new ItemStack(Material.ARROW);
                meta = rightArrow.getItemMeta();
                meta.displayName(Component.text("Next Page").decoration(TextDecoration.ITALIC, false));
                rightArrow.setItemMeta(meta);

                changeSize(size);
            }
            void updateItems() {
                highest = 0;
                for (Integer key : items.keySet()) {
                    if (key.compareTo(highest) > 0) {
                        highest = key;
                    }
                }
                inv.clear();
                limit = highest > size ? size - 9 : size;
                ItemMeta meta = paper.getItemMeta();
                meta.displayName(Component.text("Page " + (page + 1) + " / " + (limit == 0 ? 1 : (int) Math.ceil((float) highest / limit))).decoration(TextDecoration.ITALIC, false));
                paper.setItemMeta(meta);
                items.forEach((pos, item) -> {
                    if (pos >= page * limit && pos < (page + 1) * limit) {
                        inv.setItem(pos - page * limit, item.first);
                    }
                });
                if (highest > limit) {
                    inv.setItem(size - 6, leftArrow);
                    inv.setItem(size - 5, paper);
                    inv.setItem(size - 4, rightArrow);
                }
            }
            public void addItem(ItemStack item, GuiRunnable callback, int position) {
                items.put(position, new Pair<>(item, callback));
                highest = Math.max(highest, position);
            }
            public void removeItem(int position) {
                items.remove(position);
            }
            public void clearItems() {
                items.clear();
            }
            void goToPage(int page) {
                if (page > highest / limit) return;
                if (page < 0) return;
                this.page = page;
                updateItems();
            }
            void nextPage() {
                goToPage(page + 1);
            }
            void previousPage() {
                goToPage(page - 1);
            }
            void changeSize(int size) {
                this.size = Math.min(54, Math.max(9, (size / 9) * 9));
                limit = highest > this.size ? this.size - 9 : this.size;
                inv = createInventory(null, Math.min(size, 54), title);
                updateItems();
            }
            void changeTitle(Component title) {
                this.title = title;
                inv = createInventory(null, Math.min(size, 54), title);
                updateItems();
            }
        }

        public LandClaims.Claim claim;
        public OfflinePlayer editing;

        List<Screen> screens = new ArrayList<>();
        public String confirmType;
        int currentScreen = 0;
        Player player;

        public GuiInventory(Player player) {
            this.player = player;
        }

        Screen getScreen(int screen) {
            return screens.get(screen);
        }

        void changeToScreen(int screen) {
            if (screens.size() <= screen) return;
            currentScreen = screen;
            Screen current = screens.get(currentScreen);
            if (current == null) return;
            current.updateItems();
            player.openInventory(current.inv);
        }

        void changeToScreen(Screen screen) {
            int i = screens.indexOf(screen);
            if (i == -1) return;
            changeToScreen(i);
        }

        Screen addScreen(int size, Component title) {
            Screen screen = new Screen(size, title);
            screens.add(screen);
            return screen;
        }

        interface GuiRunnable {
            void run(Player player, Inventory inventory, ItemStack item);
        }

        void show(Location target) {
            if (screens.size() <= currentScreen) return;
            Screen current = screens.get(currentScreen);
            if (current == null) return;
            claim = KamsTweaks.getInstance().m_landClaims.getClaim(target);
            current.updateItems();
            player.openInventory(current.inv);
        }

        void show(Location target, int page) {
            if (screens.size() <= page) return;
            currentScreen = page;
            editing = null;
            show(target);
        }

        void close(boolean alrClosed) {
            if (!alrClosed) {
                Screen current = screens.get(currentScreen);
                if (current == null) return;
                if (player.getOpenInventory().getTopInventory() == current.inv) player.closeInventory();
            }
            var guis = KamsTweaks.getInstance().m_landClaims.gui.guis;
            guis.remove(player);
        }
    }

    void showClaimGui(Player plr, Location target) {
        if (!guis.containsKey(plr)) {
            GuiInventory ui = new GuiInventory(plr);
            GuiInventory.Screen homeScreen = ui.addScreen(9, Component.text("Land Claims"));
            GuiInventory.Screen editScreen = ui.addScreen(9, Component.text("Edit Claim Permissions"));
            GuiInventory.Screen permScreen = ui.addScreen(9, Component.text("Edit Permissions"));
            GuiInventory.Screen playerScreen = ui.addScreen(54, Component.text("Edit Player Permissions"));
            GuiInventory.Screen confirmScreen = ui.addScreen(9, Component.text("Are you sure?"));

            homeScreen.addItem(createGuiItem(Material.IRON_DOOR, Component.text("Edit Permissions").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false), Component.text("Edit permissions for the claim you're currently in.").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false), Component.text("Only works if you own the claim you're in").color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false)), (player, inv, item) -> {
                if (ui.claim == null) {
                    player.sendMessage(Component.text("This area isn't claimed.").color(NamedTextColor.RED));
                } else if(ui.claim.m_owner == null || !ui.claim.m_owner.getUniqueId().equals(player.getUniqueId())) {
                    if (player.hasPermission("kamstweaks.landclaims.manageall")) {
                        ui.confirmType = "admin-bypass";
                        confirmScreen.changeTitle(Component.text("Edit " + (ui.claim.m_owner == null ? "The Server" : ui.claim.m_owner.getName() == null ? "Unknown" : ui.claim.m_owner.getName()) + "'s claim?"));
                        ui.changeToScreen(confirmScreen);
                    } else {
                        player.sendMessage(Component.text("You don't own this area.").color(NamedTextColor.RED));
                    }
                } else {
                    ui.changeToScreen(editScreen);
                }
            }, 3);
            homeScreen.addItem(createGuiItem(Material.SHIELD, Component.text("Claim").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false), Component.text("Claim land.").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)), (player, inv, item) -> {
                for (LandClaims.Claim claim : lc.claiming) {
                    if (claim.m_owner.getUniqueId().equals(player.getUniqueId())) {
                        player.sendMessage(Component.text("You're already trying to claim other land!").color(NamedTextColor.RED));
                        return;
                    }
                }
                int count = 0;
                for (LandClaims.Claim claim : lc.claims) {
                    if (claim.m_owner.getUniqueId().equals(player.getUniqueId())) count++;
                }
                var max = KamsTweaks.getInstance().getConfig().getInt("land-claims.max-claims", 10);
                if (count > max) {
                    player.sendMessage(Component.text("You already have the max number of claims! (" + count + "/" + max + ")").color(NamedTextColor.RED));
                    return;
                }
                player.sendMessage(Component.text("Click the first corner of where you want to claim with your claim tool. (If you lost it, run ").append(Component.text("/claims get-tool").clickEvent(ClickEvent.runCommand("claims get-tool")).color(NamedTextColor.YELLOW)).append(Component.text(")").color(NamedTextColor.GOLD)).color(NamedTextColor.GOLD));
                LandClaims.Claim c = new LandClaims.Claim(player, null, null);
                lc.claiming.add(c);
                ui.close(false);
            }, 4);
            homeScreen.addItem(createGuiItem(Material.GLASS, Component.text("View Claims").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false), Component.text("Show claims with particles.").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)), (player, inv, item) -> {
                for (LandClaims.Claim claim : lc.claims) {
                    Color c;
                    if (claim.m_owner != null && claim.m_owner.getUniqueId().equals(player.getUniqueId())) {
                        c = Color.GREEN;
                    } else {
                        c = switch (claim.m_perms.getOrDefault(getServer().getOfflinePlayer(player.getUniqueId()), claim.m_default)) {
                            case NONE -> Color.RED;
                            case DOORS -> Color.ORANGE;
                            case INTERACT -> Color.PURPLE;
                            case BLOCKS -> Color.AQUA;
                        };
                    }
                    lc.showArea(player, claim.m_start, claim.m_end, 1, 20 * 10, c);
                    Location l = new Location(claim.m_start.getWorld(), (claim.m_start.x() + claim.m_end.x())/2, (claim.m_start.y() + claim.m_end.y())/2, (claim.m_start.z() + claim.m_end.z())/2);
                    TextDisplay display = player.getWorld().spawn(l, TextDisplay.class, entity -> {
                        String s;
                        if (claim.m_owner != null && claim.m_owner.getUniqueId().equals(player.getUniqueId())) {
                            s = "You own this claim.";
                        } else {
                            s = switch(claim.m_perms.getOrDefault(getServer().getOfflinePlayer(player.getUniqueId()), claim.m_default)) {
                                case NONE -> "You can only look around.";
                                case DOORS -> "You can use doors.";
                                case INTERACT -> "You can interact with blocks.";
                                case BLOCKS -> "You can interact and manage blocks.";
                            };
                        }
                        entity.text(Component.text("Owned by ").append(Component.text(claim.m_owner == null ? "the server" : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName(), NamedTextColor.GOLD).appendNewline().append(Component.text(s))));
                        entity.setBillboard(Display.Billboard.CENTER);
                        lc.cleanupList.add(entity);
                    });
                    getServer().getScheduler().scheduleSyncDelayedTask(KamsTweaks.getInstance(), () -> {
                        display.remove();
                        lc.cleanupList.remove(display);
                    }, 20 * 10);
                }
                ui.close(false);
            }, 5);

            editScreen.addItem(createGuiItem(Material.LEVER, Component.text("Change Default Permission").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false), Component.text("Change what all players can do by default.").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)), (player, inv, item) -> {
                permScreen.changeTitle(Component.text("Edit Default Permissions"));
                ui.editing = null;
                ui.changeToScreen(permScreen);
            }, 3);
            editScreen.addItem(createGuiItem(Material.PLAYER_HEAD, Component.text("Manage Player Permissions").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false), Component.text("Give different players specific permissions.").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)), (player, inv, item) -> {
                playerScreen.clearItems();
                playerScreen.changeSize(getOfflinePlayers().length);
                int i = 0;
                for (OfflinePlayer oplr : getOfflinePlayers()) {
                    if (ui.claim.m_owner != null && oplr.getUniqueId().equals(ui.claim.m_owner.getUniqueId())) continue;
                    ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta meta = (SkullMeta) head.getItemMeta();
                    try {
                        meta.setOwningPlayer(oplr);
                    } catch (Exception e) {
                        KamsTweaks.getInstance().getLogger().warning(e.getMessage());
                    }
                    meta.lore(List.of(new TextComponent[]{
                            switch (ui.claim.m_perms.getOrDefault(oplr, ui.claim.m_default)) {
                                case NONE -> Component.text("None").color(NamedTextColor.RED);
                                case DOORS -> Component.text("Doors Only").color(NamedTextColor.GOLD);
                                case INTERACT -> Component.text("Block Interactions").color(NamedTextColor.LIGHT_PURPLE);
                                case BLOCKS -> Component.text("Break/Place Blocks").color(NamedTextColor.AQUA);
                            }
                    }));
                    head.setItemMeta(meta);
                    meta.displayName(Component.text(oplr.getName() == null ? "Unknown" : oplr.getName()).color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
                    playerScreen.addItem(head, (player_, inv_, item_) -> {
                        ui.editing = oplr;
                        permScreen.changeTitle(Component.text("Edit Permissions For " + (oplr.getName() == null ? "Unknown" : oplr.getName())));
                        ui.changeToScreen(permScreen);
                    }, i);
                    i++;
                }
                playerScreen.page = 0;
                ui.changeToScreen(playerScreen);
            }, 4);

            editScreen.addItem(createGuiItem(Material.BARRIER, Component.text("Delete Claim").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false), Component.text("Delete this claim.").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)), (player, inv, item) -> {
                ui.confirmType = "delete";
                confirmScreen.changeTitle(Component.text("Delete this claim?"));
                ui.changeToScreen(confirmScreen);
            }, 5);

            permScreen.addItem(createGuiItem(Material.RED_CONCRETE, Component.text("No block interaction").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)), (player, inv, item) -> {
                if (ui.editing == null) ui.claim.m_default = LandClaims.ClaimPermission.NONE;
                else ui.claim.m_perms.put(ui.editing, LandClaims.ClaimPermission.NONE);
                ui.close(false);
            }, 1);

            permScreen.addItem(createGuiItem(Material.ORANGE_CONCRETE, Component.text("Doors only").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)), (player, inv, item) -> {
                if (ui.editing == null) ui.claim.m_default = LandClaims.ClaimPermission.DOORS;
                else ui.claim.m_perms.put(ui.editing, LandClaims.ClaimPermission.DOORS);
                ui.close(false);
            }, 3);

            permScreen.addItem(createGuiItem(Material.PURPLE_CONCRETE, Component.text("Interact with blocks").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)), (player, inv, item) -> {
                if (ui.editing == null) ui.claim.m_default = LandClaims.ClaimPermission.INTERACT;
                else ui.claim.m_perms.put(ui.editing, LandClaims.ClaimPermission.INTERACT);
                ui.close(false);
            }, 5);

            permScreen.addItem(createGuiItem(Material.CYAN_CONCRETE, Component.text("Break/place any blocks").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)), (player, inv, item) -> {
                if (ui.editing == null) ui.claim.m_default = LandClaims.ClaimPermission.BLOCKS;
                else ui.claim.m_perms.put(ui.editing, LandClaims.ClaimPermission.BLOCKS);
                ui.close(false);
            }, 7);

            confirmScreen.addItem(createGuiItem(Material.GREEN_CONCRETE, Component.text("Yes").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)), (player, inv, item) -> {
                if (ui.confirmType.equals("delete")) {
                    lc.claims.remove(ui.claim);
                    ui.close(false);
                } else if (ui.confirmType.equals("admin-bypass")) {
                    ui.changeToScreen(editScreen);
                }
            }, 3);

            confirmScreen.addItem(createGuiItem(Material.RED_CONCRETE, Component.text("No").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)), (player, inv, item) -> {
                ui.close(false);
            }, 5);

            ui.show(target);
            guis.put(plr, ui);
        } else {
            guis.get(plr).show(target, 0);
        }
    }
}
