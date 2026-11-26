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

public class SetStock extends SubCommad {
   private final Methods methods;
   private final OnyxShop plugin;
   String section;
   String itemLoc;
   String playerName;
   int amount;

   public SetStock(OnyxShop var1, Methods var2) {
      this.plugin = var1;
      this.methods = var2;
   }

   @Override
   public String getName() {
      return "set";
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
   public boolean hasPermission(CommandSender var1) {
      return PermissionsCache.hasPermission(var1, "OnyxShop.editshop.playerstock." + this.getName());
   }

   @Override
   public void perform(Object var1, String[] var2) {
      if (var2.length > 2) {
         this.playerName = var2[2].equalsIgnoreCase("all") ? null : var2[2];
         if (var2.length > 3) {
            this.section = this.methods.getSection(var1, var2[3]);
            if (this.section != null) {
               if (var2.length > 4) {
                  this.itemLoc = this.methods.getShopItemLoc(var1, this.section, var2[4]);
                  if (this.itemLoc != null) {
                     if (var2.length > 5) {
                        try {
                           this.amount = Integer.parseInt(var2[5]);
                        } catch (NumberFormatException var4) {
                           SendMessage.sendMessage(var1, Lang.NO_VALID_AMOUNT.get());
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
                           SendMessage.sendMessage(var1, Lang.PLAYERSTOCK_SET_ALL_PLAYERS.get().replace("%itemPath%", this.section + "." + this.itemLoc));
                        } else {
                           SendMessage.sendMessage(
                              var1,
                              Lang.PLAYERSTOCK_SET_SUCCESSFUL
                                 .get()
                                 .replace("%player%", this.playerName)
                                 .replace("%itemPath%", this.section + "." + this.itemLoc)
                           );
                        }
                     } else {
                        SendMessage.sendMessage(var1, this.getSyntax());
                     }
                  }
               } else {
                  SendMessage.sendMessage(var1, this.getSyntax());
               }
            }
         } else {
            SendMessage.sendMessage(var1, this.getSyntax());
         }
      } else {
         SendMessage.sendMessage(var1, this.getSyntax());
      }
   }

   @Override
   public List<String> getTabCompletion(String[] var1) {
      switch (var1.length) {
         case 3:
            if (!var1[2].isEmpty()) {
               List var6 = this.plugin.getServer().getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
               var6.add("all");
               ArrayList var3 = new ArrayList();
               StringUtil.copyPartialMatches(var1[2], var6, var3);
               Collections.sort(var3);
               return var3;
            }

            List var5 = this.plugin.getServer().getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
            var5.add("all");
            return var5;
         case 4:
            if (!var1[3].isEmpty()) {
               ArrayList var4 = new ArrayList();
               StringUtil.copyPartialMatches(var1[3], this.plugin.getShopSections(), var4);
               Collections.sort(var4);
               return var4;
            }

            return this.plugin.getShopSections();
         case 5:
            if (!this.plugin.getShopSections().contains(var1[3])) {
               return null;
            } else {
               if (!var1[4].isEmpty()) {
                  ArrayList var2 = new ArrayList();
                  StringUtil.copyPartialMatches(var1[4], this.plugin.getSection(var1[3]).getShopItemLocs(), var2);
                  Collections.sort(var2);
                  return var2;
               }

               return this.plugin.getSection(var1[3]).getShopItemLocs();
            }
         case 6:
            return Arrays.asList("1", "5", "10", "25");
         default:
            return null;
      }
   }
}
