package com.onyxshop;

import java.util.List;
import java.util.Optional;
import com.onyxshop.files.Translatable;
import com.onyxshop.util.CommandType;
import com.onyxshop.util.PotionTypes;
import com.onyxshop.util.exceptions.ItemLoadException;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public interface VersionHandler {
   boolean hasData(ItemStack var1, String var2);

   Object emptyList();

   ItemStack setOnyxNbtString(ItemStack var1, String var2, String var3);

   String getOnyxNbtString(ItemStack var1, String var2);

   ItemStack setOnyxNbtInt(ItemStack var1, String var2, Integer var3);

   Integer getOnyxNbtInt(ItemStack var1, String var2);

   ItemStack setNBTString(ItemStack var1, String var2, String var3);

   String getNBTString(ItemStack var1, String var2);

   ItemStack setNBTInt(ItemStack var1, String var2, Integer var3);

   Integer getNBTInt(ItemStack var1, String var2);

   String getNBTDataAsString(ItemStack var1, boolean var2);

   String toNBT(String var1);

   ItemStack setIndex(ItemStack var1, Integer var2);

   String getLoreAsNBT(ItemStack var1);

   Object getNMSLore(ItemStack var1);

   ItemStack applyNMSLore(ItemStack var1, Object var2);

   ItemStack setDisplayname(ItemStack var1, String var2);

   void setItemLore(Object var1, String var2, int var3);

   void addItemLore(Object var1, String var2, int var3);

   void addItemLore(Object var1, String var2);

   Integer getIndex(ItemStack var1);

   ItemStack setAction(ItemStack var1, String var2);

   String getAction(ItemStack var1);

   ItemStack setEditOption(ItemStack var1, String var2);

   String getEditOption(ItemStack var1);

   ItemStack getShopAdminItem(ItemStack var1);

   boolean isShopAdminItem(ItemStack var1);

   ItemStack addNBTtag(ItemStack var1, String var2, boolean var3) throws ItemLoadException;

   ItemStack addNBTCommands(ItemStack var1, List<String> var2, CommandType var3);

   boolean hasNBTCommand(ItemStack var1);

   List<String> getNBTCommands(ItemStack var1, CommandType var2);

   ItemStack setDisabledBackButton(ItemStack var1);

   boolean isDisabledBackButton(ItemStack var1);

   double getBuyPrice(ItemStack var1);

   double getSellPrice(ItemStack var1);

   ItemStack addNBTPrices(ItemStack var1, String var2);

   ItemStack setSpawnerType(ItemStack var1, String var2);

   ItemStack setPathToItem(ItemStack var1, String var2);

   ItemStack setItemErrorToItem(ItemStack var1);

   ItemStack setPotionType(ItemStack var1, PotionTypes var2);

   Optional<PotionTypes> getBasePotion(ItemStack var1);

   ItemStack getSpawnerToGive(EntityType var1);

   String getPathToItem(ItemStack var1);

   Boolean hasItemError(ItemStack var1);

   String getSpawnerType(ItemStack var1);

   ItemStack getItemInHand(Player var1);

   String getTitle(Inventory var1);

   boolean isSimilar(ItemStack var1, ItemStack var2, List<String> var3);

   ItemStack setPages(ItemStack var1, String var2);

   String getPages(ItemStack var1);

   void updateInventoryTitle(Player var1, Translatable var2);

   public interface AnvilGUI {
      VersionHandler.AnvilGUI.AnvilContainer newAnvilContainer(Player var1, String var2);

      int getNextContainerId(Player var1, Object var2);

      void handleInventoryCloseEvent(Player var1);

      void sendPacketOpenWindow(Player var1, int var2, String var3);

      void sendPacketCloseWindow(Player var1, int var2);

      void setActiveContainerDefault(Player var1);

      void setActiveContainer(Player var1, Object var2);

      void setActiveContainerId(Object var1, int var2);

      void addActiveContainerSlotListener(Object var1, Player var2);

      public interface AnvilContainer {
         Inventory getBukkitInventory();
      }
   }
}
