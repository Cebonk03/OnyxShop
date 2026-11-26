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

   public Destroy(OnyxShop var1, Methods var2) {
      this.plugin = var1;
      this.methods = var2;
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
   public boolean hasPermission(CommandSender var1) {
      return PermissionsCache.hasPermission(var1, "OnyxShop.editshop.shopstands." + this.getName());
   }

   @Override
   public void perform(Object var1, String[] var2) {
      if (var2.length > 2) {
         int var3;
         try {
            var3 = Integer.parseInt(var2[2]);
         } catch (NumberFormatException var5) {
            SendMessage.warnMessage(var1, Lang.NO_VALID_AMOUNT.get());
            return;
         }

         Stand var4 = this.plugin.getStandProvider().getStand(var3);
         if (var4 == null) {
            SendMessage.warnMessage(var1, Lang.UNKNOWN_SHOP_STAND_ID.get().replace("%id%", String.valueOf(var3)));
         } else {
            this.plugin.getStandProvider().destroy(var4);
            SendMessage.infoMessage(var1, Lang.REMOVED_SHOP_STAND.get().replace("%location%", var4.getLoc().toString()));
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
               ArrayList var2 = new ArrayList();
               StringUtil.copyPartialMatches(
                  var1[2], this.plugin.getStandProvider().getStands().stream().map(var0 -> String.valueOf(var0.getId())).collect(Collectors.toList()), var2
               );
               Collections.sort(var2);
               return var2;
            }

            return this.plugin.getStandProvider().getStands().stream().map(var0 -> String.valueOf(var0.getId())).collect(Collectors.toList());
         default:
            return null;
      }
   }
}
