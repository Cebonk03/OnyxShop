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

   public DeleteSection(OnyxShop plugin, Methods methods) {
      this.plugin = plugin;
      this.methods = methods;
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
   public boolean hasPermission(CommandSender sender) {
      return PermissionsCache.hasPermission(sender, "OnyxShop.editshop." + this.getName());
   }

   @Override
   public void perform(Object value, String[] values) {
      if (values.length > 1) {
         this.section = this.methods.getSection(value, values[1]);
         if (this.section != null) {
            this.deleteSection(value);
         }
      } else {
         SendMessage.warnMessage(value, this.getSyntax());
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
         default:
            return null;
      }
   }

   private void deleteSection(Object value) {
      SendMessage.sendMessage(value, Lang.EDITSHOP_REMOVING_SECTION.get());
      this.plugin.getConfigManager().deleteSection(this.section);
      this.plugin.getConfigManager().deleteShopsConfig(this.section);
      SendMessage.sendMessage(value, Lang.EDITSHOP_REMOVED_SECTION.get().replace("%itemPath%", "ShopSections." + this.section));
      SendMessage.sendMessage(value, Lang.EDITSHOP_RELOAD_SHOP_TO_SEE_CHANGES.get());
   }
}
