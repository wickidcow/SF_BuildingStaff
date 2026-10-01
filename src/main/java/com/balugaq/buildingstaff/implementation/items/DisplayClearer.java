package com.balugaq.buildingstaff.implementation.items;

import com.balugaq.buildingstaff.implementation.BuildingStaffPlugin;
import com.balugaq.buildingstaff.utils.KeyUtil;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;


/**
 * @author balugaq
 */
public class DisplayClearer extends SlimefunItem {
    public static final NamespacedKey BS_COOLDOWN_KEY = KeyUtil.newKey("cooldown");

    public DisplayClearer(final ItemGroup itemGroup, final SlimefunItemStack item, final RecipeType recipeType, final ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
        addItemHandler((ItemUseHandler) event -> {
            event.cancel();

            if (getCooldown(event.getItem()) - System.currentTimeMillis() < 5 * 1000) {
                return;
            }

            setCooldown(event.getItem());

            Player player = event.getPlayer();
            player.getWorld().getNearbyEntitiesByType(Display.class, player.getLocation(), 8, 8, 8).forEach(display -> {
                if (display.getPersistentDataContainer().has(KeyUtil.DISPLAY_PROJECTION, PersistentDataType.BYTE)) {
                    display.remove();
                }
            });
        });
    }

    public void setCooldown(ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        meta.getPersistentDataContainer().set(BS_COOLDOWN_KEY, PersistentDataType.LONG, System.currentTimeMillis());
        itemStack.setItemMeta(meta);
    }

    public long getCooldown(ItemStack itemStack) {
        return itemStack.getItemMeta().getPersistentDataContainer().getOrDefault(BS_COOLDOWN_KEY, PersistentDataType.LONG, 0L);
    }
}
