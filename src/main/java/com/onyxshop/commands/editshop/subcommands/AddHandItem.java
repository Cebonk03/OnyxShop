package com.onyxshop.commands.editshop.subcommands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.onyxshop.OnyxShop;
import com.onyxshop.commands.editshop.Methods;
import com.onyxshop.commands.editshop.SubCommad;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.files.config.Config;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.util.PermissionsCache;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;

public class AddHandItem extends SubCommad {
   private final Methods methods;
   private final OnyxShop plugin;
   String section;
   Double buyPrice;
   Double sellPrice;

   public AddHandItem(OnyxShop var1, Methods var2) {
      this.plugin = var1;
      this.methods = var2;
   }

   @Override
   public String getName() {
      return "addhanditem";
   }

   @Override
   public String getDescription() {
      return Lang.EDITSHOP_ADD_HAND_ITEM_SUBCOMMAND_DESC.get().getLegacy();
   }

   @Override
   public String getSyntax() {
      return Lang.EDITSHOP_ADD_HAND_ITEM_SUBCOMMAND_SYNTAX.get().getLegacy();
   }

   @Override
   public boolean hasPermission(CommandSender var1) {
      return PermissionsCache.hasPermission(var1, "OnyxShop.editshop." + this.getName());
   }

   @Override
   public void perform(Object var1, String[] var2) {
      if (var1 instanceof Player) {
         Player var3 = (Player)var1;
         if (var2.length > 1) {
            this.section = this.methods.getSection(var3, var2[1]);
            if (this.section != null) {
               if (var2.length > 2) {
                  this.buyPrice = this.methods.getPrice(var3, var2[2]);
                  if (this.buyPrice != null) {
                     if (var2.length > 3) {
                        this.sellPrice = this.methods.getPrice(var3, var2[3]);
                        if (this.sellPrice != null) {
                           this.addItemToShopsConfig(var3, this.plugin.versionHandler.getItemInHand(var3), Arrays.asList(var2).contains("-unsafe"));
                        }
                     } else {
                        SendMessage.sendMessage(var3, this.getSyntax());
                     }
                  }
               } else {
                  SendMessage.sendMessage(var3, this.getSyntax());
               }
            }
         } else {
            SendMessage.sendMessage(var3, this.getSyntax());
         }
      }
   }

   @Override
   public List<String> getTabCompletion(String[] var1) {
      switch (var1.length) {
         case 2:
            if (!var1[1].isEmpty()) {
               ArrayList var4 = new ArrayList();
               StringUtil.copyPartialMatches(var1[1], this.plugin.getShopSections(), var4);
               Collections.sort(var4);
               return var4;
            }

            return this.plugin.getShopSections();
         case 3:
         case 4:
            String var2 = var1[var1.length - 1];
            if (!var2.isEmpty()) {
               ArrayList var3 = new ArrayList();
               StringUtil.copyPartialMatches(var2, this.plugin.getExamplePrices(), var3);
               Collections.sort(var3);
               return var3;
            }

            return this.plugin.getExamplePrices();
         default:
            return null;
      }
   }

   private void addItemToShopsConfig(Player var1, ItemStack var2, boolean var3) {
      SendMessage.chatToPlayer(var1, Lang.EDITSHOP_ADDING_ITEM.get().replace("%shop%", this.section));
      String var4 = "pages." + this.getNextAvailablePosition();
      Map<String, Object> var5 = this.plugin.createItem.getShopItem(var2, this.buyPrice, this.sellPrice, var3);

      for (String var7 : var5.keySet()) {
         ConfigManager.getShop(this.section).set(var4 + "." + var7, var5.get(var7));
      }

      ConfigManager.saveShop(this.section);
      SendMessage.chatToPlayer(var1, Lang.EDITSHOP_ADD_ITEM_SUCCESSFUL.get().replace("%shop%", this.section).replace("%itemPath%", var4));
      SendMessage.chatToPlayer(var1, Lang.EDITSHOP_RELOAD_SHOP_TO_SEE_CHANGES.get());
   }

   private String getNextAvailablePosition() {
      Config var1 = ConfigManager.getShop(this.section);
      if (!var1.contains("pages")) {
         return "page1.items.1";
      } else {
         int var2 = var1.getConfigurationSection("pages").getKeys(false).size();
         if (var2 == 0) {
            return "page1.items.1";
         } else {
            LinkedList var3 = new LinkedList(var1.getConfigurationSection("pages").getKeys(false));
            String var4 = (String)var3.get(var2 - 1);
            int var5 = var1.getInt("pages." + var4 + ".gui-rows", 6) * 9 - (this.plugin.navBar.isEnabled(this.section) ? 9 : 0);
            List var6 = ConfigManager.getSection(this.section).getStringList("item-layout");
            if (!var6.isEmpty()) {
               ArrayList<Boolean> var7 = new ArrayList<>();

               for (int var8 = 0; var8 < var5 / 9; var8++) {
                  String[] var9 = ((String)var6.get(var8)).split(" ");
                  if (var9.length != 9) {
                     SendMessage.errorMessage(
                        "Invalid item layout for " + this.section + " page '" + var2 + "': Row " + (var8 + 1) + " contains less or more symbols, expected 9"
                     );
                     return this.getNextAvailablePositionFromShop(var1, var4, var5, var3.size() + 1);
                  }

                  if (Arrays.stream(var9).anyMatch(var0 -> !var0.equals("0") && !var0.equals("1"))) {
                     SendMessage.errorMessage(
                        "Invalid item layout for "
                           + this.section
                           + " page '"
                           + var2
                           + "': Items from row "
                           + (var8 + 1)
                           + " does not match expected values, expected 0 or 1"
                     );
                     return this.getNextAvailablePositionFromShop(var1, var4, var5, var3.size() + 1);
                  }

                  Arrays.stream(var9).forEach(var1x -> var7.add(var1x.equals("1")));
               }

               return this.getNextAvailablePositionFromShop(var1, var4, (int)var7.stream().filter(Boolean::booleanValue).count(), var3.size() + 1);
            } else {
               return this.getNextAvailablePositionFromShop(var1, var4, var5, var3.size() + 1);
            }
         }
      }
   }

   private String getNextAvailablePositionFromShop(Config var1, String var2, int var3, int var4) {
      ConfigurationSection var5 = var1.getConfigurationSection("pages." + var2 + ".items");
      if (var5 == null) {
         return "page" + var2 + ".items.1";
      } else if (var5.getKeys(false).size() >= var3) {
         return "page" + var4 + ".items.1";
      } else {
         Set var6 = var5.getKeys(false);
         int var7 = var6.size() + 1;

         while (var6.contains(String.valueOf(var7))) {
            var7++;
         }

         return var2 + ".items." + var7;
      }
   }
}
