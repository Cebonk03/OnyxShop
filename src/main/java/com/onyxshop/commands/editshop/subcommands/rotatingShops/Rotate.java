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

   public Rotate(OnyxShop plugin, Methods methods) {
      this.plugin = plugin;
      this.methods = methods;
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
   public boolean hasPermission(CommandSender sender) {
      return PermissionsCache.hasPermission(sender, "OnyxShop.editshop.rotatingshop." + this.getName());
   }

   @Override
   public void perform(Object value, String[] values) {
      if (values.length > 2) {
         this.section = this.methods.getSection(value, values[2]);
         if (this.section != null) {
            if (values.length > 3 && (values[3].equalsIgnoreCase("true") || values[3].equalsIgnoreCase("false"))) {
               this.silent = Boolean.parseBoolean(values[3]);
            }

            if (this.plugin.getShopType(this.section) != ShopType.ROTATING) {
               SendMessage.sendMessage(value, "§cShop " + this.section + " is not a rotating shop");
            } else {
               RotatingShop rotatingShop = (RotatingShop)this.plugin.getSection(this.section);
               rotatingShop.forceRotate(this.silent);
               SendMessage.infoMessage(value, "Successfully rotated items inside shop " + this.section + ", next rotation in " + rotatingShop.getNextRotation());
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
            return Arrays.asList("true", "false");
         default:
            return null;
      }
   }
}
