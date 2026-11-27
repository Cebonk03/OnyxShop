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
   public ShopGive(OnyxShop plugin, List<String> shopIds) {
      super(plugin, "shopgive", "Give a shop item to a player", "/shopgive <section> <itemIndex> [player] [amount]", "OnyxShop.shopgive", shopIds);
   }

   @Override
   public boolean execute(CommandSender sender, String command, String[] values) {
      if (!PermissionsCache.hasPermission(sender, "OnyxShop.shopgive")) {
         SendMessage.sendMessage(sender, Lang.NO_PERMISSIONS.get());
         return true;
      } else if (ConfigManager.badYaml != null) {
         SendMessage.sendMessage(sender, ChatColor.DARK_RED + "This command cannot be executed now, please fix the configuration formatting first!");
         return true;
      } else if (!this.hasAccessInWorld(sender)) {
         return true;
      } else if (values.length > 0) {
         if (this.plugin.getShopSections().stream().noneMatch(element -> element.equals(values[0]))) {
            SendMessage.sendMessage(sender, sender instanceof Player ? Lang.NO_SHOP_FOUND.get((Player)sender) : Lang.NO_SHOP_FOUND.get());
            return true;
         } else if (values.length > 1) {
            if (this.plugin.getSection(values[0]).getShopItemLocs().stream().noneMatch(element -> element.equals(values[1]))) {
               SendMessage.sendMessage(
                  sender,
                  (sender instanceof Player ? Lang.SECTION_DOES_NOT_CONTAIN_ITEM.get((Player)sender) : Lang.SECTION_DOES_NOT_CONTAIN_ITEM.get())
                     .replace("%shopsection%", values[0])
                     .replace("%itemLoc%", values[1])
               );
               return true;
            } else {
               if (values.length > 2 && !this.isInt(values[2])) {
                  Player player = this.plugin.getServer().getPlayer(values[2]);
                  if (player != null) {
                     ShopItem shopItem = this.plugin.getShopItem(values[0], values[1]);
                     int value = 1;
                     if (values.length > 3) {
                        value = this.getAmount(values[3]);
                        if (value < 1) {
                           SendMessage.sendMessage(sender, sender instanceof Player ? Lang.NO_VALID_AMOUNT.get((Player)sender) : Lang.NO_VALID_AMOUNT.get());
                           return true;
                        }
                     }

                     if (shopItem.isBuyCommand()) {
                        CommandHelper.executeCommandItem(shopItem, player, CommandType.BUY, "0", value);
                     }

                     if (!shopItem.isBuyCommand() || shopItem.isGiveItem()) {
                        ItemStack itemStack = shopItem.getItemToGive();
                        if (value != 1) {
                           itemStack.setAmount(value);
                        }

                        SendMessage.sendMessage(
                           sender,
                           (sender instanceof Player ? Lang.SHOP_ITEM_GIVEN.get((Player)sender) : Lang.SHOP_ITEM_GIVEN.get())
                              .replace("%amount%", String.valueOf(itemStack.getAmount()))
                              .replace("%section%", values[0])
                              .replace("%itemIndex%", values[1])
                              .replace("%material%", shopItem.getName())
                        );
                        this.giveItem(player, itemStack);
                     }
                  } else {
                     SendMessage.warnMessage(sender, sender instanceof Player ? Lang.PLAYER_NOT_ONLINE.get((Player)sender) : Lang.PLAYER_NOT_ONLINE.get());
                  }
               } else if (sender instanceof Player) {
                  ShopItem shopItem = this.plugin.getShopItem(values[0], values[1]);
                  int value = 1;
                  if (values.length > 2) {
                     value = this.getAmount(values[2]);
                     if (value < 1) {
                        SendMessage.chatToPlayer((Player)sender, Lang.NO_VALID_AMOUNT.get((Player)sender));
                        return true;
                     }
                  }

                  if (shopItem.isBuyCommand()) {
                     CommandHelper.executeCommandItem(shopItem, (Player)sender, CommandType.BUY, "0", value);
                  }

                  if (!shopItem.isBuyCommand() || shopItem.isGiveItem()) {
                     ItemStack itemStack = this.plugin.getShopItem(values[0] + "." + values[1]).getItemToGive();
                     if (value != 1) {
                        itemStack.setAmount(value);
                     }

                     SendMessage.sendMessage(
                        sender,
                        Lang.SHOP_ITEM_RECEIVED
                           .get((Player)sender)
                           .replace("%amount%", String.valueOf(itemStack.getAmount()))
                           .replace("%section%", values[0])
                           .replace("%itemIndex%", values[1])
                           .replace("%material%", shopItem.getName())
                     );
                     this.giveItem((Player)sender, itemStack);
                  }
               } else {
                  SendMessage.sendMessage(sender, ChatColor.GREEN + "/shopgive <section> <itemIndex> [player] [amount]");
               }

               return true;
            }
         } else {
            SendMessage.sendMessage(sender, ChatColor.GREEN + "/shopgive <section> <itemIndex> [player] [amount]");
            return true;
         }
      } else {
         SendMessage.sendMessage(sender, ChatColor.GREEN + "/shopgive <section> <itemIndex> [player] [amount]");
         return true;
      }
   }

   private void giveItem(Player player, ItemStack itemStack) {
      ArrayList itemIds = new ArrayList(player.getInventory().addItem(new ItemStack[]{itemStack}).values());

      for (int size = 0; size < itemIds.size() && size != 2; size++) {
         player.getWorld().dropItem(player.getLocation(), (ItemStack)itemIds.get(size));
      }
   }

   private ItemStack[] getStacks(ItemStack itemStack) {
      int amount = itemStack.getAmount();
      int maxStackSize = itemStack.getMaxStackSize();
      double amount2 = (double)amount / maxStackSize;
      int value = (int)Math.floor(amount2);
      int value2 = amount2 == Math.floor(amount2) ? value : (int)Math.ceil(amount2);
      int value3 = amount;
      ItemStack[] itemStack2 = new ItemStack[value2];
      if (value >= 1) {
         for (int index = 0; index < value; index++) {
            ItemStack itemStack3 = new ItemStack(itemStack);
            itemStack3.setAmount(maxStackSize);
            itemStack2[index] = itemStack3;
            value3 -= maxStackSize;
         }
      }

      if (value3 >= 1) {
         ItemStack itemStack3 = new ItemStack(itemStack);
         itemStack3.setAmount(value3);
         itemStack2[value2 - 1] = itemStack3;
      }

      return itemStack2;
   }

   private void setAmount(ItemStack itemStack, String itemId) {
      try {
         itemStack.setAmount(Integer.parseInt(itemId));
      } catch (NumberFormatException exception) {
      }
   }

   private int getAmount(String text) {
      try {
         return Integer.parseInt(text);
      } catch (NumberFormatException exception) {
         return -1;
      }
   }

   private boolean isLimit(String itemId, ItemStack itemStack) {
      try {
         return Integer.parseInt(itemId) > 128;
      } catch (NumberFormatException exception) {
         return true;
      }
   }

   private boolean isInt(String text) {
      try {
         Integer.parseInt(text);
         return true;
      } catch (NumberFormatException exception) {
         return false;
      }
   }

   private boolean hasAccessInWorld(CommandSender sender) {
      return !(sender instanceof Player) ? true : this.hasAccessInWorld((Player)sender);
   }

   @Override
   public List<String> tabComplete(CommandSender sender, String command, String[] values) {
      if (!PermissionsCache.hasPermission(sender, "OnyxShop.shopgive")) {
         return Collections.emptyList();
      } else {
         switch (values.length) {
            case 1:
               if (!values[0].isEmpty()) {
                  ArrayList values2 = new ArrayList();
                  StringUtil.copyPartialMatches(values[0], this.plugin.getShopSections(), values2);
                  Collections.sort(values2);
                  return values2;
               }

               return this.plugin.getShopSections();
            case 2:
               if (!this.isSection(values[0])) {
                  return null;
               } else {
                  if (!values[1].isEmpty()) {
                     ArrayList values2 = new ArrayList();
                     StringUtil.copyPartialMatches(values[1], this.plugin.getSection(values[0]).getShopItemLocs(), values2);
                     Collections.sort(values2);
                     return values2;
                  }

                  return this.plugin.getSection(values[0]).getShopItemLocs();
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

   private boolean isSection(String sectionId) {
      return this.plugin.getShopSections().contains(sectionId);
   }
}
