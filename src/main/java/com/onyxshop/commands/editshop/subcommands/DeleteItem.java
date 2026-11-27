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

   public DeleteItem(OnyxShop plugin, Methods methods) {
      this.plugin = plugin;
      this.methods = methods;
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
   public boolean hasPermission(CommandSender sender) {
      return PermissionsCache.hasPermission(sender, "OnyxShop.editshop." + this.getName());
   }

   @Override
   public void perform(Object value, String[] values) {
      if (values.length > 1) {
         this.section = this.methods.getSection(value, values[1]);
         if (this.section != null) {
            if (values.length > 2) {
               this.itemLoc = this.methods.getItemLoc(value, this.section, values[2]);
               if (this.itemLoc != null) {
                  this.removeItem(value);
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
         case 2:
            if (!values[1].isEmpty()) {
               ArrayList values2 = new ArrayList();
               StringUtil.copyPartialMatches(values[1], this.plugin.getShopSections(), values2);
               Collections.sort(values2);
               return values2;
            }

            return this.plugin.getShopSections();
         case 3:
            if (this.isSection(values[1])) {
               if (!values[2].isEmpty()) {
                  ArrayList values2 = new ArrayList();
                  StringUtil.copyPartialMatches(values[2], this.plugin.getSection(values[1]).getItemLocs(), values2);
                  Collections.sort(values2);
                  return values2;
               }

               return this.plugin.getSection(values[1]).getItemLocs();
            }

            return null;
         default:
            return null;
      }
   }

   private boolean isSection(String sectionId) {
      return this.plugin.getShopSections().contains(sectionId);
   }

   private List<String> getClosest(String text, Set<String> values) {
      ArrayList values2 = new ArrayList();
      StringUtil.copyPartialMatches(text, values, values2);
      Collections.sort(values2);
      return values2;
   }

   private void removeItem(Object value) {
      SendMessage.sendMessage(value, Lang.EDITSHOP_REMOVING_ITEM.get());
      ConfigManager.getShop(this.section).set("pages." + this.itemLoc, null);
      ConfigManager.saveShop(this.section);
      SendMessage.sendMessage(value, Lang.EDITSHOP_REMOVE_ITEM_SUCCESSFUL.get().replace("%itemPath%", this.section + "." + this.itemLoc));
      SendMessage.sendMessage(value, Lang.EDITSHOP_RELOAD_SHOP_TO_SEE_CHANGES.get());
   }
}
