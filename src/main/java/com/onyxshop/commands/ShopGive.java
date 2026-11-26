package com.onyxshop.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.commands.base.PluginCommand;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.util.CommandHelper;
import com.onyxshop.util.CommandType;
import com.onyxshop.util.PermissionsCache;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;

public class ShopGive extends PluginCommand {
   public ShopGive(OnyxShop var1, List<String> var2) {
      super(var1, "shopgive", "Give a shop item to a player", "/shopgive <section> <itemIndex> [player] [amount]", "OnyxShop.shopgive", var2);
   }

   @Override
   public boolean execute(CommandSender var1, String var2, String[] var3) {
      if (!PermissionsCache.hasPermission(var1, "OnyxShop.shopgive")) {
         SendMessage.sendMessage(var1, Lang.NO_PERMISSIONS.get());
         return true;
      } else if (ConfigManager.badYaml != null) {
         SendMessage.sendMessage(var1, ChatColor.DARK_RED + "This command cannot be executed now, please fix the configuration formatting first!");
         return true;
      } else if (!this.hasAccessInWorld(var1)) {
         return true;
      } else if (var3.length > 0) {
         if (this.plugin.getShopSections().stream().noneMatch(var1x -> var1x.equals(var3[0]))) {
            SendMessage.sendMessage(var1, var1 instanceof Player ? Lang.NO_SHOP_FOUND.get((Player)var1) : Lang.NO_SHOP_FOUND.get());
            return true;
         } else if (var3.length > 1) {
            if (this.plugin.getSection(var3[0]).getShopItemLocs().stream().noneMatch(var1x -> var1x.equals(var3[1]))) {
               SendMessage.sendMessage(
                  var1,
                  (var1 instanceof Player ? Lang.SECTION_DOES_NOT_CONTAIN_ITEM.get((Player)var1) : Lang.SECTION_DOES_NOT_CONTAIN_ITEM.get())
                     .replace("%shopsection%", var3[0])
                     .replace("%itemLoc%", var3[1])
               );
               return true;
            } else {
               if (var3.length > 2 && !this.isInt(var3[2])) {
                  Player var8 = this.plugin.getServer().getPlayer(var3[2]);
                  if (var8 != null) {
                     ShopItem var9 = this.plugin.getShopItem(var3[0], var3[1]);
                     int var10 = 1;
                     if (var3.length > 3) {
                        var10 = this.getAmount(var3[3]);
                        if (var10 < 1) {
                           SendMessage.sendMessage(var1, var1 instanceof Player ? Lang.NO_VALID_AMOUNT.get((Player)var1) : Lang.NO_VALID_AMOUNT.get());
                           return true;
                        }
                     }

                     if (var9.isBuyCommand()) {
                        CommandHelper.executeCommandItem(var9, var8, CommandType.BUY, "0", var10);
                     }

                     if (!var9.isBuyCommand() || var9.isGiveItem()) {
                        ItemStack var7 = var9.getItemToGive();
                        if (var10 != 1) {
                           var7.setAmount(var10);
                        }

                        SendMessage.sendMessage(
                           var1,
                           (var1 instanceof Player ? Lang.SHOP_ITEM_GIVEN.get((Player)var1) : Lang.SHOP_ITEM_GIVEN.get())
                              .replace("%amount%", String.valueOf(var7.getAmount()))
                              .replace("%section%", var3[0])
                              .replace("%itemIndex%", var3[1])
                              .replace("%material%", var9.getName())
                        );
                        this.giveItem(var8, var7);
                     }
                  } else {
                     SendMessage.warnMessage(var1, var1 instanceof Player ? Lang.PLAYER_NOT_ONLINE.get((Player)var1) : Lang.PLAYER_NOT_ONLINE.get());
                  }
               } else if (var1 instanceof Player) {
                  ShopItem var4 = this.plugin.getShopItem(var3[0], var3[1]);
                  int var5 = 1;
                  if (var3.length > 2) {
                     var5 = this.getAmount(var3[2]);
                     if (var5 < 1) {
                        SendMessage.chatToPlayer((Player)var1, Lang.NO_VALID_AMOUNT.get((Player)var1));
                        return true;
                     }
                  }

                  if (var4.isBuyCommand()) {
                     CommandHelper.executeCommandItem(var4, (Player)var1, CommandType.BUY, "0", var5);
                  }

                  if (!var4.isBuyCommand() || var4.isGiveItem()) {
                     ItemStack var6 = this.plugin.getShopItem(var3[0] + "." + var3[1]).getItemToGive();
                     if (var5 != 1) {
                        var6.setAmount(var5);
                     }

                     SendMessage.sendMessage(
                        var1,
                        Lang.SHOP_ITEM_RECEIVED
                           .get((Player)var1)
                           .replace("%amount%", String.valueOf(var6.getAmount()))
                           .replace("%section%", var3[0])
                           .replace("%itemIndex%", var3[1])
                           .replace("%material%", var4.getName())
                     );
                     this.giveItem((Player)var1, var6);
                  }
               } else {
                  SendMessage.sendMessage(var1, ChatColor.GREEN + "/shopgive <section> <itemIndex> [player] [amount]");
               }

               return true;
            }
         } else {
            SendMessage.sendMessage(var1, ChatColor.GREEN + "/shopgive <section> <itemIndex> [player] [amount]");
            return true;
         }
      } else {
         SendMessage.sendMessage(var1, ChatColor.GREEN + "/shopgive <section> <itemIndex> [player] [amount]");
         return true;
      }
   }

   private void giveItem(Player var1, ItemStack var2) {
      ArrayList var3 = new ArrayList(var1.getInventory().addItem(new ItemStack[]{var2}).values());

      for (int var4 = 0; var4 < var3.size() && var4 != 2; var4++) {
         var1.getWorld().dropItem(var1.getLocation(), (ItemStack)var3.get(var4));
      }
   }

   private ItemStack[] getStacks(ItemStack var1) {
      int var2 = var1.getAmount();
      int var3 = var1.getMaxStackSize();
      double var4 = (double)var2 / var3;
      int var6 = (int)Math.floor(var4);
      int var7 = var4 == Math.floor(var4) ? var6 : (int)Math.ceil(var4);
      int var8 = var2;
      ItemStack[] var9 = new ItemStack[var7];
      if (var6 >= 1) {
         for (int var10 = 0; var10 < var6; var10++) {
            ItemStack var11 = new ItemStack(var1);
            var11.setAmount(var3);
            var9[var10] = var11;
            var8 -= var3;
         }
      }

      if (var8 >= 1) {
         ItemStack var12 = new ItemStack(var1);
         var12.setAmount(var8);
         var9[var7 - 1] = var12;
      }

      return var9;
   }

   private void setAmount(ItemStack var1, String var2) {
      try {
         var1.setAmount(Integer.parseInt(var2));
      } catch (NumberFormatException var4) {
      }
   }

   private int getAmount(String var1) {
      try {
         return Integer.parseInt(var1);
      } catch (NumberFormatException var3) {
         return -1;
      }
   }

   private boolean isLimit(String var1, ItemStack var2) {
      try {
         return Integer.parseInt(var1) > 128;
      } catch (NumberFormatException var4) {
         return true;
      }
   }

   private boolean isInt(String var1) {
      try {
         Integer.parseInt(var1);
         return true;
      } catch (NumberFormatException var3) {
         return false;
      }
   }

   private boolean hasAccessInWorld(CommandSender var1) {
      return !(var1 instanceof Player) ? true : this.hasAccessInWorld((Player)var1);
   }

   @Override
   public List<String> tabComplete(CommandSender var1, String var2, String[] var3) {
      if (!PermissionsCache.hasPermission(var1, "OnyxShop.shopgive")) {
         return Collections.emptyList();
      } else {
         switch (var3.length) {
            case 1:
               if (!var3[0].isEmpty()) {
                  ArrayList var5 = new ArrayList();
                  StringUtil.copyPartialMatches(var3[0], this.plugin.getShopSections(), var5);
                  Collections.sort(var5);
                  return var5;
               }

               return this.plugin.getShopSections();
            case 2:
               if (!this.isSection(var3[0])) {
                  return null;
               } else {
                  if (!var3[1].isEmpty()) {
                     ArrayList var4 = new ArrayList();
                     StringUtil.copyPartialMatches(var3[1], this.plugin.getSection(var3[0]).getShopItemLocs(), var4);
                     Collections.sort(var4);
                     return var4;
                  }

                  return this.plugin.getSection(var3[0]).getShopItemLocs();
               }
            case 3:
               return this.plugin.getServer().getOnlinePlayers().stream().<String>map(Player::getName).collect(Collectors.toList());
            case 4:
               return Arrays.asList("1", "16", "32", "64", "128");
            default:
               return null;
         }
      }
   }

   private boolean isSection(String var1) {
      return this.plugin.getShopSections().contains(var1);
   }
}
