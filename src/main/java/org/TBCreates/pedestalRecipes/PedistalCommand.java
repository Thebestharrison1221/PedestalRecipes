package org.TBCreates.pedestalRecipes;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class PedistalCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        // Handle recipe creation
        if (args.length >= 3 && args[0].equalsIgnoreCase("recipe") && args[1].equalsIgnoreCase("create")) {
            String recipeName = args[2];
            PedistalMenu.openMenu(player, recipeName);
            return true;
        }

        // Handle listing recipes
        if (args.length == 1 && args[0].equalsIgnoreCase("recipes")) {
            RecipeListMenu.open(player);
            return true;
        }

        // Handle recipe deletion
        if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {
            boolean success = RecipeListMenu.deleteRecipe(args[1]);
            if (success) {
                sender.sendMessage("§aRecipe '" + args[1] + "' deleted.");
            } else {
                sender.sendMessage("§cRecipe not found.");
            }
            return true;
        }

        // Handle placing pedestal with recipe
        if (args.length == 2 && args[0].equalsIgnoreCase("place")) {
            String recipe = args[1];
            File file = new File(pedestalRecipes.getInstance().getDataFolder(), "recipes.yml");
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

            // Check if the recipe exists
            if (!config.contains("recipes." + recipe)) {
                player.sendMessage("§cRecipe '" + recipe + "' not found.");
                return true;
            }

            // Place the pedestal
            Block block = player.getLocation().getBlock();
            Material pedestalMaterial = Material.LODESTONE; // Set to Lodestone
            block.setType(pedestalMaterial); // Place pedestal block

            // Spawn the hologram
            HologramManager.spawnHologram(block.getLocation(), recipe);
            player.sendMessage("§aPedestal placed with recipe: §e" + recipe);
            return true;
        }

        // Handle removing pedestal
        if (args.length == 1 && args[0].equalsIgnoreCase("remove")) {
            Block block = player.getLocation().getBlock();

            // Check if the player is standing on a pedestal (LODESTONE)
            Block blockBelow = block.getRelative(0, -1, 0);
            if (blockBelow.getType() == Material.LODESTONE) {
                // Get the recipe associated with this pedestal
                String recipeName = getRecipeFromPedestal(blockBelow);

                // Remove the hologram associated with the recipe
                HologramManager.removeHologramByRecipe(recipeName);

                // Remove the pedestal (LODESTONE block)
                blockBelow.setType(Material.AIR);  // Replace pedestal with air

                player.sendMessage("§cPedestal and display removed.");
            } else {
                player.sendMessage("§7You're not standing on a pedestal.");
            }
            return true;
        }

        // Display usage instructions
        sender.sendMessage("§cUsage:");
        sender.sendMessage("§7/pedestal recipe create <name>");
        sender.sendMessage("§7/pedestal recipes");
        sender.sendMessage("§7/pedestal delete <name>");
        sender.sendMessage("§7/pedestal place <recipe>");
        sender.sendMessage("§7/pedestal remove");
        return true;
    }

    // Utility method to get the recipe name from a pedestal (this method should be modified based on your system)
    private String getRecipeFromPedestal(Block block) {
        // This is a placeholder method, you should implement logic to map a block to a recipe name if needed
        return "test"; // Replace with actual logic for your plugin
    }
}
