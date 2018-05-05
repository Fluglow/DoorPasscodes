package me.fluglow;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

public class PasscodeGUIListener implements Listener {

	private final PasscodeDoorManager manager;
	private Map<Inventory, Location> guiSessions = new HashMap<>();

	PasscodeGUIListener(PasscodeDoorManager manager)
	{
		this.manager = manager;
	}

	void openpasscodeSetInv(Player player, Location doorLocation)
	{
		Inventory inv = Bukkit.createInventory(player, InventoryType.CHEST, "Set your passcode");
		inv.setMaxStackSize(DoorPasscodes.MAX_CODE_NUM);
		int[] upperSlots = {1, 3, 5, 7};
		int[] slots = {10, 12, 14, 16};
		int[] bottomSlots = {19, 21, 23, 25};
		ItemStack codeItem = new ItemStack(Material.PAPER, DoorPasscodes.MIN_CODE_NUM);
		ItemMeta codeMeta = codeItem.getItemMeta();
		codeMeta.setDisplayName("1");
		codeItem.setItemMeta(codeMeta);

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
			inv.setItem(slot, codeItem);
		}

		for(int slot : upperSlots)
		{
			inv.setItem(slot, add);
		}

		for(int slot : bottomSlots)
		{
			inv.setItem(slot, decrement);
		}

		ItemStack saveItem = new ItemStack(Material.STAINED_CLAY, 1, (short)5);
		ItemMeta saveItemMeta = saveItem.getItemMeta();
		saveItemMeta.setDisplayName("Save");
		saveItem.setItemMeta(saveItemMeta);

		ItemStack backItem = new ItemStack(Material.STAINED_CLAY, 1, (short)14);
		ItemMeta backItemMeta = backItem.getItemMeta();
		backItemMeta.setDisplayName("Cancel");
		backItem.setItemMeta(backItemMeta);

		inv.setItem(18, backItem);
		inv.setItem(26, saveItem);

		guiSessions.put(inv, doorLocation);
		player.openInventory(inv);
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
		if(guiSessions.containsKey(event.getInventory()))
		{
			if(event.getClick() == ClickType.DOUBLE_CLICK || event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT)
			{
				event.setCancelled(true); //Cancel double click to stop stealing from display inv
			}
			if(inv.getType() != InventoryType.CHEST || !guiSessions.containsKey(event.getClickedInventory()))
			{
				return;
			}
			event.setCancelled(true);
			if(event.getClick() != ClickType.LEFT) return;
			ItemStack clicked = event.getCurrentItem();
			Player clicker = (Player)event.getWhoClicked();
			if(clicked == null || clicked.getItemMeta() == null || clicked.getItemMeta().getDisplayName() == null) return;
			String name = clicked.getItemMeta().getDisplayName();
			int slot;
			ItemStack item = null;
			switch(name)
			{
				case "+":
					slot = event.getSlot() + 9;
					item = inv.getItem(slot);
					if(item.getAmount() == DoorPasscodes.MAX_CODE_NUM)
					{
						item.setAmount(DoorPasscodes.MIN_CODE_NUM);
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
						item.setAmount(DoorPasscodes.MAX_CODE_NUM);
					}
					else
					{
						item.setAmount(item.getAmount() - 1);
					}
					break;
				case "Save":
					saveDoor(inv, guiSessions.get(inv));
					clicker.closeInventory();
					clicker.sendMessage(ChatColor.GREEN + "Passcode added!");
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

	private void saveDoor(Inventory inv, Location doorLocation)
	{
		int[] slots = {10, 12, 14, 16};
		int[] code = new int[4];
		for(int i = 0; i < slots.length; i++)
		{
			ItemStack item = inv.getItem(slots[i]);
			if(item == null || item.getType() == Material.AIR) return;
			code[i] = item.getAmount();
		}
		manager.createPasscodeDoor(doorLocation, code);
	}
}
