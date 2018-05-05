package me.fluglow;

import org.bukkit.plugin.java.JavaPlugin;

public class DoorPasscodes extends JavaPlugin {

	static int MAX_CODE_NUM;
	static int MIN_CODE_NUM;
	static int INCORRECT_PASSCODE_WAIT_MILLISECONDS;
	static int DOOR_OPEN_DURATION_SECONDS;
	static int ALLOWED_WRONG_TRIES;

	@Override
	public void onEnable() {
		saveDefaultConfig();
		getConfig().options().copyDefaults(true);
		PasscodeDoorManager manager = new PasscodeDoorManager(getDataFolder());
		PasscodeGUIListener guiListener = new PasscodeGUIListener(manager);

		getCommand("addpasscode").setExecutor(new AddPasscodeCmd(guiListener, manager));
		getCommand("removepasscode").setExecutor(new DeletePasscodeCmd(manager));

		getServer().getPluginManager().registerEvents(guiListener, this);
		getServer().getPluginManager().registerEvents(new DoorOpenListener(this, manager), this);

		INCORRECT_PASSCODE_WAIT_MILLISECONDS = getConfig().getInt("incorrect_passcode_wait_time_seconds") * 1000;
		if(INCORRECT_PASSCODE_WAIT_MILLISECONDS < 0)
		{
			getLogger().warning("Incorrect passcode wait time can't be under zero. Defaulting to zero.");
			INCORRECT_PASSCODE_WAIT_MILLISECONDS = 0;
			getConfig().set("incorrect_passcode_wait_time_seconds", 0);
		}

		DOOR_OPEN_DURATION_SECONDS = getConfig().getInt("door_open_duration_seconds");
		if(DOOR_OPEN_DURATION_SECONDS < 1)
		{
			getLogger().warning("Door open duration can't be under one. Defaulting to one.");
			DOOR_OPEN_DURATION_SECONDS = 1;
			getConfig().set("door_open_duration_seconds", 1);
		}

		ALLOWED_WRONG_TRIES = getConfig().getInt("allowed_wrong_tries");
		if(ALLOWED_WRONG_TRIES < 0)
		{
			getLogger().warning("Allowed wrong tries can't be under zero. Defaulting to zero.");
			ALLOWED_WRONG_TRIES = 0;
			getConfig().set("allowed_wrong_tries", 0);
		}
		loadMinMaxCodeNumber();
		saveConfig();
	}
	
	private void loadMinMaxCodeNumber()
	{
		MIN_CODE_NUM = getConfig().getInt("minimum_code_number");
		MAX_CODE_NUM = getConfig().getInt("maximum_code_number");

		boolean errors = false;

		if(MIN_CODE_NUM > MAX_CODE_NUM)
		{
			getLogger().warning("Minimum code number is more than maximum code number! Maxmimum code number will be set to " + (MIN_CODE_NUM + 1) + ".");
			MAX_CODE_NUM = MIN_CODE_NUM + 1;
			errors = true;
		}

		if(MIN_CODE_NUM < 1)
		{
			getLogger().warning("Minimum code number is less than one! This is not allowed. Minimum code number will be set to 1.");
			MIN_CODE_NUM = 1;
			errors = true;
		}

		if(MAX_CODE_NUM < 1)
		{
			getLogger().warning("Maximum code number is less than one! This is not allowed. Maximum code number will be set to " + (MIN_CODE_NUM + 1) + ".");
			MAX_CODE_NUM = MIN_CODE_NUM + 1;
			errors = true;
		}

		if(errors)
		{
			getConfig().set("minimum_code_number", MIN_CODE_NUM);
			getConfig().set("maximum_code_number", MAX_CODE_NUM);
		}
	}

	@Override
	public void onDisable() {

	}
}
