package ru.sooslick.artefact;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ru.sooslick.artefact.outlaw.util.LoggerUtil;
import ru.sooslick.artefact.outlaw.util.WorldUtil;

import java.util.LinkedHashSet;
import java.util.Set;

public class ArtefactPlugin extends JavaPlugin {
    private static final String PLUGIN_CREATE_DATAFOLDER = "Created plugin data folder";
    private static final String PLUGIN_CREATE_DATAFOLDER_FAILED = "§eCannot create plugin data folder. Default config will be loaded.\n Do you have sufficient rights?";
    private static final String PLUGIN_INIT_SUCCESS = "Init Artefact Plugin - success";

    private static ArtefactPlugin instance;

    private boolean gameRunning = false;
    private Set<Player> votestarters;
    private int countdown;
    private LobbyEvents lobbyEvents;
    private GameEvents gameEvents;
    private SpawnProtectEvents protectEvents;
    private Block artefactBlock;
    private Set<String> activePlayers;
    private Player carry;
    private LivingEntity placeholder;
    private ScoreboardHolder scoreboardHolder;

    public static ArtefactPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        //init working folder and config file
        if (!(getDataFolder().exists())) {
            if (getDataFolder().mkdir()) {
                LoggerUtil.info(PLUGIN_CREATE_DATAFOLDER);
                saveDefaultConfig();
            } else {
                LoggerUtil.warn(PLUGIN_CREATE_DATAFOLDER_FAILED);
            }
        }

        //register "artifact" command
        CommandListener cmdl = new CommandListener();
        PluginCommand cmd = getCommand(CommandListener.COMMAND_ARTIFACT);
        assert cmd != null;
        cmd.setExecutor(cmdl);
        Bukkit.getPluginManager().registerEvents(cmdl, this);

        //init game variables
        triggerLobby();
        LoggerUtil.info(PLUGIN_INIT_SUCCESS);
    }

    public void votestart(Player p) {
        if (gameRunning) {
            p.sendMessage(Messages.GAME_IS_RUNNING);
            return;
        }
        if (votestarters.add(p)) {
            Bukkit.broadcastMessage(String.format(Messages.START_VOTE, p.getName()));
            broadcastVotesCount();
            if (votestarters.size() == Cfg.minVotes) {
                countdown = 30;
                Bukkit.getScheduler().scheduleSyncDelayedTask(this, this::countdownImpl, 20);
                Bukkit.broadcastMessage(String.format(Messages.START_COUNTDOWN, countdown));
            }
        }
    }

    public void unvote(Player p) {
        if (votestarters.remove(p)) {
            broadcastVotesCount();
            if (votestarters.size() < Cfg.minVotes) {
                countdown = Cfg.prestartTimer;
                Bukkit.broadcastMessage(Messages.NOT_ENOUGH_PLAYERS);
            }
        }
    }

    public void broadcastVotesCount() {
        Bukkit.broadcastMessage(String.format(Messages.START_VOTES_COUNT, votestarters.size(), Cfg.minVotes));
    }

    public boolean isGameRunning() {
        return gameRunning;
    }

    public void join(Player p) {
        if (activePlayers.contains(p.getName())) {
            p.sendMessage(Messages.ALREADY_PLAYING);
            return;
        }
        activePlayers.add(p.getName());
        respawnPlayer(p);
        scoreboardHolder.joinPlayer(p);
    }

    public boolean isPlaying(Player p) {
        return activePlayers.contains(p.getName());
    }

    public Block getArtefactBlock() {
        return artefactBlock;
    }

    public Player getCarry() {
        return carry;
    }

    public void dropArtefact(Block droppedBlock) {
        if (carry != null) {
            carry.setGlowing(false);
            carry.removePotionEffect(PotionEffectType.SLOW);
            carry.getInventory().remove(Material.BEDROCK);
            carry = null;
            spawnArtefact(droppedBlock);
        }
    }

    public void pickupArtefact(Player p) {
        PlayerInventory inv = p.getInventory();
        if (inv.firstEmpty() != -1) {
            placeholder.remove();
            placeholder = null;
            carry = p;
            carry.setGlowing(true);
            artefactBlock.setType(Material.AIR);
            ItemStack is = new ItemStack(Material.BEDROCK);
            ItemMeta im = is.getItemMeta();
            if (im != null) {
                im.setDisplayName(Messages.ARTEFACT_NAME);
                is.setItemMeta(im);
            }
            inv.addItem(is);
            carry.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 200, 3));
            Bukkit.broadcastMessage(String.format(Messages.ARTEFACT_PICKUP, p.getName()));
        }
    }

    public void spawnDefaultArtefact() {
        spawnArtefact(WorldUtil.getRandomLocation(Cfg.artefactLocation, Cfg.artefactSpawnRadius).getBlock());
    }

    public void spawnArtefact(Block b) {
        String s = SpawnFinder.getPlayerBySpawn(b);
        if (s == null) {
            b.setType(Material.BEDROCK);
            artefactBlock = b;
            Shulker e = (Shulker) Bukkit.getWorlds().get(0).spawnEntity(b.getLocation(), EntityType.SHULKER);
            e.setAI(false);
            e.setInvulnerable(true);
            e.setGlowing(true);
            e.setInvisible(true);
            e.setLootTable(null);
            e.setCustomName("Artefact");
            placeholder = e;
            LoggerUtil.debug("Spawned special block at " + b.getLocation());
            scoreboardHolder.adjustArtefact(e);
        } else {
            scoreboardHolder.goal(s);
            spawnDefaultArtefact();
        }
    }

    public void respawnPlayer(Player p) {
        Location resp = SpawnFinder.getSpawnLocation(p);
        p.teleport(resp);
        p.setHealth(20);
        p.setFoodLevel(20);
        p.setSaturation(5);
        p.setExhaustion(0);
        p.setTotalExperience(0);
        p.setExp(0);
        p.setLevel(0);
        p.getInventory().clear();
        p.getActivePotionEffects().forEach(pe -> p.removePotionEffect(pe.getType()));
        p.setBedSpawnLocation(null);
        p.setGameMode(GameMode.SURVIVAL);
        p.getInventory().addItem(new ItemStack(Material.COMPASS));
    }

    protected void triggerLobby() {
        gameRunning = false;
        reloadConfig();
        Cfg.readConfig(getConfig());
        votestarters = new LinkedHashSet<>();
        countdown = Cfg.prestartTimer;
        SpawnFinder.launchJob();
        if (gameEvents != null)
            HandlerList.unregisterAll(gameEvents);
        if (protectEvents != null)
            HandlerList.unregisterAll(protectEvents);
        lobbyEvents = new LobbyEvents();
        Bukkit.getPluginManager().registerEvents(lobbyEvents, this);
    }

    protected void triggerGame() {
        gameRunning = true;
        HandlerList.unregisterAll(lobbyEvents);
        SpawnFinder.bindSpawns();
        carry = null;
        scoreboardHolder = new ScoreboardHolder(Bukkit.getScoreboardManager());
        activePlayers = new LinkedHashSet<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            respawnPlayer(p);
            activePlayers.add(p.getName());
            scoreboardHolder.joinPlayer(p);
        }
        spawnDefaultArtefact();
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, this::gameImpl, 1);
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, this::highlightImpl, 20);
        gameEvents = new GameEvents();
        Bukkit.getPluginManager().registerEvents(gameEvents, this);
        if (Cfg.spawnProtection) {
            protectEvents = new SpawnProtectEvents();
            Bukkit.getPluginManager().registerEvents(protectEvents, this);
        }
        Bukkit.broadcastMessage(Messages.GAME_STARTED);
    }

    private void countdownImpl() {
        if (votestarters.size() < Cfg.minVotes)
            return;
        countdown--;
        if (countdown <= 0) {
            triggerGame();
            return;
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, this::countdownImpl, 20);
        if (countdown % 10 == 0) {
            Bukkit.broadcastMessage(String.format(Messages.START_COUNTDOWN, countdown));
        }
    }

    private void gameImpl() {
        if (!gameRunning)
            return;
        Location compass = artefactBlock.getLocation();
        if (carry != null) {
            compass = carry.getLocation();
            if (!carry.hasPotionEffect(PotionEffectType.SLOW))
                carry.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 200, 3));
            String pl = SpawnFinder.getPlayerBySpawn(carry.getLocation().getBlock());
            if (pl != null && pl.equals(carry.getName())) {
                dropArtefact(carry.getLocation().getBlock());
            }
        }
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.equals(carry))
                onlinePlayer.setCompassTarget(SpawnFinder.getSpawnLocation(onlinePlayer));
            else
                onlinePlayer.setCompassTarget(compass);
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, this::gameImpl, 1);
    }

    private void highlightImpl() {
        if (!gameRunning)
            return;
        SpawnFinder.highlightSpawns();
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, this::highlightImpl, 8);
    }
}
