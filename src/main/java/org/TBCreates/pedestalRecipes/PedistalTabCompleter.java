package org.TBCreates.pedestalRecipes;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class PedistalTabCompleter implements TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList("recipe", "recipes", "delete", "place", "remove");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("place")) {
                return getRecipeNames().stream()
                        .filter(name -> name.startsWith(args[1].toLowerCase()))
                        .toList();
            }
            if (args[0].equalsIgnoreCase("recipe")) {
                return List.of("create");
            }
        }

        return Collections.emptyList();
    }

    private List<String> getRecipeNames() {
        File file = new File(pedestalRecipes.getInstance().getDataFolder(), "recipes.yml");
        if (!file.exists()) return List.of();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        return new ArrayList<>(config.getConfigurationSection("recipes").getKeys(false));
    }
}
