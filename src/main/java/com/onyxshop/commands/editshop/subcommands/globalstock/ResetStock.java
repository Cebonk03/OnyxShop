package com.onyxshop.commands.editshop.subcommands.globalstock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.onyxshop.OnyxShop;
import com.onyxshop.commands.editshop.Methods;
import com.onyxshop.commands.editshop.SubCommad;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.util.PermissionsCache;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;

public class ResetStock extends SubCommad {
   private final Methods methods;
   private final OnyxShop plugin;
   String section;
   String itemLoc;
   String playerName;

   public ResetStock(OnyxShop plugin, Methods methods) {
      this.plugin = plugin;
      this.methods = methods;
   }

   @Override
   public String getName() {
      return "reset";
   }

   @Override
   public String getDescription() {
      return Lang.EDITSHOP_RESET_GLOBALSTOCK_COMMAND_DESC.get().getLegacy();
   }

   @Override
   public String getSyntax() {
      return Lang.EDITSHOP_RESET_GLOBALSTOCK_COMMAND_SYNTAX.get().getLegacy();
   }

   @Override
   public boolean hasPermission(CommandSender sender) {
      return PermissionsCache.hasPermission(sender, "OnyxShop.editshop.globalstock." + this.getName());
   }

   @Override
   public void perform(Object value, String[] values) {
      if (values.length > 2) {
         String text = values[2].equals("*") ? null : values[2];
         if (text != null) {
            this.section = this.methods.getSection(value, text);
            if (this.section == null) {
               return;
            }
         } else {
            this.section = null;
         }

         if (this.section != null) {
            if (values.length <= 3) {
               SendMessage.sendMessage(value, this.getSyntax());
               return;
            }

            text = values[3].equals("*") ? null : values[3];
            if (text != null) {
               this.itemLoc = this.methods.getShopItemLoc(value, this.section, text);
               if (this.itemLoc == null) {
                  return;
               }

               if (this.plugin.getLimitedStockProvider().getItemStockMode(this.section + "." + this.itemLoc) != 1) {
                  SendMessage.warnMessage(value, "This item does not have any global stock limits");
                  return;
               }
            } else {
               this.itemLoc = null;
            }
         }

         try {
            long startTime = System.currentTimeMillis();
            this.plugin
               .getLimitedStockProvider()
               .resetItemStock(this.section, this.itemLoc, this.playerName == null ? null : Bukkit.getOfflinePlayer(this.playerName).getUniqueId(), 1);
            SendMessage.infoMessage(value, Lang.STOCK_LIMIT_RESET_SUCCESSFUL.get().replace("%time%", String.valueOf(System.currentTimeMillis() - startTime)));
         } catch (Exception exception) {
            SendMessage.warnMessage(value, "Failed to reset stock limit, see console for details");
            exception.printStackTrace();
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
               ArrayList values2 = new ArrayList();
               StringUtil.copyPartialMatches(values[2], this.plugin.getShopSections(), values2);
               Collections.sort(values2);
               values2.add("*");
               return values2;
            }

            ArrayList values2 = new ArrayList<>(this.plugin.getShopSections());
            values2.add("*");
            return values2;
         case 4:
            if (values[2].equals("*") && !this.plugin.getShopSections().contains(values[2])) {
               return null;
            } else {
               if (!values[3].isEmpty()) {
                  ArrayList values3 = new ArrayList();
                  StringUtil.copyPartialMatches(values[3], this.plugin.getSection(values[2]).getShopItemLocs(), values3);
                  Collections.sort(values3);
                  values3.add("*");
                  return values3;
               }

               List shopItemLocs = this.plugin.getSection(values[2]).getShopItemLocs();
               shopItemLocs.add("*");
               return shopItemLocs;
            }
         default:
            return null;
      }
   }
}
