package com.balugaq.buildingstaff.compat;

import io.github.pylonmc.rebar.block.BlockStorage;
import io.github.pylonmc.rebar.block.RebarBlock;
import io.github.pylonmc.rebar.block.context.BlockBreakContext;
import io.github.pylonmc.rebar.item.RebarItem;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Typed Rebar bridge. This class is only touched when the Rebar plugin is enabled.
 * Pylon is built on Rebar, so its custom blocks flow through the same lifecycle.
 */
final class RebarCompatibility {
    private RebarCompatibility() {
    }

    static boolean isRebarBlock(@NotNull Block block) {
        return BlockStorage.isRebarBlock(block);
    }

    static @Nullable ItemStack getPickItem(@NotNull Block block, @NotNull Player player) {
        RebarBlock rebarBlock = BlockStorage.get(block);
        if (rebarBlock == null) {
            return null;
        }

        ItemStack pickItem = rebarBlock.getPickItem(player);
        if (pickItem == null) {
            return null;
        }

        ItemStack result = pickItem.clone();
        result.setAmount(1);
        return result;
    }

    static boolean isSameBlockType(@NotNull Block source, @NotNull Block candidate) {
        RebarBlock sourceBlock = BlockStorage.get(source);
        RebarBlock candidateBlock = BlockStorage.get(candidate);

        if (sourceBlock == null || candidateBlock == null) {
            return sourceBlock == null
                    && candidateBlock == null
                    && source.getType() == candidate.getType();
        }

        return sourceBlock.getKey().equals(candidateBlock.getKey());
    }

    static boolean isPlacedFromItem(@NotNull Block block, @NotNull ItemStack item) {
        RebarBlock rebarBlock = BlockStorage.get(block);
        RebarItem rebarItem = RebarItem.fromStack(item);
        if (rebarBlock == null || rebarItem == null) {
            return false;
        }

        NamespacedKey blockKey = rebarItem.getRebarBlock();
        return blockKey != null && blockKey.equals(rebarBlock.getKey());
    }

    /**
     * Attempts to remove a just-created Rebar block without drops.
     *
     * @return true when the matching Rebar block is no longer registered after the rollback attempt
     */
    static boolean rollbackPlacement(@NotNull Block block, @NotNull ItemStack item) {
        if (!isPlacedFromItem(block, item)) {
            return true;
        }

        BlockStorage.breakBlock(block, new BlockBreakContext.PluginBreak(block, false, true));
        return !isPlacedFromItem(block, item);
    }
}
