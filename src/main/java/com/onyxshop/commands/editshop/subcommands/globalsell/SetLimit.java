package com.onyxshop.commands.editshop.subcommands.globalsell;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import com.onyxshop.OnyxShop;
import com.onyxshop.commands.editshop.Methods;
import com.onyxshop.commands.editshop.SubCommad;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.util.PermissionsCache;
import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;

public class SetLimit extends SubCommad {
   private final Methods methods;
   private final OnyxShop plugin;
   String section;
   String itemLoc;
   int amount;

   public SetLimit(OnyxShop var1, Methods var2) {
      this.plugin = var1;
      this.methods = var2;
   }

   @Override
   public String getName() {
      return "set";
   }

   @Override
   public String getDescription() {
      return Lang.EDITSHOP_SET_GLOBALSELL_COMMAND_DESC.get().getLegacy();
   }

   @Override
   public String getSyntax() {
      return Lang.EDITSHOP_SET_GLOBALSELL_COMMAND_SYNTAX.get().getLegacy();
   }

   @Override
   public boolean hasPermission(CommandSender var1) {
      return PermissionsCache.hasPermission(var1, "OnyxShop.editshop.globalsell." + this.getName());
   }

   @Override
   public void perform(Object var1, String[] var2) {
      if (var2.length > 2) {
         this.section = this.methods.getSection(var1, var2[2]);
         if (this.section != null) {
            if (var2.length > 3) {
               this.itemLoc = this.methods.getShopItemLoc(var1, this.section, var2[3]);
               if (this.itemLoc != null) {
                  if (var2.length > 4) {
                     try {
                        this.amount = Integer.parseInt(var2[4]);
                     } catch (NumberFormatException var4) {
                        SendMessage.warnMessage(var1, Lang.NO_VALID_AMOUNT.get());
                        return;
                     }

                     ShopItem var3 = this.plugin.getShopItem(this.section, this.itemLoc);
                     this.plugin.getLimitedSellProvider().setLimit(var3, null, this.amount);
                     SendMessage.sendMessage(var1, Lang.GLOBALSELL_SET_SUCCESSFUL.get().replace("%itemPath%", var3.getItemPath()));
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
   }

   @Override
   public List<String> getTabCompletion(String[] var1) {
      switch (var1.length) {
         case 3:
            if (!var1[2].isEmpty()) {
               ArrayList var3 = new ArrayList();
               StringUtil.copyPartialMatches(var1[2], this.plugin.getShopSections(), var3);
               Collections.sort(var3);
               return var3;
            }

            return this.plugin.getShopSections();
         case 4:
            if (!this.plugin.getShopSections().contains(var1[2])) {
               return null;
            } else {
               if (!var1[3].isEmpty()) {
                  ArrayList var2 = new ArrayList();
                  StringUtil.copyPartialMatches(var1[3], this.plugin.getSection(var1[2]).getShopItemLocs(), var2);
                  Collections.sort(var2);
                  return var2;
               }

               return new ArrayList<>(this.plugin.getSection(var1[2]).getShopItemLocs());
            }
         case 5:
            return Arrays.asList("1", "5", "10", "25");
         default:
            return null;
      }
   }
}
