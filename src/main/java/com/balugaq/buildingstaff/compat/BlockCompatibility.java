package com.balugaq.buildingstaff.compat;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Compatibility entry point for custom block systems supported by BuildingStaff.
 *
 * <p>This class deliberately has no direct Rebar references so BuildingStaff can still
 * load when Rebar and Pylon are not installed.</p>
 */
public final class BlockCompatibility {
    private static final String REBAR_PLUGIN = "Rebar";

    private BlockCompatibility() {
    }

    public static boolean isRebarAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled(REBAR_PLUGIN);
    }

    public static boolean isCustomBlock(@NotNull Block block) {
        if (!isRebarAvailable()) {
            return false;
        }

        try {
            return RebarCompatibility.isRebarBlock(block);
        } catch (LinkageError | RuntimeException ignored) {
            return false;
        }
    }

    /**
     * Gets the item that represents the block for staff placement.
     * Vanilla blocks use a plain material stack. Rebar/Pylon blocks use their pick item.
     *
     * <p>When Rebar is installed, compatibility failures are fail-closed. Returning null is safer
     * than accidentally treating a custom machine as its vanilla backing material.</p>
     */
    public static @Nullable ItemStack getPlacementItem(@NotNull Block block, @NotNull Player player) {
        if (!isRebarAvailable()) {
            return new ItemStack(block.getType(), 1);
        }

        try {
            if (!RebarCompatibility.isRebarBlock(block)) {
                return new ItemStack(block.getType(), 1);
            }
            return RebarCompatibility.getPickItem(block, player);
        } catch (LinkageError | RuntimeException ignored) {
            return null;
        }
    }

    /**
     * Strict block matching used by the connected-surface search.
     * A vanilla block never matches a Rebar/Pylon block that happens to share its backing material.
     */
    public static boolean isSameBlockType(@NotNull Block source, @NotNull Block candidate) {
        if (!isRebarAvailable()) {
            return source.getType() == candidate.getType();
        }

        try {
            return RebarCompatibility.isSameBlockType(source, candidate);
        } catch (LinkageError | RuntimeException ignored) {
            // Fail closed when Rebar is present. Expanding a strict staff selection is unsafe if
            // custom block identity cannot be determined reliably.
            return false;
        }
    }

    /**
     * Confirms that Rebar actually registered the custom block represented by an item.
     */
    public static boolean isPlacedFromItem(@NotNull Block block, @NotNull ItemStack item) {
        if (!isRebarAvailable()) {
            return false;
        }

        try {
            return RebarCompatibility.isPlacedFromItem(block, item);
        } catch (LinkageError | RuntimeException ignored) {
            return false;
        }
    }

    /**
     * Removes a just-created Rebar block without drops before restoring the replaced Bukkit block.
     */
    public static void rollbackCustomPlacement(@NotNull Block block, @NotNull ItemStack item) {
        if (!isRebarAvailable()) {
            return;
        }

        try {
            RebarCompatibility.rollbackPlacement(block, item);
        } catch (LinkageError | RuntimeException ignored) {
            // Best effort rollback; callers only use this after Rebar successfully registered the block.
        }
    }
}
