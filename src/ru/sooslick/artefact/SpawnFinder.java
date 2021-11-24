package ru.sooslick.artefact;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import ru.sooslick.artefact.outlaw.util.CommonUtil;
import ru.sooslick.artefact.outlaw.util.LoggerUtil;
import ru.sooslick.artefact.outlaw.util.WorldUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SpawnFinder {
    public static final String CANNOT_CALCULATE = "Cannot calculate spawns normally. Getting some random positions...";
    public static final String HIT = "Found safe location at %s";
    public static final double PI2 = Math.PI * 2;

    private static boolean running = false;
    private static Map<Location, Double> safeSpawns;
    private static double angle;
    private static double step;
    private static boolean randomHit;
    private static Map<String, Location> spawnBinds;

    public static void launchJob() {
        safeSpawns = new HashMap<>();
        spawnBinds = new HashMap<>();
        angle = 0;
        step = Math.PI / Cfg.maxPlayers;
        randomHit = false;
        running = true;
        if (Cfg.customPlayerSpawnsEnabled)
            Cfg.customPlayerSpawns.forEach(l -> safeSpawns.put(l, 0D));
        else
            Bukkit.getScheduler().scheduleSyncDelayedTask(ArtefactPlugin.getInstance(), SpawnFinder::findSpawnTick, 4);
        LoggerUtil.debug("launchJob");
    }

    public static void bindSpawns() {
        LoggerUtil.debug("called bindSpawns");
        running = false;
        int pls = Bukkit.getOnlinePlayers().size();
        if (pls == 0)
            return;
        if (Cfg.customPlayerSpawnsEnabled || safeSpawns.size() < pls) {
            findSpawnsForce();
            return;
        }
        // sort by angle
        LoggerUtil.debug("bindSpawns - sorting found spawns");
        List<Map.Entry<Location, Double>> sortedSpawns = new ArrayList<>(safeSpawns.entrySet());
        sortedSpawns.sort(Map.Entry.comparingByValue());
        // calc distances
        LoggerUtil.debug("bindSpawns - calc distances between existing spawns");
        LinkedHashMap<Location, Double> spawnDistances = new LinkedHashMap<>();
        double current = 0;
        spawnDistances.put(sortedSpawns.get(0).getKey(), current);
        for (int i = 1; i < sortedSpawns.size(); i++) {
            current += WorldUtil.distance2d(sortedSpawns.get(i).getKey(), sortedSpawns.get(i - 1).getKey());
            spawnDistances.put(sortedSpawns.get(i).getKey(), current);
        }
        current += WorldUtil.distance2d(sortedSpawns.get(0).getKey(), sortedSpawns.get(sortedSpawns.size() - 1).getKey());
        spawnDistances.put(Cfg.artefactLocation, current);
        List<Map.Entry<Location, Double>> sortedDistances = new ArrayList<>(spawnDistances.entrySet());

        // search best distance between player spawns
        double targetDistance = current / pls;
        boolean next = pls > 1;
        LoggerUtil.debug("bindSpawns - process distances, targetDistance = " + targetDistance);
        while (next) {
            int plsLeft = pls - 1;
            double lastSavedCoord = sortedDistances.get(0).getValue();
            int index = 0;
            while (plsLeft > 0) {
                index++;
                if (index >= sortedDistances.size() - 1) {
                    break;
                }
                double cd = sortedDistances.get(index).getValue();
                if (cd - lastSavedCoord >= targetDistance) {
                    lastSavedCoord = cd;
                    plsLeft--;
                    if (plsLeft <= 0) {
                        double maxd = sortedDistances.get(sortedDistances.size() - 1).getValue();
                        if (maxd - lastSavedCoord >= targetDistance)
                            next = false;
                    }
                }
            }
            targetDistance /= 2;
            if (targetDistance < 5) {
                LoggerUtil.warn(CANNOT_CALCULATE);
                safeSpawns.clear();
                findSpawnsForce();
                return;
            }
            LoggerUtil.debug("bindSpawns - process distances, targetDistance = " + targetDistance);
        }

        // bind result
        double cd = -targetDistance;
        int index = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            while (sortedDistances.get(index).getValue() - cd <= targetDistance)
                index++;
            cd = sortedDistances.get(index).getValue();
            Location spawn = sortedDistances.get(index).getKey();
            spawnBinds.put(p.getName(), spawn);
            LoggerUtil.debug("bind spawn to player " + p.getName() + " -> " + spawn);
            safeSpawns.remove(spawn);
            if (!WorldUtil.isSafeLocation(spawn))
                WorldUtil.safetizeLocation(spawn);
        }
    }

    public static boolean isProtected(Block b) {
        return spawnBinds.values().stream()
                .anyMatch(l -> (Math.abs(b.getX() - l.getBlockX()) <= 2 &&
                        Math.abs(b.getY() - l.getBlockY()) <= 2 &&
                        Math.abs(b.getZ() - l.getBlockZ()) <= 2));
    }

    public static boolean isProtected(Block b, Player p) {
        for (Map.Entry<String, Location> e : spawnBinds.entrySet()) {
            Location l = e.getValue();
            if (!p.getName().equals(e.getKey()) &&
                    Math.abs(b.getX() - l.getBlockX()) <= 2 &&
                    Math.abs(b.getY() - l.getBlockY()) <= 2 &&
                    Math.abs(b.getZ() - l.getBlockZ()) <= 2)
                return true;
        }
        return false;
    }

    public static Location getSpawnLocation(Player p) {
        Location spawn = spawnBinds.get(p.getName());
        if (spawn == null)
            allocateSpawn(p);
        return spawnBinds.get(p.getName());
    }

    public static String getPlayerBySpawn(Block b) {
        for (Map.Entry<String, Location> e : spawnBinds.entrySet()) {
            Location l = e.getValue();
            if (Math.abs(b.getX() - l.getBlockX()) <= 2 &&
                    Math.abs(b.getY() - l.getBlockY()) <= 2 &&
                    Math.abs(b.getZ() - l.getBlockZ()) <= 2)
                return e.getKey();
        }
        return null;
    }

    public static void allocateSpawn(Player p) {
        LoggerUtil.debug("called allocateSpawn");
        if (safeSpawns.size() > 0) {
            Location l = safeSpawns.entrySet().stream().findAny().map(Map.Entry::getKey).orElse(WorldUtil.getRandomDistanceLocation(Cfg.artefactLocation, Cfg.spawnDistance));
            safeSpawns.remove(l);
            if (!WorldUtil.isSafeLocation(l))
                WorldUtil.safetizeLocation(l);
            spawnBinds.put(p.getName(), l);
            LoggerUtil.debug("allocated spawn from safeSpawns");
            return;
        }
        double startAngle = CommonUtil.RANDOM.nextDouble() * PI2;
        while (true) {
            Location test = WorldUtil.getDistanceLocation(Cfg.artefactLocation, Cfg.spawnDistance, startAngle);
            if (spawnBinds.values().stream()
                    .map(loc -> WorldUtil.distance2d(test, loc))
                    .noneMatch(d -> d < 6)) {
                spawnBinds.put(p.getName(), test);
                LoggerUtil.debug("allocated random spawn");
                if (!WorldUtil.isSafeLocation(test))
                    WorldUtil.safetizeLocation(test);
                return;
            }
            startAngle += 0.05D;
        }
    }

    public static void highlightSpawns() {
        List<Location> hls = new LinkedList<>();
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        for (Location spawn : spawnBinds.values()) {
            if (players.stream().anyMatch(p -> WorldUtil.distance2d(p.getLocation(), spawn) < 16))
                hls.add(spawn);
        }
        World w = Bukkit.getWorlds().get(0);
        for (Location l : hls) {
            // vert
            w.spawnParticle(Particle.WAX_ON, l.getX() - 2.5, l.getY(), l.getZ() - 2.5, 2, 0.1, 2.5, 0.1);
            w.spawnParticle(Particle.WAX_ON, l.getX() + 2.5, l.getY(), l.getZ() - 2.5, 2, 0.1, 2.5, 0.1);
            w.spawnParticle(Particle.WAX_ON, l.getX() - 2.5, l.getY(), l.getZ() + 2.5, 2, 0.1, 2.5, 0.1);
            w.spawnParticle(Particle.WAX_ON, l.getX() + 2.5, l.getY(), l.getZ() + 2.5, 2, 0.1, 2.5, 0.1);
            // top
            w.spawnParticle(Particle.WAX_ON, l.getX() + 2.5, l.getY() + 2.5, l.getZ(), 2, 0.1, 0.1, 2.5);
            w.spawnParticle(Particle.WAX_ON, l.getX() - 2.5, l.getY() + 2.5, l.getZ(), 2, 0.1, 0.1, 2.5);
            w.spawnParticle(Particle.WAX_ON, l.getX(), l.getY() + 2.5, l.getZ() + 2.5, 2, 2.5, 0.1, 0.1);
            w.spawnParticle(Particle.WAX_ON, l.getX(), l.getY() + 2.5, l.getZ() - 2.5, 2, 2.5, 0.1, 0.1);
            // bottom
            w.spawnParticle(Particle.WAX_ON, l.getX() + 2.5, l.getY() - 2.5, l.getZ(), 2, 0.1, 0.1, 2.5);
            w.spawnParticle(Particle.WAX_ON, l.getX() - 2.5, l.getY() - 2.5, l.getZ(), 2, 0.1, 0.1, 2.5);
            w.spawnParticle(Particle.WAX_ON, l.getX(), l.getY() - 2.5, l.getZ() + 2.5, 2, 2.5, 0.1, 0.1);
            w.spawnParticle(Particle.WAX_ON, l.getX(), l.getY() - 2.5, l.getZ() - 2.5, 2, 2.5, 0.1, 0.1);
        }
    }

    private static void findSpawnTick() {
        if (!running)
            return;

        Location hit = WorldUtil.getDistanceLocation(Cfg.artefactLocation, Cfg.spawnDistance, angle);
        if (WorldUtil.isSafeLocation(hit)) {
            if (safeSpawns.keySet().stream()
                    .map(loc -> WorldUtil.distance2d(hit, loc))
                    .noneMatch(d -> d < 6)) {
                safeSpawns.put(hit, (angle % PI2));
                LoggerUtil.debug(String.format(HIT, WorldUtil.formatLocation(hit)));
            }
        }

        if (randomHit) {
            if (safeSpawns.size() >= Cfg.maxPlayers) {
                running = false;
                LoggerUtil.debug("found enough safe spawns (random)");
                return;
            }
            angle = CommonUtil.RANDOM.nextDouble() * PI2;
        } else {
            angle += step;
            if (angle >= PI2 && angle - step < PI2)
                angle += step / 2;
            else if (angle >= PI2 * 2) {
                if (safeSpawns.size() >= Cfg.maxPlayers) {
                    LoggerUtil.debug("found enough safe spawns (normal)");
                    running = false;
                    return;
                }
                randomHit = true;
                angle = CommonUtil.RANDOM.nextDouble() * PI2;
            }
        }

        Bukkit.getScheduler().scheduleSyncDelayedTask(ArtefactPlugin.getInstance(), SpawnFinder::findSpawnTick, 4);
    }

    private static void findSpawnsForce() {
        LoggerUtil.debug("called findSpawnsForce");
        Bukkit.getOnlinePlayers().forEach(SpawnFinder::allocateSpawn);
    }

    private SpawnFinder() {
    }
}
