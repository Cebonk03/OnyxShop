package com.onyxshop.commands.editshop.subcommands;

import java.util.ArrayList;
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

public class DeleteSection extends SubCommad {
   private final Methods methods;
   private final OnyxShop plugin;
   private String section;

   public DeleteSection(OnyxShop var1, Methods var2) {
      this.plugin = var1;
      this.methods = var2;
   }

   @Override
   public String getName() {
      return "deletesection";
   }

   @Override
   public String getDescription() {
      return Lang.EDITSHOP_REMOVE_SECTION_SUBCOMMAND_DESC.get().getLegacy();
   }

   @Override
   public String getSyntax() {
      return Lang.EDITSHOP_REMOVE_SECTION_SUBCOMMAND_SYNTAX.get().getLegacy();
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
            this.deleteSection(var1);
         }
      } else {
         SendMessage.warnMessage(var1, this.getSyntax());
      }
   }

   @Override
   public List<String> getTabCompletion(String[] var1) {
      switch (var1.length) {
         case 2:
            if (!var1[1].isEmpty()) {
               ArrayList var2 = new ArrayList();
               StringUtil.copyPartialMatches(var1[1], this.plugin.getShopSections(), var2);
               Collections.sort(var2);
               return var2;
            }

            return this.plugin.getShopSections();
         default:
            return null;
      }
   }

   private void deleteSection(Object var1) {
      SendMessage.sendMessage(var1, Lang.EDITSHOP_REMOVING_SECTION.get());
      this.plugin.getConfigManager().deleteSection(this.section);
      this.plugin.getConfigManager().deleteShopsConfig(this.section);
      SendMessage.sendMessage(var1, Lang.EDITSHOP_REMOVED_SECTION.get().replace("%itemPath%", "ShopSections." + this.section));
      SendMessage.sendMessage(var1, Lang.EDITSHOP_RELOAD_SHOP_TO_SEE_CHANGES.get());
   }
}
