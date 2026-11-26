package com.onyxshop.commands.editshop.subcommands.dynamicPricing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.onyxshop.OnyxShop;
import com.onyxshop.commands.editshop.Methods;
import com.onyxshop.commands.editshop.SubCommad;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.util.ConfirmRequest;
import com.onyxshop.util.PermissionsCache;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class ResetDynamicPricing extends SubCommad {
   private final OnyxShop plugin;
   private final Methods methods;
   String section;
   String itemLoc;

   public ResetDynamicPricing(OnyxShop var1, Methods var2) {
      this.plugin = var1;
      this.methods = var2;
   }

   @Override
   public String getName() {
      return "reset";
   }

   @Override
   public String getDescription() {
      return Lang.EDITSHOP_RESET_DYNAMICPRICING_SUBCOMMAND_DESC.get().getLegacy();
   }

   @Override
   public String getSyntax() {
      return Lang.EDITSHOP_RESET_DYNAMICPRICING_SUBCOMMAND_SYNTAX.get().getLegacy();
   }

   @Override
   public boolean hasPermission(CommandSender var1) {
      return PermissionsCache.hasPermission(var1, "OnyxShop.editshop.dynamicpricing." + this.getName());
   }

   @Override
   public void perform(Object var1, String[] var2) {
      if (!this.plugin.dynamicPricing) {
         SendMessage.warnMessage(var1, "DynamicPricing needs to be enabled to use this command, you may enable it inside the config.yml or per item/section.");
      } else if (var2.length > 2) {
         if (!var2[2].equalsIgnoreCase("all") && !var2[2].equalsIgnoreCase("*")) {
            this.section = this.methods.getSection(var1, var2[2]);
            if (this.section != null) {
               if (var2.length > 3) {
                  this.itemLoc = this.methods.getShopItemLoc(var1, this.section, var2[3]);
                  if (this.itemLoc != null) {
                     this.plugin.getDynamicPricingProvider().resetDynamicPricing(this.section + "." + this.itemLoc);
                     SendMessage.sendMessage(var1, Lang.DYNAMICPRICING_RESET_SPECIFIC_ITEM.get().replace("%itemPath%", this.section + "." + this.itemLoc));
                  }
               } else {
                  SendMessage.sendMessage(var1, this.getSyntax());
               }
            }
         } else {
            if (var1 instanceof Player) {
               new ConfirmRequest(
                  this.plugin,
                  ChatColor.RED
                     + "Are you sure you want to continue? This will reset all dynamic pricing data from the database.\n"
                     + ChatColor.GREEN
                     + "Enter 'yes' or 'no' in chat to continue",
                  (Player)var1,
                  var2x -> {
                     if (var2x) {
                        this.plugin.resetDynamicPricing(var1);
                     }
                  }
               );
            } else {
               this.plugin.resetDynamicPricing(var1);
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
               List var5 = this.plugin.getShopSections();
               var5.add("all");
               ArrayList var3 = new ArrayList();
               StringUtil.copyPartialMatches(var1[2], var5, var3);
               Collections.sort(var3);
               return var3;
            }

            List var4 = this.plugin.getShopSections();
            var4.add("all");
            return var4;
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

               return this.plugin.getSection(var1[2]).getShopItemLocs();
            }
         default:
            return null;
      }
   }
}
