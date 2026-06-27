package ru.sooslick.artefact;

import org.bukkit.Material;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.stream.Collectors;

public class SpawnProtectEvents implements Listener {
    @EventHandler(ignoreCancelled = true)
    public void onCreeper(EntityExplodeEvent e) {
        e.blockList().removeAll(e.blockList().stream()
                .filter(SpawnManager::isProtected)
                .collect(Collectors.toList()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onTnt(BlockExplodeEvent e) {
        e.blockList().removeAll(e.blockList().stream()
                .filter(SpawnManager::isProtected)
                .collect(Collectors.toList()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonPush(BlockPistonExtendEvent e) {
        if (e.getBlocks().stream()
                .anyMatch(SpawnManager::isProtected))
            e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonPull(BlockPistonRetractEvent e) {
        if (e.getBlocks().stream()
                .anyMatch(SpawnManager::isProtected))
            e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() != null && e.getClickedBlock().getState() instanceof Container)
            if (SpawnManager.isProtected(e.getClickedBlock(), e.getPlayer()))
                e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player)
            if (SpawnManager.isProtected(e.getEntity().getLocation().getBlock(), (Player) e.getEntity()))
                e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        if (e.getBlockPlaced().getType() != Material.BEDROCK) {
            if (SpawnManager.isProtected(e.getBlockPlaced(), e.getPlayer()))
                e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBurn(BlockBurnEvent e) {
        if (SpawnManager.isProtected(e.getBlock()))
            e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucket(PlayerBucketEmptyEvent e) {
        if (SpawnManager.isProtected(e.getBlock(), e.getPlayer()))
            e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFlow(BlockFromToEvent e) {
        if (SpawnManager.isProtected(e.getToBlock()) && !SpawnManager.isProtected(e.getBlock()))
            e.setCancelled(true);
    }
}
