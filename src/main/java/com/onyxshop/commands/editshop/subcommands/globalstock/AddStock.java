package com.onyxshop.commands.editshop.subcommands.globalstock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import com.onyxshop.OnyxShop;
import com.onyxshop.commands.editshop.Methods;
import com.onyxshop.commands.editshop.SubCommad;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.util.PermissionsCache;
import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;

public class AddStock extends SubCommad {
   private final Methods methods;
   private final OnyxShop plugin;
   String section;
   String itemLoc;
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
      return Lang.EDITSHOP_ADD_GLOBALSTOCK_COMMAND_DESC.get().getLegacy();
   }

   @Override
   public String getSyntax() {
      return Lang.EDITSHOP_ADD_GLOBALSTOCK_COMMAND_SYNTAX.get().getLegacy();
   }

   @Override
   public boolean hasPermission(CommandSender sender) {
      return PermissionsCache.hasPermission(sender, "OnyxShop.editshop.globalstock." + this.getName());
   }

   @Override
   public void perform(Object value, String[] values) {
      if (values.length > 2) {
         this.section = this.methods.getSection(value, values[2]);
         if (this.section != null) {
            if (values.length > 3) {
               this.itemLoc = this.methods.getShopItemLoc(value, this.section, values[3]);
               if (this.itemLoc != null) {
                  if (values.length > 4) {
                     try {
                        this.amount = Integer.parseInt(values[4]);
                     } catch (NumberFormatException exception) {
                        SendMessage.sendMessage(value, Lang.NO_VALID_AMOUNT.get());
                        return;
                     }

                     String sectionId = this.section + "." + this.itemLoc;
                     this.plugin.getLimitedStockProvider().setItemStock(sectionId, null, 1, this.plugin.getLimitedStockProvider().getItemStock(sectionId) + this.amount);
                     SendMessage.sendMessage(value, Lang.GLOBALSTOCK_SET_SUCCESSFUL.get().replace("%itemPath%", sectionId));
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
   }

   @Override
   public List<String> getTabCompletion(String[] values) {
      switch (values.length) {
         case 3:
            if (!values[2].isEmpty()) {
               ArrayList values2 = new ArrayList();
               StringUtil.copyPartialMatches(values[2], this.plugin.getShopSections(), values2);
               Collections.sort(values2);
               return values2;
            }

            return this.plugin.getShopSections();
         case 4:
            if (!this.plugin.getShopSections().contains(values[2])) {
               return null;
            } else {
               if (!values[3].isEmpty()) {
                  ArrayList values2 = new ArrayList();
                  StringUtil.copyPartialMatches(values[3], this.plugin.getSection(values[2]).getShopItemLocs(), values2);
                  Collections.sort(values2);
                  return values2;
               }

               return new ArrayList<>(this.plugin.getSection(values[2]).getShopItemLocs());
            }
         case 5:
            return Arrays.asList("1", "5", "10", "25");
         default:
            return null;
      }
   }
}
