package com.balugaq.buildingstaff.api.items;

import com.balugaq.buildingstaff.compat.BlockCompatibility;
import com.balugaq.buildingstaff.utils.StaffUtil;
import com.balugaq.buildingstaff.utils.WorldUtils;
import com.destroystokyo.paper.MaterialTags;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

@Getter
public abstract class BuildingStaff extends SlimefunItem implements Staff {
    private final int limitBlocks;
    private final boolean blockStrict;
    private final boolean opOnly;

    public BuildingStaff(@NotNull ItemGroup itemGroup, @NotNull SlimefunItemStack item, @NotNull RecipeType recipeType, ItemStack @NotNull [] recipe, int limitBlocks, boolean blockStrict, boolean opOnly) {
        super(itemGroup, item, recipeType, recipe);
        this.limitBlocks = limitBlocks;
        this.blockStrict = blockStrict;
        this.opOnly = opOnly;
    }

    @NotNull
    private static BlockFace getBlockFaceAsCartesian(@NotNull BlockFace originalFacing) {
        BlockFace lookingFacing = originalFacing.getOppositeFace();
        if (!originalFacing.isCartesian()) {
            switch (originalFacing) {
                case NORTH_EAST, NORTH_WEST, NORTH_NORTH_EAST, NORTH_NORTH_WEST -> lookingFacing = BlockFace.NORTH;
                case SOUTH_EAST, SOUTH_WEST, SOUTH_SOUTH_EAST, SOUTH_SOUTH_WEST -> lookingFacing = BlockFace.SOUTH;
                case EAST_NORTH_EAST, EAST_SOUTH_EAST -> lookingFacing = BlockFace.EAST;
                case WEST_NORTH_WEST, WEST_SOUTH_WEST -> lookingFacing = BlockFace.WEST;
                default -> {
                }
            }
        }
        return lookingFacing;
    }

    @Override
    public void preRegister() {
        super.preRegister();
        addItemHandler((ItemUseHandler) playerRightClickEvent -> {
            if (playerRightClickEvent.getInteractEvent().getHand() != EquipmentSlot.HAND) {
                return;
            }

            Player player = playerRightClickEvent.getPlayer();
            if (opOnly && !player.isOp()) {
                return;
            }

            if (player.getGameMode() == GameMode.SPECTATOR) {
                return;
            }

            if (isDisabledIn(player.getWorld())) {
                return;
            }

            Block lookingAtBlock = player.getTargetBlockExact(6, FluidCollisionMode.NEVER);
            if (lookingAtBlock == null || lookingAtBlock.getType() == Material.AIR) {
                return;
            }

            Material material = lookingAtBlock.getType();
            boolean customBlock = BlockCompatibility.isCustomBlock(lookingAtBlock);

            // Rebar/Pylon blocks own their placement lifecycle and may intentionally use a
            // backing material that vanilla BuildingStaff normally rejects (furnaces, tables, etc.).
            if (!customBlock && isDisabledMaterial(material)) {
                return;
            }

            ItemStack placementItem = BlockCompatibility.getPlacementItem(lookingAtBlock, player);
            if (placementItem == null || placementItem.getType().isAir() || !placementItem.getType().isBlock()) {
                return;
            }
            placementItem.setAmount(1);

            // Rebar guarantees that a block item and its registered block share a material.
            // Refuse a mismatched pick item rather than risk corrupting custom block storage.
            if (customBlock && placementItem.getType() != material) {
                return;
            }

            int playerHas = player.getGameMode() == GameMode.CREATIVE
                    ? 4096
                    : countAvailableItems(player, placementItem, customBlock);
            if (playerHas <= 0) {
                return;
            }

            BlockFace originalFacing = player.getTargetBlockFace(6, FluidCollisionMode.NEVER);
            if (originalFacing == null) {
                return;
            }

            BlockFace lookingFacing = getBlockFaceAsCartesian(originalFacing);
            ItemStack staffItem = player.getInventory().getItemInMainHand();
            Set<Location> buildingLocations = StaffUtil.getBuildingLocations(
                    player,
                    Math.min(limitBlocks, playerHas),
                    getAxis(staffItem),
                    blockStrict
            );

            if (customBlock) {
                scheduleCustomBlockPlacement(player, buildingLocations, lookingFacing, placementItem);
                return;
            }

            int consumed = 0;
            Set<Block> blocks = new HashSet<>();
            for (Location location : buildingLocations) {
                Block block = location.getBlock();
                if (isReplaceable(block)) {
                    BlockPlaceEvent blockPlaceEvent = new BlockPlaceEvent(
                            block,
                            block.getState(),
                            block.getRelative(lookingFacing.getOppositeFace()),
                            placementItem,
                            player,
                            Slimefun.getProtectionManager().hasPermission(player, block, Interaction.PLACE_BLOCK),
                            EquipmentSlot.HAND
                    );
                    Bukkit.getPluginManager().callEvent(blockPlaceEvent);
                    if (!blockPlaceEvent.isCancelled()) {
                        blocks.add(block);
                        consumed++;
                    }
                }
            }

            // Preserve the original vanilla behavior: perform the physical placement one tick later
            // so the staff interaction does not recursively trigger another PlayerInteractEvent.
            Bukkit.getScheduler().runTaskLater(getAddon().getJavaPlugin(), () -> {
                for (Block block : blocks) {
                    if (copyStateAble(material)) {
                        WorldUtils.copyBlockState(lookingAtBlock.getState(), block);
                    } else {
                        block.setType(material);
                    }
                    block.getState().update(true, true);
                }
            }, 1);

            if (player.getGameMode() != GameMode.CREATIVE && consumed > 0) {
                player.getInventory().removeItem(new ItemStack(material, consumed));
            }
        });
    }

    /**
     * Rebar's PlayerPlace context expects the physical Bukkit block to already be present when
     * BlockPlaceEvent fires. BuildingStaff normally places blocks one tick after its synthetic event,
     * so custom blocks use a dedicated delayed transaction that mirrors vanilla placement ordering.
     */
    private void scheduleCustomBlockPlacement(
            @NotNull Player player,
            @NotNull Set<Location> buildingLocations,
            @NotNull BlockFace lookingFacing,
            @NotNull ItemStack placementItem
    ) {
        ItemStack template = placementItem.clone();
        Set<Location> locations = new HashSet<>(buildingLocations);

        Bukkit.getScheduler().runTaskLater(getAddon().getJavaPlugin(), () -> {
            int available = player.getGameMode() == GameMode.CREATIVE
                    ? limitBlocks
                    : countAvailableItems(player, template, true);
            int maxPlacements = Math.min(limitBlocks, available);
            int placed = 0;

            for (Location location : locations) {
                if (placed >= maxPlacements) {
                    break;
                }

                Block block = location.getBlock();
                if (!isReplaceable(block)) {
                    continue;
                }

                if (!Slimefun.getProtectionManager().hasPermission(player, block, Interaction.PLACE_BLOCK)) {
                    continue;
                }

                BlockState replacedState = block.getState();
                ItemStack eventItem = template.clone();
                eventItem.setAmount(1);

                // A real BlockPlaceEvent is fired after Minecraft has set the physical block.
                // Rebar's PlayerPlace context intentionally uses shouldSetType=false for that reason.
                block.setType(eventItem.getType(), false);

                BlockPlaceEvent blockPlaceEvent = new BlockPlaceEvent(
                        block,
                        replacedState,
                        block.getRelative(lookingFacing.getOppositeFace()),
                        eventItem,
                        player,
                        true,
                        EquipmentSlot.HAND
                );
                Bukkit.getPluginManager().callEvent(blockPlaceEvent);

                boolean registered = BlockCompatibility.isPlacedFromItem(block, eventItem);
                if (blockPlaceEvent.isCancelled() || !registered) {
                    if (registered) {
                        BlockCompatibility.rollbackCustomPlacement(block, eventItem);
                    }
                    replacedState.update(true, false);
                    continue;
                }

                placed++;
            }

            if (player.getGameMode() != GameMode.CREATIVE && placed > 0) {
                removeExactItems(player, template, placed);
            }
        }, 1);
    }

    private int countAvailableItems(@NotNull Player player, @NotNull ItemStack target, boolean exactMatch) {
        int count = 0;
        for (ItemStack itemStack : player.getInventory().getContents()) {
            if (itemStack == null || itemStack.getType().isAir()) {
                continue;
            }

            boolean matches = exactMatch
                    ? itemStack.isSimilar(target)
                    : SlimefunUtils.isItemSimilar(itemStack, target, true, false);
            if (matches) {
                count += itemStack.getAmount();
                if (count >= limitBlocks) {
                    return count;
                }
            }
        }
        return count;
    }

    private static void removeExactItems(@NotNull Player player, @NotNull ItemStack target, int amount) {
        int remaining = amount;
        int storageSize = player.getInventory().getStorageContents().length;

        for (int slot = 0; slot < storageSize && remaining > 0; slot++) {
            ItemStack current = player.getInventory().getItem(slot);
            if (current == null || !current.isSimilar(target)) {
                continue;
            }

            int removed = Math.min(remaining, current.getAmount());
            int newAmount = current.getAmount() - removed;
            if (newAmount <= 0) {
                player.getInventory().setItem(slot, null);
            } else {
                current.setAmount(newAmount);
                player.getInventory().setItem(slot, current);
            }
            remaining -= removed;
        }
    }

    private static boolean isReplaceable(@NotNull Block block) {
        Material type = block.getType();
        return type == Material.AIR || type == Material.WATER || type == Material.LAVA;
    }

    public static boolean copyStateAble(@NotNull Material material) {
        return MaterialTags.FENCE_GATES.isTagged(material)
                || material.name().endsWith("_SLAB")
                || material.name().endsWith("_STAIRS")
                || material.name().endsWith("_TRAPDOOR")
                || material.name().endsWith("_HEAD")
                || material.name().endsWith("_LOG")
                || material == Material.END_ROD
                || material.name().endsWith("LIGHTNING_ROD")
                || material.name().endsWith("CHAIN")
                || material.name().endsWith("_BARS")
                || material == Material.DAYLIGHT_DETECTOR
                || material == Material.ENDER_CHEST
                || material == Material.NOTE_BLOCK
                || material == Material.REDSTONE_ORE
                || material == Material.DEEPSLATE_REDSTONE_ORE
                || material.name().endsWith("_WALL");
    }

    public static boolean isDisabledMaterial(@NotNull Material material) {
        if (MaterialTags.SHULKER_BOXES.isTagged(material)
                || (material.name().endsWith("CHEST") && material != Material.ENDER_CHEST)
                || material == Material.BARREL
                || material == Material.LECTERN
                || material == Material.DISPENSER
                || material == Material.DROPPER
                || material == Material.HOPPER
                || material == materialValueOf("VAULT")
                || material.name().endsWith("_SHELF")
                || material == Material.SUSPICIOUS_SAND
                || material == Material.SUSPICIOUS_GRAVEL

                || MaterialTags.BEDS.isTagged(material)
                || MaterialTags.DOORS.isTagged(material)
                || material == Material.TALL_GRASS
                || material == Material.LARGE_FERN
                || material == Material.TALL_SEAGRASS
                || material == Material.SUNFLOWER
                || material == Material.LILAC
                || material == Material.ROSE_BUSH
                || material == Material.PEONY
                || material == Material.PITCHER_PLANT
                || material == Material.PISTON_HEAD
                || material == Material.PISTON
                || material == Material.STICKY_PISTON

                || material.name().endsWith("CANDLE")
                || material == Material.SEA_PICKLE
                || material == Material.TURTLE_EGG
                || material == Material.FROGSPAWN

                || material.isAir()
                || !material.isBlock()

                || material == Material.END_PORTAL_FRAME
                || material == Material.BEDROCK
                || material == Material.COMMAND_BLOCK
                || material == Material.CHAIN_COMMAND_BLOCK
                || material == Material.REPEATING_COMMAND_BLOCK
                || material == Material.STRUCTURE_VOID
                || material == Material.STRUCTURE_BLOCK
                || material == Material.JIGSAW
                || material == Material.BARRIER
                || material == Material.LIGHT
                || material == materialValueOf("TEST_BLOCK")
                || material == materialValueOf("TEST_INSTANCE_BLOCK")
                || material == Material.SPAWNER
                || material == materialValueOf("TRIAL_SPAWNER")
                || material == Material.CHORUS_FLOWER
                || material == Material.NETHER_WART
                || material == Material.CAVE_VINES
                || material == Material.CAVE_VINES_PLANT
                || material == Material.FROSTED_ICE
                || material == Material.WATER_CAULDRON
                || material == Material.LAVA_CAULDRON
                || material == Material.POWDER_SNOW_CAULDRON
                || material.name().startsWith("POTTED_")
                || material == Material.FIRE
                || material == Material.SOUL_FIRE
                || material == Material.END_PORTAL
                || material == Material.END_GATEWAY
                || material == Material.NETHER_PORTAL
                || material == Material.BUBBLE_COLUMN
                || material == Material.POWDER_SNOW
                || material == Material.MUSHROOM_STEM

                || material == Material.CRAFTING_TABLE
                || material == Material.STONECUTTER
                || material == Material.CARTOGRAPHY_TABLE
                || material == Material.SMITHING_TABLE
                || material == Material.GRINDSTONE
                || material == Material.LOOM
                || material == Material.FURNACE
                || material == Material.SMOKER
                || material == Material.BLAST_FURNACE
                || material == Material.CAMPFIRE
                || material == Material.SOUL_CAMPFIRE
                || material == Material.ANVIL
                || material == Material.CHIPPED_ANVIL
                || material == Material.DAMAGED_ANVIL
                || material == Material.COMPOSTER
                || material == Material.JUKEBOX
                || material == Material.ENCHANTING_TABLE
                || material == Material.BREWING_STAND
                || material == Material.CAULDRON
                || material == Material.BEACON
                || material == Material.BEE_NEST
                || material == Material.BEEHIVE
                || material == Material.FLOWER_POT
                || material == Material.DECORATED_POT
                || material == Material.CHISELED_BOOKSHELF
                || MaterialTags.SIGNS.isTagged(material)
                || material == materialValueOf("CRAFTER")

                || material == Material.PLAYER_HEAD
                || material == Material.PLAYER_WALL_HEAD
                || material.name().endsWith("CAKE")
                || material.name().endsWith("_BUTTON")
                || material == Material.TRIPWIRE
                || material == materialValueOf("CREAKING_HEART")

                || material == Material.POINTED_DRIPSTONE
                || material.name().endsWith("_BANNER")
                || material == Material.LEVER
                || material.name().endsWith("TORCH")
                || material.name().endsWith("LANTERN")
                || material == Material.LADDER
                || material == Material.REPEATER
                || material == Material.COMPARATOR
                || material == Material.VINE
                || material == Material.GLOW_LICHEN
                || material == Material.SCULK_VEIN
                || material == Material.BELL
                || material == Material.TRIPWIRE_HOOK
                || material.name().endsWith("_RAIL")
                || material.name().endsWith("_CORAL")
                || material.name().endsWith("_CORAL_FAN")
                || material.name().endsWith("_CARPET")
                || material == Material.HANGING_ROOTS
                || material == Material.REDSTONE_WIRE
                || material == Material.BIG_DRIPLEAF_STEM
                || material == Material.CHORUS_PLANT
                || material == Material.DRAGON_EGG
                || material == Material.SNOW
                || material.name().endsWith("_PRESSURE_PLATE")
                || material == Material.SMALL_AMETHYST_BUD
                || material == Material.MEDIUM_AMETHYST_BUD
                || material == Material.LARGE_AMETHYST_BUD
                || material == Material.AMETHYST_CLUSTER
                || material.name().endsWith("_SAPLING")
                || material == Material.AZALEA
                || material == Material.FLOWERING_AZALEA
                || material == Material.BROWN_MUSHROOM
                || material == Material.RED_MUSHROOM
                || material == Material.CRIMSON_FUNGUS
                || material == Material.WARPED_FUNGUS
                || material == materialValueOf("GRASS")
                || material == materialValueOf("SHORT_GRASS")
                || material == materialValueOf("FIREFLY_BUSH")
                || material == Material.FERN
                || material == Material.DEAD_BUSH
                || material == Material.DANDELION
                || material == Material.POPPY
                || material == Material.BLUE_ORCHID
                || material == Material.ALLIUM
                || material == Material.AZURE_BLUET
                || material == Material.RED_TULIP
                || material == Material.ORANGE_TULIP
                || material == Material.WHITE_TULIP
                || material == Material.PINK_TULIP
                || material == Material.OXEYE_DAISY
                || material == Material.CORNFLOWER
                || material == Material.LILY_OF_THE_VALLEY
                || material == Material.TORCHFLOWER
                || material == Material.WITHER_ROSE
                || material == Material.PINK_PETALS
                || material == Material.SPORE_BLOSSOM
                || material == Material.BAMBOO
                || material == Material.SUGAR_CANE
                || material == Material.CACTUS
                || material == Material.CRIMSON_ROOTS
                || material == Material.WARPED_ROOTS
                || material == Material.NETHER_SPROUTS
                || material == Material.WEEPING_VINES
                || material == Material.TWISTING_VINES
                || material == Material.WEEPING_VINES_PLANT
                || material == Material.TWISTING_VINES_PLANT
                || material == Material.COCOA
                || material == Material.SWEET_BERRY_BUSH
                || material == Material.TORCHFLOWER_CROP
                || material == Material.WHEAT
                || material == Material.MELON_STEM
                || material == Material.PUMPKIN_STEM
                || material == Material.POTATOES
                || material == Material.CARROTS
                || material == Material.BEETROOTS
                || material == Material.KELP
                || material == Material.KELP_PLANT
                || material == Material.SEAGRASS
                || material == Material.LILY_PAD
                || material == materialValueOf("OPEN_EYEBLOSSOM")
                || material == materialValueOf("CLOSED_EYEBLOSSOM")
                || material == materialValueOf("PALE_HANGING_MOSS")
                || material == Material.MANGROVE_PROPAGULE
                || material == materialValueOf("WILDFLOWERS")
                || material == materialValueOf("LEAF_LITTER")
                || material.name().endsWith("_WALL_FAN")
                || material == materialValueOf("RESIN_CLUMP")) {
            return true;
        }

        return false;
    }

    @NotNull
    private static Material materialValueOf(@NotNull String name) {
        try {
            return Material.valueOf(name);
        } catch (IllegalArgumentException | NullPointerException e) {
            return Material.AIR;
        }
    }
}
