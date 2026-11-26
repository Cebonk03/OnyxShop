package com.onyxshop.commands.editshop.subcommands.playerstock;

import java.util.ArrayList;
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

public class ResetStock extends SubCommad {
   private final Methods methods;
   private final OnyxShop plugin;
   String section;
   String itemLoc;
   String playerName;

   public ResetStock(OnyxShop var1, Methods var2) {
      this.plugin = var1;
      this.methods = var2;
   }

   @Override
   public String getName() {
      return "reset";
   }

   @Override
   public String getDescription() {
      return Lang.EDITSHOP_RESET_PLAYERSTOCK_COMMAND_DESC.get().getLegacy();
   }

   @Override
   public String getSyntax() {
      return Lang.EDITSHOP_RESET_PLAYERSTOCK_COMMAND_SYNTAX.get().getLegacy();
   }

   @Override
   public boolean hasPermission(CommandSender var1) {
      return PermissionsCache.hasPermission(var1, "OnyxShop.editshop.playerstock." + this.getName());
   }

   @Override
   public void perform(Object var1, String[] var2) {
      if (var2.length > 2) {
         this.playerName = var2[2].equals("*") ? null : var2[2];
         if (var2.length > 3) {
            String var3 = var2[3].equals("*") ? null : var2[3];
            if (var3 != null) {
               this.section = this.methods.getSection(var1, var3);
               if (this.section == null) {
                  return;
               }
            } else {
               this.section = null;
            }

            if (this.section != null) {
               if (var2.length <= 4) {
                  SendMessage.sendMessage(var1, this.getSyntax());
                  return;
               }

               var3 = var2[4].equals("*") ? null : var2[4];
               if (var3 != null) {
                  this.itemLoc = this.methods.getShopItemLoc(var1, this.section, var3);
                  if (this.itemLoc == null) {
                     return;
                  }

                  if (this.plugin.getLimitedStockProvider().getItemStockMode(this.section + "." + this.itemLoc) != 2) {
                     SendMessage.warnMessage(var1, "This item does not have any player stock limits");
                     return;
                  }
               } else {
                  this.itemLoc = null;
               }
            }

            try {
               long var7 = System.currentTimeMillis();
               this.plugin
                  .getLimitedStockProvider()
                  .resetItemStock(this.section, this.itemLoc, this.playerName == null ? null : Bukkit.getOfflinePlayer(this.playerName).getUniqueId(), 2);
               SendMessage.infoMessage(var1, Lang.STOCK_LIMIT_RESET_SUCCESSFUL.get().replace("%time%", String.valueOf(System.currentTimeMillis() - var7)));
            } catch (Exception var5) {
               SendMessage.sendMessage(var1, "Failed to reset stock limit, see console for details");
               var5.printStackTrace();
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
               List var8 = this.plugin.getServer().getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
               var8.add("*");
               ArrayList var3 = new ArrayList();
               StringUtil.copyPartialMatches(var1[2], var8, var3);
               Collections.sort(var3);
               return var3;
            }

            List var7 = this.plugin.getServer().getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
            var7.add("*");
            return var7;
         case 4:
            if (!var1[3].isEmpty()) {
               ArrayList var6 = new ArrayList();
               StringUtil.copyPartialMatches(var1[3], this.plugin.getShopSections(), var6);
               Collections.sort(var6);
               var6.add("*");
               return var6;
            }

            ArrayList var5 = new ArrayList<>(this.plugin.getShopSections());
            var5.add("*");
            return var5;
         case 5:
            if (var1[3].equals("*") && !this.plugin.getShopSections().contains(var1[3])) {
               return null;
            } else {
               if (!var1[4].isEmpty()) {
                  ArrayList var4 = new ArrayList();
                  StringUtil.copyPartialMatches(var1[4], this.plugin.getSection(var1[3]).getShopItemLocs(), var4);
                  Collections.sort(var4);
                  var4.add("*");
                  return var4;
               }

               List var2 = this.plugin.getSection(var1[3]).getShopItemLocs();
               var2.add("*");
               return var2;
            }
         default:
            return null;
      }
   }
}
