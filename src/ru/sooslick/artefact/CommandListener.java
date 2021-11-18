package ru.sooslick.artefact;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.TabCompleteEvent;
import org.jetbrains.annotations.NotNull;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class CommandListener implements CommandExecutor, Listener {
    public static final String COMMAND_ARTIFACT = "artefact";
    private static final String COMMAND_HELP = "help";
    private static final String COMMAND_HELP_ALIAS = "?";
    private static final String COMMAND_JOIN = "join";
    private static final String COMMAND_RULES = "rules";
    private static final String COMMAND_START = "start";
    private static final String COMMAND_VOTE = "votestart";
    private static final String COMMAND_VOTE_ALIAS = "v";
    private static final String PERMISSION_START = "artefact.force.start";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase(COMMAND_HELP) || args[0].equalsIgnoreCase(COMMAND_HELP_ALIAS)) {
            showHelp(sender);
            return true;
        }
        if (args[0].equalsIgnoreCase(COMMAND_START)) {
            if (!sender.hasPermission(PERMISSION_START))
                sender.sendMessage(Messages.NOT_PERMITTED);
            else if (ArtefactPlugin.getInstance().isGameRunning())
                sender.sendMessage(Messages.GAME_IS_RUNNING);
            else {
                Bukkit.broadcastMessage(String.format(Messages.START_FORCED, sender.getName()));
                ArtefactPlugin.getInstance().triggerGame();
            }
            return true;
        }
        if (args[0].equalsIgnoreCase(COMMAND_RULES)) {
            sender.sendMessage(Messages.RULES);
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.CONSOLE_CANNOT_DO_THIS);
            return true;
        }
        if (args[0].equalsIgnoreCase(COMMAND_VOTE) || args[0].equalsIgnoreCase(COMMAND_VOTE_ALIAS)) {
            ArtefactPlugin.getInstance().votestart((Player) sender);
            return true;
        }
        if (args[0].equalsIgnoreCase(COMMAND_JOIN)) {
            if (ArtefactPlugin.getInstance().isGameRunning())
                ArtefactPlugin.getInstance().join((Player) sender);
            else
                sender.sendMessage(Messages.GAME_IS_NOT_RUNNING);
            return true;
        }

        return true;
    }

    public void showHelp(CommandSender sender) {
        sender.sendMessage(Messages.COMMANDS_AVAILABLE);
    }

    @EventHandler
    public void onTabComplete(TabCompleteEvent e) {
        String[] args = e.getBuffer().replaceAll("\\s+", " ").trim().split(" ");
        if (args.length == 0)
            return;
        if (args.length == 1)
            e.setCompletions(Arrays.asList(COMMAND_HELP, COMMAND_RULES, COMMAND_VOTE, COMMAND_START, COMMAND_JOIN));
    }
}
