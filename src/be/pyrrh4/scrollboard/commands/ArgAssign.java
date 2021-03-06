package be.pyrrh4.scrollboard.commands;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import be.pyrrh4.core.Core;
import be.pyrrh4.core.Perm;
import be.pyrrh4.core.User;
import be.pyrrh4.core.command.CommandArgument;
import be.pyrrh4.core.command.CommandCall;
import be.pyrrh4.core.command.Param;
import be.pyrrh4.core.messenger.Messenger;
import be.pyrrh4.core.messenger.Messenger.Level;
import be.pyrrh4.core.util.Handler;
import be.pyrrh4.core.util.Utils;
import be.pyrrh4.scrollboard.ScrollBoard;
import be.pyrrh4.scrollboard.ScrollBoardUser;

public class ArgAssign extends CommandArgument {

	private static final Param paramPlayer = new Param(Utils.asList("player", "p"), "name", null, true);
	private static final Param paramScrollboard = new Param(Utils.asList("scrollboard"), "path", null, true);

	public ArgAssign() {
		super(ScrollBoard.instance(), Utils.asList("assign", "set"), "assign a scoreboard", Perm.SCROLLBOARD_ADMIN, false, paramPlayer, paramScrollboard);
	}

	@Override
	public void perform(CommandCall call) {
		Player player = call.getSenderAsPlayer();
		Player target = paramPlayer.getPlayer(call, true);
		String path = paramScrollboard.getString(call);

		if (!ScrollBoard.instance().getConfiguration().contains("scrollboards." + path) && !path.equalsIgnoreCase("{default}") && !path.equalsIgnoreCase("{none}")) {
			Messenger.send(player, Level.SEVERE_ERROR, "ScrollBoard", "Could not find scrollboard " + path);
			return;
		}

		final String uuid = player.getUniqueId().toString();
		final String finalPath = path.equalsIgnoreCase("{default}") ? ScrollBoard.instance().getDefaultScrollboard() : path;

		// plugin data
		User.from(UUID.fromString(uuid)).getPluginData(ScrollBoardUser.class).setScrollboard(finalPath);

		// mySQL
		if (Core.instance().getMySQL() != null && ScrollBoard.instance().getConfiguration().getBoolean("mysql_enable")) {
			new Handler() {
				@Override
				public void execute() {
					Core.instance().getMySQL().executeQuery("REPLACE INTO scrollboard_players(uuid,path) VALUES('" + uuid + "', '" + finalPath + "');");
				}
			}.runAsync();
		}

		// update
		if (player != null && player.isOnline())
		{
			if (finalPath.equalsIgnoreCase("{none}")) {
				player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
			}

			ScrollBoard.instance().getScrollboardManager().updateAll();
		}

		// message
		Messenger.send(player, Level.NORMAL_SUCCESS, "ScrollBoard", "§a" + target.getName() + "'s scrollboard was set to " + (path.equalsIgnoreCase("{default}") ? finalPath : path) + " !");
	}

}
