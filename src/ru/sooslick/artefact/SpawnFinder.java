package ru.sooslick.artefact;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import ru.sooslick.artefact.outlaw.util.CommonUtil;
import ru.sooslick.artefact.outlaw.util.LoggerUtil;
import ru.sooslick.artefact.outlaw.util.WorldUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

public class SpawnFinder {
    public static final double PI2 = Math.PI * 2;

    private final Map<Location, Double> safeSpawns = new HashMap<>();   // double is angle
    private final double randomOffset = CommonUtil.RANDOM.nextDouble() * PI2;
    private final double spawnDiameter = Cfg.spawnAreaRadius * 2;

    private boolean running = true;
    private double angle = 0;
    private double step = Math.PI / Cfg.maxPlayers;
    private int fullCirclesChecked = 0;

    public SpawnFinder() {
        if (Cfg.customPlayerSpawnsEnabled)
            Cfg.customPlayerSpawns.forEach(l -> safeSpawns.put(l, 0D));
        else
            Bukkit.getScheduler().scheduleSyncDelayedTask(ArtefactPlugin.getInstance(), this::findSpawnTick, 4);
        LoggerUtil.debug("Created new SpawnFinder");
    }

    private void findSpawnTick() {
        if (!running)
            return;

        double testAngle = this.angle + this.randomOffset;
        Location testLocation = WorldUtil.getDistanceLocation(Cfg.artefactLocation, Cfg.spawnDistance, testAngle);

        // step 1: check test location safety
        if (WorldUtil.isSafeLocation(testLocation)) {
            // test if we already found spawnpoint in really close proximity to test location
            if (safeSpawns.keySet().stream()
                    .map(loc -> WorldUtil.distance2d(testLocation, loc))
                    .noneMatch(d -> d < spawnDiameter)) {
                safeSpawns.put(testLocation, (testAngle % PI2));
                LoggerUtil.debug("Found safe location at " + WorldUtil.formatLocation(testLocation));
            }
        }

        angle += step;
        if (angle >= PI2) {
            fullCirclesChecked++;
            angle = step / Math.pow(2, fullCirclesChecked);
            if (fullCirclesChecked > 1)
                step /= 2;
        }

        if (fullCirclesChecked > 1 && safeSpawns.size() >= Cfg.maxPlayers) {
            LoggerUtil.debug("found enough safe spawns to start the game");
            running = false;
            return;
        }

        Bukkit.getScheduler().scheduleSyncDelayedTask(ArtefactPlugin.getInstance(), this::findSpawnTick, 4);
    }

    public Location enforeSafeSpawn() {
        LoggerUtil.debug("Picking random location and making it safe for player to spawn");
        double randomAngle = CommonUtil.RANDOM.nextDouble() * PI2;
        // to prevent two spawn areas from overlapping we make random spawn a little further from the center
        // it's might be quite unfair for player which will be assigned to this slot
        // but we must ensure that we found a proper spawn for everyone no matter what
        Location test = WorldUtil.getDistanceLocation(Cfg.artefactLocation, Cfg.spawnDistance + spawnDiameter, randomAngle);
        if (!WorldUtil.isSafeLocation(test))
            WorldUtil.safetizeLocation(test);
        safeSpawns.put(test, randomAngle);
        return test;
    }

    private void ensureWeHaveEnoughSafeSpawns() {
        LoggerUtil.debug("Couldn't find enough safe spawns before game start, so let's create some safe locations using creative mode");
        int pls = Bukkit.getOnlinePlayers().size();
        if (safeSpawns.size() < pls) {
            if (Cfg.customPlayerSpawnsEnabled) {
                // todo we should not start the game if we have more player than manually assigned spawns
                return;
            }
            for (int i = safeSpawns.size(); i < pls; i++)
                enforeSafeSpawn();
        }
    }

    public void stop() {
        running = false;
    }

    public HashMap<String, Location> bindSpawns() {
        LoggerUtil.debug("bindSpawns: Assigning player spawns to found safe locations");
        running = false;
        int pls = Bukkit.getOnlinePlayers().size();
        if (pls == 0)
            return new HashMap<>();
        ensureWeHaveEnoughSafeSpawns();

        LoggerUtil.debug("bindSpawns: sorting found spawns by angle");
        ArrayList<Map.Entry<Location, Double>> sortedSpawns = new ArrayList<>(safeSpawns.entrySet());
        sortedSpawns.sort(Map.Entry.comparingByValue());

        LoggerUtil.debug("bindSpawns: calculate distances between existing spawns");
        int spawns = pls + 1;
        int[] spawnpoints = new int[spawns];
        spawnpoints[0] = 0;
        Location firstSpawn = sortedSpawns.get(0).getKey();
        Location previous = firstSpawn;
        for (int i = 1; i < pls; i++) {
            Location current = sortedSpawns.get(i).getKey();
            spawnpoints[i] = (int) Math.ceil(WorldUtil.distance2d(current, previous));
            previous = current;
        }
        spawnpoints[pls] = (int) Math.ceil(WorldUtil.distance2d(firstSpawn, previous));

        LoggerUtil.debug("bindSpawns: calculate distance jumps");
        TreeSet<Integer> possibleJumps = new TreeSet<>();
        for (int i = 0; i < spawns - 1; i++)
            for (int j = i + 1; j < spawns; j++)
                possibleJumps.add(spawnpoints[j] - spawnpoints[i]);

        LoggerUtil.debug("bindSpawns: find optimal distance jump for fair and even player placement");
        int optimalJump = 0;
        for (int step : possibleJumps) {
            int plsToPlace = pls;
            int currentPoint = spawnpoints[0];
            int currentIndex = 0;
            while (plsToPlace > 0) {
                currentPoint+= step;
                boolean overflow = false;
                while (spawnpoints[currentIndex] < currentPoint) {
                    if (++currentIndex >= spawns) {
                        overflow = true;
                        break;
                    }
                }
                if (overflow)
                    break;
                currentPoint = spawnpoints[currentIndex];
                plsToPlace--;
            }

            if (plsToPlace <= 0)
                optimalJump = step;
            else
                break;
        }

        LoggerUtil.debug("bindSpawns: assigning players to spawns");
        HashMap<String, Location> spawnBinds = new HashMap<>();
        int index = 0;
        int currentPoint = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            Location currentSpawn = sortedSpawns.get(index).getKey();
            spawnBinds.put(p.getName(), currentSpawn);

            currentPoint+= optimalJump;
            while (spawnpoints[index] < currentPoint) {
                if (++index >= spawns) {
                    // todo ensure extra safety, this "if" must never hit true
                    break;
                }
            }
            currentPoint = spawnpoints[index];
        }

        LoggerUtil.debug("bindSpawns: finish job");
        return spawnBinds;
    }
}
