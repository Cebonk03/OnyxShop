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

   public ResetDynamicPricing(OnyxShop plugin, Methods methods) {
      this.plugin = plugin;
      this.methods = methods;
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
   public boolean hasPermission(CommandSender sender) {
      return PermissionsCache.hasPermission(sender, "OnyxShop.editshop.dynamicpricing." + this.getName());
   }

   @Override
   public void perform(Object value, String[] values) {
      if (!this.plugin.dynamicPricing) {
         SendMessage.warnMessage(value, "DynamicPricing needs to be enabled to use this command, you may enable it inside the config.yml or per item/section.");
      } else if (values.length > 2) {
         if (!values[2].equalsIgnoreCase("all") && !values[2].equalsIgnoreCase("*")) {
            this.section = this.methods.getSection(value, values[2]);
            if (this.section != null) {
               if (values.length > 3) {
                  this.itemLoc = this.methods.getShopItemLoc(value, this.section, values[3]);
                  if (this.itemLoc != null) {
                     this.plugin.getDynamicPricingProvider().resetDynamicPricing(this.section + "." + this.itemLoc);
                     SendMessage.sendMessage(value, Lang.DYNAMICPRICING_RESET_SPECIFIC_ITEM.get().replace("%itemPath%", this.section + "." + this.itemLoc));
                  }
               } else {
                  SendMessage.sendMessage(value, this.getSyntax());
               }
            }
         } else {
            if (value instanceof Player) {
               new ConfirmRequest(
                  this.plugin,
                  ChatColor.RED
                     + "Are you sure you want to continue? This will reset all dynamic pricing data from the database.\n"
                     + ChatColor.GREEN
                     + "Enter 'yes' or 'no' in chat to continue",
                  (Player)value,
                  element -> {
                     if (element) {
                        this.plugin.resetDynamicPricing(value);
                     }
                  }
               );
            } else {
               this.plugin.resetDynamicPricing(value);
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
               List shopSections = this.plugin.getShopSections();
               shopSections.add("all");
               ArrayList values2 = new ArrayList();
               StringUtil.copyPartialMatches(values[2], shopSections, values2);
               Collections.sort(values2);
               return values2;
            }

            List shopSections = this.plugin.getShopSections();
            shopSections.add("all");
            return shopSections;
         case 4:
            if (!this.plugin.getShopSections().contains(values[2])) {
               return null;
            } else {
               if (!values[3].isEmpty()) {
                  ArrayList values2 = new ArrayList();
                  StringUtil.copyPartialMatches(values[3], this.plugin.getSection(values[2]).getShopItemLocs(), values2);
                  Collections.sort(values2);
                  return values2;
               }

               return this.plugin.getSection(values[2]).getShopItemLocs();
            }
         default:
            return null;
      }
   }
}
