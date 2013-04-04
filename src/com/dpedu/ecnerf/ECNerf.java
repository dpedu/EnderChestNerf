package com.dpedu.ecnerf;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import net.minecraft.server.v1_5_R2.EntityPlayer;
import net.minecraft.server.v1_5_R2.MinecraftServer;
import net.minecraft.server.v1_5_R2.PlayerInteractManager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.v1_5_R2.CraftServer;
import org.bukkit.craftbukkit.v1_5_R2.entity.CraftPlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class ECNerf extends JavaPlugin implements Listener {
	
	// List of ender chest locations
	public Vector<PlacedChestData> placedCache = new Vector<PlacedChestData>();
	// List of ender chest contents
	private HashMap<String,EnderINV> enderContentsCache = new HashMap<String,EnderINV>();
	
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
		// Load the placedCache
		loadCache();
	}
	public void onDisable() {
		// Save ender locations data
		saveCache();
	}
	// Block default ender chest open action
	@EventHandler
	public void onInventoryOpen(InventoryOpenEvent event) {
		if(event.getInventory().getType()==InventoryType.ENDER_CHEST) {
			event.setCancelled(true);
		}
	}
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent pie) {
		// Verify we have right clicked an ender chest
		if (pie.getAction() == Action.RIGHT_CLICK_BLOCK && pie.getClickedBlock().getType()==Material.ENDER_CHEST ) {
			// Search for this chest in the database of chests
			Block targetBlock = pie.getClickedBlock();
			PlacedChestData cb = findChestByLocation(targetBlock.getX(), targetBlock.getY(), targetBlock.getZ(), targetBlock.getWorld().getName());
			if(cb==null) {
				// The chest wasnt in our database, so we'll assume it belongs to whoever is opening it.
				cb = new PlacedChestData(targetBlock.getX(), targetBlock.getY(), targetBlock.getZ(), pie.getPlayer().getName(), targetBlock.getWorld().getName());
				placedCache.add(cb);
				saveCache();
			}
			// Tell them who the chest belongs to
			//pie.getPlayer().sendMessage( "That enderchest belongs to "+cb.owner );
			
			EnderINV target = null;
			
			// See if they're in the cache already
			if(enderContentsCache.containsKey( cb.owner )) {
				target = enderContentsCache.get(cb.owner);
			} else {
				// Not in cache, load the target's chest contents
				CraftPlayer owner = null;
				// Find the player
				Player p = this.getServer().getPlayer( cb.owner );
				// p will be null if they're offline. So perform black magic
				if ( p == null )
				{
					MinecraftServer server = ( (CraftServer) Bukkit.getServer() ).getServer();
					EntityPlayer entity = new EntityPlayer(server, server.getWorldServer(0), cb.owner, new PlayerInteractManager(server.getWorldServer(0)));
					owner = (CraftPlayer)((entity == null) ? null : entity.getBukkitEntity());
					owner.loadData();
				} else {
					owner = ( CraftPlayer ) p;
				}
				target = new EnderINV( owner, this );
				this.enderContentsCache.put( cb.owner, target );
			}
			// We've fetched the chest contents, so simply show it!
			((CraftPlayer) pie.getPlayer()).getHandle().openContainer( target );
			pie.setCancelled(true);
		}
	}
	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
        Block block = (Block) event.getBlockPlaced();
        if(block.getType()==Material.ENDER_CHEST) {
        	// When an enderchest is placed, we need to catch and save it.
        	// Figure out the owner's name
        	ItemStack placedChest = event.getItemInHand();
        	ItemMeta blockInfo = placedChest.getItemMeta();
        	String[] chestName;
        	String chestOwnerName;
        	try {
        		chestName = blockInfo.getDisplayName().split("'");
        		chestOwnerName = chestName[0];
        	} catch(Exception e) {
        		event.setCancelled(true);
        		return;
        	}
        	// Since we're placing a block, we can assume that there isn't a record for this already.
        	BlockState placedAt = event.getBlockReplacedState();
        	PlacedChestData cb = new PlacedChestData(placedAt.getX(), placedAt.getY(), placedAt.getZ(), chestOwnerName, event.getPlayer().getWorld().getName());
        	placedCache.add(cb);
        	System.out.println("Placed an enderchest with owner '"+chestOwnerName+"' at "+placedAt.getX()+","+placedAt.getY()+","+placedAt.getZ());
        	// Save the data to disk
        	saveCache();
        }
	}
	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		if(event.getBlock().getType()==Material.ENDER_CHEST) {
			// Search for a placed record where this happened 
			PlacedChestData chestHere = findChestByLocation(event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ(), event.getBlock().getWorld().getName());
			String owner = event.getPlayer().getName();
			if(chestHere!=null) {
				owner = chestHere.owner;
			}
			// Check if silk touch was used to break this chest 
			Map<Enchantment,Integer> toolEnchantments = event.getPlayer().getItemInHand().getEnchantments();
			if(toolEnchantments!=null && toolEnchantments.get(Enchantment.SILK_TOUCH)!=null && toolEnchantments.get(Enchantment.SILK_TOUCH)>=1) {
				// It was, so cancel the event and drop a named chest
				event.setCancelled(true);
				// Set where the chest was to air
				World curWorld = event.getPlayer().getWorld();
				Block block = curWorld.getBlockAt(event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ());
				block.setTypeId(0);
				// Drop a named chest
				ItemStack droppedChest = new ItemStack(Material.ENDER_CHEST);
				ItemMeta blockInfo = droppedChest.getItemMeta();
				blockInfo.setDisplayName(owner+"'s Chest");
				droppedChest.setItemMeta(blockInfo);
				curWorld.dropItem(new Location(curWorld, (double)event.getBlock().getX(), (double)event.getBlock().getY(), (double)event.getBlock().getZ()), droppedChest);
			} else {
				// It wasn't, so act normal. Explode the chest
			}
			// Remove it from the cache.
			placedCache.remove(chestHere);
			saveCache();
		}
	}

	@EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getType() == InventoryType.ANVIL && event.getCurrentItem().getType()==Material.ENDER_CHEST) {
        	// Block moving of an Ebder chest into the Anvil
            event.setCancelled(true);
        }
    }
	// Automatically name an item when crafted
	@EventHandler
	public void onCraftItem(CraftItemEvent event) {
		CraftingInventory craftInv = event.getInventory();
		// Check if we're making a ender chest
		if(craftInv.getResult().getType() == Material.ENDER_CHEST) {
			// Block shift-crafting because this is buggy
			if(event.isShiftClick()) {
				event.setCancelled(true);
				return;
			}			
			// Create a result for the crafting
			ItemStack craftedChest = new ItemStack(Material.ENDER_CHEST);
			ItemMeta blockInfo = craftedChest.getItemMeta();
			blockInfo.setDisplayName(event.getView().getPlayer().getName()+"'s Chest");
			craftedChest.setItemMeta(blockInfo);
			craftInv.setResult(craftedChest);
			//event.setCancelled(true);
		}
	}
	// Find a chest from location
	public PlacedChestData findChestByLocation(int x, int y, int z, String world) {
		for(int i=0;i<placedCache.size();i++) {
			PlacedChestData test = (PlacedChestData)placedCache.get(i);
			if(test.x == x && test.y==y && test.z==z && test.world.equals(world)) {
				return test;
			}
		}
		return null;
	}
	public void saveCache() {
		// Save the locations cache
		try {
			ObjectOutputStream oos = new ObjectOutputStream( new FileOutputStream( this.getDataFolder() + "/ChestLocations.dat" ) );
			oos.writeObject( (Object)this.placedCache );
			oos.flush();
			oos.close();
		} catch ( Exception e) {
			e.printStackTrace();
		}
	}
	public void loadCache() {
		// Load the locations cache
		try {
			ObjectInputStream ois = new ObjectInputStream( new FileInputStream( this.getDataFolder() + "/ChestLocations.dat" ) );
			this.placedCache = ( Vector<PlacedChestData> ) ois.readObject();
			ois.close();
		} catch ( FileNotFoundException e ) {
			// Initialize a blank vector
			this.placedCache = new Vector<PlacedChestData>();
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}
}