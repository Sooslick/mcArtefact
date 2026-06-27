package ru.sooslick.artefact;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import ru.sooslick.artefact.outlaw.util.WorldUtil;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SpawnManager {

    private static SpawnFinder spawnFinder;
    private static Map<String, Location> spawnBinds;

    public static void prepareSpawns() {
        if (spawnFinder != null)
            spawnFinder.stop();
        spawnFinder = new SpawnFinder();
    }

    public static void bindSpawns() {
        spawnBinds = spawnFinder.bindSpawns();
    }

    public static boolean isProtected(Block b) {
        return spawnBinds.values().stream()
                .anyMatch(l -> (Math.abs(b.getX() - l.getBlockX()) <= Cfg.spawnAreaRadius &&
                        Math.abs(b.getY() - l.getBlockY()) <= Cfg.spawnAreaRadius &&
                        Math.abs(b.getZ() - l.getBlockZ()) <= Cfg.spawnAreaRadius));
    }

    public static boolean isProtected(Block b, Player p) {
        for (Map.Entry<String, Location> e : spawnBinds.entrySet()) {
            Location l = e.getValue();
            if (!p.getName().equals(e.getKey()) &&
                    Math.abs(b.getX() - l.getBlockX()) <= Cfg.spawnAreaRadius &&
                    Math.abs(b.getY() - l.getBlockY()) <= Cfg.spawnAreaRadius &&
                    Math.abs(b.getZ() - l.getBlockZ()) <= Cfg.spawnAreaRadius)
                return true;
        }
        return false;
    }

    public static String getPlayerBySpawn(Block b) {
        for (Map.Entry<String, Location> e : spawnBinds.entrySet()) {
            Location l = e.getValue();
            if (Math.abs(b.getX() - l.getBlockX()) <= Cfg.spawnAreaRadius &&
                    Math.abs(b.getY() - l.getBlockY()) <= Cfg.spawnAreaRadius &&
                    Math.abs(b.getZ() - l.getBlockZ()) <= Cfg.spawnAreaRadius)
                return e.getKey();
        }
        return null;
    }

    public static Location getSpawnLocation(Player p) {
        return spawnBinds.computeIfAbsent(p.getName(), name -> spawnFinder.enforeSafeSpawn());
    }

    public static void highlightSpawns() {
        List<Location> hls = new LinkedList<>();
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        for (Location spawn : spawnBinds.values()) {
            if (players.stream().anyMatch(p -> WorldUtil.distance2d(p.getLocation(), spawn) < 16))
                hls.add(spawn);
        }
        World w = Bukkit.getWorlds().get(0);
        double spawnRadius = Cfg.spawnAreaRadius + 0.5;
        double spawnHeight = 4d;
        double lineThickness = 0.1d;
        for (Location l : hls) {
            // vert
            w.spawnParticle(Particle.WAX_ON, l.getX() - spawnRadius, l.getY(), l.getZ() - spawnRadius, 2, lineThickness, spawnHeight, lineThickness);
            w.spawnParticle(Particle.WAX_ON, l.getX() + spawnRadius, l.getY(), l.getZ() - spawnRadius, 2, lineThickness, spawnHeight, lineThickness);
            w.spawnParticle(Particle.WAX_ON, l.getX() - spawnRadius, l.getY(), l.getZ() + spawnRadius, 2, lineThickness, spawnHeight, lineThickness);
            w.spawnParticle(Particle.WAX_ON, l.getX() + spawnRadius, l.getY(), l.getZ() + spawnRadius, 2, lineThickness, spawnHeight, lineThickness);
            // top
            w.spawnParticle(Particle.WAX_ON, l.getX() + spawnRadius, l.getY() + spawnHeight, l.getZ(), 2, lineThickness, lineThickness, spawnRadius);
            w.spawnParticle(Particle.WAX_ON, l.getX() - spawnRadius, l.getY() + spawnHeight, l.getZ(), 2, lineThickness, lineThickness, spawnRadius);
            w.spawnParticle(Particle.WAX_ON, l.getX(), l.getY() + spawnHeight, l.getZ() + spawnRadius, 2, spawnRadius, lineThickness, lineThickness);
            w.spawnParticle(Particle.WAX_ON, l.getX(), l.getY() + spawnHeight, l.getZ() - spawnRadius, 2, spawnRadius, lineThickness, lineThickness);
            // bottom
            w.spawnParticle(Particle.WAX_ON, l.getX() + spawnRadius, l.getY(), l.getZ(), 2, lineThickness, lineThickness, spawnRadius);
            w.spawnParticle(Particle.WAX_ON, l.getX() - spawnRadius, l.getY(), l.getZ(), 2, lineThickness, lineThickness, spawnRadius);
            w.spawnParticle(Particle.WAX_ON, l.getX(), l.getY(), l.getZ() + spawnRadius, 2, spawnRadius, lineThickness, lineThickness);
            w.spawnParticle(Particle.WAX_ON, l.getX(), l.getY(), l.getZ() - spawnRadius, 2, spawnRadius, lineThickness, lineThickness);
        }
    }

    private SpawnManager() {}
}
