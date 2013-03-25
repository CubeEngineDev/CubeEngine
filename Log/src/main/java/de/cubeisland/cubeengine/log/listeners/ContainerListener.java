package de.cubeisland.cubeengine.log.listeners;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Furnace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import de.cubeisland.cubeengine.core.bukkit.BukkitUtils;
import de.cubeisland.cubeengine.core.logger.LogLevel;
import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.log.Log;
import de.cubeisland.cubeengine.log.storage.LogManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import static de.cubeisland.cubeengine.core.util.InventoryUtil.getMissingSpace;
import static de.cubeisland.cubeengine.log.storage.LogManager.ITEM_INSERT;
import static de.cubeisland.cubeengine.log.storage.LogManager.ITEM_REMOVE;

public class ContainerListener implements Listener
{

    private LogManager manager;
    private Log module;

    private TLongObjectHashMap<TObjectIntHashMap<ItemData>> inventoryChanges = new TLongObjectHashMap<TObjectIntHashMap<ItemData>>();

    public ContainerListener(Log module, LogManager manager)
    {
        this.module = module;
        this.manager = manager;
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event)
    {
        if (event.getPlayer() instanceof Player)
        {
            User user = this.module.getCore().getUserManager().getExactUser((Player)event.getPlayer());
            TObjectIntHashMap<ItemData> itemDataMap = this.inventoryChanges.get(user.key);
            if (itemDataMap != null)
            {
                Location location = this.getLocationForHolder(event.getInventory().getHolder());
                if (location == null) return;
                for (ItemData itemData : itemDataMap.keySet())
                {
                    int amount = itemDataMap.get(itemData);
                    if (amount == 0) continue;
                    String additional = this.serializeAdditionalItemData(itemData,amount);
                    this.manager.queueLog(location,amount < 0 ? ITEM_REMOVE : ITEM_INSERT,
                            user.key, itemData.material, itemData.dura, event.getInventory().getType().name(), additional);
                }
            }
            this.inventoryChanges.remove(user.key);
        }
    }

    private String serializeAdditionalItemData(ItemData itemData, int amount)
    {
        Map<String,Object> dataMap = new HashMap<String, Object>();
        dataMap.put("amount",amount);
        if (itemData.displayName != null)
        {
            dataMap.put("display",itemData.displayName);
        }
        if (itemData.lore != null)
        {
            dataMap.put("lore",itemData.lore);
        }
        if (itemData.enchantments != null)
        {
            Map<String,Integer> enchantments = new HashMap<String, Integer>();
            for (Map.Entry<Enchantment,Integer> entry : itemData.enchantments.entrySet())
            {
                enchantments.put(entry.getKey().getName(),entry.getValue());
            }
            dataMap.put("enchant",enchantments);
        }
        try {
            return this.manager.mapper.writeValueAsString(dataMap);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not parse itemmap!",e);
        }
    }

    private Location getLocationForHolder(InventoryHolder holder)
    {
        if (holder instanceof Entity)
        {
            return ((Entity)holder).getLocation();
        }
        else if (holder instanceof DoubleChest)
        {
            return ((DoubleChest)holder).getLocation();//TODO get a blocklocation
            //TODO get the correct chest
        }
        else if (holder instanceof BlockState)
        {
            return ((BlockState)holder).getLocation();
        }
        if (holder == null)
        {
            this.module.getLogger().log(LogLevel.DEBUG,"Inventory Holder is null! Logging is impossible.");
        }
        else
        {
            this.module.getLogger().log(LogLevel.DEBUG,"Unknown InventoryHolder:" + holder.toString());
        }
        return null;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event)
    {
        if (event.getPlayer() instanceof Player)
        {
            if (this.manager.isIgnored(event.getPlayer().getWorld(),LogManager.ITEM_CHANGE_IN_CONTAINER,event.getInventory().getHolder())) return;
            User user = this.module.getCore().getUserManager().getExactUser((Player)event.getPlayer());
            this.inventoryChanges.put(user.key,new TObjectIntHashMap<ItemData>());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event)
    {
        if (event.getSlot() == -999)
        {
            return;
        }
        if (event.getWhoClicked() instanceof Player)
        {
            final User user = this.module.getCore().getUserManager().getExactUser((Player)event.getWhoClicked());
            if (!this.inventoryChanges.containsKey(user.key)) return;
            final World world = event.getWhoClicked().getWorld();
            if (this.manager.isIgnored(world, ITEM_INSERT)
             && this.manager.isIgnored(world, ITEM_REMOVE)) return;
            Inventory inventory = event.getInventory();
            InventoryHolder holder = inventory.getHolder();
            if (this.manager.isIgnored(world,LogManager.ITEM_CHANGE_IN_CONTAINER,holder)) return;
            ItemStack inventoryItem = event.getCurrentItem();
            ItemStack cursorItem = event.getCursor();
            System.out.print("------------Click Event----------- in "+holder);
            System.out.print("Raw slot: "+ event.getRawSlot());
            System.out.print("Slot: "+ event.getSlot());
            System.out.print("Cursor: "+ cursorItem);
            System.out.print("Inventory: "+ inventoryItem);
            System.out.print((event.isShiftClick() ? "SHIFT-" : "") + (event.isRightClick() ? "RIGHT" : "LEFT"));
            System.out.print((event.getRawSlot() < event.getView().getTopInventory().getSize() ? "TOP" : "BOT") + "-click");

            if (inventoryItem.getType().equals(Material.AIR) && cursorItem.getType().equals(Material.AIR))
            {
                return; // nothing to log
            }
            if (event.getRawSlot() < event.getView().getTopInventory().getSize()) // click in top inventory
            {
                if (event.isShiftClick()) // top & shift -> remove items
                {
                    if (inventoryItem.getType().equals(Material.AIR)
                    || this.manager.isIgnored(world, ITEM_REMOVE))
                    {
                        return;
                    }
                    int missingSpace = getMissingSpace(event.getView().getTopInventory(),inventoryItem);
                    int amountTake = inventoryItem.getAmount() - missingSpace;
                    if (amountTake > 0)
                    {
                        this.prepareForLogging(user, new ItemData(inventoryItem),-amountTake);
                    }
                }
                else
                {
                    if (cursorItem.getType().equals(Material.AIR)) // remove items
                    {
                        if (this.manager.isIgnored(world, ITEM_REMOVE)) return;
                        int remove = event.isLeftClick() ? inventoryItem.getAmount() : (inventoryItem.getAmount() + 1) / 2;
                        this.prepareForLogging(user, new ItemData(inventoryItem),-remove);
                    }
                    else if (inventoryItem.getType().equals(Material.AIR)) // put items
                    {
                        if (this.manager.isIgnored(world, ITEM_INSERT)) return;
                        int put = event.isLeftClick() ? cursorItem.getAmount() : 1;
                        if (holder instanceof BrewingStand) // handle BrewingStands separatly
                        {
                            if (event.getRawSlot() == 3)
                            {
                                if (!BukkitUtils.canBePlacedInBrewingstand(cursorItem.getType())) // can be put
                                {
                                    return;
                                }
                            }
                            else if (cursorItem.getType().equals(Material.POTION) || cursorItem.getType().equals(Material.GLASS_BOTTLE)) // bottle slot
                            {
                                put = 1;
                            }
                            else
                            {
                                return;
                            }
                        }
                        this.prepareForLogging(user, new ItemData(cursorItem),put);
                    }
                    else
                    {
                        if (inventoryItem.isSimilar(cursorItem))
                        {
                            int put = event.isLeftClick() ? inventoryItem.getAmount() : 1;
                            if (put > inventoryItem.getMaxStackSize() - inventoryItem.getAmount()) //if stack to big
                            {
                                put = inventoryItem.getMaxStackSize() - inventoryItem.getAmount(); //set to missing to fill
                            }
                            if (put == 0)
                                return;
                            if (put > 0)
                            {
                                if (this.manager.isIgnored(world, ITEM_INSERT)) return;
                            }
                            else
                            {
                                if (this.manager.isIgnored(world, ITEM_REMOVE)) return;
                            }
                            this.prepareForLogging(user, new ItemData(inventoryItem),put);
                        }
                        else
                        {
                            if (holder instanceof BrewingStand) // handle BrewingStands separatly
                            {
                                if (event.getRawSlot() == 3)
                                {
                                    if (!BukkitUtils.canBePlacedInBrewingstand(cursorItem.getType())) // can be put
                                    {
                                        return;
                                    }
                                }
                                else if (cursorItem.getType().equals(Material.POTION) || cursorItem.getType().equals(Material.GLASS_BOTTLE)) // bottle slot
                                {
                                    if (cursorItem.getAmount() > 1) return; // nothing happens when more than 1
                                    // else swap items
                                }
                                else
                                {
                                    return;
                                }
                            }
                            if (!this.manager.isIgnored(world, ITEM_INSERT))
                            {
                                this.prepareForLogging(user, new ItemData(cursorItem),cursorItem.getAmount());
                            }
                            if (!this.manager.isIgnored(world, ITEM_REMOVE))
                            {
                                this.prepareForLogging(user, new ItemData(inventoryItem),-inventoryItem.getAmount());
                            }
                        }
                    }
                }
            }
            else if (event.isShiftClick())// click in bottom inventory AND shift -> put | ELSE no container change
            {
                if (inventoryItem.getType().equals(Material.AIR)
                        || this.manager.isIgnored(world, ITEM_INSERT))
                {
                    return;
                }
                if (holder instanceof BrewingStand)
                {
                    BrewerInventory brewerInventory = (BrewerInventory) event.getView().getTopInventory();
                    if (BukkitUtils.canBePlacedInBrewingstand(inventoryItem.getType()))
                    {
                        if (inventoryItem.isSimilar(brewerInventory.getIngredient())) // could fit into inventory
                        {
                            ItemStack brewerItem = brewerInventory.getIngredient();
                            int amountPutIn = inventoryItem.getAmount();
                            if (brewerItem.getAmount() + inventoryItem.getAmount() > inventoryItem.getMaxStackSize())
                            {
                                amountPutIn = inventoryItem.getMaxStackSize() - brewerItem.getAmount();
                                if (amountPutIn <= 0) return;
                            }
                            this.prepareForLogging(user,new ItemData(inventoryItem), amountPutIn);
                        }
                    }
                    else if (inventoryItem.getType().equals(Material.POTION))
                    {
                        for (int i = 0 ; i <= 2; ++i)
                        {
                            ItemStack item = brewerInventory.getItem(i);
                            if (item == null) // space for a potion?
                            {
                                this.prepareForLogging(user,new ItemData(inventoryItem), inventoryItem.getAmount());
                                return;
                            }
                            // else no space found
                        }
                    }
                    else if (inventoryItem.getType().equals(Material.GLASS_BOTTLE))
                    {
                        int bottlesFound = 0;
                        int bottleSlots = 0;
                        for (int i = 0 ; i <= 2; ++i)
                        {
                            ItemStack item = brewerInventory.getItem(i);
                            if (item == null) // space for the stack ?
                            {
                                this.prepareForLogging(user,new ItemData(inventoryItem), inventoryItem.getAmount());
                                return;
                            }
                            else if (item.getType().equals(Material.GLASS_BOTTLE))
                            {
                                bottleSlots++;
                                bottlesFound += item.getAmount();
                            }
                        }
                        if (bottleSlots > 0)
                        {
                            int space = Material.GLASS_BOTTLE.getMaxStackSize() * bottleSlots - bottlesFound;
                            if (space <= 0) return;
                            int putInto = inventoryItem.getAmount();
                            if (putInto > space)
                            {
                                putInto = space;
                            }
                            this.prepareForLogging(user,new ItemData(inventoryItem), putInto);
                        }
                    }
                }
                else if (holder instanceof Furnace)
                {
                    FurnaceInventory furnaceInventory= (FurnaceInventory) event.getView().getTopInventory();
                    int putInto = 0;
                    if (BukkitUtils.isSmeltable(inventoryItem))
                    {
                        ItemStack item = furnaceInventory.getSmelting();
                        if (item == null)
                        {
                            putInto = inventoryItem.getAmount();
                        }
                        else if (inventoryItem.isSimilar(item))
                        {
                            int space = inventoryItem.getMaxStackSize() - item.getAmount();
                            if (space <= 0) return;
                            putInto = inventoryItem.getAmount();
                            if (putInto > space)
                            {
                                putInto = space;
                            }
                        }
                    }
                    else if (BukkitUtils.isFuel(inventoryItem))
                    {
                        ItemStack item = furnaceInventory.getFuel();
                        if (item == null)
                        {
                            putInto = inventoryItem.getAmount();
                        }
                        else if (inventoryItem.isSimilar(item))
                        {
                            int space = inventoryItem.getMaxStackSize() - item.getAmount();
                            if (space <= 0) return;
                            putInto = inventoryItem.getAmount();
                            if (putInto > space)
                            {
                                putInto = space;
                            }
                        }
                    }
                    if (putInto == 0) return;
                    this.prepareForLogging(user,new ItemData(inventoryItem), putInto);
                }
                else
                {
                    event.getView().getTopInventory().getContents();

                    int missingSpace = getMissingSpace(event.getView().getTopInventory(),inventoryItem);
                    int amountPut = inventoryItem.getAmount() - missingSpace;
                    if (amountPut > 0)
                    {
                        this.prepareForLogging(user, new ItemData(inventoryItem),amountPut);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onItemMove(InventoryMoveItemEvent event)
    {
        //TODO
    }

    private void prepareForLogging(User user, ItemData itemData, int amount)
    {
        TObjectIntHashMap<ItemData> itemDataMap = this.inventoryChanges.get(user.key);
        if (itemDataMap == null)
        {
            itemDataMap = new TObjectIntHashMap<ItemData>();
            this.inventoryChanges.put(user.key,itemDataMap);
        }
        int oldAmount = itemDataMap.get(itemData); // if not yet set this returns 0
        itemDataMap.put(itemData,oldAmount + amount);
        System.out.print((amount < 0 ? "TAKE " : "PUT ") + itemData.material.name()+":"+itemData.dura+" x"+amount);//TODO remove this
    }

    public static class ItemData
    {
        public Material material;
        public short dura;
        public String displayName;
        public List<String> lore;
        public Map<Enchantment,Integer> enchantments;

        public ItemData(ItemStack itemStack)
        {
            this.material = itemStack.getType();
            this.dura = itemStack.getDurability();
            if (itemStack.hasItemMeta())
            {
                ItemMeta meta = itemStack.getItemMeta();
                if (meta.hasDisplayName())
                {
                    displayName = meta.getDisplayName();
                }
                if (meta.hasLore())
                {
                    lore = meta.getLore();
                }
                if (meta.hasEnchants())
                {
                    enchantments = meta.getEnchants();
                }
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ItemData itemData = (ItemData) o;

            if (dura != itemData.dura) return false;
            if (displayName != null ? !displayName.equals(itemData.displayName) : itemData.displayName != null)
                return false;
            if (enchantments != null ? !enchantments.equals(itemData.enchantments) : itemData.enchantments != null)
                return false;
            if (lore != null ? !lore.equals(itemData.lore) : itemData.lore != null) return false;
            if (material != itemData.material) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = material != null ? material.hashCode() : 0;
            result = 31 * result + (int) dura;
            result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
            result = 31 * result + (lore != null ? lore.hashCode() : 0);
            result = 31 * result + (enchantments != null ? enchantments.hashCode() : 0);
            return result;
        }
    }
}