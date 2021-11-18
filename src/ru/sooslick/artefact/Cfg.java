package ru.sooslick.artefact;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import ru.sooslick.artefact.outlaw.util.LoggerUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Cfg {
    private static final String DEBUG_MODE = "debugMode";
    private static final String MAX_PLAYERS = "maxPlayers";
    private static final String MIN_VOTES = "minVotesToStart";
    private static final String PRESTART_TIMER = "prestartTimer";
    private static final String SPAWN_DISTANCE = "spawnDistance";
    private static final String ARTEFACT_LOCATION = "artefactLocation";
    private static final String ARTEFACT_SPAWN_RADIUS = "artefactSpawnRadius";
    private static final String GOAL_SCORE = "goalScore";
    private static final String SPAWN_PROTECTION = "spawnProtection";

    private static final Pattern XZ_REGEX = Pattern.compile("([-]?\\d+),[ ]?([-]?\\d+)");
    private static final Location ZERO_POINT = new Location(null, 0, 0, 0);

    public static boolean debugMode = false;
    public static int maxPlayers = 8;
    public static int minVotes = 2;
    public static int prestartTimer = 30;
    public static int spawnDistance = 180;
    public static Location artefactLocation = ZERO_POINT;
    public static int artefactSpawnRadius = 5;
    public static int goalScore = 2;
    public static boolean spawnProtection = true;

    public static void readConfig(FileConfiguration cfg) {
        debugMode = cfg.getBoolean(DEBUG_MODE, false);
        maxPlayers = cfg.getInt(MAX_PLAYERS, 8);
        minVotes = cfg.getInt(MIN_VOTES, 2);
        prestartTimer = cfg.getInt(PRESTART_TIMER, 30);
        spawnDistance = cfg.getInt(SPAWN_DISTANCE, 180);
        artefactSpawnRadius = cfg.getInt(ARTEFACT_SPAWN_RADIUS, 5);
        goalScore = cfg.getInt(GOAL_SCORE, 2);
        spawnProtection = cfg.getBoolean(SPAWN_PROTECTION, true);

        String artLoc = cfg.getString(ARTEFACT_LOCATION, "0, 0");
        try {
            Matcher m = XZ_REGEX.matcher(artLoc);
            if (m.find()) {
                int x = Integer.parseInt(m.group(1));
                int z = Integer.parseInt(m.group(2));
                artefactLocation = new Location(null, x, 0, z);
            } else
                artefactLocation = ZERO_POINT;
        } catch (Exception e) {
            artefactLocation = ZERO_POINT;
        }

        validate();
    }

    public static void validate() {
        LoggerUtil.setupLevel(debugMode);
        if (prestartTimer < 1) prestartTimer = 1;
        if (minVotes < 1) minVotes = 1;
        if (maxPlayers < minVotes) maxPlayers = minVotes;
        if (spawnDistance < maxPlayers) spawnDistance = maxPlayers;
        if (artefactSpawnRadius < 0) artefactSpawnRadius = 0;
        if (goalScore < 1) goalScore = 1;
    }
}