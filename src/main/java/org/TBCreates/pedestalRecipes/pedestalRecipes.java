package org.TBCreates.pedestalRecipes;
import org.bukkit.plugin.java.JavaPlugin;

public final class pedestalRecipes extends JavaPlugin {

    private static org.TBCreates.pedestalRecipes.pedestalRecipes instance;

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        getCommand("pedestal").setExecutor(new org.TBCreates.pedestalRecipes.PedistalCommand());
        getCommand("pedestal").setTabCompleter(new org.TBCreates.pedestalRecipes.PedistalTabCompleter());
        getServer().getPluginManager().registerEvents(new HologramManager(), this);
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(new org.TBCreates.pedestalRecipes.MenuListener(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static org.TBCreates.pedestalRecipes.pedestalRecipes getInstance() {
        return instance;
    }
}
