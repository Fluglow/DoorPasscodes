package me.fluglow;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Door;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

import static me.fluglow.DoorPasscodes.*;

public class DoorOpenListener implements Listener {

	private final String failedAttemptsMessage = ChatColor.RED + "You can't open this door because of too many failed attempts! Please try again later.";

	private final PasscodeDoorManager manager;
	private final DoorPasscodes mainPlugin;

	private Map<UUID, PlayerDoorOpenTries> openTries = new HashMap<>();
	private Map<Inventory, Location> guiSessions = new HashMap<>();

	private Map<PasscodeDoor, List<UUID>> allowedPassers = new HashMap<>();

	DoorOpenListener(DoorPasscodes mainPlugin, PasscodeDoorManager manager)
	{
		this.mainPlugin = mainPlugin;
		this.manager = manager;
		doorOpenTryClearTask();
	}

	@EventHandler
	public void blockRedstoneEvent(BlockRedstoneEvent event)
	{
		Location location = event.getBlock().getLocation();
		if(location.getBlock().getType() == Material.WOODEN_DOOR)
		{
			PasscodeDoor door = manager.getDoorByLocation(location);
			if(door == null) return;
			if(!door.isOpen())
			{
				event.setNewCurrent(0);
			}
		}
	}

	@EventHandler
	public void doorPassEvent(PlayerMoveEvent event)
	{
		Location to = event.getTo().getBlock().getLocation();

		if(event.getFrom().getBlock().getLocation().equals(to)) return;
		if(to.getBlock().getType() == Material.WOODEN_DOOR)
		{
			PasscodeDoor toDoor = manager.getDoorByLocation(to);
			if(toDoor == null) return;
			if(allowedPassers.get(toDoor) == null || !allowedPassers.get(toDoor).contains(event.getPlayer().getUniqueId()))
			{
				//Player's who fall on a door will get stuck :^)
				//Warn downloaders, no need to work around if no one requests a fix.
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void interactEvent(PlayerInteractEvent event)
	{
		if(event.getAction() == Action.RIGHT_CLICK_BLOCK)
		{
			BlockState state = event.getClickedBlock().getState();
			if(!(state.getData() instanceof Door))
			{
				return;
			}
			Location doorLocation = state.getLocation();
			PasscodeDoor passcodeDoor = manager.getDoorByLocation(doorLocation);
			if(passcodeDoor == null)
			{
				return; //Door isn't passcodeDoor, return.
			}
			event.setCancelled(true);
			if(passcodeDoor.isOpen()) return; //If door is already open, don't open GUI.
			DoorOpenResult openResult = isAllowedToOpen(event.getPlayer(), passcodeDoor);
			if(openResult == DoorOpenResult.FAILURE_ATTEMPTS)
			{
				event.getPlayer().sendMessage(failedAttemptsMessage);
				return;
			}
			else if(openResult == DoorOpenResult.FAILURE_PERMISSION)
			{
				event.getPlayer().sendMessage(ChatColor.RED + "You're not allowed to open that.");
				return;
			}

			openCodeGUI(event.getPlayer(), doorLocation);
		}
	}

	@EventHandler
	public void inventoryClose(InventoryCloseEvent event)
	{
		guiSessions.remove(event.getInventory());
	}

	@EventHandler
	public void inventoryDrag(InventoryDragEvent event)
	{
		Inventory inv = event.getInventory();
		if(inv == null) return;
		if(inv.getType() == InventoryType.CHEST && guiSessions.containsKey(inv))
		{
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void inventoryClick(InventoryClickEvent event)
	{
		Inventory inv = event.getClickedInventory();
		if(inv == null) return;
		if(guiSessions.containsKey(event.getInventory())) //If this inventory is registered for a session
		{
			if(event.getClick() == ClickType.DOUBLE_CLICK || event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT)
			{
				event.setCancelled(true); //Cancel double click to stop stealing from display inv
			}
			if(inv.getType() != InventoryType.CHEST || !guiSessions.containsKey(event.getClickedInventory())) //If the player didn't itneract with the top inventory, return.
			{
				return;
			}
			event.setCancelled(true);
			if(event.getClick() != ClickType.LEFT) return;
			ItemStack clicked = event.getCurrentItem();
			Player clicker = (Player)event.getWhoClicked();
			if(clicked == null || clicked.getItemMeta() == null) return;
			String name = clicked.getItemMeta().getDisplayName();
			int slot;
			ItemStack item = null;
			switch(name) {
				case "+":
					slot = event.getSlot() + 9;
					item = inv.getItem(slot);
					if(item.getAmount() == MAX_CODE_NUM)
					{
						item.setAmount(MIN_CODE_NUM);
					}
					else
					{
						item.setAmount(item.getAmount() + 1);
					}
					break;
				case "-":
					slot = event.getSlot() - 9;
					item = inv.getItem(slot);
					if(item.getAmount() == DoorPasscodes.MIN_CODE_NUM)
					{
						item.setAmount(MAX_CODE_NUM);
					}
					else
					{
						item.setAmount(item.getAmount() - 1);
					}
					break;
				case "Open":
					tryOpenDoor(getCode(inv), clicker, manager.getDoorByLocation(guiSessions.get(inv)));
					clicker.closeInventory();
					break;
				case "Cancel":
					clicker.closeInventory();
			}
			if(item != null)
			{
				ItemMeta meta = item.getItemMeta();
				meta.setDisplayName(Integer.toString(item.getAmount()));
				item.setItemMeta(meta);
			}
		}
	}

	private DoorOpenResult isAllowedToOpen(Player player, PasscodeDoor door)
	{
		if(!player.hasPermission("doorpasscodes.open")) return DoorOpenResult.FAILURE_PERMISSION;
		if(!openTries.containsKey(player.getUniqueId()))
		{
			return DoorOpenResult.SUCCESS;
		}

		PlayerDoorOpenTries tries = openTries.get(player.getUniqueId());

		if(tries.contains(door) && System.currentTimeMillis() - tries.getCreationTimestamp(door) > INCORRECT_PASSCODE_WAIT_MILLISECONDS)
		{
			tries.expire(door);
			if(tries.isEmpty()) openTries.remove(player.getUniqueId());
			return DoorOpenResult.SUCCESS;
		}

		return tries.getTries(door) <= ALLOWED_WRONG_TRIES ? DoorOpenResult.SUCCESS : DoorOpenResult.FAILURE_ATTEMPTS;
	}

	private enum DoorOpenResult {
		FAILURE_ATTEMPTS, FAILURE_PERMISSION, SUCCESS
	}

	private void tryOpenDoor(int[] code, Player player, PasscodeDoor door)
	{
		if(code == null) return;
		if(door.isCorrectCode(code))
		{
			openDoor(player, door);
			player.sendMessage(ChatColor.GREEN + "Correct passcode!");
			if(openTries.containsKey(player.getUniqueId()))
			{
				openTries.get(player.getUniqueId()).expire(door); //Expire tries since player successfully opened door.
			}
		}
		else
		{
			if(!openTries.containsKey(player.getUniqueId()))
			{
				openTries.put(player.getUniqueId(), new PlayerDoorOpenTries());
			}
			int currentTries = openTries.get(player.getUniqueId()).addTry(door);

			if(currentTries == ALLOWED_WRONG_TRIES + 1)
			{
				player.sendMessage(failedAttemptsMessage);
			}
			else
			{
				player.sendMessage(ChatColor.RED + "Incorrect passcode! (" +  + currentTries + "/" + ALLOWED_WRONG_TRIES + ")");
			}
		}
	}

	private void openDoor(Player player, PasscodeDoor door)
	{
		door.setOpenState(true);

		List<UUID> allowed = allowedPassers.getOrDefault(door, new ArrayList<>());
		allowed.add(player.getUniqueId());
		allowedPassers.put(door, allowed);

		new BukkitRunnable() {
			@Override
			public void run() {
				door.setOpenState(false);
				List<UUID> allowed = allowedPassers.get(door);
				allowed.remove(player.getUniqueId());
				allowedPassers.put(door, allowed);

				if(allowedPassers.get(door).isEmpty())
				{
					allowedPassers.remove(door);
				}
			}
		}.runTaskLater(mainPlugin, DOOR_OPEN_DURATION_SECONDS*20);
	}

	private int[] getCode(Inventory inv)
	{
		int[] slots = {10, 12, 14, 16};
		int[] code = new int[4];
		for(int i = 0; i < slots.length; i++)
		{
			ItemStack item = inv.getItem(slots[i]);
			if(item == null || item.getType() == Material.AIR) return null;
			code[i] = item.getAmount();
		}
		return code;
	}

	private void openCodeGUI(Player player, Location doorLocation)
	{
		Inventory inv = Bukkit.createInventory(player, InventoryType.CHEST, "Door passcode");
		int[] upperSlots = {1, 3, 5, 7};
		int[] slots = {10, 12, 14, 16};
		int[] bottomSlots = {19, 21, 23, 25};
		ItemStack code0 = new ItemStack(Material.PAPER, DoorPasscodes.MIN_CODE_NUM);
		ItemMeta code0Meta = code0.getItemMeta();
		code0Meta.setDisplayName("1");
		code0.setItemMeta(code0Meta);

		ItemStack add = new ItemStack(Material.STICK, 1);
		ItemMeta addItemMeta = add.getItemMeta();
		addItemMeta.setDisplayName("+");
		add.setItemMeta(addItemMeta);

		ItemStack decrement = new ItemStack(Material.STICK, 1);
		ItemMeta decItemMeta = add.getItemMeta();
		decItemMeta.setDisplayName("-");
		decrement.setItemMeta(decItemMeta);

		for(int slot : slots)
		{
			inv.setItem(slot, code0);
		}

		for(int slot : upperSlots)
		{
			inv.setItem(slot, add);
		}

		for(int slot : bottomSlots)
		{
			inv.setItem(slot, decrement);
		}

		ItemStack openItem = new ItemStack(Material.STAINED_CLAY, 1, (short)5);
		ItemMeta saveItemMeta = openItem.getItemMeta();
		saveItemMeta.setDisplayName("Open");
		openItem.setItemMeta(saveItemMeta);

		ItemStack backItem = new ItemStack(Material.STAINED_CLAY, 1, (short)14);
		ItemMeta backItemMeta = backItem.getItemMeta();
		backItemMeta.setDisplayName("Cancel");
		backItem.setItemMeta(backItemMeta);

		inv.setItem(18, backItem);
		inv.setItem(26, openItem);

		guiSessions.put(inv, doorLocation);

		player.openInventory(inv);
	}

	private void doorOpenTryClearTask()
	{
		new BukkitRunnable() {
			@Override
			public void run() {
				for(Map.Entry<UUID, PlayerDoorOpenTries> tries : openTries.entrySet())
				{
					tries.getValue().expireAllIfDone();
					if(tries.getValue().isEmpty())
					{
						openTries.remove(tries.getKey());
					}
				}
			}
		}.runTaskTimer(mainPlugin, 10*60*20, 10*60*20); //10*60*20 = 10 minutes
	}

	class PlayerDoorOpenTries {
		private Map<PasscodeDoor, Integer> tries;
		private Map<PasscodeDoor, Long> timeStamps;

		PlayerDoorOpenTries()
		{
			tries = new HashMap<>();
			this.timeStamps = new HashMap<>();
		}

		int getTries(PasscodeDoor door) {
			return tries.getOrDefault(door, 0);
		}

		boolean contains(PasscodeDoor door)
		{
			return tries.containsKey(door);
		}

		long getCreationTimestamp(PasscodeDoor door) {
			return timeStamps.get(door);
		}

		void expire(PasscodeDoor door)
		{
			tries.remove(door);
			timeStamps.remove(door);
		}

		void expireIfDone(PasscodeDoor door)
		{
			if(!timeStamps.containsKey(door)) return;
			if(System.currentTimeMillis() - timeStamps.get(door) > INCORRECT_PASSCODE_WAIT_MILLISECONDS)
			{
				expire(door);
			}
		}

		void expireAllIfDone()
		{
			for(PasscodeDoor door : tries.keySet())
			{
				expireIfDone(door);
			}
		}

		boolean isEmpty()
		{
			return tries.isEmpty();
		}

		int addTry(PasscodeDoor door)
		{
			tries.put(door, tries.getOrDefault(door, 0) + 1);
			timeStamps.put(door, System.currentTimeMillis());
			return tries.get(door);
		}
	}
}
