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

   public static int giveItems(Player var0, ShopItem var1, int var2) {
      int var3 = var1.getItemToGive().getMaxStackSize();
      double var4 = (double)var2 / var3;
      int var6 = (int)Math.floor(var4);
      int var7 = var4 == Math.floor(var4) ? var6 : (int)Math.ceil(var4);
      int var8 = var2;
      ItemStack[] var9 = new ItemStack[var7];
      if (var6 >= 1) {
         for (int var10 = 0; var10 < var6; var10++) {
            ItemStack var11 = new ItemStack(var1.getItemToGive());
            if (!var1.getItgPlaceholders().isEmpty()) {
               PlaceholderAPIHook.parsePlaceholders(var1, var11, var0);
            }

            var11.setAmount(var3);
            var9[var10] = var11;
            var8 -= var3;
         }
      }

      if (var8 >= 1) {
         ItemStack var12 = new ItemStack(var1.getItemToGive());
         if (!var1.getItgPlaceholders().isEmpty()) {
            PlaceholderAPIHook.parsePlaceholders(var1, var12, var0);
         }

         var12.setAmount(var8);
         var9[var7 - 1] = var12;
      }

      for (TaggableItemType var14 : var1.getTaggables()) {
         switch (var14) {
            case EXECUTABLE_ITEMS:
               var9 = Arrays.stream(var9).map(var1x -> ExecutableItemsHook.tag(var1x, var0)).collect(Collectors.toList()).toArray(new ItemStack[var7]);
               break;
            case UPGRADEABLE_SPAWNERS:
               var9 = Arrays.stream(var9).map(var1x -> UpgradeableSpawnersHook.tag(var1x, var0)).collect(Collectors.toList()).toArray(new ItemStack[var7]);
               break;
            case MYTHIC_MOBS_ITEM:
               var9 = Arrays.stream(var9).map(var1x -> MythicMobsHook.tag(var1x, var0)).collect(Collectors.toList()).toArray(new ItemStack[var7]);
               break;
            case CRAFT_ENGINE:
               var9 = Arrays.stream(var9).map(var1x -> CraftEngineHook.tag(var1x, var0)).collect(Collectors.toList()).toArray(new ItemStack[var7]);
         }
      }

      return addItems(var0, var9);
   }

   private static int addItems(Player var0, ItemStack[] var1) {
      HashMap<Integer, ItemStack> var2 = var0.getInventory().addItem(var1);
      int var7 = 0;
      if (!var2.isEmpty()) {
         if (OnyxShop.getInstance().dropItemsOnGround) {
            Location var3 = var0.getLocation();
            World var4 = var0.getWorld();

            for (ItemStack var6 : var2.values()) {
               var4.dropItem(var3, var6);
            }

            SendMessage.chatToPlayer(var0, Lang.NOT_ENOUGH_SPACE_INSIDE_INVENTORY.get());
         } else {
            for (ItemStack var9 : var2.values()) {
               var7 += var9.getAmount();
            }

            SendMessage.chatToPlayer(var0, Lang.COULD_NOT_STORE_ALL_ITEMS.get().replace("%amount%", Integer.toString(var7)));
         }
      }

      var0.updateInventory();
      return var7;
   }

   public static boolean canSell(ShopItem var0, Player var1) {
      if (var0.isSellCommand() && !var0.isRequireItem()) {
         return true;
      } else {
         for (int var2 = 0; var2 < 36; var2++) {
            ItemStack var3 = var1.getInventory().getItem(var2);
            if (var3 != null && !var3.getType().equals(Material.AIR) && var0.match(var3)) {
               return true;
            }
         }

         return false;
      }
   }

   public static ArrayList<ItemStack> getSellAble(ShopItem var0, Player var1, int var2) {
      return getSellAble(var0, var1, var2, true);
   }

   public static ArrayList<ItemStack> getSellAble(ShopItem var0, Player var1, int var2, boolean var3) {
      if (var3 && var0.isMaxSell(var2)) {
         var2 = var0.getMaxSell();
      }

      ArrayList var4 = new ArrayList();

      for (int var5 = 0; var5 < 36; var5++) {
         ItemStack var6 = var1.getInventory().getItem(var5);
         if (var6 != null && !var6.getType().equals(Material.AIR) && var0.match(var6)) {
            if (var2 < var6.getAmount()) {
               ItemStack var7 = new ItemStack(var6);
               var7.setAmount(var2);
               var4.add(var7);
               break;
            }

            var4.add(var6);
            var2 -= var6.getAmount();
         }
      }

      return var4;
   }

   public static boolean canPay(Player var0, EcoType var1, double var2) {
      int var4 = 0;
      int var5 = ((ItemEconomy)plugin.getEcoHandler().getEcon(var1)).getItem().getMaxStackSize();

      for (int var6 = 0; var6 < 36; var6++) {
         ItemStack var7 = var0.getInventory().getItem(var6);
         if (var7 == null || var7.getType().equals(Material.AIR)) {
            var4 += var5;
         } else if (var7.isSimilar(((ItemEconomy)plugin.getEcoHandler().getEcon(var1)).getItem()) && var7.getAmount() < var5) {
            var4 += var5 - var7.getAmount();
         }
      }

      return var4 >= var2;
   }

   public static boolean canBuy(Player var0, ShopItem var1) {
      return var0.getInventory().firstEmpty() != -1
         || Arrays.stream(var0.getInventory().getContents())
            .anyMatch(var1x -> var1x != null && var1x.isSimilar(var1.getItemToGive()) && var1x.getAmount() < var1x.getMaxStackSize());
   }

   public static int getFinalAmount(ShopItem var0, Player var1, int var2) {
      return getFinalAmount(var0, var1, var2, true);
   }

   public static int getFinalAmount(ShopItem var0, Player var1, int var2, boolean var3) {
      int var4 = 0;
      int var5 = var0.getItemToGive().getMaxStackSize();
      List var6 = Arrays.asList(var1.getInventory().getContents());

      for (int var7 = 0; var7 < 36; var7++) {
         ItemStack var8 = (ItemStack)var6.get(var7);
         if (var8 == null || var8.getType().equals(Material.AIR)) {
            var4 += var5;
         } else if (var8.isSimilar(var0.getItemToGive()) && var8.getAmount() < var5) {
            var4 += var5 - var8.getAmount();
         }
      }

      if (var3 && var0.isMaxBuy(var4)) {
         var4 = var0.getMaxBuy();
      }

      return !plugin.dropItemsOnGround ? Math.min(var2, var4) : var2;
   }

   public static String getRawDisplayName(Material var0, ItemMeta var1) {
      if (var1.hasDisplayName()) {
         return plugin.getMetaUtils().getRawDisplayName(var1);
      } else {
         return ServerInfo.supportsComponents() && var1.hasItemName() ? plugin.getMetaUtils().getRawItemName(var1) : plugin.getMaterialName(var0.name());
      }
   }

   public static String getDisplayName(ItemMeta var0) {
      if (var0.hasDisplayName()) {
         return plugin.getMetaUtils().getRawDisplayName(var0);
      } else {
         return ServerInfo.supportsComponents() && var0.hasItemName() ? plugin.getMetaUtils().getRawItemName(var0) : null;
      }
   }

   public static boolean hasCustomName(ItemMeta var0) {
      return var0.hasDisplayName() || ServerInfo.supportsComponents() && var0.hasItemName();
   }
}
