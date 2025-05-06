package org.TBCreates.pedestalRecipes;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class MenuListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        PedistalMenu.handleClick(event);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        PedistalMenu.handleDrag(event);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals("§8All Recipes")) {
            RecipeListMenu.handleClick(e);
        }
    }
}