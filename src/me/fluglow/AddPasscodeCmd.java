package me.fluglow;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.material.Door;

public class AddPasscodeCmd implements CommandExecutor {

	private final PasscodeGUIListener guiListener;
	private PasscodeDoorManager doorManager;

	AddPasscodeCmd(PasscodeGUIListener guiListener, PasscodeDoorManager doorManager)
	{
		this.guiListener = guiListener;
		this.doorManager = doorManager;
	}

	@Override
	public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
		if(!(commandSender instanceof Player))
		{
			commandSender.sendMessage(ChatColor.RED + "Only a player can use this command.");
			return true;
		}

		Player player = (Player)commandSender;
		Block block = player.getTargetBlock(null, 5);
		if(block == null)
		{
			player.sendMessage(ChatColor.RED + "Please look at the door you want to set a passcode for.");
			return true;
		}

		BlockState state = block.getState();
		if(!(state.getData() instanceof Door))
		{
			player.sendMessage(ChatColor.RED + "You must be looking at a door.");
			return true;
		}

		Location doorLoc = block.getLocation();
		if(((Door)state.getData()).isTopHalf()) //If this is the top half, get bottom half
		{
			doorLoc.add(0, -1, 0);
		}

		if(doorManager.getDoorByLocation(doorLoc) != null)
		{
			player.sendMessage(ChatColor.RED + "That door already has a passcode.");
			return true;
		}

		guiListener.openpasscodeSetInv(player, doorLoc);
		return true;
	}
}
