package org.TBCreates.pedestalRecipes;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Sound;
import org.bukkit.Particle;

import java.io.File;
import java.util.Map;
import java.util.Set;

public class HologramManager implements Listener {

    // Store a reference to the hologram for each recipe to easily manage it
    private static final Map<String, Hologram> hologramMap = new java.util.HashMap<>();
    private static final Map<String, Map<Player, Long>> recipeCooldowns = new java.util.HashMap<>();  // Track player cooldowns for specific recipes

    public static void spawnHologram(Location location, String recipeName) {
        // Get the recipe configuration
        File file = new File(pedestalRecipes.getInstance().getDataFolder(), "recipes.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        String path = "recipes." + recipeName;
        if (!config.contains(path)) {
            return; // If recipe doesn't exist, do nothing
        }

        // Extract the recipe data
        int limit = config.getInt(path + ".limit", 1);
        String resultType = config.getString(path + ".result.type", "STONE");
        int resultAmount = config.getInt(path + ".result.amount", 1);
        ItemStack resultItem = new ItemStack(Material.matchMaterial(resultType), resultAmount);

        // Create the hologram
        Location hologramLocation = location.clone().add(0.5, 1.5, 0.5); // Position hologram above the block
        Hologram hologram = DHAPI.createHologram("pedestal_" + recipeName, hologramLocation, true);

        // Add recipe lines to the hologram
        DHAPI.addHologramLine(hologram, "§eOutput: " + resultItem.getType().name());
        DHAPI.addHologramLine(hologram, "§bLimit: §f" + (limit == 0 ? "∞" : limit));
        DHAPI.addHologramLine(hologram, "§eRecipe:");

        // Add ingredients to the hologram
        if (config.contains(path + ".ingredients")) {
            Set<String> keys = config.getConfigurationSection(path + ".ingredients").getKeys(false);
            if (keys != null && !keys.isEmpty()) {
                for (String key : keys) {
                    String type = config.getString(path + ".ingredients." + key + ".type");
                    int amount = config.getInt(path + ".ingredients." + key + ".amount");
                    DHAPI.addHologramLine(hologram, "§7" + type + " x" + amount);
                }
            }
        } else {
            DHAPI.addHologramLine(hologram, "§7None");
        }

        // Add the result item line (for the recipe's output)
        DHAPI.addHologramLine(hologram, "§eResult: " + resultItem.getType().name() + " x" + resultAmount);

        // Store the hologram in the map for easy removal
        hologramMap.put(recipeName, hologram);
    }

    public static void removeHologramByRecipe(String recipeName) {
        Hologram hologram = hologramMap.remove(recipeName);  // Get and remove the hologram for the recipe

        if (hologram != null) {
            hologram.delete();  // Delete the hologram
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location playerLocation = player.getLocation();

        if (player.isSneaking()) {  // Check if the player is crouching
            for (Map.Entry<String, Hologram> entry : hologramMap.entrySet()) {
                Hologram hologram = entry.getValue();
                Location hologramLocation = hologram.getLocation();

                // Check if the player is within a certain radius (e.g., 2 blocks) of the hologram
                if (playerLocation.distance(hologramLocation) <= 2.0) {
                    String recipeName = entry.getKey();
                    long currentTime = System.currentTimeMillis();

                    // If the player is within cooldown for the specific recipe, inform them and stop the process
                    if (isInCooldown(player, recipeName)) {
                        long timeRemaining = getRemainingCooldownTime(player, recipeName);
                        player.sendActionBar("This recipe is on cooldown. Please wait!");
                        return; // Ignore if the player is still on cooldown for this recipe
                    }

                    // Check if the recipe limit is reached
                    if (checkCraftingLimit(recipeName)) {
                        player.sendMessage("This recipe has already been crafted the max amount of times.");
                        return; // If the limit is reached, don't start the countdown
                    }

                    // Check if the player has the necessary ingredients
                    if (!checkIngredients(player, recipeName)) {
                        player.sendMessage("You don't have enough ingredients to craft this!");
                        return; // If the player doesn't have enough ingredients, don't start the countdown
                    }

                    // Set the player's cooldown for this specific recipe (so they can't spam the countdown)
                    setRecipeCooldown(player, recipeName, currentTime);

                    // Start countdown and crafting process if the player is near the hologram and crouching
                    startCraftingCountdown(player, recipeName, hologramLocation);
                    return;
                }
            }
        }
    }

    private void setRecipeCooldown(Player player, String recipeName, long currentTime) {
        recipeCooldowns
                .computeIfAbsent(recipeName, k -> new java.util.HashMap<>())
                .put(player, currentTime);
    }

    private boolean isInCooldown(Player player, String recipeName) {
        return recipeCooldowns.containsKey(recipeName) &&
                recipeCooldowns.get(recipeName).containsKey(player) &&
                System.currentTimeMillis() - recipeCooldowns.get(recipeName).get(player) < 900000;  // 15 minutes in milliseconds
    }

    private long getRemainingCooldownTime(Player player, String recipeName) {
        long cooldownEnd = recipeCooldowns.get(recipeName).get(player) + 900000;  // 15 minutes in milliseconds
        return cooldownEnd - System.currentTimeMillis();
    }

    private void startCraftingCountdown(final Player player, final String recipeName, final Location hologramLocation) {
        final int countdownTime = 5;  // Countdown time in seconds
        final int[] timeLeft = {countdownTime};  // Array to hold the countdown time (used in lambda)

        // Inform the player that the countdown has started
        player.sendMessage("You are crouching near the hologram. Crafting will start in " + countdownTime + " seconds.");

        // Start a delayed task to handle countdown and crafting
        new BukkitRunnable() {
            @Override
            public void run() {
                // Every second, update the countdown
                if (timeLeft[0] > 0) {
                    player.sendTitle("Crafting in " + timeLeft[0] + "...", "");
                    timeLeft[0]--;
                } else {
                    // Countdown finished, start crafting
                    player.sendMessage("Crafting now...");

                    // Perform the crafting action (including the animation)
                    if (checkIngredients(player, recipeName)) {
                        handleCrafting(player, recipeName);
                    } else {
                        player.sendActionBar("You don't have enough ingredients to craft this!");
                    }

                    // Show a particle effect or any animation before giving the item
                    showCraftingAnimation(hologramLocation);

                    // Stop the task after the countdown ends
                    cancel();
                }
            }
        }.runTaskTimer(pedestalRecipes.getInstance(), 0L, 20L);  // Runs every 20 ticks (1 second)
    }

    private void showCraftingAnimation(Location hologramLocation) {
        // Example: Spawn a particle effect around the hologram location
        hologramLocation.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, hologramLocation, 20);
        hologramLocation.getWorld().playSound(hologramLocation, Sound.BLOCK_ANVIL_USE, 1.0F, 1.0F);
    }

    private boolean checkIngredients(Player player, String recipeName) {
        File file = new File(pedestalRecipes.getInstance().getDataFolder(), "recipes.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        String path = "recipes." + recipeName;
        Set<String> keys = config.getConfigurationSection(path + ".ingredients").getKeys(false);

        if (keys == null || keys.isEmpty()) {
            return false;
        }

        for (String key : keys) {
            String type = config.getString(path + ".ingredients." + key + ".type");
            int amount = config.getInt(path + ".ingredients." + key + ".amount");

            Material material = Material.matchMaterial(type);
            if (material == null) {
                continue; // Skip if the material is invalid
            }

            int materialAmount = 0;
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == material) {
                    materialAmount += item.getAmount();
                }
            }

            if (materialAmount < amount) {
                return false; // Player doesn't have enough of the required material
            }
        }

        return true;
    }

    private boolean checkCraftingLimit(String recipeName) {
        File file = new File(pedestalRecipes.getInstance().getDataFolder(), "recipes.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        String path = "recipes." + recipeName;
        int limit = config.getInt(path + ".limit", 1);

        // In this example, the crafting limit is simply checked
        // You can extend this logic based on your needs, such as keeping track of how many times the player has crafted the item
        return limit == 0; // Unlimited crafting
    }

    private void handleCrafting(Player player, String recipeName) {
        File file = new File(pedestalRecipes.getInstance().getDataFolder(), "recipes.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        String path = "recipes." + recipeName;
        String resultType = config.getString(path + ".result.type", "STONE");
        int resultAmount = config.getInt(path + ".result.amount", 1);

        Material resultMaterial = Material.matchMaterial(resultType);
        if (resultMaterial != null) {
            ItemStack resultItem = new ItemStack(resultMaterial, resultAmount);
            player.getInventory().addItem(resultItem);
            player.sendMessage("You crafted " + resultAmount + " " + resultMaterial.name());
        }
    }
}
