package com.onyxshop.commands.editshop.subcommands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import com.onyxshop.OnyxShop;
import com.onyxshop.commands.editshop.Methods;
import com.onyxshop.commands.editshop.SubCommad;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.util.PermissionsCache;
import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;

public class DeleteItem extends SubCommad {
   private final Methods methods;
   private final OnyxShop plugin;
   String section;
   String itemLoc;

   public DeleteItem(OnyxShop var1, Methods var2) {
      this.plugin = var1;
      this.methods = var2;
   }

   @Override
   public String getName() {
      return "deleteitem";
   }

   @Override
   public String getDescription() {
      return Lang.EDITSHOP_REMOVE_ITEM_SUBCOMMAND_DESC.get().getLegacy();
   }

   @Override
   public String getSyntax() {
      return Lang.EDITSHOP_REMOVE_ITEM_SUBCOMMAND_SYNTAX.get().getLegacy();
   }

   @Override
   public boolean hasPermission(CommandSender var1) {
      return PermissionsCache.hasPermission(var1, "OnyxShop.editshop." + this.getName());
   }

   @Override
   public void perform(Object var1, String[] var2) {
      if (var2.length > 1) {
         this.section = this.methods.getSection(var1, var2[1]);
         if (this.section != null) {
            if (var2.length > 2) {
               this.itemLoc = this.methods.getItemLoc(var1, this.section, var2[2]);
               if (this.itemLoc != null) {
                  this.removeItem(var1);
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
         case 2:
            if (!var1[1].isEmpty()) {
               ArrayList var3 = new ArrayList();
               StringUtil.copyPartialMatches(var1[1], this.plugin.getShopSections(), var3);
               Collections.sort(var3);
               return var3;
            }

            return this.plugin.getShopSections();
         case 3:
            if (this.isSection(var1[1])) {
               if (!var1[2].isEmpty()) {
                  ArrayList var2 = new ArrayList();
                  StringUtil.copyPartialMatches(var1[2], this.plugin.getSection(var1[1]).getItemLocs(), var2);
                  Collections.sort(var2);
                  return var2;
               }

               return this.plugin.getSection(var1[1]).getItemLocs();
            }

            return null;
         default:
            return null;
      }
   }

   private boolean isSection(String var1) {
      return this.plugin.getShopSections().contains(var1);
   }

   private List<String> getClosest(String var1, Set<String> var2) {
      ArrayList var3 = new ArrayList();
      StringUtil.copyPartialMatches(var1, var2, var3);
      Collections.sort(var3);
      return var3;
   }

   private void removeItem(Object var1) {
      SendMessage.sendMessage(var1, Lang.EDITSHOP_REMOVING_ITEM.get());
      ConfigManager.getShop(this.section).set("pages." + this.itemLoc, null);
      ConfigManager.saveShop(this.section);
      SendMessage.sendMessage(var1, Lang.EDITSHOP_REMOVE_ITEM_SUCCESSFUL.get().replace("%itemPath%", this.section + "." + this.itemLoc));
      SendMessage.sendMessage(var1, Lang.EDITSHOP_RELOAD_SHOP_TO_SEE_CHANGES.get());
   }
}
