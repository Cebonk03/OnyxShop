package com.onyxshop.commands.editshop.subcommands.shopstands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.commands.editshop.Methods;
import com.onyxshop.commands.editshop.SubCommad;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.stands.Stand;
import com.onyxshop.util.PermissionsCache;
import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;

public class Destroy extends SubCommad {
   private final Methods methods;
   private final OnyxShop plugin;

   public Destroy(OnyxShop plugin, Methods methods) {
      this.plugin = plugin;
      this.methods = methods;
   }

   @Override
   public String getName() {
      return "destroy";
   }

   @Override
   public String getDescription() {
      return Lang.EDITSHOP_SHOPSTANDS_DESTROY_SUBCOMMAND_DESC.get().getLegacy();
   }

   @Override
   public String getSyntax() {
      return Lang.EDITSHOP_SHOPSTANDS_DESTROY_SUBCOMMAND_SYNTAX.get().getLegacy();
   }

   @Override
   public boolean hasPermission(CommandSender sender) {
      return PermissionsCache.hasPermission(sender, "OnyxShop.editshop.shopstands." + this.getName());
   }

   @Override
   public void perform(Object value, String[] values) {
      if (values.length > 2) {
         int value2;
         try {
            value2 = Integer.parseInt(values[2]);
         } catch (NumberFormatException exception) {
            SendMessage.warnMessage(value, Lang.NO_VALID_AMOUNT.get());
            return;
         }

         Stand stand = this.plugin.getStandProvider().getStand(value2);
         if (stand == null) {
            SendMessage.warnMessage(value, Lang.UNKNOWN_SHOP_STAND_ID.get().replace("%id%", String.valueOf(value2)));
         } else {
            this.plugin.getStandProvider().destroy(stand);
            SendMessage.infoMessage(value, Lang.REMOVED_SHOP_STAND.get().replace("%location%", stand.getLoc().toString()));
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
               StringUtil.copyPartialMatches(
                  values[2], this.plugin.getStandProvider().getStands().stream().map(element -> String.valueOf(element.getId())).collect(Collectors.toList()), values2
               );
               Collections.sort(values2);
               return values2;
            }

            return this.plugin.getStandProvider().getStands().stream().map(element -> String.valueOf(element.getId())).collect(Collectors.toList());
         default:
            return null;
      }
   }
}
