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
   boolean hasData(ItemStack itemStack, String itemId);

   Object emptyList();

   ItemStack setOnyxNbtString(ItemStack itemStack, String itemId, String text);

   String getOnyxNbtString(ItemStack itemStack, String itemId);

   ItemStack setOnyxNbtInt(ItemStack itemStack, String itemId, Integer value);

   Integer getOnyxNbtInt(ItemStack itemStack, String itemId);

   ItemStack setNBTString(ItemStack itemStack, String itemId, String text);

   String getNBTString(ItemStack itemStack, String itemId);

   ItemStack setNBTInt(ItemStack itemStack, String itemId, Integer value);

   Integer getNBTInt(ItemStack itemStack, String itemId);

   String getNBTDataAsString(ItemStack itemStack, boolean flag);

   String toNBT(String text);

   ItemStack setIndex(ItemStack itemStack, Integer index);

   String getLoreAsNBT(ItemStack itemStack);

   Object getNMSLore(ItemStack itemStack);

   ItemStack applyNMSLore(ItemStack itemStack, Object value);

   ItemStack setDisplayname(ItemStack itemStack, String itemId);

   void setItemLore(Object value, String itemId, int value2);

   void addItemLore(Object value, String itemId, int value2);

   void addItemLore(Object value, String itemId);

   Integer getIndex(ItemStack itemStack);

   ItemStack setAction(ItemStack itemStack, String itemId);

   String getAction(ItemStack itemStack);

   ItemStack setEditOption(ItemStack itemStack, String itemId);

   String getEditOption(ItemStack itemStack);

   ItemStack getShopAdminItem(ItemStack itemStack);

   boolean isShopAdminItem(ItemStack itemStack);

   ItemStack addNBTtag(ItemStack itemStack, String itemId, boolean flag) throws ItemLoadException;

   ItemStack addNBTCommands(ItemStack itemStack, List<String> itemIds, CommandType commandType);

   boolean hasNBTCommand(ItemStack itemStack);

   List<String> getNBTCommands(ItemStack itemStack, CommandType commandType);

   ItemStack setDisabledBackButton(ItemStack itemStack);

   boolean isDisabledBackButton(ItemStack itemStack);

   double getBuyPrice(ItemStack itemStack);

   double getSellPrice(ItemStack itemStack);

   ItemStack addNBTPrices(ItemStack itemStack, String itemId);

   ItemStack setSpawnerType(ItemStack itemStack, String type);

   ItemStack setPathToItem(ItemStack itemStack, String path);

   ItemStack setItemErrorToItem(ItemStack itemStack);

   ItemStack setPotionType(ItemStack itemStack, PotionTypes potionTypes);

   Optional<PotionTypes> getBasePotion(ItemStack itemStack);

   ItemStack getSpawnerToGive(EntityType entityType);

   String getPathToItem(ItemStack itemStack);

   Boolean hasItemError(ItemStack itemStack);

   String getSpawnerType(ItemStack itemStack);

   ItemStack getItemInHand(Player player);

   String getTitle(Inventory inventory);

   boolean isSimilar(ItemStack itemStack, ItemStack itemStack2, List<String> itemIds);

   ItemStack setPages(ItemStack itemStack, String itemId);

   String getPages(ItemStack itemStack);

   void updateInventoryTitle(Player player, Translatable text);

   public interface AnvilGUI {
      VersionHandler.AnvilGUI.AnvilContainer newAnvilContainer(Player player, String playerName);

      int getNextContainerId(Player player, Object value);

      void handleInventoryCloseEvent(Player player);

      void sendPacketOpenWindow(Player player, int value, String playerName);

      void sendPacketCloseWindow(Player player, int value);

      void setActiveContainerDefault(Player player);

      void setActiveContainer(Player player, Object value);

      void setActiveContainerId(Object value, int id);

      void addActiveContainerSlotListener(Object value, Player player);

      public interface AnvilContainer {
         Inventory getBukkitInventory();
      }
   }
}
