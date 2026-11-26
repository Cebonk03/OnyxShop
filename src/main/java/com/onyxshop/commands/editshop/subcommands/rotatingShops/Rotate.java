package com.onyxshop.commands.editshop.subcommands.rotatingShops;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import com.onyxshop.OnyxShop;
import com.onyxshop.commands.editshop.Methods;
import com.onyxshop.commands.editshop.SubCommad;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.shops.ShopType;
import com.onyxshop.objects.shops.types.RotatingShop;
import com.onyxshop.util.PermissionsCache;
import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;

public class Rotate extends SubCommad {
   private final Methods methods;
   private final OnyxShop plugin;
   String section;
   boolean silent = true;

   public Rotate(OnyxShop var1, Methods var2) {
      this.plugin = var1;
      this.methods = var2;
   }

   @Override
   public String getName() {
      return "rotate";
   }

   @Override
   public String getDescription() {
      return "§aForces a rotation of this shop";
   }

   @Override
   public String getSyntax() {
      return "§a/editshop rotatingshop rotate <section> [silent]";
   }

   @Override
   public boolean hasPermission(CommandSender var1) {
      return PermissionsCache.hasPermission(var1, "OnyxShop.editshop.rotatingshop." + this.getName());
   }

   @Override
   public void perform(Object var1, String[] var2) {
      if (var2.length > 2) {
         this.section = this.methods.getSection(var1, var2[2]);
         if (this.section != null) {
            if (var2.length > 3 && (var2[3].equalsIgnoreCase("true") || var2[3].equalsIgnoreCase("false"))) {
               this.silent = Boolean.parseBoolean(var2[3]);
            }

            if (this.plugin.getShopType(this.section) != ShopType.ROTATING) {
               SendMessage.sendMessage(var1, "§cShop " + this.section + " is not a rotating shop");
            } else {
               RotatingShop var3 = (RotatingShop)this.plugin.getSection(this.section);
               var3.forceRotate(this.silent);
               SendMessage.infoMessage(var1, "Successfully rotated items inside shop " + this.section + ", next rotation in " + var3.getNextRotation());
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
               ArrayList var2 = new ArrayList();
               StringUtil.copyPartialMatches(var1[2], this.plugin.getShopSections(), var2);
               Collections.sort(var2);
               return var2;
            }

            return this.plugin.getShopSections();
         case 4:
            return Arrays.asList("true", "false");
         default:
            return null;
      }
   }
}
