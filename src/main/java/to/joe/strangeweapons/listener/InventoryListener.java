package to.joe.strangeweapons.listener;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import to.joe.strangeweapons.MetaParser;
import to.joe.strangeweapons.StrangeWeapons;
import to.joe.strangeweapons.Util;
import to.joe.strangeweapons.meta.Crate;
import to.joe.strangeweapons.meta.StrangePart;
import to.joe.strangeweapons.meta.StrangeWeapon;

public class InventoryListener implements Listener
{

    private StrangeWeapons plugin;

    public InventoryListener(StrangeWeapons plugin)
    {
        this.plugin = plugin;
        this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event)
    {
        final Player player = (Player) event.getWhoClicked();
        if (event.getInventory().getType() == InventoryType.BREWING) {
            ItemStack item = event.getCursor();
            if (((event.getSlot() == 3 && event.getSlotType() == SlotType.FUEL) || (event.getSlotType() == SlotType.CRAFTING && (event.getSlot() == 0 || event.getSlot() == 1 || event.getSlot() == 2))) && (StrangeWeapon.isStrangeWeapon(item) || Crate.isCrate(item) || MetaParser.isKey(item) || StrangePart.isPart(item) || MetaParser.isNameTag(item) || MetaParser.isDescriptionTag(item))) {
                event.setCancelled(true);
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    public void run() {
                        player.updateInventory();
                    }
                }, 1);
                player.sendMessage(ChatColor.RED + "You may not use that in a brewing stand.");
            }
        }
        if (event.getInventory().getType() == InventoryType.ANVIL) {
            ItemStack item = event.getCursor();
            if ((event.getSlot() == 0 || event.getSlot() == 1) && event.getSlotType() == SlotType.CRAFTING && (Crate.isCrate(item) || MetaParser.isKey(item) || StrangePart.isPart(item) || MetaParser.isNameTag(item) || MetaParser.isDescriptionTag(item))) {
                event.setCancelled(true);
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    public void run() {
                        player.updateInventory();
                    }
                }, 1);
                player.sendMessage(ChatColor.RED + "You may not use that on an anvil.");
            }
        }
        if (event.getInventory().getType() == InventoryType.FURNACE) {
            ItemStack item = event.getCursor();
            if (item.getType() != Material.AIR && ((event.getSlot() == 0 && event.getSlotType() == SlotType.CONTAINER) || (event.getSlot() == 1 && event.getSlotType() == SlotType.FUEL)) && (StrangeWeapon.isStrangeWeapon(item) || Crate.isCrate(item) || MetaParser.isKey(item) || StrangePart.isPart(item) || MetaParser.isNameTag(item) || MetaParser.isDescriptionTag(item))) {
                event.setCancelled(true);
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    public void run() {
                        player.updateInventory();
                    }
                }, 1);
                player.sendMessage(ChatColor.RED + "You may not use that in a furnace.");
            }
        }
        if (event.getInventory().getType() == InventoryType.MERCHANT) {
            ItemStack item = event.getCursor();
            if (item.getType() != Material.AIR && (event.getSlot() == 0 || event.getSlot() == 1) && event.getSlotType() == SlotType.CRAFTING && (StrangeWeapon.isStrangeWeapon(item) || Crate.isCrate(item) || MetaParser.isKey(item) || StrangePart.isPart(item) || MetaParser.isNameTag(item) || MetaParser.isDescriptionTag(item))) {
                event.setCancelled(true);
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    public void run() {
                        player.updateInventory();
                    }
                }, 1);
                player.sendMessage(ChatColor.RED + "You may not trade that with a villager.");
            }
        }

        /**
         * If a crafting slot was clicked...
         */
        if (event.getSlotType() == SlotType.CRAFTING)
        {
            if (!(event.getInventory() instanceof CraftingInventory))
                return;

            final CraftingInventory craftingInventory = (CraftingInventory) event.getInventory();

            new BukkitRunnable() {
                public void run() {
                    ItemStack strangeWeapon = null;
                    int numStrangeWeapons = 0;
                    ItemStack crate = null;
                    int numCrates = 0;
                    ItemStack key = null;
                    int numKeys = 0;
                    ItemStack strangePart = null;
                    int numStrangeParts = 0;
                    ItemStack nameTag = null;
                    int numNameTags = 0;
                    ItemStack descriptionTag = null;
                    int numDescriptionTags = 0;
                    ItemStack normalItem = null;
                    int numNormalItems = 0;
                    int numTotalItems = 0;
                    for (ItemStack i : craftingInventory.getContents()) {
                        if (i == null || i.getType() == Material.AIR) {
                            continue;
                        }
                        /**
                         * I guess he's counting instances of an item, and setting the item stack to such a variable... I doubt this is a good way to go about this
                         */
                        if (StrangeWeapon.isStrangeWeapon(i)) {
                            numStrangeWeapons++;
                            strangeWeapon = i;
                        } else if (Crate.isCrate(i)) {
                            numCrates++;
                            crate = i;
                        } else if (MetaParser.isKey(i)) {
                            numKeys++;
                            key = i;
                        } else if (StrangePart.isPart(i)) {
                            numStrangeParts++;
                            strangePart = i;
                        } else if (MetaParser.isNameTag(i)) {
                            numNameTags++;
                            nameTag = i;
                        } else if (MetaParser.isDescriptionTag(i)) {
                            numDescriptionTags++;
                            descriptionTag = i;
                        } else {
                            numNormalItems++;
                            normalItem = i;
                        }
                        numTotalItems++;
                    }
                    /**
                     * If there's one itemstack of crates and one itemstack of keys...
                     */
                    if (numCrates == 1 && numKeys == 1 && numTotalItems == 2) {
                        //Prepare fake item
                        ItemStack fakeItem = new ItemStack(Material.POTATO_ITEM);
                        ItemMeta meta = fakeItem.getItemMeta();
                        meta.setDisplayName(ChatColor.DARK_PURPLE + "" + ChatColor.ITALIC + "Mystery Item!");
                        fakeItem.setItemMeta(meta);
                        fakeItem.setType(Material.POTATO_ITEM);

                        //set fake item in result slot, and update inventory to reflect this on next tick
                        craftingInventory.setResult(fakeItem);
                        new BukkitRunnable() {
                            public void run() {
                                player.updateInventory();
                            }
                        }.runTaskLater(plugin, 1);
                        return;
                    }
                    if (numStrangeWeapons == 1 && numStrangeParts == 1 && numTotalItems == 2) {
                        StrangeWeapon weapon = new StrangeWeapon(strangeWeapon.clone());
                        StrangePart part = new StrangePart(strangePart);
                        if (weapon.getParts().size() > plugin.config.maxParts) {
                            craftingInventory.setResult(null);
                            player.sendMessage(ChatColor.RED + "You may only have " + plugin.config.maxParts + " strange parts on a weapon");
                        } else if (weapon.getParts().containsKey(part.getPart())) {
                            craftingInventory.setResult(null);
                            player.sendMessage(ChatColor.RED + "This weapon is already tracking " + part.getPart().getName());
                        } else {
                            weapon.getParts().put(part.getPart(), 0);
                            craftingInventory.setResult(weapon.previewItemStack());
                        }
                        new BukkitRunnable() {
                            public void run() {
                                player.updateInventory();

                            }
                        }.runTaskLater(plugin, 1);
                        return;
                    }
                    if (numStrangeWeapons == 1 && numNameTags == 1 && numTotalItems == 2) {
                        if (!plugin.tags.containsKey(player.getName())) {
                            player.sendMessage(ChatColor.RED + "Set a name with /tag before using a name tag");
                            return;
                        }
                        StrangeWeapon weapon = new StrangeWeapon(strangeWeapon.clone());
                        weapon.setCustomName(plugin.tags.get(player.getName()));
                        craftingInventory.setResult(weapon.previewItemStack());
                        new BukkitRunnable() {
                            public void run() {
                                player.updateInventory();
                            }
                        }.runTaskLater(plugin, 1);
                        return;
                    }
                    if (numStrangeWeapons == 1 && numDescriptionTags == 1 && numTotalItems == 2) {
                        if (!plugin.tags.containsKey(player.getName())) {
                            player.sendMessage(ChatColor.RED + "Set a description with /tag before using a description tag");
                            return;
                        }
                        StrangeWeapon weapon = new StrangeWeapon(strangeWeapon.clone());
                        weapon.setDescription(plugin.tags.get(player.getName()));
                        craftingInventory.setResult(weapon.previewItemStack());
                        new BukkitRunnable() {
                            public void run() {
                                player.updateInventory();
                            }
                        }.runTaskLater(plugin, 1);
                        return;
                    }
                    //Clear result slot if there's StrangeWeapon items in there that hasn't matched conditions above
                    if (numNormalItems != numTotalItems) {
                        craftingInventory.setResult(null);
                        new BukkitRunnable() {
                            public void run() {
                                player.updateInventory();
                            }
                        }.runTaskLater(plugin, 1);
                        return;
                    }
                }
            }.runTaskLater(plugin, 1L);
        }
            /**
             * if the RESULT slot was clicked...
             */
            else if (event.getSlotType() == SlotType.RESULT)
            {
                CraftingInventory craftingInventory = (CraftingInventory) event.getInventory();
                ItemStack[] matrix = craftingInventory.getMatrix();
                ItemStack strangeWeapon = null;
                int numStrangeWeapons = 0;
                ItemStack crate = null;
                int numCrates = 0;
                ItemStack key = null;
                int numKeys = 0;
                ItemStack strangePart = null;
                int numStrangeParts = 0;
                ItemStack nameTag = null;
                int numNameTags = 0;
                ItemStack descriptionTag = null;
                int numDescriptionTags = 0;
                ItemStack normalItem = null;
                int numNormalItems = 0;
                int numTotalItems = 0;

                //Iterate through each slot in the crafting matrix...
                for (ItemStack i : matrix)
                {
                    if (i == null || i.getTypeId() == 0) //Skip slot if slot is empty
                    {
                        continue;
                    }
                    if (StrangeWeapon.isStrangeWeapon(i)) //Do the same retarded stuff as before
                    {
                        numStrangeWeapons++;
                        strangeWeapon = i;
                    } else if (Crate.isCrate(i)) {
                        numCrates++;
                        crate = i;
                    } else if (MetaParser.isKey(i)) {
                        numKeys++;
                        key = i;
                    } else if (StrangePart.isPart(i)) {
                        numStrangeParts++;
                        strangePart = i;
                    } else if (MetaParser.isNameTag(i)) {
                        numNameTags++;
                        nameTag = i;
                    } else if (MetaParser.isDescriptionTag(i)) {
                        numDescriptionTags++;
                        descriptionTag = i;
                    } else {
                        numNormalItems++;
                        normalItem = i;
                    }
                    numTotalItems++;
                }
                /**
                 * If there's one itemstack of crates and one itemstack of keys...
                 * He probably should've made a method for this instead which accepts a boolean (result or crafting) and such idk wutevar
                 */
                if (numCrates == 1 && numKeys == 1 && numTotalItems == 2)
                {
                    ItemStack loot = new Crate(crate).getUncratedItem();
                    if (StrangeWeapon.isStrangeWeapon(loot))
                    {
                        loot = new StrangeWeapon(loot).clone();
                    }
                /*
                 * if (loot == null) { getLogger().severe( "LOOT IS NULL - Report this to the plugin author!"); getLogger().severe("Player " + player.getName() + " tried to uncrate a crate!" + crate.serialize().toString()); }
                 */// http://pastie.org/private/borniaknvtofbio6mfza

                    //Determine loot's display name
                    String lootName;
                    if (loot.getItemMeta().hasDisplayName()) {
                        lootName = loot.getItemMeta().getDisplayName();
                    } else {
                        lootName = ChatColor.YELLOW + Util.toTitleCase(loot.getType().toString().toLowerCase().replaceAll("_", " "));
                    }
                    //Broadcast the uncrating
                    //TODO: hook into my sound API and play much fanfare
                    plugin.getServer().broadcastMessage(player.getDisplayName() + ChatColor.WHITE + " has unboxed: " + ChatColor.YELLOW + lootName);

                    // Maybe this fixes it?
                    // Dupe fix?

                    /**
                     * So here he's trying to remove items from the crafting matrix.
                     * Weird things happen if there's more than one key in the matrix for whatever reason.
                     * Unless I'm blind and can't see the problem here, I think the server is messing up when the ItemClickEvent fires in
                     * result slot with stuff in the matrix... I guess. Probably because of the fake item
                     */

                    event.setResult(Event.Result.DENY); //The answer to fixing the weird server duplication. Except it also removes the player's loot lol

                    ItemStack[] beforeCraft = craftingInventory.getContents();
                    boolean itemsStillRemain = true;
                    for (int i = 0; i < beforeCraft.length; i++)
                    {
                        if (MetaParser.isKey(beforeCraft[i])) {
                            if (beforeCraft[i].getAmount() > 0) {
                                beforeCraft[i].setAmount(beforeCraft[i].getAmount() - 1);
                                if (beforeCraft[i].getAmount() <= 0)
                                {
                                    beforeCraft[i].setType(Material.AIR);
                                    itemsStillRemain = false;
                                }
                            } else {
                                beforeCraft[i].setType(Material.AIR);
                                player.kickPlayer("Uhm something wrong happened, please join IRC at techfort.us.to and explain what happened. Debug code 1");
                            }
                        } else if (Crate.isCrate(beforeCraft[i])) {
                            if (beforeCraft[i].getAmount() > 0) {
                                beforeCraft[i].setAmount(beforeCraft[i].getAmount() - 1);
                                if (beforeCraft[i].getAmount() <= 0)
                                {
                                    beforeCraft[i].setType(Material.AIR);
                                    itemsStillRemain = false;
                                }
                            } else {
                                beforeCraft[i].setType(Material.AIR);
                                player.kickPlayer("Uhm something wrong happened, please join IRC at techfort.us.to and explain what happened. Debug code 2");
                            }
                        }
                    }
                    event.getInventory().setContents(beforeCraft);
                    //Put loot on player's cursor
                    event.setCursor(loot); //Although deprecated, works because we've denied the result

                    if (itemsStillRemain)
                    {
                        //Prepare fake item
                        ItemStack fakeItem = new ItemStack(Material.POTATO_ITEM);
                        ItemMeta meta = fakeItem.getItemMeta();
                        meta.setDisplayName(ChatColor.DARK_PURPLE + "" + ChatColor.ITALIC + "Mystery Item!");
                        fakeItem.setItemMeta(meta);
                        fakeItem.setType(Material.POTATO_ITEM);

                        //set fake item in result slot, and update inventory to reflect this on next tick
                        craftingInventory.setResult(fakeItem);
                    }
                    new BukkitRunnable() {
                        public void run() {
                            player.updateInventory();
                        }
                    }.runTaskLater(plugin, 1);
                }
                if (numStrangeWeapons == 1 && numStrangeParts == 1 && numTotalItems == 2) {
                    StrangeWeapon weapon = new StrangeWeapon(strangeWeapon);
                    StrangePart part = new StrangePart(strangePart);
                    weapon.getParts().put(part.getPart(), 0);
                    craftingInventory.clear();
                    event.setCurrentItem(weapon.getItemStack());
                    new BukkitRunnable() {
                        public void run() {
                            player.updateInventory();
                        }
                    }.runTaskLater(plugin, 1);
                }
                if (numStrangeWeapons == 1 && numNameTags == 1 && numTotalItems == 2) {
                    if (!plugin.tags.containsKey(player.getName())) {
                        player.sendMessage(ChatColor.RED + "Set a name with /tag before using a name tag");
                        return;
                    }
                    StrangeWeapon weapon = new StrangeWeapon(strangeWeapon);
                    weapon.setCustomName(plugin.tags.get(player.getName()));
                    craftingInventory.clear();
                    event.setCurrentItem(weapon.getItemStack());
                    new BukkitRunnable() {
                        public void run() {
                            player.updateInventory();
                        }
                    }.runTaskLater(plugin, 1);
                }
                if (numStrangeWeapons == 1 && numDescriptionTags == 1 && numTotalItems == 2) {
                    if (!plugin.tags.containsKey(player.getName())) {
                        player.sendMessage(ChatColor.RED + "Set a description with /tag before using a description tag");
                        return;
                    }
                    StrangeWeapon weapon = new StrangeWeapon(strangeWeapon);
                    weapon.setDescription(plugin.tags.get(player.getName()));
                    craftingInventory.clear();
                    event.setCurrentItem(weapon.getItemStack());
                    new BukkitRunnable() {
                        public void run() {
                            player.updateInventory();
                        }
                    }.runTaskLater(plugin, 1);
                }
            }
        }
    @EventHandler(ignoreCancelled = true)
    void onCraftEvent(CraftItemEvent event)
    {
        for (ItemStack item : event.getInventory())
        {
            if (item == null)
                continue;
            if (StrangeWeapon.isStrangeWeapon(item) || Crate.isCrate(item) || MetaParser.isKey(item) || StrangePart.isPart(item) || MetaParser.isNameTag(item) || MetaParser.isDescriptionTag(item))
            {
                plugin.getLogger().info(event.getWhoClicked().getName() + " Attempted to craft something with a crate or key");
                event.setCancelled(true);
                return;
            }
        }
    }
}