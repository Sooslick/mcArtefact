package ru.sooslick.artefact.outlaw.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class with common world and location methods
 */
public class WorldUtil {
    private static final double DISTANCE_MAX = 100500d;
    private static final String COMMA = ", ";
    private static final String PLACEHOLDER = "";
    private static final String SAFETIZE = "Created safe location at %s";
    private static final String SAFELOC_FAIL = "getSafeRandomLocation - fail, reason: %s | %s";
    private static final String SAFELOC_FAIL_LIQUID = "liquid";
    private static final String SAFELOC_FAIL_OBSTRUCTION = "obstruction";

    public static final List<Material> DANGERS;
    public static final List<Material> EXCLUDES;

    static {
        DANGERS = new ArrayList<>();
        DANGERS.add(Material.FIRE);
        DANGERS.add(Material.CACTUS);
        DANGERS.add(Material.VINE);
        DANGERS.add(Material.LADDER);
        DANGERS.add(Material.COBWEB);
        DANGERS.add(Material.AIR);
        DANGERS.add(Material.TRIPWIRE);
        DANGERS.add(Material.TRIPWIRE_HOOK);
        DANGERS.add(Material.SWEET_BERRY_BUSH);
        DANGERS.add(Material.MAGMA_BLOCK);
        DANGERS.add(Material.SEAGRASS);
        DANGERS.add(Material.TALL_SEAGRASS);
        DANGERS.add(Material.DRIPSTONE_BLOCK);
        DANGERS.add(Material.POWDER_SNOW);

        EXCLUDES = new ArrayList<>();
        EXCLUDES.add(Material.AIR);
        EXCLUDES.add(Material.GRASS);
        EXCLUDES.add(Material.TALL_GRASS);
        EXCLUDES.add(Material.DANDELION);
        EXCLUDES.add(Material.POPPY);
        EXCLUDES.add(Material.BLUE_ORCHID);
        EXCLUDES.add(Material.ALLIUM);
        EXCLUDES.add(Material.AZURE_BLUET);
        EXCLUDES.add(Material.RED_TULIP);
        EXCLUDES.add(Material.WHITE_TULIP);
        EXCLUDES.add(Material.ORANGE_TULIP);
        EXCLUDES.add(Material.PINK_TULIP);
        EXCLUDES.add(Material.OXEYE_DAISY);
        EXCLUDES.add(Material.CORNFLOWER);
        EXCLUDES.add(Material.LILY_OF_THE_VALLEY);
        EXCLUDES.add(Material.SUNFLOWER);
        EXCLUDES.add(Material.LILAC);
        EXCLUDES.add(Material.ROSE_BUSH);
        EXCLUDES.add(Material.PEONY);
        EXCLUDES.add(Material.DEAD_BUSH);
        EXCLUDES.add(Material.FERN);
        EXCLUDES.add(Material.LARGE_FERN);
        EXCLUDES.add(Material.SNOW);
    }

    /**
     * Get random main world location on the surface within specified radius (square)
     * @param bound search radius
     * @return random location
     */
    public static Location getRandomLocation(Location center, int bound) {
        int dbound = bound * 2;
        int x = CommonUtil.RANDOM.nextInt(dbound) - bound + center.getBlockX();
        int z = CommonUtil.RANDOM.nextInt(dbound) - bound + center.getBlockZ();
        return Bukkit.getWorlds().get(0).getHighestBlockAt(x, z).getLocation().add(0.5, 1, 0.5);
    }

    /**
     * Get random location from a specified distance
     * @param src source location
     * @param dist required distance from source location
     * @return random location
     */
    public static Location getRandomDistanceLocation(Location src, int dist) {
        return getDistanceLocation(src, dist, CommonUtil.RANDOM.nextDouble() * Math.PI * 2);
    }

    public static Location getDistanceLocation(Location src, int dist, double angle) {
        int x = (int) (src.getX() + (Math.cos(angle) * dist));
        int z = (int) (src.getZ() + (Math.sin(angle) * dist));
        return Bukkit.getWorlds().get(0).getHighestBlockAt(x, z).getLocation().add(0.5, 1, 0.5);
    }

    /**
     * Check spawn safety on the specified location
     * @param l location to check
     * @return true if the location is safe to spawn, otherwise false
     */
    public static boolean isSafeLocation(Location l) {
        l.getChunk().load();
        Block groundBlock = l.getBlock().getRelative(0, -1, 0);
        Material m = groundBlock.getType();
        if (groundBlock.isLiquid()) {
            LoggerUtil.debug(String.format(SAFELOC_FAIL, SAFELOC_FAIL_LIQUID, formatLocation(groundBlock.getLocation())));
            return false;
        }
        if (DANGERS.contains(m)) {
            LoggerUtil.debug(String.format(SAFELOC_FAIL, m.name(), formatLocation(groundBlock.getLocation())));
            return false;
        }
        for (int i = -1; i <= 1; i++)
            for (int j = -1; j <= 1; j++)
                for (int k = 1; k <= 2; k++)
                    if (!EXCLUDES.contains(groundBlock.getRelative(i, k, j).getType())) {
                        LoggerUtil.debug(String.format(SAFELOC_FAIL, SAFELOC_FAIL_OBSTRUCTION, formatLocation(groundBlock.getLocation())));
                        return false;
                    }
        return true;
    }

    /**
     * Return the distance between two locations
     * @param l1 first location
     * @param l2 second location
     * @return distance between locations, or hard-coded max value if worlds of this locations are different
     */
    public static double distance2d(Location l1, Location l2) {
        //return max distance if both worlds are specified and not equals
        if (l1.getWorld() != null && l2.getWorld() != null && l1.getWorld() != l2.getWorld())
            return DISTANCE_MAX;
        double x = l1.getX() - l2.getX();
        double z = l1.getZ() - l2.getZ();
        return Math.sqrt(x * x + z * z);
    }

    /**
     * Format location as string
     * @param l location to format
     * @return formatted string
     */
    public static String formatLocation(Location l) {
        String ws = l.getWorld() != null ? l.getWorld().getName() + COMMA : PLACEHOLDER;
        return ws +
                l.getBlockX() + COMMA +
                l.getBlockY() + COMMA +
                l.getBlockZ();
    }

    /**
     * Make selected location safe for spawn
     * @param l location to safe
     */
    public static void safetizeLocation(Location l) {
        World w = l.getWorld();
        int x = l.getBlockX();
        int y = l.getBlockY();
        int z = l.getBlockZ();
        //fill stone
        Filler f = new Filler().setWorld(w).setMaterial(Material.STONE)
                .setStartX(x - 2).setEndX(x + 2)
                .setStartY(y - 2).setEndY(y - 2)
                .setStartZ(z - 2).setEndZ(z + 2);
        f.fill();
        //fill ground log
        f.setMaterial(Material.OAK_LOG).setStartY(y - 1).setEndY(y - 1).fill();
        //clear spawn area
        f.setMaterial(Material.AIR).setStartY(y).setEndY(y + 2).fill();
        LoggerUtil.debug(String.format(SAFETIZE, formatLocation(l)));
    }

    public static int calcChunk(int coord) {
        return (coord >> 4);
    }

    private WorldUtil() {
    }
}