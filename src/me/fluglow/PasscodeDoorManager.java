package me.fluglow;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.material.Door;

import java.io.File;
import java.io.IOException;
import java.util.*;

class PasscodeDoorManager {
	private Map<Integer, PasscodeDoor> doors = new LinkedHashMap<>();
	private final File doorSaveFile;
	private final YamlConfiguration doorSaveConfig;
	PasscodeDoorManager(File dataFolder)
	{
		doorSaveFile = getDoorSaveFile(dataFolder);
		doorSaveConfig = getDoorSaveConfig(doorSaveFile);
		loadDoors();
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	private File getDoorSaveFile(File dataFolder)
	{
		if (!dataFolder.exists()) {
			dataFolder.mkdirs();
		}
		return new File(dataFolder, "PasscodeDoors.yml");
	}

	private YamlConfiguration getDoorSaveConfig(File doorSaveFile)
	{
		return YamlConfiguration.loadConfiguration(doorSaveFile);
	}

	private void saveDoorConfig()
	{
		try {
			doorSaveConfig.save(doorSaveFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void saveDoors()
	{
		for(Map.Entry<Integer, PasscodeDoor> doorEntry : doors.entrySet())
		{
			doorSaveConfig.set(Integer.toString(doorEntry.getKey()), doorEntry.getValue().serialize());
		}
		saveDoorConfig();
	}

	void deleteDoor(PasscodeDoor door)
	{
		doorSaveConfig.set(Integer.toString(door.getId()), null);
		doors.remove(door.getId());
		saveDoorConfig();
	}

	private void loadDoors()
	{
		List<Integer> keys = new ArrayList<>();
		for(String strKey : doorSaveConfig.getKeys(false))
		{
			try {
				keys.add(Integer.parseInt(strKey));
			} catch(NumberFormatException e)
			{
				Bukkit.getLogger().warning("[DoorPasscodes] Could not load door with id " + strKey + " because the id is not an integer!");
			}
		}

		keys.sort(Comparator.comparingInt(Integer::intValue));

		for(int key : keys)
		{
			PasscodeDoor door = PasscodeDoor.deserialize(doorSaveConfig.getConfigurationSection(Integer.toString(key)).getValues(true), key);
			if(door.getLocation().getBlock().getType() == Material.WOODEN_DOOR)
			{
				doors.put(key, door);
				if(!door.hasValidPasscode())
				{
					Bukkit.getLogger().warning("[DoorPasscodes] The door at " + prettyLocationString(door.getLocation()) + " has a passcode that conflicts with minimum or maximum code number settings (has the config been changed?)");
					Bukkit.getLogger().warning("[DoorPasscodes] The door will not be disabled, but can not be opened until configuration is changed or the passcode is removed.");
				}
			}
			else
			{
				Bukkit.getLogger().warning("[DoorPasscodes] Could not load door at " + prettyLocationString(door.getLocation()) + " because the door doesn't exist!");
			}
		}
	}

	private String prettyLocationString(Location location)
	{
		return "(" + location.getWorld().getName() + ", x: " + location.getX() + ", y: " + location.getY() + ", z: " + location.getZ() + ")";
	}

	void createPasscodeDoor(Location doorLocation, int[] code)
	{
		int id = getNextId();
		doors.put(id, new PasscodeDoor(id, doorLocation, code));
		saveDoors();
	}

	private int getNextId()
	{
		int freeId = 0;
		while(doors.keySet().contains(freeId))
		{
			freeId++;
		}
		return freeId;
	}

	private Location getDoorBottomHalf(Location location)
	{
		Block doorBlock = location.getBlock();
		BlockState state = doorBlock.getState();
		if(!(state.getData() instanceof Door)) return null;

		Door doorData = (Door)state.getData();
		if(doorData.isTopHalf())
		{
			return state.getLocation().add(0, -1, 0);
		}
		return state.getLocation();
	}

	PasscodeDoor getDoorByLocation(Location location)
	{
		for(PasscodeDoor door : doors.values())
		{
			//Compare 0, -1, 0 too since we save top half position.
			if(door.getLocation().equals(getDoorBottomHalf(location)))
			{
				return door;
			}
		}
		return null;
	}
}
