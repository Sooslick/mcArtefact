package ru.sooslick.artefact;

import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class GameEvents implements Listener {
    private Player holdPlayer = null;
    private int holdTicks = 0;

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        ArtefactPlugin af = ArtefactPlugin.getInstance();
        Block b = e.getClickedBlock();
        if (b != null && b.equals(af.getArtefactBlock()) && !e.getPlayer().equals(holdPlayer)) {
            holdPlayer = e.getPlayer();
            holdTicks = 0;
            Bukkit.getScheduler().scheduleSyncDelayedTask(af, this::holdTick, 1);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        ArtefactPlugin af = ArtefactPlugin.getInstance();
        if (af.getCarry() != null && af.getCarry().equals(e.getPlayer()))
            if (e.getBlockPlaced().getType() == Material.BEDROCK)
                af.dropArtefact(e.getBlockPlaced());
    }

    @EventHandler(ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent e) {
        ArtefactPlugin af = ArtefactPlugin.getInstance();
        if (af.getCarry() != null && af.getCarry().equals(e.getEntity()))
            af.dropArtefact(e.getEntity().getLocation().getBlock());
    }

    @EventHandler(ignoreCancelled = true)
    public void onLeave(PlayerQuitEvent e) {
        ArtefactPlugin af = ArtefactPlugin.getInstance();
        if (af.getCarry() != null && af.getCarry().equals(e.getPlayer()))
            af.dropArtefact(e.getPlayer().getLocation().getBlock());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        ArtefactPlugin af = ArtefactPlugin.getInstance();
        Player p = e.getPlayer();
        ScoreboardHolder.adjustPlayerScoreboard(p);
        if (!af.isPlaying(e.getPlayer())) {
            p.setGameMode(GameMode.SPECTATOR);
            p.sendMessage(Messages.ABOUT);
            p.sendMessage(Messages.HINT_JOIN);
            return;
        }
        p.sendMessage(Messages.HINT_CONTINUE);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        ArtefactPlugin af = ArtefactPlugin.getInstance();
        if (af.getCarry() != null && af.getCarry().equals(e.getPlayer()))
            if (e.getItemDrop().getItemStack().getType() == Material.BEDROCK) {
                e.setCancelled(true);
                e.getPlayer().sendMessage(Messages.ARTEFACT_DROP);
            }
    }

    @EventHandler(ignoreCancelled = true)
    public void onRespawn(PlayerRespawnEvent e) {
        ArtefactPlugin af = ArtefactPlugin.getInstance();
        if (af.isPlaying(e.getPlayer())) {
            af.respawnPlayer(e.getPlayer());
            e.setRespawnLocation(SpawnFinder.getSpawnLocation(e.getPlayer()));
        } else
            e.getPlayer().setGameMode(GameMode.SPECTATOR);
    }

    private void holdTick() {
        if (holdPlayer == null)
            return;
        ArtefactPlugin af = ArtefactPlugin.getInstance();
        Block actualTarget = holdPlayer.getTargetBlockExact(4, FluidCollisionMode.NEVER);
        if (actualTarget != null && actualTarget.equals(af.getArtefactBlock())) {
            holdTicks++;
            if (holdTicks >= 20) {
                af.pickupArtefact(holdPlayer);
                holdPlayer = null;
            } else
                Bukkit.getScheduler().scheduleSyncDelayedTask(af, this::holdTick, 1);
        } else {
            holdPlayer = null;
        }
    }
}
