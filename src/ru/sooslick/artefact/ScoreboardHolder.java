package ru.sooslick.artefact;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import ru.sooslick.artefact.outlaw.util.CommonUtil;
import ru.sooslick.artefact.outlaw.util.LoggerUtil;

import java.util.Objects;

/**
 * Represents the game's scoreboard which will function in the current game
 */
public class ScoreboardHolder {
    private static final String DEBUG_SET_SCOREBOARD = "Set custom scoreboard for player ";

    private static ScoreboardHolder instance;

    private final Scoreboard scoreboard;
    private final Team artefactTeam;
    private final Objective goals;

    public static void adjustPlayerScoreboard(Player p) {
        p.setScoreboard(instance.getScoreboard());
    }

    ScoreboardHolder() {
        scoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getNewScoreboard();
        artefactTeam = scoreboard.registerNewTeam("AF");
        artefactTeam.setColor(CommonUtil.getRandomColor());
        goals = scoreboard.registerNewObjective("Artefact", "dummy", "Artefact");
        goals.setDisplaySlot(DisplaySlot.PLAYER_LIST);
        instance = this;
    }

    /**
     * Return the current Scoreboard
     *
     * @return current Scoreboard
     */
    public Scoreboard getScoreboard() {
        return scoreboard;
    }

    /**
     * Fix custom scoreboard so the player can see current objectives and teams
     *
     * @param p player requires the fix
     */
    // for rejoin bugfix
    public void setPlayerScoreboard(Player p) {
        p.setScoreboard(scoreboard);
        LoggerUtil.debug(DEBUG_SET_SCOREBOARD + p.getName());
    }

    public void joinPlayer(Player p) {
        Team t = scoreboard.registerNewTeam(p.getName());
        t.addEntry(p.getName());
        t.setColor(CommonUtil.getRandomColor());
        setPlayerScoreboard(p);
    }

    public void adjustArtefact(LivingEntity e) {
        artefactTeam.addEntry(e.getUniqueId().toString());
    }

    public void goal(String name) {
        Bukkit.broadcastMessage(String.format(Messages.WINROUND, name));
        Score s = goals.getScore(name);
        s.setScore(s.getScore() + 1);
        if (s.getScore() >= Cfg.goalScore) {
            Bukkit.broadcastMessage(String.format(Messages.WINGAME, name));
            ArtefactPlugin.getInstance().triggerLobby();
        }
    }
}
