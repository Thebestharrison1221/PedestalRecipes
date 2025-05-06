package org.TBCreates.pedestalRecipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PedistalMenu {

    private static final Map<UUID, RecipeSession> activeSessions = new HashMap<>();

    public static void openMenu(Player player, String recipeName) {
        Inventory gui = Bukkit.createInventory(player, 27, "§8Pedestal Recipe: " + recipeName);

        // Decorative borders
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.setDisplayName(" ");
        border.setItemMeta(borderMeta);

        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, border);
        }

        // Input slots: 10-16
        for (int i = 10; i <= 16; i++) {
            gui.setItem(i, new ItemStack(Material.AIR));
        }

        // Output indicator
        ItemStack output = new ItemStack(Material.CHEST);
        ItemMeta outputMeta = output.getItemMeta();
        outputMeta.setDisplayName("§eDrag result item here");
        output.setItemMeta(outputMeta);
        gui.setItem(24, output);

        // Limit button
        ItemStack limit = new ItemStack(Material.NAME_TAG);
        ItemMeta limitMeta = limit.getItemMeta();
        limitMeta.setDisplayName("§bCraft Limit: §f1");
        limitMeta.setLore(List.of(
                "§7Left-click to increase",
                "§7Right-click to decrease",
                "§70 = unlimited"
        ));
        limit.setItemMeta(limitMeta);
        gui.setItem(22, limit);

        // Confirm
        ItemStack confirm = new ItemStack(Material.LIME_WOOL);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.setDisplayName("§aConfirm & Save Recipe");
        confirm.setItemMeta(confirmMeta);
        gui.setItem(26, confirm);

        activeSessions.put(player.getUniqueId(), new RecipeSession(recipeName, gui));
        player.openInventory(gui);
    }

    public static void handleClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory inv = event.getInventory();

        if (!event.getView().getTitle().startsWith("§8Pedestal Recipe")) return;
        if (!activeSessions.containsKey(player.getUniqueId())) return;

        int slot = event.getRawSlot();

        // Cancel click outside GUI
        if (slot >= inv.getSize()) return;

        // Prevent clicking non-editable slots
        if (!Set.of(10,11,12,13,14,15,16,22,24,26).contains(slot)) {
            event.setCancelled(true);
            return;
        }

        RecipeSession session = activeSessions.get(player.getUniqueId());

        // Limit button
        if (slot == 22) {
            event.setCancelled(true);
            if (event.isLeftClick()) {
                session.limit++;
            } else if (event.isRightClick()) {
                session.limit = Math.max(0, session.limit - 1);
            }

            ItemStack limitItem = inv.getItem(22);
            if (limitItem != null) {
                ItemMeta meta = limitItem.getItemMeta();
                meta.setDisplayName("§bCraft Limit: §f" + session.limit);
                limitItem.setItemMeta(meta);
                inv.setItem(22, limitItem);
            }

            player.sendMessage("§bCraft limit set to: §f" + session.limit);
        }

        // Confirm button
        if (slot == 26) {
            event.setCancelled(true);
            saveRecipe(player);
        }

        // Prevent output slot from being taken
        if (slot == 24 && event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
            event.setCancelled(true);
            ItemStack placed = event.getCursor().clone();
            inv.setItem(24, placed);
            player.setItemOnCursor(null);
        }
    }

    public static void handleDrag(InventoryDragEvent event) {
        if (!event.getView().getTitle().startsWith("§8Pedestal Recipe")) return;

        for (int slot : event.getRawSlots()) {
            if (!Set.of(10,11,12,13,14,15,16,24).contains(slot)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private static void saveRecipe(Player player) {
        RecipeSession session = activeSessions.get(player.getUniqueId());
        Inventory inv = session.inventory;

        List<ItemStack> ingredients = new ArrayList<>();
        for (int i = 10; i <= 16; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                ingredients.add(item.clone());
            }
        }

        ItemStack result = inv.getItem(24);
        if (result == null || result.getType() == Material.AIR) {
            player.sendMessage("§cYou must add an output item.");
            return;
        }

        // Save to YAML
        File file = new File(pedestalRecipes.getInstance().getDataFolder(), "recipes.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        String path = "recipes." + session.recipeName;
        config.set(path + ".limit", session.limit);
        config.set(path + ".result.type", result.getType().name());
        config.set(path + ".result.amount", result.getAmount());

        for (int i = 0; i < ingredients.size(); i++) {
            ItemStack item = ingredients.get(i);
            config.set(path + ".ingredients." + i + ".type", item.getType().name());
            config.set(path + ".ingredients." + i + ".amount", item.getAmount());
        }

        try {
            config.save(file);
            player.sendMessage("§aRecipe '" + session.recipeName + "' saved successfully!");
        } catch (IOException e) {
            player.sendMessage("§cFailed to save recipe file.");
            e.printStackTrace();
        }

        activeSessions.remove(player.getUniqueId());
        player.closeInventory();
    }

    private static class RecipeSession {
        String recipeName;
        List<ItemStack> ingredients;
        ItemStack result;
        int limit;
        Inventory inventory;

        RecipeSession(String name, Inventory inv) {
            this.recipeName = name;
            this.inventory = inv;
            this.ingredients = new ArrayList<>();
            this.limit = 1;
        }
    }
}
