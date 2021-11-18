package ru.sooslick.artefact;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class LobbyEvents implements Listener {
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        p.setGameMode(GameMode.SPECTATOR);
        p.sendMessage(Messages.ABOUT);
        ArtefactPlugin.getInstance().broadcastVotesCount();
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        ArtefactPlugin.getInstance().unvote(e.getPlayer());
    }
}
