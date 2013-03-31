package to.joe.strangeweapons;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.PoweredMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import to.joe.strangeweapons.command.ContentsCommand;
import to.joe.strangeweapons.command.CratesCommand;
import to.joe.strangeweapons.command.DropsCommand;
import to.joe.strangeweapons.command.ListPartsCommand;
import to.joe.strangeweapons.command.NewCrateCommand;
import to.joe.strangeweapons.command.NewDescriptionTagCommand;
import to.joe.strangeweapons.command.NewKeyCommand;
import to.joe.strangeweapons.command.NewNameTagCommand;
import to.joe.strangeweapons.command.NewPartCommand;
import to.joe.strangeweapons.command.PlaytimeCommand;
import to.joe.strangeweapons.command.StrangeCommand;
import to.joe.strangeweapons.command.TagCommand;
import to.joe.strangeweapons.datastorage.Cache;
import to.joe.strangeweapons.datastorage.DataStorageException;
import to.joe.strangeweapons.datastorage.DataStorageInterface;
import to.joe.strangeweapons.datastorage.MySQLDataStorage;
import to.joe.strangeweapons.datastorage.PlayerDropData;
import to.joe.strangeweapons.datastorage.YamlDataStorage;
import to.joe.strangeweapons.meta.Crate;
import to.joe.strangeweapons.meta.StrangeWeapon;

public class StrangeWeapons extends JavaPlugin implements Listener {

    public Config config;
    public Map<String, String> tags = new HashMap<String, String>();
    private Map<String, Long> joinTimes = new HashMap<String, Long>();
    public Random random = new Random();
    private DataStorageInterface dataStorage;

    @Override
    public void onEnable() {
        getConfig().options().copyDefaults(true);
        saveConfig();

        getCommand("strange").setExecutor(new StrangeCommand());
        getCommand("newcrate").setExecutor(new NewCrateCommand(this));
        getCommand("newkey").setExecutor(new NewKeyCommand());
        getCommand("newpart").setExecutor(new NewPartCommand());
        getCommand("newnametag").setExecutor(new NewNameTagCommand());
        getCommand("newdescriptiontag").setExecutor(new NewDescriptionTagCommand());
        getCommand("tag").setExecutor(new TagCommand(this));
        getCommand("crates").setExecutor(new CratesCommand(this));
        getCommand("contents").setExecutor(new ContentsCommand());
        getCommand("drops").setExecutor(new DropsCommand(this));
        getCommand("playtime").setExecutor(new PlaytimeCommand(this));
        getCommand("listparts").setExecutor(new ListPartsCommand());
        getServer().getPluginManager().registerEvents(this, this);

        for (String s : getConfig().getStringList("idstrings")) {
            StrangeWeapon.idStrings.add(s.replace("{#}", "([0-9]+)"));
        }

        try {
            String storageType = getConfig().getString("datastorage");
            if (storageType.equalsIgnoreCase("mysql")) {
                dataStorage = new Cache(this, new MySQLDataStorage(this, getConfig().getString("database.url"), getConfig().getString("database.username"), getConfig().getString("database.password")));
                getLogger().info("Using cached mySQL for datastorage");
            } else if (storageType.equalsIgnoreCase("mysql_nocache")) {
                dataStorage = new MySQLDataStorage(this, getConfig().getString("database.url"), getConfig().getString("database.username"), getConfig().getString("database.password"));
                getLogger().warning("Using uncached mySQL for datastorage. Expect poor performance!");
            } else if (storageType.equalsIgnoreCase("yaml")) {
                dataStorage = new YamlDataStorage(this);
                getLogger().info("Using yaml for datastorage");
            } else {
                getLogger().severe("No datastorage selected!");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Error connecting to database", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Error loading yaml file", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Crate.plugin = this;
        StrangeWeapon.plugin = this;
        PlayerDropData.plugin = this;
        Util.plugin = this;

        config = new Config(getConfig());

        getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                for (Entry<String, Long> time : joinTimes.entrySet()) {
                    try {
                        PlayerDropData data = dataStorage.getPlayerDropData(time.getKey());
                        data.setPlayTime(data.getPlayTime() + (int) ((System.currentTimeMillis() - time.getValue()) / 1000));
                        if (data.getPlayTime() >= data.getNextItemDrop() && dataStorage.itemCanDrop(data)) {
                            Map<ItemStack, Double> map = new HashMap<ItemStack, Double>();
                            if (getConfig().contains("drops")) {
                                ConfigurationSection cs = getConfig().getConfigurationSection("drops");
                                for (String item : cs.getKeys(false)) {
                                    ConfigurationSection i = cs.getConfigurationSection(item);
                                    map.put(i.getItemStack("item"), i.getDouble("weight"));
                                }
                            }

                            RandomCollection<ItemStack> rc = new RandomCollection<ItemStack>(random);
                            for (Entry<ItemStack, Double> i : map.entrySet()) {
                                rc.add(i.getValue(), i.getKey());
                            }

                            if (map.isEmpty()) {
                                getLogger().warning("There are no items that can be dropped!");
                            } else {
                                Player player = getServer().getPlayerExact(time.getKey());
                                if (player.hasPermission("strangeweapons.drop.dropitems")) { //Make sure they can receive items
                                    ItemStack item = rc.next();
                                    if (StrangeWeapon.isStrangeWeapon(item)) {
                                        item = new StrangeWeapon(item).clone();
                                    }
                                    Map<Integer, ItemStack> fail = player.getInventory().addItem(item); //If the player has a full inventory, we skip this drop for them or drop it at their feet
                                    if (config.dropAtFeet) {
                                        for (ItemStack failedItem : fail.values()) {
                                            player.getWorld().dropItem(player.getLocation(), failedItem);
                                        }
                                    }
                                    if (fail.isEmpty() || config.dropAtFeet) {
                                        dataStorage.recordDrop(player.getName(), item, false);
                                        String lootName;
                                        if (item.getItemMeta().hasDisplayName()) {
                                            lootName = item.getItemMeta().getDisplayName();
                                        } else {
                                            lootName = ChatColor.YELLOW + toTitleCase(item.getType().toString().toLowerCase().replaceAll("_", " "));
                                        }
                                        if (player.hasPermission("strangeweapons.drop.announceexempt")) { //If the player has this perm, we don't announce their drops in case they may be vanished
                                            player.sendMessage(ChatColor.GOLD + "You" + ChatColor.WHITE + " have found: " + ChatColor.YELLOW + lootName);
                                        } else {
                                            getServer().broadcastMessage(player.getDisplayName() + ChatColor.WHITE + " has found: " + ChatColor.YELLOW + lootName);
                                        }
                                    } else {
                                        player.sendMessage(ChatColor.GOLD + "TIP: " + ChatColor.AQUA + "Make sure you have at least one empty spot in your inventory to receive random drops!");
                                    }
                                }
                            }

                            data.rollItem();
                        }
                        if (data.getPlayTime() >= data.getNextCrateDrop() && dataStorage.crateCanDrop(data)) {
                            Set<String> allCrates;
                            if (getConfig().contains("crates")) {
                                allCrates = getConfig().getConfigurationSection("crates").getKeys(false);
                            } else {
                                allCrates = new HashSet<String>();
                            }
                            Iterator<String> i = allCrates.iterator();
                            while (i.hasNext()) {
                                String crate = i.next();
                                if (!getConfig().getBoolean("crates." + crate + ".drops")) {
                                    i.remove();
                                }
                            }
                            if (allCrates.isEmpty()) {
                                getLogger().warning("There are no crates that can be dropped!");
                            } else {
                                ArrayList<String> crates = new ArrayList<String>(allCrates);
                                Collections.shuffle(crates);
                                ItemStack item = new Crate(Integer.parseInt(crates.get(0))).getItemStack();
                                Player player = getServer().getPlayerExact(time.getKey());
                                if (player.hasPermission("strangeweapons.drop.dropcrates")) { //Make sure the player can receive crates
                                    Map<Integer, ItemStack> fail = player.getInventory().addItem(item); //If the player has a full inventory, we skip this drop for them or drop it at their feet
                                    if (config.dropAtFeet) {
                                        for (ItemStack failedItem : fail.values()) {
                                            player.getWorld().dropItem(player.getLocation(), failedItem);
                                        }
                                    }
                                    if (fail.isEmpty() || config.dropAtFeet) {
                                        dataStorage.recordDrop(player.getName(), item, true);
                                        if (player.hasPermission("strangeweapons.drop.announceexempt")) { //If the player has this perm, we don't announce their drops in case they may be vanished
                                            player.sendMessage(ChatColor.GOLD + "You" + ChatColor.WHITE + " have found: " + item.getItemMeta().getDisplayName());
                                        } else {
                                            getServer().broadcastMessage(player.getDisplayName() + ChatColor.WHITE + " has found: " + item.getItemMeta().getDisplayName());
                                        }
                                    } else {
                                        player.sendMessage(ChatColor.GOLD + "TIP: " + ChatColor.AQUA + "Make sure you have at least one empty spot in your inventory to receive random drops!");
                                    }
                                }
                            }
                            data.rollCrate();
                        }
                        dataStorage.updatePlayerDropData(data);
                    } catch (DataStorageException e) {
                        getLogger().log(Level.SEVERE, "Error reading/saving data for " + time.getKey(), e);
                    }
                }
                long currentTime = System.currentTimeMillis();
                ArrayList<String> onlinePlayers = new ArrayList<String>();
                for (Player p : getServer().getOnlinePlayers()) {
                    onlinePlayers.add(p.getName());
                }
                for (String player : joinTimes.keySet()) {
                    if (onlinePlayers.contains(player)) {
                        joinTimes.put(player, currentTime);
                    } else {
                        joinTimes.remove(player);
                    }
                }
            }
        }, 600, 1200);
    }

    @Override
    public void onDisable() {
        if (dataStorage instanceof Cache) {
            ((Cache) dataStorage).shutdown();
        }
        if (dataStorage instanceof YamlDataStorage) {
            ((YamlDataStorage) dataStorage).shutdown();
        }
    }

    public static String toTitleCase(String string) {
        StringBuilder titleString = new StringBuilder();
        for (String s : string.split(" ")) {
            titleString.append(Character.toUpperCase(s.charAt(0))).append(s.substring(1)).append(" ");
        }
        return titleString.substring(0, titleString.length() - 1);
    }

    public String getWeaponName(int stat) {
        while (!config.weaponText.containsKey(stat)) {
            stat--;
            if (stat < 0)
                return "Sub-par";
        }
        return config.weaponText.get(stat);
    }

    public String getWeaponName(ItemStack item, int stat) {
        return getWeaponName(stat) + " " + toTitleCase(item.getType().toString().toLowerCase().replaceAll("_", " "));
    }

    public DataStorageInterface getDSI() {
        return dataStorage;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        joinTimes.put(event.getPlayer().getName(), System.currentTimeMillis());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        try {
            PlayerDropData data = dataStorage.getPlayerDropData(event.getPlayer().getName());
            data.setPlayTime(data.getPlayTime() + (int) ((System.currentTimeMillis() - joinTimes.get(event.getPlayer().getName())) / 1000));
            dataStorage.updatePlayerDropData(data);
            joinTimes.remove(event.getPlayer().getName());
        } catch (DataStorageException e) {
            getLogger().log(Level.SEVERE, "Error saving playtime on leave", e);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            Player p = event.getEntity().getKiller();
            if (p.getItemInHand().getAmount() > 0 && StrangeWeapon.isStrangeWeapon(p.getItemInHand())) {
                StrangeWeapon item = new StrangeWeapon(p.getItemInHand());
                Entry<Part, Integer> oldPrimary = item.getPrimary();
                String oldName = getWeaponName(p.getItemInHand(), (int) (oldPrimary.getValue() * oldPrimary.getKey().getMultiplier()));
                item.incrementStat(Part.PLAYER_KILLS, 1);
                Entry<Part, Integer> newPrimary = item.getPrimary();
                String newName = getWeaponName(p.getItemInHand(), (int) (newPrimary.getValue() * newPrimary.getKey().getMultiplier()));
                if (!oldName.equals(newName)) {
                    getServer().broadcastMessage(p.getDisplayName() + "'s " + toTitleCase(p.getItemInHand().getType().toString().toLowerCase().replaceAll("_", " ")) + ChatColor.WHITE + " has reached a new rank: " + ChatColor.GOLD + getWeaponName((int) (newPrimary.getValue() * newPrimary.getKey().getMultiplier())));
                }
                p.setItemInHand(item.getItemStack());
            }
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        Player p = null;
        if (event.getDamager() instanceof Player) {
            p = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Arrow && ((Arrow) event.getDamager()).getShooter() instanceof Player) {
            p = (Player) ((Arrow) event.getDamager()).getShooter();
        }
        if (p != null) {
            if (p.getItemInHand().getAmount() > 0 && StrangeWeapon.isStrangeWeapon(p.getItemInHand())) {
                if (!config.durability) {
                    p.getItemInHand().setDurability((short) 0);
                    p.updateInventory();
                }
                StrangeWeapon item = new StrangeWeapon(p.getItemInHand());
                Entry<Part, Integer> oldPrimary = item.getPrimary();
                String oldName = getWeaponName(p.getItemInHand(), (int) (oldPrimary.getValue() * oldPrimary.getKey().getMultiplier()));
                item.incrementStat(Part.DAMAGE, event.getDamage());
                Entry<Part, Integer> newPrimary = item.getPrimary();
                String newName = getWeaponName(p.getItemInHand(), (int) (newPrimary.getValue() * newPrimary.getKey().getMultiplier()));
                if (!oldName.equals(newName)) {
                    getServer().broadcastMessage(p.getDisplayName() + "'s " + toTitleCase(p.getItemInHand().getType().toString().toLowerCase().replaceAll("_", " ")) + ChatColor.WHITE + " has reached a new rank: " + getWeaponName((int) (newPrimary.getValue() * newPrimary.getKey().getMultiplier())));
                }
                p.setItemInHand(item.getItemStack());
            }
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player p = event.getPlayer();
        if (p.getItemInHand().getAmount() > 0 && StrangeWeapon.isStrangeWeapon(p.getItemInHand())) {
            if (!config.durability && p.getItemInHand().getType() != Material.BOW) {
                p.getItemInHand().setDurability((short) 0);
                p.updateInventory();
            }
            StrangeWeapon item = new StrangeWeapon(p.getItemInHand());
            Entry<Part, Integer> oldPrimary = item.getPrimary();
            String oldName = getWeaponName(p.getItemInHand(), (int) (oldPrimary.getValue() * oldPrimary.getKey().getMultiplier()));
            item.incrementStat(Part.BLOCKS_BROKEN, 1);
            Entry<Part, Integer> newPrimary = item.getPrimary();
            String newName = getWeaponName(p.getItemInHand(), (int) (newPrimary.getValue() * newPrimary.getKey().getMultiplier()));
            if (!oldName.equals(newName)) {
                getServer().broadcastMessage(p.getDisplayName() + "'s " + toTitleCase(p.getItemInHand().getType().toString().toLowerCase().replaceAll("_", " ")) + ChatColor.WHITE + " has reached a new rank: " + getWeaponName((int) (newPrimary.getValue() * newPrimary.getKey().getMultiplier())));
            }
            p.setItemInHand(item.getItemStack());
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onBowFire(EntityShootBowEvent event) {
        if (!config.durability && event.getEntity() instanceof Player && StrangeWeapon.isStrangeWeapon(event.getBow())) {
            event.getBow().setDurability((short) 0);
            ((Player) event.getEntity()).updateInventory();
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getItem() == null) {
            return;
        }
        Material mat = event.getItem().getType();
        if (!config.durability && event.getAction() == Action.RIGHT_CLICK_BLOCK && (mat.equals(Material.WOOD_HOE) || mat.equals(Material.STONE_HOE) || mat.equals(Material.IRON_HOE) || mat.equals(Material.GOLD_HOE) || mat.equals(Material.DIAMOND_HOE)) && StrangeWeapon.isStrangeWeapon(event.getItem())) {
            event.getItem().setDurability((short) 0);
            event.getPlayer().updateInventory();
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        Player p = event.getPlayer();
        ItemStack item = event.getPlayer().getItemInHand();
        if (event.getRightClicked() instanceof PoweredMinecart && item.getType() == Material.COAL && StrangeWeapon.isStrangeWeapon(p.getItemInHand())) {
            event.setCancelled(true);
            p.updateInventory();
            p.sendMessage(ChatColor.RED + "You may not use that in a powered minecart.");
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onFish(PlayerFishEvent event) {
        if (!config.durability && event.getPlayer().getItemInHand().getType().equals(Material.FISHING_ROD) && StrangeWeapon.isStrangeWeapon(event.getPlayer().getItemInHand())) {
            event.getPlayer().getItemInHand().setDurability((short) 0);
            event.getPlayer().updateInventory();
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            Player p = event.getEntity().getKiller();
            if (p.getItemInHand().getAmount() > 0 && StrangeWeapon.isStrangeWeapon(p.getItemInHand())) {
                StrangeWeapon item = new StrangeWeapon(p.getItemInHand());
                Entry<Part, Integer> oldPrimary = item.getPrimary();
                String oldName = getWeaponName(p.getItemInHand(), (int) (oldPrimary.getValue() * oldPrimary.getKey().getMultiplier()));
                Part thisKill;
                try {
                    thisKill = Part.valueOf(event.getEntityType().name());
                } catch (IllegalArgumentException e) {
                    return;
                }
                item.incrementStat(thisKill, 1);
                item.incrementStat(Part.MOB_KILLS, 1);
                Entry<Part, Integer> newPrimary = item.getPrimary();
                String newName = getWeaponName(p.getItemInHand(), (int) (newPrimary.getValue() * newPrimary.getKey().getMultiplier()));
                if (!oldName.equals(newName)) {
                    getServer().broadcastMessage(p.getDisplayName() + "'s " + toTitleCase(p.getItemInHand().getType().toString().toLowerCase().replaceAll("_", " ")) + ChatColor.WHITE + " has reached a new rank: " + getWeaponName((int) (newPrimary.getValue() * newPrimary.getKey().getMultiplier())));
                }
                p.setItemInHand(item.getItemStack());
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (Crate.isCrate(event.getItemInHand())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You may not place Steve Co. Supply Crates");
        } else if (event.getItemInHand().getType().isBlock() && StrangeWeapon.isStrangeWeapon(event.getItemInHand())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You may not place strange weapons");
        }
    }
}