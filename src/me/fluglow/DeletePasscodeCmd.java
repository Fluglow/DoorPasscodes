package me.fluglow;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.material.Door;

public class DeletePasscodeCmd implements CommandExecutor {

	private final PasscodeDoorManager manager;

	DeletePasscodeCmd(PasscodeDoorManager manager)
	{
		this.manager = manager;
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
			player.sendMessage(ChatColor.RED + "Please look at the door you want to remove a passcode from.");
			return true;
		}

		BlockState state = block.getState();
		if(!(state.getData() instanceof Door))
		{
			player.sendMessage(ChatColor.RED + "You must be looking at a door!");
			return true;
		}
		manager.deleteDoor(manager.getDoorByLocation(state.getLocation()));
		player.sendMessage(ChatColor.YELLOW + "passcode removed!");
		return true;
	}
}
