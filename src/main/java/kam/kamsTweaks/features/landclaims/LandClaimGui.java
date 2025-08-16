package kam.kamsTweaks.features.landclaims;

import kam.kamsTweaks.ItemManager;
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.ItemManager.ItemType;
import kam.kamsTweaks.utils.Pair;
import kam.kamsTweaks.Logger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

import static org.bukkit.Bukkit.*;

public class LandClaimGui implements Listener {

    Map<Player, GuiInventory> guis = new HashMap<>();

    LandClaims lc;

    public LandClaimGui(LandClaims lc) {
        this.lc = lc;
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
        if (e.getInventory().getType() == InventoryType.GRINDSTONE) {
            ItemStack result = e.getInventory().getItem(2);
            if (result != null && ItemManager.getType(result) == ItemType.CLAIMER) {
                e.setCancelled(true);
                e.getWhoClicked().sendMessage(Component.text("You cannot disenchant this item.").color(NamedTextColor.RED));
                return;
            }
        }
        if (guis.containsKey((Player) e.getWhoClicked())) {
            for (GuiInventory.Screen screen : guis.get((Player) e.getWhoClicked()).screens) {
                if (e.getInventory().equals(screen.inv)) {
                    e.setCancelled(true);
                    if (e.getCurrentItem() == null) return;
                    if (Objects.equals(e.getCurrentItem(), screen.leftArrow)) {
                        screen.previousPage();
                    } else if (Objects.equals(e.getCurrentItem(), screen.rightArrow)) {
                        screen.nextPage();
                    } else {
                        screen.items.forEach((slot, item) -> {
                            var idStr = e.getCurrentItem().getItemMeta().getPersistentDataContainer().getOrDefault(new NamespacedKey("kamstweaks", "guibutton"), PersistentDataType.STRING, "");
                            if (idStr.isEmpty()) return;
                            var id = UUID.fromString(idStr);
                            if (item.first.second.equals(id)) {
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
            public Map<Integer, Pair<Pair<ItemStack, UUID>, GuiRunnable>> items = new HashMap<>();
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
                        inv.setItem(pos - page * limit, item.first.first);
                    }
                });
                if (highest > limit) {
                    inv.setItem(size - 6, leftArrow);
                    inv.setItem(size - 5, paper);
                    inv.setItem(size - 4, rightArrow);
                }
            }

            public void addItem(ItemStack item, GuiRunnable callback, int position) {
                UUID id = UUID.randomUUID();
                var meta = item.getItemMeta();
                meta.getPersistentDataContainer().set(new NamespacedKey("kamstweaks", "guibutton"), PersistentDataType.STRING, id.toString());
                item.setItemMeta(meta);
                items.put(position, new Pair<>(new Pair<>(item, id), callback));
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
                inv = createInventory(null, this.size, title);
                updateItems();
            }

            void changeTitle(Component title) {
                this.title = title;
                inv = createInventory(null, Math.min(size, 54), title);
                updateItems();
            }
        }

        public LandClaims.Claim claim;
        Entity targetEntity;
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
            if (target != null) claim = KamsTweaks.getInstance().m_landClaims.getClaim(target);
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
            GuiInventory.Screen entityEditScreen = ui.addScreen(9, Component.text("Edit Entity Permissions"));
            GuiInventory.Screen entityPermScreen = ui.addScreen(9, Component.text("Edit Permissions"));

            homeScreen.addItem(createGuiItem(Material.IRON_DOOR, Component.text("Edit Permissions").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false), Component.text("Edit permissions for the claim you're currently in.").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false), Component.text("Only works if you own the claim you're in").color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false)), (player, inv, item) -> {
                if (ui.claim == null) {
                    player.sendMessage(Component.text("This area isn't claimed.").color(NamedTextColor.RED));
                } else if (ui.claim.m_owner == null || !ui.claim.m_owner.getUniqueId().equals(player.getUniqueId())) {
                    // admin abuse made me remove :/
                    /* if (player.hasPermission("kamstweaks.landclaims.manageall")) {
                          ui.confirmType = "admin-bypass";
                          confirmScreen.changeTitle(Component.text("Edit " + (ui.claim.m_owner == null ? "The Server" : ui.claim.m_owner.getName() == null ? "Unknown" : ui.claim.m_owner.getName()) + "'s claim?"));
                          ui.changeToScreen(confirmScreen);
                      } else { */
                        player.sendMessage(Component.text("You don't own this area.").color(NamedTextColor.RED));
                    //}
                } else {
                    ui.changeToScreen(editScreen);
                }
            }, 3);
            homeScreen.addItem(createGuiItem(Material.SHIELD, Component.text("Claim").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false), Component.text("Claim land.").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)), (player, inv, item) -> {
                if (!player.hasPermission("kamstweaks.landclaims.claim")) {
                    ui.close(false);
                    player.sendMessage(Component.text("You do not have permission to claim land.").color(NamedTextColor.RED));
                    return;
                }
                Iterator<LandClaims.Claim> iterator = lc.claiming.iterator();
                while (iterator.hasNext()) {
                    LandClaims.Claim claim = iterator.next();
                    if (claim.m_owner.getUniqueId().equals(player.getUniqueId())) {
                        player.sendMessage(Component.text("Cancelled claiming land.").color(NamedTextColor.RED));
                        ui.close(false);
                        iterator.remove();
                        return;
                    }
                }
                int count = 0;
                for (LandClaims.Claim claim : lc.claims) {
                    if (claim.m_owner.getUniqueId().equals(player.getUniqueId())) count++;
                }
                var max = KamsTweaks.getInstance().getConfig().getInt("land-claims.max-claims", 10);
                if (count >= max) {
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
                    if (claim.m_start.getWorld() != player.getWorld()) continue;
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
                    Location l = new Location(claim.m_start.getWorld(), (claim.m_start.x() + claim.m_end.x()) / 2, (claim.m_start.y() + claim.m_end.y()) / 2, (claim.m_start.z() + claim.m_end.z()) / 2);
                    if (l.distance(player.getLocation()) > 100) continue;
                    TextDisplay display = player.getWorld().spawn(l, TextDisplay.class, entity -> {
                        String s;
                        if (claim.m_owner != null && claim.m_owner.getUniqueId().equals(player.getUniqueId())) {
                            s = "You own this claim.";
                        } else {
                            s = switch (claim.m_perms.getOrDefault(getServer().getOfflinePlayer(player.getUniqueId()), claim.m_default)) {
                                case NONE -> "You can only look around.";
                                case DOORS -> "You can use doors.";
                                case INTERACT -> "You can interact with blocks.";
                                case BLOCKS -> "You can interact and manage blocks.";
                            };
                        }
                        entity.text(Component.text("Owned by ").append(Component.text(claim.m_owner == null ? "the server" : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName(), NamedTextColor.GOLD).appendNewline().append(Component.text(s))));
                        entity.setBillboard(Display.Billboard.CENTER);
                        entity.setPersistent(false);
                        for (Player h : getOnlinePlayers()) {
                            if (!h.equals(player)) {
                                h.hideEntity(KamsTweaks.getInstance(), entity);
                            }
                        }
                    });
                    Listener joinListener = new Listener() {
                        @EventHandler
                        public void onPlayerJoin(PlayerJoinEvent event) {
                            Player joining = event.getPlayer();
                            if (!joining.equals(target)) {
                                Bukkit.getScheduler().runTask(KamsTweaks.getInstance(), () -> joining.hideEntity(KamsTweaks.getInstance(), display));
                            }
                        }
                    };
                    Bukkit.getPluginManager().registerEvents(joinListener, KamsTweaks.getInstance());
                    getServer().getScheduler().scheduleSyncDelayedTask(KamsTweaks.getInstance(), () -> {
                        display.remove();
                        HandlerList.unregisterAll(joinListener);
                    }, 20 * 10);
                }
                ui.close(false);
            }, 5);
            homeScreen.addItem(createGuiItem(Material.SPYGLASS, Component.text("View Your Claims").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false), Component.text("Tells you where your claims are in chat.").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)), (player, inv, item) -> {
                StringBuilder claimsMsg = new StringBuilder();
                int i = 0;
                for (LandClaims.Claim claim : lc.claims) {
                    if (claim.m_owner.equals(player)) {
                        i++;
                        claimsMsg
                                .append("\nClaim ").append(i).append(": ")
                                .append(claim.m_start.getBlockX())
                                .append(", ")
                                .append(claim.m_start.getBlockY())
                                .append(", ")
                                .append(claim.m_start.getBlockZ())
                                .append(" to ")
                                .append(claim.m_end.getBlockX())
                                .append(", ")
                                .append(claim.m_end.getBlockY())
                                .append(", ")
                                .append(claim.m_end.getBlockZ())
                                .append(" in ")
                                .append(claim.m_start.getWorld().getName());
                    }
                }
                claimsMsg.insert(0, "You have " + i + " claims.");
                player.sendMessage(claimsMsg.toString());
                ui.close(false);
            }, 6);

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
                        Logger.warn(e.getMessage());
                    }
                    meta.lore(List.of(new TextComponent[]{
                            switch (ui.claim.m_perms.getOrDefault(oplr, ui.claim.m_default)) {
                                case NONE -> Component.text("None").color(NamedTextColor.RED);
                                case DOORS -> Component.text("Doors Only").color(NamedTextColor.GOLD);
                                case INTERACT ->
                                        Component.text("Block Interactions").color(NamedTextColor.LIGHT_PURPLE);
                                case BLOCKS -> Component.text("Break/Place Blocks").color(NamedTextColor.AQUA);
                            }
                    }));
                    head.setItemMeta(meta);
                    meta.displayName(Component.text(oplr.getName() == null ? "Unknown" : oplr.getName()).color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
                    playerScreen.addItem(head, (player_, inv_, item_) -> {
                        ui.editing = oplr;
                        permScreen.changeTitle(Component.text("Edit Permissions For " + (oplr.getName() == null ? "Unknown" : oplr.getName())));
                        if (ui.claim != null) {
                            ui.changeToScreen(permScreen);
                        } else if (ui.targetEntity != null) {
                            ui.changeToScreen(entityPermScreen);
                        } else {
                            Logger.error(player_.name() + " was somehow in the gui without a claim or entity selected?!?!");
                        }
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
                if (!ui.claim.m_owner.getUniqueId().equals(ui.player.getUniqueId()))
                    Logger.warn("[Claim management] " + player.getName() + " just edited " + ui.claim.m_owner.getName() + "'s claim: " + (ui.editing == null ? "Default" : ui.editing.getName() + "'s") + " permissions from " + ui.claim.m_default + " to none");
                if (ui.editing == null) ui.claim.m_default = LandClaims.ClaimPermission.NONE;
                else ui.claim.m_perms.put(ui.editing, LandClaims.ClaimPermission.NONE);
                ui.close(false);
            }, 1);

            permScreen.addItem(createGuiItem(Material.ORANGE_CONCRETE, Component.text("Doors only").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)), (player, inv, item) -> {
                if (!ui.claim.m_owner.getUniqueId().equals(ui.player.getUniqueId()))
                    Logger.warn("[Claim management] " + player.getName() + " just edited " + ui.claim.m_owner.getName() + "'s claim: " + (ui.editing == null ? "Default" : ui.editing.getName() + "'s") + " permissions from " + ui.claim.m_default + " to doors");
                if (ui.editing == null) ui.claim.m_default = LandClaims.ClaimPermission.DOORS;
                else ui.claim.m_perms.put(ui.editing, LandClaims.ClaimPermission.DOORS);
                ui.close(false);
            }, 3);

            permScreen.addItem(createGuiItem(Material.PURPLE_CONCRETE, Component.text("Interact with blocks").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)), (player, inv, item) -> {
                if (!ui.claim.m_owner.getUniqueId().equals(ui.player.getUniqueId()))
                    Logger.warn("[Claim management] " + player.getName() + " just edited " + ui.claim.m_owner.getName() + "'s claim: " + (ui.editing == null ? "Default" : ui.editing.getName() + "'s") + " permissions from " + ui.claim.m_default + " to interactions");
                if (ui.editing == null) ui.claim.m_default = LandClaims.ClaimPermission.INTERACT;
                else ui.claim.m_perms.put(ui.editing, LandClaims.ClaimPermission.INTERACT);
                ui.close(false);
            }, 5);

            permScreen.addItem(createGuiItem(Material.CYAN_CONCRETE, Component.text("Break/place any blocks").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)), (player, inv, item) -> {
                if (!ui.claim.m_owner.getUniqueId().equals(ui.player.getUniqueId()))
                    Logger.warn("[Claim management] " + player.getName() + " just edited " + ui.claim.m_owner.getName() + "'s claim: " + (ui.editing == null ? "Default" : ui.editing.getName() + "'s") + " permissions from " + ui.claim.m_default + " to blocks");
                if (ui.editing == null) ui.claim.m_default = LandClaims.ClaimPermission.BLOCKS;
                else ui.claim.m_perms.put(ui.editing, LandClaims.ClaimPermission.BLOCKS);
                ui.close(false);
            }, 7);

            confirmScreen.addItem(createGuiItem(Material.GREEN_CONCRETE, Component.text("Yes").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)), (player, inv, item) -> {
                switch (ui.confirmType) {
                    case "delete" -> {
                        if (!ui.claim.m_owner.getUniqueId().equals(ui.player.getUniqueId())) Logger.warn("[Claim management] " + player.getName() + " just deleted " + ui.claim.m_owner.getName() + "'s claim.");
                        lc.claims.remove(ui.claim);
                        ui.close(false);
                    }
                    case "admin-bypass" -> ui.changeToScreen(editScreen);
                    case "admin-bypass-entity" -> ui.changeToScreen(entityEditScreen);
                    case "entity-claim" -> {
                        KamsTweaks.getInstance().m_entityClaims.claims.put(ui.targetEntity.getUniqueId(), new EntityClaims.EntityClaim(player));
                        ui.close(false);
                    }
                    case "unclaim" -> {
                        EntityClaims.EntityClaim claim = KamsTweaks.getInstance().m_entityClaims.claims.get(ui.targetEntity.getUniqueId());
                        if (!claim.m_owner.getUniqueId().equals(ui.player.getUniqueId())) Logger.warn("[Claim management] " + player.getName() + " just deleted " + KamsTweaks.getInstance().m_entityClaims.claims.get(ui.targetEntity.getUniqueId()).m_owner.getName() + "'s entity claim.");
                        KamsTweaks.getInstance().m_entityClaims.claims.remove(ui.targetEntity.getUniqueId());
                        ui.close(false);
                    }
                }
            }, 3);

            confirmScreen.addItem(createGuiItem(Material.RED_CONCRETE, Component.text("No").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)), (player, inv, item) -> ui.close(false), 5);

            entityEditScreen.addItem(createGuiItem(Material.LEVER, Component.text("Change Default Permission").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false), Component.text("Change what all players can do by default.").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)), (player, inv, item) -> {
                permScreen.changeTitle(Component.text("Edit Default Permissions"));
                ui.editing = null;
                if (ui.claim != null) {
                    ui.changeToScreen(permScreen);
                } else if (ui.targetEntity != null) {
                    ui.changeToScreen(entityPermScreen);
                }
            }, 3);
            entityEditScreen.addItem(createGuiItem(Material.PLAYER_HEAD, Component.text("Manage Player Permissions").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false), Component.text("Give different players specific permissions.").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)), (player, inv, item) -> {
                playerScreen.clearItems();
                playerScreen.changeSize(getOfflinePlayers().length);
                int i = 0;
                var ecs = KamsTweaks.getInstance().m_entityClaims;
                if (!ecs.claims.containsKey(ui.targetEntity.getUniqueId())) return;
                var claim = ecs.claims.get(ui.targetEntity.getUniqueId());
                for (OfflinePlayer oplr : getOfflinePlayers()) {
                    if (claim.m_owner != null && oplr.getUniqueId().equals(claim.m_owner.getUniqueId())) continue;
                    ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta meta = (SkullMeta) head.getItemMeta();
                    try {
                        meta.setOwningPlayer(oplr);
                    } catch (Exception e) {
                        Logger.warn(e.getMessage());
                    }
                    meta.lore(List.of(new TextComponent[]{
                            switch (claim.m_perms.getOrDefault(oplr, claim.m_default)) {
                                case NONE -> Component.text("None").color(NamedTextColor.RED);
                                case INTERACT -> Component.text("Interactions").color(NamedTextColor.GOLD);
                                case KILL -> Component.text("Damage").color(NamedTextColor.AQUA);
                            }
                    }));
                    head.setItemMeta(meta);
                    meta.displayName(Component.text(oplr.getName() == null ? "Unknown" : oplr.getName()).color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
                    playerScreen.addItem(head, (player_, inv_, item_) -> {
                        ui.editing = oplr;
                        permScreen.changeTitle(Component.text("Edit Permissions For " + (oplr.getName() == null ? "Unknown" : oplr.getName())));
                        ui.changeToScreen(entityPermScreen);
                    }, i);
                    i++;
                }
                playerScreen.page = 0;
                ui.changeToScreen(playerScreen);
            }, 4);

            entityEditScreen.addItem(createGuiItem(Material.BARRIER, Component.text("Unclaim Entity").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false), Component.text("Unclaim this entity.").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)), (player, inv, item) -> {
                ui.confirmType = "unclaim";
                confirmScreen.changeTitle(Component.text("Unclaim this entity?"));
                ui.changeToScreen(confirmScreen);
            }, 5);

            entityPermScreen.addItem(createGuiItem(Material.RED_CONCRETE, Component.text("No interaction").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)), (player, inv, item) -> {
                if (!KamsTweaks.getInstance().m_entityClaims.claims.containsKey(ui.targetEntity.getUniqueId())) return;
                EntityClaims.EntityClaim claim = KamsTweaks.getInstance().m_entityClaims.claims.get(ui.targetEntity.getUniqueId());
                if (!claim.m_owner.getUniqueId().equals(ui.player.getUniqueId()))
                    Logger.warn("[Claim management] " + player.getName() + " just edited " + claim.m_owner.getName() + "'s entity claim: " + (ui.editing == null ? "Default" : ui.editing.getName() + "'s") + " permissions from " + claim.m_default + " to none");
                if (ui.editing == null) claim.m_default = EntityClaims.EntityPermission.NONE;
                else claim.m_perms.put(ui.editing, EntityClaims.EntityPermission.NONE);
                ui.close(false);
            }, 2);

            entityPermScreen.addItem(createGuiItem(Material.ORANGE_CONCRETE, Component.text("Interactions only").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)), (player, inv, item) -> {
                if (!KamsTweaks.getInstance().m_entityClaims.claims.containsKey(ui.targetEntity.getUniqueId())) return;
                EntityClaims.EntityClaim claim = KamsTweaks.getInstance().m_entityClaims.claims.get(ui.targetEntity.getUniqueId());
                if (!claim.m_owner.getUniqueId().equals(ui.player.getUniqueId()))
                    Logger.warn("[Claim management] " + player.getName() + " just edited " + claim.m_owner.getName() + "'s entity claim: " + (ui.editing == null ? "Default" : ui.editing.getName() + "'s") + " permissions from " + claim.m_default + " to interactions");
                if (ui.editing == null) claim.m_default = EntityClaims.EntityPermission.INTERACT;
                else claim.m_perms.put(ui.editing, EntityClaims.EntityPermission.INTERACT);
                ui.close(false);
            }, 4);

            entityPermScreen.addItem(createGuiItem(Material.CYAN_CONCRETE, Component.text("Damage entities").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)), (player, inv, item) -> {
                if (!KamsTweaks.getInstance().m_entityClaims.claims.containsKey(ui.targetEntity.getUniqueId())) return;
                EntityClaims.EntityClaim claim = KamsTweaks.getInstance().m_entityClaims.claims.get(ui.targetEntity.getUniqueId());
                if (!claim.m_owner.getUniqueId().equals(ui.player.getUniqueId()))
                    Logger.warn("[Claim management] " + player.getName() + " just edited " + claim.m_owner.getName() + "'s entity claim: " + (ui.editing == null ? "Default" : ui.editing.getName() + "'s") + " permissions from " + claim.m_default + " to damage");
                if (ui.editing == null) claim.m_default = EntityClaims.EntityPermission.KILL;
                else claim.m_perms.put(ui.editing, EntityClaims.EntityPermission.KILL);
                ui.close(false);
            }, 6);

            ui.show(target);
            guis.put(plr, ui);
        } else {
            guis.get(plr).show(target, 0);
        }
    }
}
