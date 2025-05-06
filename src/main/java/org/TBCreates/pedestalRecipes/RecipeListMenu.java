package org.TBCreates.pedestalRecipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;

public class RecipeListMenu {

    private static final Map<UUID, String> viewing = new HashMap<>();

    public static void open(Player player) {
        File file = new File(pedestalRecipes.getInstance().getDataFolder(), "recipes.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection section = config.getConfigurationSection("recipes");
        if (section == null || section.getKeys(false).isEmpty()) {
            player.sendMessage("§cNo recipes found.");
            return;
        }

        List<String> keys = new ArrayList<>(section.getKeys(false));
        int size = Math.min(54, ((keys.size() / 9) + 1) * 9); // max 54 slots
        Inventory inv = Bukkit.createInventory(player, size, "§8All Recipes");

        for (int i = 0; i < keys.size() && i < size; i++) {
            String key = keys.get(i);
            String path = "recipes." + key;

            Material resultMat = Material.matchMaterial(config.getString(path + ".result.type", "STONE"));
            int amount = config.getInt(path + ".result.amount", 1);
            int limit = config.getInt(path + ".limit", 1);

            ItemStack item = new ItemStack(resultMat != null ? resultMat : Material.BARRIER, amount);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§a" + key);
            List<String> lore = new ArrayList<>();
            lore.add("§7Limit: " + (limit == 0 ? "§einfinite" : "§f" + limit));
            lore.add("§cClick to delete");
            meta.setLore(lore);
            item.setItemMeta(meta);

            inv.setItem(i, item);
        }

        viewing.put(player.getUniqueId(), "recipes");
        player.openInventory(inv);
    }

    public static void handleClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        if (!e.getView().getTitle().equals("§8All Recipes")) return;

        e.setCancelled(true);
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String recipeName = clicked.getItemMeta().getDisplayName().replace("§a", "");
        boolean deleted = deleteRecipe(recipeName);

        if (deleted) {
            player.sendMessage("§aDeleted recipe: §f" + recipeName);
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(pedestalRecipes.getInstance(), () -> open(player), 2L);
        } else {
            player.sendMessage("§cFailed to delete recipe.");
        }
    }

    public static boolean deleteRecipe(String name) {
        File file = new File(pedestalRecipes.getInstance().getDataFolder(), "recipes.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        if (config.contains("recipes." + name)) {
            config.set("recipes." + name, null);
            try {
                config.save(file);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
