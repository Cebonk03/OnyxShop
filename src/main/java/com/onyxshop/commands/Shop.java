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
   public Shop(OnyxShop plugin, List<String> shopIds, List<String> values) {
      super(plugin, (String)shopIds.get(0), "Opens the server shop", "/" + (String)shopIds.remove(0) + " <section/search>", "OnyxShop.shop", shopIds, values);
   }

   @Override
   public boolean execute(CommandSender sender, String command, String[] values) {
      if (ConfigManager.badYaml != null) {
         SendMessage.sendMessage(sender, ChatColor.DARK_RED + "This command cannot be executed now, please fix the configuration formatting first!");
         return true;
      } else {
         if (sender instanceof Player) {
            Player player = (Player)sender;
            if (this.canUse(player)) {
               if (values.length == 0) {
                  MainMenu.open(player);
                  this.plugin.getSoundEffects().openMainMenu(player);
               } else if (this.plugin.shopSearch && values[0].equalsIgnoreCase("search")) {
                  if (PermissionsCache.hasPermission(player, "OnyxShop.shop.search")) {
                     if (values.length == 1) {
                        new AnvilGUI.Builder()
                           .onComplete((player2, input) -> {
                              new ShopSearch(player2, input);
                              return AnvilGUI.Response.close();
                           })
                           .onClose(player2 -> OnyxShop.getInstance().runTaskLaterFor(player::updateInventory, player, 1L))
                           .itemLeft(new ItemStack(Material.NAME_TAG))
                           .text(Lang.SHOPSEARCH_PRE_ENTERED_VALUE.get(player).getLegacy())
                           .title(TitleManager.get(TitleManager.Menu.SHOPSEARCH, player).getLegacy())
                           .plugin(OnyxShop.getInstance())
                           .open(player);
                     } else {
                        new ShopSearch(player, values[1]);
                     }
                  } else {
                     SendMessage.chatToPlayer(player, Lang.NO_PERMISSIONS.get(player));
                  }
               } else if (this.plugin.getShopSections().contains(values[0]) && !this.plugin.getSection(values[0]).isSubSection()) {
                  if (PermissionsCache.hasPermission(player, "OnyxShop.shop." + values[0])) {
                     if (values.length == 2) {
                        try {
                           int value = Integer.parseInt(values[1]);
                           ShopSection section = this.plugin.getSection(values[0]);
                           if (section.getShopPageItems(value) == null) {
                              throw new NumberFormatException();
                           }

                           section.openShopSection(player, value, BackButton.DIRECT);
                        } catch (NumberFormatException exception) {
                           SendMessage.warnMessage(player, Lang.NO_SHOP_PAGE_FOUND.get(player).replace("%page%", values[1]));
                        }
                     } else {
                        this.plugin.getSection(values[0]).openShopSection(player, BackButton.DIRECT);
                     }
                  } else {
                     SendMessage.chatToPlayer(player, Lang.NO_PERMISSIONS_TO_OPEN_SHOP.get(player));
                  }
               } else {
                  SendMessage.chatToPlayer(player, Lang.NO_SHOP_FOUND.get(player));
               }
            }
         } else if (sender instanceof ConsoleCommandSender || sender instanceof BlockCommandSender || sender instanceof RemoteConsoleCommandSender) {
            if (values.length == 0) {
               SendMessage.warnMessage(sender, ChatColor.RED + "Not enough params: /shop <section/search> <player> [page/searchParam]");
            } else if (values.length == 1) {
               Player player = this.plugin.getServer().getPlayer(values[0]);
               if (player != null) {
                  MainMenu.open(player);
                  this.plugin.getSoundEffects().openMainMenu(player);
               } else {
                  SendMessage.warnMessage(sender, Lang.PLAYER_NOT_ONLINE.get());
               }
            } else {
               if (values.length < 2) {
                  return false;
               }

               Player player = this.plugin.getServer().getPlayer(values[1]);
               if (player != null) {
                  if (this.plugin.shopSearch && values[0].equalsIgnoreCase("search")) {
                     if (values.length == 2) {
                        new AnvilGUI.Builder()
                           .onComplete((player2, input) -> {
                              new ShopSearch(player2, input);
                              return AnvilGUI.Response.close();
                           })
                           .onClose(player2 -> OnyxShop.getInstance().runTaskLaterFor(player2::updateInventory, player2, 1L))
                           .itemLeft(new ItemStack(Material.NAME_TAG))
                           .text(Lang.SHOPSEARCH_PRE_ENTERED_VALUE.get(player).getLegacy())
                           .title(TitleManager.get(TitleManager.Menu.SHOPSEARCH, player).getLegacy())
                           .plugin(OnyxShop.getInstance())
                           .open(player);
                     } else {
                        new ShopSearch(player, values[2]);
                     }
                  } else if (this.plugin.getShopSections().contains(values[0])) {
                     if (values.length >= 3) {
                        if (values[2].matches("-?\\d+")) {
                           try {
                              int value = Integer.parseInt(values[2]);
                              ShopSection section = this.plugin.getSection(values[0]);
                              if (section.getShopPageItems(value) == null) {
                                 throw new NumberFormatException();
                              }

                              section.openShopSection(player, value, BackButton.DIRECT_CONSOLE);
                           } catch (NumberFormatException exception) {
                              SendMessage.warnMessage(sender, Lang.NO_SHOP_PAGE_FOUND.get().replace("%page%", values[2]));
                           }
                        } else if (this.plugin.getSection(values[0]).getItemLocs().contains(values[2])) {
                           if (values.length >= 4) {
                              try {
                                 Transaction.Mode mode = Transaction.Mode.valueOf(values[3].toUpperCase());
                                 new TransactionMenu(
                                       player,
                                       this.plugin.getShopItem(values[0], values[2]),
                                       null,
                                       BackButton.DIRECT_CONSOLE,
                                       mode,
                                       mode == Transaction.Mode.BUY ? Transaction.Type.BUY_SCREEN : Transaction.Type.SELL_SCREEN,
                                       1
                                    )
                                    .open(false);
                              } catch (IllegalArgumentException exception) {
                                 SendMessage.warnMessage(sender, ChatColor.RED + "Invalid transaction mode, should be buy/sell");
                              }
                           } else {
                              SendMessage.warnMessage(sender, ChatColor.RED + "/shop <section> <player> <item> <buy/sell>");
                           }
                        } else {
                           SendMessage.warnMessage(
                              sender, Lang.SECTION_DOES_NOT_CONTAIN_ITEM.get().replace("%shopsection%", values[0]).replace("%itemLoc%", values[2])
                           );
                        }
                     } else {
                        this.plugin.getSection(values[0]).openShopSection(player, BackButton.DIRECT_CONSOLE);
                     }
                  } else {
                     SendMessage.warnMessage(sender, Lang.NO_SHOP_FOUND.get());
                  }
               } else {
                  SendMessage.warnMessage(sender, Lang.PLAYER_NOT_ONLINE.get());
               }
            }
         }

         return true;
      }
   }

   @Override
   public List<String> tabComplete(CommandSender sender, String command, String[] values) {
      if (!PermissionsCache.hasPermission(sender, "OnyxShop.shop")) {
         return Collections.emptyList();
      } else if (values.length == 1) {
         List values2 = this.plugin
            .getSections()
            .values()
            .stream()
            .filter(
               element -> !element.isSubSection() && !element.isCommandItem() && PermissionsCache.hasPermission(sender, "OnyxShop.shop." + element.getSection())
            )
            .map(ShopSection::getSection)
            .collect(Collectors.toList());
         if (this.plugin.shopSearch) {
            values2.add("search");
         }

         if (!values[0].isEmpty()) {
            ArrayList values3 = new ArrayList();
            StringUtil.copyPartialMatches(values[0], values2, values3);
            Collections.sort(values3);
            return values3;
         } else {
            return values2;
         }
      } else {
         if (values.length == 2 && !values[0].equalsIgnoreCase("search")) {
            ShopSection section = this.plugin.getSection(values[0]);
            if (section != null) {
               return IntStream.rangeClosed(1, section.getPages()).mapToObj(String::valueOf).collect(Collectors.toList());
            }
         }

         return Collections.emptyList();
      }
   }
}
