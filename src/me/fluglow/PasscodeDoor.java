package me.fluglow;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.material.Door;

import java.util.*;

import static me.fluglow.DoorPasscodes.MAX_CODE_NUM;
import static me.fluglow.DoorPasscodes.MIN_CODE_NUM;

public class PasscodeDoor implements ConfigurationSerializable {
	private final int id;
	private final Location location;
	private final int[] passcode;

	PasscodeDoor(int id, Location location, int[] passcode)
	{
		this.id = id;
		this.location = location;
		this.passcode = passcode;
	}


	boolean hasValidPasscode()
	{
		boolean valid = true;
		for(int digit : passcode)
		{
			if(digit > MAX_CODE_NUM || digit < MIN_CODE_NUM)
			{
				valid = false;
			}
		}
		return valid;
	}

	int getId() {
		return id;
	}

	Location getLocation() {
		return location;
	}

	boolean isCorrectCode(int[] code)
	{
		return Arrays.equals(passcode, code);
	}

	void setOpenState(boolean open)
	{
		Block doorBlock = getLocation().getBlock();
		BlockState state = doorBlock.getState();
		if(!(state.getData() instanceof Door)) return;

		Door doorData = (Door)state.getData();
		doorData.setOpen(open);
		state.setData(doorData);
		state.update();
	}

	boolean isOpen()
	{
		Block doorBlock = getLocation().getBlock();
		BlockState state = doorBlock.getState();
		if(!(state.getData() instanceof Door)) return false;

		Door doorData = (Door)state.getData();
		return doorData.isOpen();
	}

	@Override
	public Map<String, Object> serialize() {
		Map<String, Object> serialized = new LinkedHashMap<>();
		serialized.put("location", location);
		serialized.put("passcode", passcode);
		return serialized;
	}

	@SuppressWarnings("unchecked")
	static PasscodeDoor deserialize(Map<String, Object> serialized, int id)
	{
		Location doorLoc = (Location)serialized.get("location");
		List<Integer> passcode = (ArrayList<Integer>)serialized.get("passcode");
		return new PasscodeDoor(id, doorLoc, passcode.stream().mapToInt(i->i).toArray());
	}
}
