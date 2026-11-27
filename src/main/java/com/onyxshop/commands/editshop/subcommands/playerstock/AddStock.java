package com.onyxshop.commands.editshop.subcommands.playerstock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.commands.editshop.Methods;
import com.onyxshop.commands.editshop.SubCommad;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.util.PermissionsCache;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class AddStock extends SubCommad {
   private final Methods methods;
   private final OnyxShop plugin;
   String section;
   String itemLoc;
   String playerName;
   int amount;

   public AddStock(OnyxShop plugin, Methods methods) {
      this.plugin = plugin;
      this.methods = methods;
   }

   @Override
   public String getName() {
      return "add";
   }

   @Override
   public String getDescription() {
      return Lang.EDITSHOP_SET_PLAYERSTOCK_COMMAND_DESC.get().getLegacy();
   }

   @Override
   public String getSyntax() {
      return Lang.EDITSHOP_SET_PLAYERSTOCK_COMMAND_SYNTAX.get().getLegacy();
   }

   @Override
   public boolean hasPermission(CommandSender sender) {
      return PermissionsCache.hasPermission(sender, "OnyxShop.editshop.playerstock." + this.getName());
   }

   @Override
   public void perform(Object value, String[] values) {
      if (values.length > 2) {
         this.playerName = values[2].equalsIgnoreCase("all") ? null : values[2];
         if (values.length > 3) {
            this.section = this.methods.getSection(value, values[3]);
            if (this.section != null) {
               if (values.length > 4) {
                  this.itemLoc = this.methods.getShopItemLoc(value, this.section, values[4]);
                  if (this.itemLoc != null) {
                     if (values.length > 5) {
                        try {
                           this.amount = Integer.parseInt(values[5]);
                        } catch (NumberFormatException exception) {
                           SendMessage.sendMessage(value, Lang.NO_VALID_AMOUNT.get());
                           return;
                        }

                        this.plugin
                           .getLimitedStockProvider()
                           .setItemStock(
                              this.section + "." + this.itemLoc,
                              this.playerName == null ? null : Bukkit.getOfflinePlayer(this.playerName).getUniqueId(),
                              2,
                              this.amount
                           );
                        if (this.playerName == null) {
                           SendMessage.sendMessage(value, Lang.PLAYERSTOCK_SET_ALL_PLAYERS.get().replace("%itemPath%", this.section + "." + this.itemLoc));
                        } else {
                           SendMessage.sendMessage(
                              value,
                              Lang.PLAYERSTOCK_SET_SUCCESSFUL
                                 .get()
                                 .replace("%player%", this.playerName)
                                 .replace("%itemPath%", this.section + "." + this.itemLoc)
                           );
                        }
                     } else {
                        SendMessage.sendMessage(value, this.getSyntax());
                     }
                  }
               } else {
                  SendMessage.sendMessage(value, this.getSyntax());
               }
            }
         } else {
            SendMessage.sendMessage(value, this.getSyntax());
         }
      } else {
         SendMessage.sendMessage(value, this.getSyntax());
      }
   }

   @Override
   public List<String> getTabCompletion(String[] values) {
      switch (values.length) {
         case 3:
            if (!values[2].isEmpty()) {
               List playerNames = this.plugin.getServer().getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
               playerNames.add("all");
               ArrayList values2 = new ArrayList();
               StringUtil.copyPartialMatches(values[2], playerNames, values2);
               Collections.sort(values2);
               return values2;
            }

            List names = this.plugin.getServer().getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
            names.add("all");
            return names;
         case 4:
            if (!values[3].isEmpty()) {
               ArrayList values2 = new ArrayList();
               StringUtil.copyPartialMatches(values[3], this.plugin.getShopSections(), values2);
               Collections.sort(values2);
               return values2;
            }

            return this.plugin.getShopSections();
         case 5:
            if (!this.plugin.getShopSections().contains(values[3])) {
               return null;
            } else {
               if (!values[4].isEmpty()) {
                  ArrayList values2 = new ArrayList();
                  StringUtil.copyPartialMatches(values[4], this.plugin.getSection(values[2]).getShopItemLocs(), values2);
                  Collections.sort(values2);
                  return values2;
               }

               return new ArrayList<>(this.plugin.getSection(values[2]).getShopItemLocs());
            }
         case 6:
            return Arrays.asList("1", "5", "10", "25");
         default:
            return null;
      }
   }
}
