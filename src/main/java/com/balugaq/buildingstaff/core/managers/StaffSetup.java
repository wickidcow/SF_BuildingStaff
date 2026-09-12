package com.balugaq.buildingstaff.core.managers;

import com.balugaq.buildingstaff.api.interfaces.IManager;
import com.balugaq.buildingstaff.implementation.BuildingStaffPlugin;
import com.balugaq.buildingstaff.implementation.items.BlockStrictBuildingStaff4096;
import com.balugaq.buildingstaff.implementation.items.BlockStrictBuildingStaff64;
import com.balugaq.buildingstaff.implementation.items.BlockStrictBuildingStaff9;
import com.balugaq.buildingstaff.implementation.items.BreakingStaff4096;
import com.balugaq.buildingstaff.implementation.items.BreakingStaff64;
import com.balugaq.buildingstaff.implementation.items.BreakingStaff9;
import com.balugaq.buildingstaff.implementation.items.BuildingStaff4096;
import com.balugaq.buildingstaff.implementation.items.BuildingStaff64;
import com.balugaq.buildingstaff.implementation.items.BuildingStaff9;
import com.balugaq.buildingstaff.implementation.items.DisplayClearer;
import com.balugaq.buildingstaff.utils.KeyUtil;
import com.balugaq.buildingstaff.utils.SlimefunItemUtil;
import com.balugaq.buildingstaff.utils.compatibility.Converter;
import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class StaffSetup implements IManager {
    public static ItemGroup mainGroup;
    public static BuildingStaff9 buildingStaff9;
    public static BuildingStaff64 buildingStaff64;
    public static BuildingStaff4096 buildingStaff4096;
    public static BlockStrictBuildingStaff9 blockStrictBuildingStaff9;
    public static BlockStrictBuildingStaff64 blockStrictBuildingStaff64;
    public static BlockStrictBuildingStaff4096 blockStrictBuildingStaff4096;
    public static BreakingStaff9 breakingStaff9;
    public static BreakingStaff64 breakingStaff64;
    public static BreakingStaff4096 breakingStaff4096;
    public static DisplayClearer displayClearer;
    private final BuildingStaffPlugin plugin;

    public StaffSetup(BuildingStaffPlugin plugin) {
        this.plugin = plugin;
    }

    public static void unregisterAll(@NotNull SlimefunAddon instance) {
        SlimefunItemUtil.unregisterItem(breakingStaff4096);
        SlimefunItemUtil.unregisterItem(breakingStaff64);
        SlimefunItemUtil.unregisterItem(breakingStaff9);
        SlimefunItemUtil.unregisterItem(blockStrictBuildingStaff4096);
        SlimefunItemUtil.unregisterItem(blockStrictBuildingStaff64);
        SlimefunItemUtil.unregisterItem(blockStrictBuildingStaff9);
        SlimefunItemUtil.unregisterItem(buildingStaff4096);
        SlimefunItemUtil.unregisterItem(buildingStaff64);
        SlimefunItemUtil.unregisterItem(buildingStaff9);
        SlimefunItemUtil.unregisterItem(displayClearer);
        SlimefunItemUtil.unregisterItemGroup(mainGroup);
        SlimefunItemUtil.unregisterItems(instance);
        SlimefunItemUtil.unregisterItemGroups(instance);
    }

    @Override
    public void setup() {
        ItemStack stick = new ItemStack(Material.STICK);
        ItemStack cc = Converter.getItem(SlimefunItems.COMPRESSED_CARBON);
        ItemStack staffElemental = Converter.getItem(SlimefunItems.STAFF_ELEMENTAL);
        ItemStack damascusSteel = Converter.getItem(SlimefunItems.DAMASCUS_STEEL_INGOT);
        ItemStack brass = Converter.getItem(SlimefunItems.BRASS_INGOT);
        ItemStack bronze = Converter.getItem(SlimefunItems.BRONZE_INGOT);
        ItemStack glass = new ItemStack(Material.GLASS);
        ItemStack diamond = new ItemStack(Material.DIAMOND);
        ItemStack goldIngot = new ItemStack(Material.GOLD_INGOT);

        mainGroup = new ItemGroup(KeyUtil.newKey("building_staff"), Converter.getItem(
                Material.BLAZE_ROD,
                "&aBuilding Staff"
        ));

        mainGroup.register(plugin);

        buildingStaff9 = new BuildingStaff9(
                mainGroup,
                new SlimefunItemStack(
                        "BUILDING_STAFF_9",
                        new ItemStack(Material.IRON_SWORD),
                        "&aBuilding Staff | &99 Blocks",
                        "&7Right-click to place blocks",
                        "&aMaximum range: 9 blocks",
                        "&aSelected plane may contain any block type"
                ),
                RecipeType.ENHANCED_CRAFTING_TABLE,
                new ItemStack[]{
                        cc, cc, damascusSteel,
                        cc, stick, cc,
                        stick, cc, cc
                }
        );

        buildingStaff9.register(plugin);

        buildingStaff64 = new BuildingStaff64(
                mainGroup,
                new SlimefunItemStack(
                        "BUILDING_STAFF_64",
                        new ItemStack(Material.GOLDEN_SWORD),
                        "&aBuilding Staff | &664 Blocks",
                        "&7Right-click to place blocks",
                        "&eMaximum range: 64 blocks",
                        "&aSelected plane may contain any block type"
                ),
                RecipeType.ANCIENT_ALTAR,
                new ItemStack[]{
                        buildingStaff9.getItem(), buildingStaff9.getItem(), buildingStaff9.getItem(),
                        buildingStaff9.getItem(), staffElemental, buildingStaff9.getItem(),
                        buildingStaff9.getItem(), buildingStaff9.getItem(), buildingStaff9.getItem()
                }
        );

        buildingStaff64.register(plugin);

        buildingStaff4096 = new BuildingStaff4096(
                mainGroup,
                new SlimefunItemStack(
                        "BUILDING_STAFF_4096",
                        new ItemStack(Material.DIAMOND_SWORD),
                        "&aBuilding Staff | &e4096 Blocks",
                        "&7Right-click to place blocks",
                        "&cMaximum range: 4096 blocks",
                        "&aSelected plane may contain any block type",
                        "&cAdmin use only"
                ),
                RecipeType.NULL,
                new ItemStack[]{}
        );

        buildingStaff4096.register(plugin);

        blockStrictBuildingStaff9 = new BlockStrictBuildingStaff9(
                mainGroup,
                new SlimefunItemStack(
                        "BLOCK_STRICT_BUILDING_STAFF_9",
                        new ItemStack(Material.IRON_SWORD),
                        "&aStrict Building Staff | &99 Blocks",
                        "&7Right-click to place blocks",
                        "&aMaximum range: 9 blocks",
                        "&cSelected plane must contain only one block type"
                ),
                RecipeType.ENHANCED_CRAFTING_TABLE,
                new ItemStack[]{
                        cc, cc, brass,
                        cc, stick, cc,
                        stick, cc, cc
                }
        );

        blockStrictBuildingStaff9.register(plugin);

        blockStrictBuildingStaff64 = new BlockStrictBuildingStaff64(
                mainGroup,
                new SlimefunItemStack(
                        "BLOCK_STRICT_BUILDING_STAFF_64",
                        new ItemStack(Material.GOLDEN_SWORD),
                        "&aStrict Building Staff | &664 Blocks",
                        "&7Right-click to place blocks",
                        "&eMaximum range: 64 blocks",
                        "&cSelected plane must contain only one block type"
                ),
                RecipeType.ENHANCED_CRAFTING_TABLE,
                new ItemStack[]{
                        blockStrictBuildingStaff9.getItem(), blockStrictBuildingStaff9.getItem(), blockStrictBuildingStaff9.getItem(),
                        blockStrictBuildingStaff9.getItem(), staffElemental, blockStrictBuildingStaff9.getItem(),
                        blockStrictBuildingStaff9.getItem(), blockStrictBuildingStaff9.getItem(), blockStrictBuildingStaff9.getItem()
                }
        );

        blockStrictBuildingStaff64.register(plugin);

        blockStrictBuildingStaff4096 = new BlockStrictBuildingStaff4096(
                mainGroup,
                new SlimefunItemStack(
                        "BLOCK_STRICT_BUILDING_STAFF_4096",
                        new ItemStack(Material.DIAMOND_SWORD),
                        "&aStrict Building Staff | &e4096 Blocks",
                        "&7Right-click to place blocks",
                        "&cMaximum range: 4096 blocks",
                        "&cSelected plane must contain only one block type",
                        "&cAdmin use only"
                ),
                RecipeType.NULL,
                new ItemStack[]{}
        );

        blockStrictBuildingStaff4096.register(plugin);

        breakingStaff9 = new BreakingStaff9(
                mainGroup,
                new SlimefunItemStack(
                        "BREAKING_STAFF_9",
                        new ItemStack(Material.IRON_SWORD),
                        "&cBreaking Staff | &99 Blocks",
                        "&7Right-click to break blocks",
                        "&aMaximum range: 9 blocks",
                        "&cSelected plane must contain only one block type"
                ),
                RecipeType.ENHANCED_CRAFTING_TABLE,
                new ItemStack[]{
                        cc, cc, bronze,
                        cc, stick, cc,
                        stick, cc, cc
                }
        );

        breakingStaff9.register(plugin);

        breakingStaff64 = new BreakingStaff64(
                mainGroup,
                new SlimefunItemStack(
                        "BREAKING_STAFF_64",
                        new ItemStack(Material.GOLDEN_SWORD),
                        "&cBreaking Staff | &664 Blocks",
                        "&7Right-click to break blocks",
                        "&eMaximum range: 64 blocks",
                        "&cSelected plane must contain only one block type"
                ),
                RecipeType.ENHANCED_CRAFTING_TABLE,
                new ItemStack[]{
                        breakingStaff9.getItem(), breakingStaff9.getItem(), breakingStaff9.getItem(),
                        breakingStaff9.getItem(), staffElemental, breakingStaff9.getItem(),
                        breakingStaff9.getItem(), breakingStaff9.getItem(), breakingStaff9.getItem()
                }
        );

        breakingStaff64.register(plugin);

        breakingStaff4096 = new BreakingStaff4096(
                mainGroup,
                new SlimefunItemStack(
                        "BREAKING_STAFF_4096",
                        new ItemStack(Material.DIAMOND_SWORD),
                        "&cBreaking Staff | &e4096 Blocks",
                        "&7Right-click to break blocks",
                        "&cMaximum range: 4096 blocks",
                        "&cSelected plane must contain only one block type",
                        "&cAdmin use only"
                ),
                RecipeType.NULL,
                new ItemStack[]{}
        );

        breakingStaff4096.register(plugin);

        displayClearer = new DisplayClearer(
                mainGroup,
                new SlimefunItemStack(
                        "BUILDING_STAFF_DISPLAY_CLEARER",
                        Material.CLAY_BALL,
                        "&eDisplay Clearer",
                        "&eClears holographic entities left by Building Staff"
                ),
                RecipeType.ENHANCED_CRAFTING_TABLE,
                new ItemStack[] {
                        glass, glass, goldIngot,
                        diamond, diamond, glass,
                        glass, glass, goldIngot
                }
        );

        displayClearer.register(plugin);
    }

    @Override
    public void shutdown() {
        unregisterAll(plugin);
    }
}
