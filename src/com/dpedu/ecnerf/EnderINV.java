package com.dpedu.ecnerf;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.v1_5_R2.EntityHuman;
import net.minecraft.server.v1_5_R2.IInventory;
import net.minecraft.server.v1_5_R2.InventoryEnderChest;
import net.minecraft.server.v1_5_R2.InventorySubcontainer;

import org.bukkit.craftbukkit.v1_5_R2.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_5_R2.entity.CraftPlayer;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;


public class EnderINV extends InventorySubcontainer implements IInventory {
	public List<HumanEntity> transaction = new ArrayList<HumanEntity>();
	private CraftPlayer owner;
	private InventoryEnderChest enderChest;
	private int maxStack = 64;
	private JavaPlugin plugin;
	
	public EnderINV( CraftPlayer player, JavaPlugin plugin )
	{
		super( player.getHandle().getEnderChest().getName(), true, player.getHandle().getEnderChest().getSize() );
		this.enderChest = player.getHandle().getEnderChest();
		this.owner = player;
		this.items = this.enderChest.getContents();
		this.plugin = plugin;
	}

	public net.minecraft.server.v1_5_R2.ItemStack[] getContents()
	{
		return this.items;
	}

	public void onOpen(CraftHumanEntity who)
	{
		this.transaction.add(who);
	}

	public void onClose(CraftHumanEntity who)
	{
		this.transaction.remove(who);
	}

	public List<HumanEntity> getViewers()
	{
		return this.transaction;
	}

	public InventoryHolder getOwner()
	{
		return this.owner;
	}

	public void setMaxStackSize(int size)
	{
		this.maxStack = size;
	}

	public int getMaxStackSize()
	{
		return this.maxStack;
	}
 
	public boolean a(EntityHuman entityhuman)
	{
		return true;
	}

	public void update()
	{
		this.enderChest.update(); 
	}
	
	public String getName()
	{
		return this.owner.getName()+"' Chest";
	}
}
