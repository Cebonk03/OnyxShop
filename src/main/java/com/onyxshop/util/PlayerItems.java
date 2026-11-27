package com.onyxshop.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.providers.PlaceholderAPIHook;
import com.onyxshop.providers.economys.ItemEconomy;
import com.onyxshop.providers.items.CraftEngineHook;
import com.onyxshop.providers.items.ExecutableItemsHook;
import com.onyxshop.providers.items.MythicMobsHook;
import com.onyxshop.providers.spawners.UpgradeableSpawnersHook;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class PlayerItems {
   private static final OnyxShop plugin = OnyxShop.getInstance();

   public PlayerItems() {
   }

   public static int giveItems(Player player, ShopItem shopItem, int value) {
      int maxStackSize = shopItem.getItemToGive().getMaxStackSize();
      double amount = (double)value / maxStackSize;
      int value2 = (int)Math.floor(amount);
      int value3 = amount == Math.floor(amount) ? value2 : (int)Math.ceil(amount);
      int value4 = value;
      ItemStack[] itemStack = new ItemStack[value3];
      if (value2 >= 1) {
         for (int index = 0; index < value2; index++) {
            ItemStack itemStack2 = new ItemStack(shopItem.getItemToGive());
            if (!shopItem.getItgPlaceholders().isEmpty()) {
               PlaceholderAPIHook.parsePlaceholders(shopItem, itemStack2, player);
            }

            itemStack2.setAmount(maxStackSize);
            itemStack[index] = itemStack2;
            value4 -= maxStackSize;
         }
      }

      if (value4 >= 1) {
         ItemStack itemStack2 = new ItemStack(shopItem.getItemToGive());
         if (!shopItem.getItgPlaceholders().isEmpty()) {
            PlaceholderAPIHook.parsePlaceholders(shopItem, itemStack2, player);
         }

         itemStack2.setAmount(value4);
         itemStack[value3 - 1] = itemStack2;
      }

      for (TaggableItemType taggableItemType : shopItem.getTaggables()) {
         switch (taggableItemType) {
            case EXECUTABLE_ITEMS:
               itemStack = Arrays.stream(itemStack).map(element -> ExecutableItemsHook.tag(element, player)).collect(Collectors.toList()).toArray(new ItemStack[value3]);
               break;
            case UPGRADEABLE_SPAWNERS:
               itemStack = Arrays.stream(itemStack).map(element -> UpgradeableSpawnersHook.tag(element, player)).collect(Collectors.toList()).toArray(new ItemStack[value3]);
               break;
            case MYTHIC_MOBS_ITEM:
               itemStack = Arrays.stream(itemStack).map(element -> MythicMobsHook.tag(element, player)).collect(Collectors.toList()).toArray(new ItemStack[value3]);
               break;
            case CRAFT_ENGINE:
               itemStack = Arrays.stream(itemStack).map(element -> CraftEngineHook.tag(element, player)).collect(Collectors.toList()).toArray(new ItemStack[value3]);
         }
      }

      return addItems(player, itemStack);
   }

   private static int addItems(Player player, ItemStack[] playerNames) {
      HashMap<Integer, ItemStack> map = player.getInventory().addItem(playerNames);
      int value = 0;
      if (!map.isEmpty()) {
         if (OnyxShop.getInstance().dropItemsOnGround) {
            Location location = player.getLocation();
            World world = player.getWorld();

            for (ItemStack itemStack : map.values()) {
               world.dropItem(location, itemStack);
            }

            SendMessage.chatToPlayer(player, Lang.NOT_ENOUGH_SPACE_INSIDE_INVENTORY.get());
         } else {
            for (ItemStack itemStack : map.values()) {
               value += itemStack.getAmount();
            }

            SendMessage.chatToPlayer(player, Lang.COULD_NOT_STORE_ALL_ITEMS.get().replace("%amount%", Integer.toString(value)));
         }
      }

      player.updateInventory();
      return value;
   }

   public static boolean canSell(ShopItem shopItem, Player player) {
      if (shopItem.isSellCommand() && !shopItem.isRequireItem()) {
         return true;
      } else {
         for (int index = 0; index < 36; index++) {
            ItemStack itemStack = player.getInventory().getItem(index);
            if (itemStack != null && !itemStack.getType().equals(Material.AIR) && shopItem.match(itemStack)) {
               return true;
            }
         }

         return false;
      }
   }

   public static ArrayList<ItemStack> getSellAble(ShopItem shopItem, Player player, int value) {
      return getSellAble(shopItem, player, value, true);
   }

   public static ArrayList<ItemStack> getSellAble(ShopItem shopItem, Player player, int value, boolean flag) {
      if (flag && shopItem.isMaxSell(value)) {
         value = shopItem.getMaxSell();
      }

      ArrayList values = new ArrayList();

      for (int index = 0; index < 36; index++) {
         ItemStack itemStack = player.getInventory().getItem(index);
         if (itemStack != null && !itemStack.getType().equals(Material.AIR) && shopItem.match(itemStack)) {
            if (value < itemStack.getAmount()) {
               ItemStack itemStack2 = new ItemStack(itemStack);
               itemStack2.setAmount(value);
               values.add(itemStack2);
               break;
            }

            values.add(itemStack);
            value -= itemStack.getAmount();
         }
      }

      return values;
   }

   public static boolean canPay(Player player, EcoType ecoType, double amount) {
      int value = 0;
      int maxStackSize = ((ItemEconomy)plugin.getEcoHandler().getEcon(ecoType)).getItem().getMaxStackSize();

      for (int index = 0; index < 36; index++) {
         ItemStack itemStack = player.getInventory().getItem(index);
         if (itemStack == null || itemStack.getType().equals(Material.AIR)) {
            value += maxStackSize;
         } else if (itemStack.isSimilar(((ItemEconomy)plugin.getEcoHandler().getEcon(ecoType)).getItem()) && itemStack.getAmount() < maxStackSize) {
            value += maxStackSize - itemStack.getAmount();
         }
      }

      return value >= amount;
   }

   public static boolean canBuy(Player player, ShopItem shopItem) {
      return player.getInventory().firstEmpty() != -1
         || Arrays.stream(player.getInventory().getContents())
            .anyMatch(element -> element != null && element.isSimilar(shopItem.getItemToGive()) && element.getAmount() < element.getMaxStackSize());
   }

   public static int getFinalAmount(ShopItem shopItem, Player player, int amount) {
      return getFinalAmount(shopItem, player, amount, true);
   }

   public static int getFinalAmount(ShopItem shopItem, Player player, int amount, boolean flag) {
      int value = 0;
      int maxStackSize = shopItem.getItemToGive().getMaxStackSize();
      List values = Arrays.asList(player.getInventory().getContents());

      for (int index = 0; index < 36; index++) {
         ItemStack itemStack = (ItemStack)values.get(index);
         if (itemStack == null || itemStack.getType().equals(Material.AIR)) {
            value += maxStackSize;
         } else if (itemStack.isSimilar(shopItem.getItemToGive()) && itemStack.getAmount() < maxStackSize) {
            value += maxStackSize - itemStack.getAmount();
         }
      }

      if (flag && shopItem.isMaxBuy(value)) {
         value = shopItem.getMaxBuy();
      }

      return !plugin.dropItemsOnGround ? Math.min(amount, value) : amount;
   }

   public static String getRawDisplayName(Material material, ItemMeta itemMeta) {
      if (itemMeta.hasDisplayName()) {
         return plugin.getMetaUtils().getRawDisplayName(itemMeta);
      } else {
         return ServerInfo.supportsComponents() && itemMeta.hasItemName() ? plugin.getMetaUtils().getRawItemName(itemMeta) : plugin.getMaterialName(material.name());
      }
   }

   public static String getDisplayName(ItemMeta itemMeta) {
      if (itemMeta.hasDisplayName()) {
         return plugin.getMetaUtils().getRawDisplayName(itemMeta);
      } else {
         return ServerInfo.supportsComponents() && itemMeta.hasItemName() ? plugin.getMetaUtils().getRawItemName(itemMeta) : null;
      }
   }

   public static boolean hasCustomName(ItemMeta itemMeta) {
      return itemMeta.hasDisplayName() || ServerInfo.supportsComponents() && itemMeta.hasItemName();
   }
}
