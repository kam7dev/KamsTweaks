package kam.kamsTweaks.features.landclaims;

import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.utils.Pair;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Display;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

import static org.bukkit.Bukkit.getOfflinePlayers;
import static org.bukkit.Bukkit.getServer;

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
                    screen.items.forEach((slot, item) -> {
                        if (item.first.equals(e.getCurrentItem())) {
                            item.second.run((Player) e.getWhoClicked(), e.getInventory(), e.getCurrentItem());
                        }
                    });
                }
            }
        }
//        if(e.getInventory() == editInv) {
//            e.setCancelled(true);
//            if(e.getCurrentItem() == null) return;
//            if (e.getCurrentItem().isSimilar(defaultPermissionItem)) {
//                e.getWhoClicked().openInventory(permInv);
//            }
//        } else if(e.getInventory() == permInv) {
//            e.setCancelled(true);
//            if(e.getCurrentItem() == null) return;
//            if (e.getCurrentItem().isSimilar(defaultPermissionItem)) {
//                e.getWhoClicked().openInventory(permInv);
//            }
//        }
    }

    public static class GuiInventory {
        static class Screen {
            public Map<Integer, Pair<ItemStack, GuiRunnable>> items = new HashMap<>();
            public Inventory inv;
            private int limit;
            public final Component title;
            int page = 0;
            public Screen(int size, Component title) {
                this.title = title;
                size = Math.max(9, size);
                limit = size > 54 ? 45 : size;
                inv = Bukkit.createInventory(null, Math.min(size, 54), title);
            }
            public void addItem(ItemStack item, GuiRunnable callback, int position) {
                items.put(position, new Pair<>(item, callback));
                if (page * limit <= position && (page + 1) * limit > position) {
                    inv.setItem(position - (page * limit), item);
                }
            }
            public void removeItem(int position) {
                if (page * limit <= position && (page + 1) * limit > position) {
                    inv.setItem(position - (page * limit), null);
                }
                items.remove(position);
            }
            public void clearItems() {
                for (int i = 0; i < limit; i++) {
                    inv.setItem(i, null);
                }
                items.clear();
            }
            void goToPage(int page) {
                this.page = page;
                inv.clear();
                items.forEach((position, item) -> {
                    if (page * limit <= position && (page + 1) * limit > position) {
                        inv.setItem(position - (page * limit), item.first);
                    }
                });
            }
            void changeSize(int size) {
                size = Math.max(9, size);
                limit = size > 54 ? 45 : size;
                inv = Bukkit.createInventory(null, Math.min(size, 54), title);
            }
        }

        LandClaims.Claim claim;
        Player editing;

        List<Screen> screens = new ArrayList<>();
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

        void show() {
            if (screens.size() <= currentScreen) return;
            Screen current = screens.get(currentScreen);
            if (current == null) return;
            claim = KamsTweaks.getInstance().m_landClaims.getClaim(player.getLocation());
            player.openInventory(current.inv);
        }

        void show(int page) {
            if (screens.size() <= page) return;
            currentScreen = page;
            editing = null;
            show();
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

    void showClaimGui(Player plr) {
        if (!guis.containsKey(plr)) {
            LandClaimGui.GuiInventory ui = new LandClaimGui.GuiInventory(plr);
            LandClaimGui.GuiInventory.Screen homeScreen = ui.addScreen(9, Component.text("Land Claims"));
            LandClaimGui.GuiInventory.Screen editScreen = ui.addScreen(9, Component.text("Edit Claim Permissions"));
            LandClaimGui.GuiInventory.Screen permScreen = ui.addScreen(9, Component.text("Edit Permissions"));
            LandClaimGui.GuiInventory.Screen playerScreen = ui.addScreen(getOfflinePlayers().length, Component.text("Edit Player Permissions"));

            homeScreen.addItem(createGuiItem(Material.IRON_DOOR, Component.text("Edit Permissions").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false), Component.text("Edit permissions for the claim you're currently in.").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false), Component.text("Only works if you own the claim you're in").color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false)), (player, inv, item) -> {
                if (ui.claim == null) {
                    player.sendMessage(Component.text("This area isn't claimed.").color(NamedTextColor.RED));
                } else if(ui.claim.m_owner == null || !ui.claim.m_owner.getUniqueId().equals(player.getUniqueId())) {
                    player.sendMessage(Component.text("You don't own this area.").color(NamedTextColor.RED));
                } else {
                    ui.changeToScreen(editScreen);
                }
            }, 3);
            homeScreen.addItem(createGuiItem(Material.SHIELD, Component.text("Claim").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false), Component.text("Claim land.").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)), (player, inv, item) -> {
                for (LandClaims.Claim claim : lc.claiming) {
                    if (claim.m_owner.getUniqueId().equals(player.getUniqueId())) {
                        player.sendMessage(Component.text("You're already currently claiming land!").color(NamedTextColor.RED));
                        return;
                    }
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
                        entity.text(Component.text("Owned by ").append(Component.text(claim.m_owner == null ? "the server" : claim.m_owner.getName(), NamedTextColor.GOLD).appendNewline().append(Component.text(s))));
                        entity.setBillboard(Display.Billboard.CENTER);
                        lc.cleanupList.add(entity);
                    });
                    getServer().getScheduler().scheduleSyncDelayedTask(KamsTweaks.getInstance(), () -> {
                        display.remove();
                        lc.cleanupList.remove(display);
                    }, 20 * 10);
                    ui.close(false);
                }
            }, 5);

            editScreen.addItem(createGuiItem(Material.LEVER, Component.text("Change Default Permission").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false), Component.text("Change what all players can do by default.").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)), (player, inv, item) -> {
                ui.changeToScreen(permScreen);
            }, 3);
            editScreen.addItem(createGuiItem(Material.PLAYER_HEAD, Component.text("Manage Player Permissions").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false), Component.text("Give different players specific permissions.").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)), (player, inv, item) -> {
                playerScreen.clearItems();
                playerScreen.changeSize(getOfflinePlayers().length);
                int i = 0;
                for (OfflinePlayer oplr : Bukkit.getOfflinePlayers()) {
                    ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta meta = (SkullMeta) head.getItemMeta();
                    meta.setOwningPlayer(oplr);
                    playerScreen.addItem(head, (player_, inv_, item_) -> {
                        ui.editing = player_;
                        ui.changeToScreen(permScreen);
                    }, i++);
                }
                ui.changeToScreen(playerScreen);
            }, 4);
            editScreen.addItem(createGuiItem(Material.BARRIER, Component.text("Delete Claim").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false), Component.text("Delete this claim.").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)), (player, inv, item) -> {

            }, 5);

            ui.show();
            guis.put(plr, ui);
        } else {
            guis.get(plr).show(0);
        }
    }
}
