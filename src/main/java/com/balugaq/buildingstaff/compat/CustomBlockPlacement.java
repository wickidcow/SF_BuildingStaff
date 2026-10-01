package com.balugaq.buildingstaff.compat;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import java.util.Set;
import java.util.logging.Level;

/** Synchronous, exact-item placement. No registered block is overwritten during recovery. */
public final class CustomBlockPlacement {
    private CustomBlockPlacement() {}
    public static int countItems(PlayerInventory inventory,ItemStack template,int limit) {
        if (limit<=0) return 0;
        int count=0;
        for (ItemStack item:inventory.getStorageContents()) {
            if (item!=null && !item.getType().isAir() && item.getAmount()>0 && item.isSimilar(template)) {
                count+=Math.min(limit-count,item.getAmount());
                if (count==limit) return count;
            }
        }
        return count;
    }
    private static boolean loaded(Location location) {
        return location.getWorld()!=null && location.getWorld().isChunkLoaded(location.getBlockX()>>4,location.getBlockZ()>>4);
    }
    private static boolean allowed(Player player,Location location) {
        return player.isOnline() && player.getWorld().equals(location.getWorld()) && loaded(location)
                && (player.getGameMode()==GameMode.SURVIVAL || player.getGameMode()==GameMode.CREATIVE)
                && location.getWorld().getWorldBorder().isInside(location);
    }
    public static void placeBatch(Plugin plugin,Player player,Set<Location> targets,BlockFace facing,ItemStack template,int limit) {
        if (!Bukkit.isPrimaryThread() || !BlockCompatibility.isRebarAvailable()) return;
        ItemStack held=player.getInventory().getItemInMainHand().clone();
        GameMode mode=player.getGameMode();int attempted=0;
        for (Location location:targets) {
            if (attempted++>=limit) break;
            if (!allowed(player,location) || mode!=player.getGameMode()
                    || !held.isSimilar(player.getInventory().getItemInMainHand())) break;
            placeOne(plugin,player,location.getBlock(),facing,template);
        }
    }
    static PlacementTransaction.Result placeOne(Plugin plugin,Player player,Block block,BlockFace facing,ItemStack template) {
        Material material=block.getType();
        if (!Bukkit.isPrimaryThread() || !allowed(player,block.getLocation()) || !template.getType().isBlock()
                || !(material==Material.AIR || material==Material.WATER || material==Material.LAVA)
                || BlockCompatibility.token(block)!=null
                || me.mrCookieSlime.Slimefun.api.BlockStorage.hasBlockInfo(block)
                || !Slimefun.getProtectionManager().hasPermission(player,block,Interaction.PLACE_BLOCK)) {
            return PlacementTransaction.Result.NO_PAYMENT;
        }
        BlockState previous=block.getState();
        ItemStack one=template.clone();one.setAmount(1);
        var operations=new PlacementTransaction.Operations() {
            ItemStack escrow;
            Object created;
            @Override public boolean reserve() {
                if (player.getGameMode()==GameMode.CREATIVE) return true;
                PlayerInventory inventory=player.getInventory();
                int slots=inventory.getStorageContents().length;
                for(int slot=0;slot<slots;slot++) {
                    ItemStack current=inventory.getItem(slot);
                    if(current==null || current.getAmount()<1 || !current.isSimilar(one)) continue;
                    escrow=current.clone();escrow.setAmount(1);
                    ItemStack remainder=current.clone();remainder.setAmount(current.getAmount()-1);
                    inventory.setItem(slot,remainder.getAmount()==0?null:remainder);
                    return true;
                }
                return false;
            }
            @Override public boolean place() {
                block.setType(one.getType(),false);
                if (block.getType()!=one.getType()) return false;
                var event=new BlockPlaceEvent(block,previous,block.getRelative(facing.getOppositeFace()),
                        one.clone(),player,true,EquipmentSlot.HAND);
                Bukkit.getPluginManager().callEvent(event);
                created=BlockCompatibility.token(block);
                return !event.isCancelled() && event.canBuild() && created!=null
                        && created!=BlockCompatibility.UNAVAILABLE && block.getType()==one.getType()
                        && BlockCompatibility.isPlacedFromItem(block,one);
            }
            @Override public boolean rollback() {
                Object now=BlockCompatibility.token(block);
                if (now==BlockCompatibility.UNAVAILABLE || (now!=null && (created==null || now!=created))) return false;
                if (now!=null && !BlockCompatibility.isPlacedFromItem(block,one)) return false;
                if (now!=null && !BlockCompatibility.rollback(block,created)) return false;
                if (BlockCompatibility.token(block)!=null
                        || me.mrCookieSlime.Slimefun.api.BlockStorage.hasBlockInfo(block)) return false;
                Material current=block.getType();
                if (current!=Material.AIR && current!=one.getType() && current!=previous.getType()) return false;
                return previous.update(true,false);
            }
            @Override public void refund() {
                if (escrow==null) return;
                for(ItemStack leftover:player.getInventory().addItem(escrow).values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(),leftover);
                }
                escrow=null;
            }
            @Override public void report(Throwable failure) {
                plugin.getLogger().log(Level.WARNING,"Custom staff placement refused at "+block.getLocation(),failure);
            }
        };
        var result=PlacementTransaction.execute(operations);
        if(result==PlacementTransaction.Result.RECOVERY_REQUIRED) {
            plugin.getLogger().warning("Custom staff placement needs recovery at "+block.getLocation()
                    +"; no registered block was overwritten and no unverified refund was issued.");
        }
        return result;
    }
    /** Rebar owns custom drops and entity cleanup; never run vanilla breakNaturally afterward. */
    public static void breakCustom(Plugin plugin,Player player,Block block) {
        if(!Bukkit.isPrimaryThread() || !allowed(player,block.getLocation())
                || !Slimefun.getProtectionManager().hasPermission(player,block,Interaction.BREAK_BLOCK)) return;
        Object token=BlockCompatibility.token(block);
        if(token==null || token==BlockCompatibility.UNAVAILABLE) return;
        Material original=block.getType();
        try {
            var event=new BlockBreakEvent(block,player);Bukkit.getPluginManager().callEvent(event);
            Object after=BlockCompatibility.token(block);
            // The actual provider remove event must have removed its registration.
            // Cancellation or a same-key replacement cannot authorize clearing it.
            if(after==null && !event.isDropItems() && block.getType()==original) block.setType(Material.AIR,false);
        } catch(RuntimeException | LinkageError failure) {
            plugin.getLogger().log(Level.WARNING,"Custom staff break refused at "+block.getLocation(),failure);
        }
    }
}
