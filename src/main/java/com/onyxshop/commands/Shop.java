package com.onyxshop.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import com.onyxshop.OnyxShop;
import com.onyxshop.commands.base.PluginCommands;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.MainMenu;
import com.onyxshop.objects.ShopSearch;
import com.onyxshop.objects.TransactionMenu;
import com.onyxshop.objects.shops.ShopSection;
import com.onyxshop.providers.TitleManager;
import com.onyxshop.util.AnvilGUI;
import com.onyxshop.util.BackButton;
import com.onyxshop.util.PermissionsCache;
import com.onyxshop.util.Transaction;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;

public class Shop extends PluginCommands {
   public Shop(OnyxShop var1, List<String> var2, List<String> var3) {
      super(var1, (String)var2.get(0), "Opens the server shop", "/" + (String)var2.remove(0) + " <section/search>", "OnyxShop.shop", var2, var3);
   }

   @Override
   public boolean execute(CommandSender var1, String var2, String[] var3) {
      if (ConfigManager.badYaml != null) {
         SendMessage.sendMessage(var1, ChatColor.DARK_RED + "This command cannot be executed now, please fix the configuration formatting first!");
         return true;
      } else {
         if (var1 instanceof Player) {
            Player var4 = (Player)var1;
            if (this.canUse(var4)) {
               if (var3.length == 0) {
                  MainMenu.open(var4);
                  this.plugin.getSoundEffects().openMainMenu(var4);
               } else if (this.plugin.shopSearch && var3[0].equalsIgnoreCase("search")) {
                  if (PermissionsCache.hasPermission(var4, "OnyxShop.shop.search")) {
                     if (var3.length == 1) {
                        new AnvilGUI.Builder()
                           .onComplete((var0, var1x) -> {
                              new ShopSearch(var0, var1x);
                              return AnvilGUI.Response.close();
                           })
                           .onClose(var1x -> OnyxShop.getInstance().runTaskLater(var4::updateInventory, 1L))
                           .itemLeft(new ItemStack(Material.NAME_TAG))
                           .text(Lang.SHOPSEARCH_PRE_ENTERED_VALUE.get(var4).getLegacy())
                           .title(TitleManager.get(TitleManager.Menu.SHOPSEARCH, var4).getLegacy())
                           .plugin(OnyxShop.getInstance())
                           .open(var4);
                     } else {
                        new ShopSearch(var4, var3[1]);
                     }
                  } else {
                     SendMessage.chatToPlayer(var4, Lang.NO_PERMISSIONS.get(var4));
                  }
               } else if (this.plugin.getShopSections().contains(var3[0]) && !this.plugin.getSection(var3[0]).isSubSection()) {
                  if (PermissionsCache.hasPermission(var4, "OnyxShop.shop." + var3[0])) {
                     if (var3.length == 2) {
                        try {
                           int var5 = Integer.parseInt(var3[1]);
                           ShopSection var6 = this.plugin.getSection(var3[0]);
                           if (var6.getShopPageItems(var5) == null) {
                              throw new NumberFormatException();
                           }

                           var6.openShopSection(var4, var5, BackButton.DIRECT);
                        } catch (NumberFormatException var9) {
                           SendMessage.warnMessage(var4, Lang.NO_SHOP_PAGE_FOUND.get(var4).replace("%page%", var3[1]));
                        }
                     } else {
                        this.plugin.getSection(var3[0]).openShopSection(var4, BackButton.DIRECT);
                     }
                  } else {
                     SendMessage.chatToPlayer(var4, Lang.NO_PERMISSIONS_TO_OPEN_SHOP.get(var4));
                  }
               } else {
                  SendMessage.chatToPlayer(var4, Lang.NO_SHOP_FOUND.get(var4));
               }
            }
         } else if (var1 instanceof ConsoleCommandSender || var1 instanceof BlockCommandSender || var1 instanceof RemoteConsoleCommandSender) {
            if (var3.length == 0) {
               SendMessage.warnMessage(var1, ChatColor.RED + "Not enough params: /shop <section/search> <player> [page/searchParam]");
            } else if (var3.length == 1) {
               Player var10 = this.plugin.getServer().getPlayer(var3[0]);
               if (var10 != null) {
                  MainMenu.open(var10);
                  this.plugin.getSoundEffects().openMainMenu(var10);
               } else {
                  SendMessage.warnMessage(var1, Lang.PLAYER_NOT_ONLINE.get());
               }
            } else {
               if (var3.length < 2) {
                  return false;
               }

               Player var11 = this.plugin.getServer().getPlayer(var3[1]);
               if (var11 != null) {
                  if (this.plugin.shopSearch && var3[0].equalsIgnoreCase("search")) {
                     if (var3.length == 2) {
                        new AnvilGUI.Builder()
                           .onComplete((var0, var1x) -> {
                              new ShopSearch(var0, var1x);
                              return AnvilGUI.Response.close();
                           })
                           .onClose(var0 -> OnyxShop.getInstance().runTaskLater(var0::updateInventory, 1L))
                           .itemLeft(new ItemStack(Material.NAME_TAG))
                           .text(Lang.SHOPSEARCH_PRE_ENTERED_VALUE.get(var11).getLegacy())
                           .title(TitleManager.get(TitleManager.Menu.SHOPSEARCH, var11).getLegacy())
                           .plugin(OnyxShop.getInstance())
                           .open(var11);
                     } else {
                        new ShopSearch(var11, var3[2]);
                     }
                  } else if (this.plugin.getShopSections().contains(var3[0])) {
                     if (var3.length >= 3) {
                        if (var3[2].matches("-?\\d+")) {
                           try {
                              int var12 = Integer.parseInt(var3[2]);
                              ShopSection var14 = this.plugin.getSection(var3[0]);
                              if (var14.getShopPageItems(var12) == null) {
                                 throw new NumberFormatException();
                              }

                              var14.openShopSection(var11, var12, BackButton.DIRECT_CONSOLE);
                           } catch (NumberFormatException var8) {
                              SendMessage.warnMessage(var1, Lang.NO_SHOP_PAGE_FOUND.get().replace("%page%", var3[2]));
                           }
                        } else if (this.plugin.getSection(var3[0]).getItemLocs().contains(var3[2])) {
                           if (var3.length >= 4) {
                              try {
                                 Transaction.Mode var13 = Transaction.Mode.valueOf(var3[3].toUpperCase());
                                 new TransactionMenu(
                                       var11,
                                       this.plugin.getShopItem(var3[0], var3[2]),
                                       null,
                                       BackButton.DIRECT_CONSOLE,
                                       var13,
                                       var13 == Transaction.Mode.BUY ? Transaction.Type.BUY_SCREEN : Transaction.Type.SELL_SCREEN,
                                       1
                                    )
                                    .open(false);
                              } catch (IllegalArgumentException var7) {
                                 SendMessage.warnMessage(var1, ChatColor.RED + "Invalid transaction mode, should be buy/sell");
                              }
                           } else {
                              SendMessage.warnMessage(var1, ChatColor.RED + "/shop <section> <player> <item> <buy/sell>");
                           }
                        } else {
                           SendMessage.warnMessage(
                              var1, Lang.SECTION_DOES_NOT_CONTAIN_ITEM.get().replace("%shopsection%", var3[0]).replace("%itemLoc%", var3[2])
                           );
                        }
                     } else {
                        this.plugin.getSection(var3[0]).openShopSection(var11, BackButton.DIRECT_CONSOLE);
                     }
                  } else {
                     SendMessage.warnMessage(var1, Lang.NO_SHOP_FOUND.get());
                  }
               } else {
                  SendMessage.warnMessage(var1, Lang.PLAYER_NOT_ONLINE.get());
               }
            }
         }

         return true;
      }
   }

   @Override
   public List<String> tabComplete(CommandSender var1, String var2, String[] var3) {
      if (!PermissionsCache.hasPermission(var1, "OnyxShop.shop")) {
         return Collections.emptyList();
      } else if (var3.length == 1) {
         List var6 = this.plugin
            .getSections()
            .values()
            .stream()
            .filter(
               var1x -> !var1x.isSubSection() && !var1x.isCommandItem() && PermissionsCache.hasPermission(var1, "OnyxShop.shop." + var1x.getSection())
            )
            .map(ShopSection::getSection)
            .collect(Collectors.toList());
         if (this.plugin.shopSearch) {
            var6.add("search");
         }

         if (!var3[0].isEmpty()) {
            ArrayList var5 = new ArrayList();
            StringUtil.copyPartialMatches(var3[0], var6, var5);
            Collections.sort(var5);
            return var5;
         } else {
            return var6;
         }
      } else {
         if (var3.length == 2 && !var3[0].equalsIgnoreCase("search")) {
            ShopSection var4 = this.plugin.getSection(var3[0]);
            if (var4 != null) {
               return IntStream.rangeClosed(1, var4.getPages()).mapToObj(String::valueOf).collect(Collectors.toList());
            }
         }

         return Collections.emptyList();
      }
   }
}
