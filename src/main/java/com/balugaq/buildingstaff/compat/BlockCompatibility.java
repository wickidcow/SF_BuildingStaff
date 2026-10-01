package com.balugaq.buildingstaff.compat;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Optional bridge. No Rebar types occur in signatures used without that provider. */
public final class BlockCompatibility {
    static final Object UNAVAILABLE = new Object();
    private BlockCompatibility() {}
    public static boolean isRebarAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("Rebar");
    }
    public static boolean isCustomBlock(@NotNull Block block) {
        if (!isRebarAvailable()) return false;
        try { return RebarCompatibility.isRebarBlock(block); }
        catch (RuntimeException | LinkageError refused) {
            // Unknown custom state must not be treated as ordinary vanilla state.
            return true;
        }
    }
    public static @Nullable ItemStack getPlacementItem(@NotNull Block block,@NotNull Player player) {
        if (!isRebarAvailable()) return new ItemStack(block.getType(),1);
        try {
            if (!RebarCompatibility.isRebarBlock(block)) return new ItemStack(block.getType(),1);
            return RebarCompatibility.getPickItem(block,player);
        } catch (RuntimeException | LinkageError refused) { return null; }
    }
    public static boolean isSameBlockType(@NotNull Block source,@NotNull Block candidate) {
        if (!isRebarAvailable()) return source.getType()==candidate.getType();
        try { return RebarCompatibility.isSameBlockType(source,candidate); }
        catch (RuntimeException | LinkageError refused) { return false; }
    }
    public static boolean isPlacedFromItem(@NotNull Block block,@NotNull ItemStack item) {
        if (!isRebarAvailable()) return false;
        try { return RebarCompatibility.isPlacedFromItem(block,item); }
        catch (RuntimeException | LinkageError refused) { return false; }
    }
    static Object token(Block block) {
        if (!isRebarAvailable()) return UNAVAILABLE;
        try { return RebarCompatibility.token(block); }
        catch (RuntimeException | LinkageError refused) { return UNAVAILABLE; }
    }
    static boolean rollback(Block block,Object expected) {
        if (!isRebarAvailable() || expected==UNAVAILABLE) return false;
        try { return RebarCompatibility.rollback(block,expected); }
        catch (RuntimeException | LinkageError refused) { return false; }
    }
}
