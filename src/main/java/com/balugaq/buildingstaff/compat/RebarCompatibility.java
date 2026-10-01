package com.balugaq.buildingstaff.compat;

import io.github.pylonmc.rebar.block.BlockStorage;
import io.github.pylonmc.rebar.block.RebarBlock;
import io.github.pylonmc.rebar.block.context.BlockBreakContext;
import io.github.pylonmc.rebar.item.RebarItem;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Typed provider calls are resolved only while Rebar is enabled. */
final class RebarCompatibility {
    private RebarCompatibility() {}
    static boolean isRebarBlock(Block block) { return BlockStorage.get(block)!=null; }
    static Object token(Block block) { return BlockStorage.get(block); }
    static ItemStack getPickItem(Block block,Player player) {
        RebarBlock value=BlockStorage.get(block);
        if (value==null) return null;
        ItemStack picked=value.getPickItem(player);
        if (picked==null) return null;
        ItemStack result=picked.clone();result.setAmount(1);return result;
    }
    static boolean isSameBlockType(Block source,Block candidate) {
        RebarBlock left=BlockStorage.get(source),right=BlockStorage.get(candidate);
        if (left==null || right==null) return left==null && right==null && source.getType()==candidate.getType();
        return left.getKey().equals(right.getKey());
    }
    static boolean isPlacedFromItem(Block block,ItemStack item) {
        RebarBlock value=BlockStorage.get(block);RebarItem source=RebarItem.fromStack(item);
        return value!=null && source!=null && value.getKey().equals(source.getRebarBlock());
    }
    static boolean rollback(Block block,Object expected) {
        Object current=BlockStorage.get(block);
        if (current==null) return true;
        // Even a new block with the same key is not the instance created by this transaction.
        if (current!=expected) return false;
        var drops=BlockStorage.breakBlock(block,new BlockBreakContext.PluginBreak(block,false,true));
        // A veto, exceptional provider state or custom callback drops need manual recovery,
        // not a refund that might duplicate a stateful item or its contents.
        return drops!=null && drops.isEmpty() && BlockStorage.get(block)==null;
    }
}
