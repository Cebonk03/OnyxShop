package com.onyxshop.commands.editshop.subcommands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import com.onyxshop.OnyxShop;
import com.onyxshop.commands.editshop.Methods;
import com.onyxshop.commands.editshop.SubCommad;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.files.config.Config;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.shops.ShopType;
import com.onyxshop.util.PermissionsCache;
import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;

public class AddItem extends SubCommad {
   private final Methods methods;
   private final OnyxShop plugin;
   String section;
   String material;
   String displayname;
   Double buyPrice;
   Double sellPrice;

   public AddItem(OnyxShop var1, Methods var2) {
      this.plugin = var1;
      this.methods = var2;
   }

   @Override
   public String getName() {
      return "additem";
   }

   @Override
   public String getDescription() {
      return Lang.EDITSHOP_ADD_ITEM_SUBCOMMAND_DESC.get().getLegacy();
   }

   @Override
   public String getSyntax() {
      return Lang.EDITSHOP_ADD_ITEM_SUBCOMMAND_SYNTAX.get().getLegacy();
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
            if (var2.length > 2) {
               this.material = this.methods.getMaterial(var1, var2[2]);
               if (this.material != null) {
                  if (var2.length > 3) {
                     this.buyPrice = this.methods.getPrice(var1, var2[3]);
                     if (this.buyPrice != null) {
                        if (var2.length > 4) {
                           this.sellPrice = this.methods.getPrice(var1, var2[4]);
                           if (this.sellPrice != null) {
                              if (var2.length > 5) {
                                 StringBuilder var3 = new StringBuilder();

                                 for (int var4 = 5; var4 < var2.length; var4++) {
                                    var3.append(var2[var4]).append(" ");
                                 }

                                 this.displayname = var3.substring(0, var3.length() - 1);
                              }

                              this.addItemToShopsConfig(var1);
                           }
                        } else {
                           SendMessage.sendMessage(var1, this.getSyntax());
                        }
                     }
                  } else {
                     SendMessage.sendMessage(var1, this.getSyntax());
                  }
               }
            } else {
               SendMessage.sendMessage(var1, this.getSyntax());
            }
         }
      } else {
         SendMessage.sendMessage(var1, this.getSyntax());
      }
   }

   @Override
   public List<String> getTabCompletion(String[] var1) {
      switch (var1.length) {
         case 2:
            if (!var1[1].isEmpty()) {
               ArrayList var5 = new ArrayList();
               StringUtil.copyPartialMatches(var1[1], this.plugin.getShopSections(), var5);
               Collections.sort(var5);
               return var5;
            }

            return this.plugin.getShopSections();
         case 3:
            if (!var1[2].isEmpty()) {
               ArrayList var4 = new ArrayList();
               StringUtil.copyPartialMatches(var1[2], this.plugin.getSupportedMatNames(), var4);
               Collections.sort(var4);
               return var4;
            }

            return this.plugin.getSupportedMatNames();
         case 4:
         case 5:
            String var2 = var1[var1.length - 1];
            if (!var2.isEmpty()) {
               ArrayList var6 = new ArrayList();
               StringUtil.copyPartialMatches(var2, this.plugin.getExamplePrices(), var6);
               Collections.sort(var6);
               return var6;
            }

            return this.plugin.getExamplePrices();
         case 6:
            if (!var1[5].isEmpty()) {
               ArrayList var3 = new ArrayList();
               StringUtil.copyPartialMatches(var1[0], this.methods.getExampleItemNames(), var3);
               Collections.sort(var3);
               return var3;
            }

            return this.methods.getExampleItemNames();
         default:
            return null;
      }
   }

   private void addItemToShopsConfig(Object var1) {
      SendMessage.sendMessage(var1, Lang.EDITSHOP_ADDING_ITEM.get().replace("%shop%", this.section));
      String var2 = this.getNextAvailablePosition();
      ConfigManager.getShop(this.section).set(var2 + ".material", this.material);
      if (this.displayname != null) {
         ConfigManager.getShop(this.section).set(var2 + ".displayname", this.displayname);
      }

      ConfigManager.getShop(this.section).set(var2 + ".buy", this.buyPrice);
      ConfigManager.getShop(this.section).set(var2 + ".sell", this.sellPrice);
      ConfigManager.saveShop(this.section);
      SendMessage.sendMessage(var1, Lang.EDITSHOP_ADD_ITEM_SUCCESSFUL.get().replace("%shop%", this.section).replace("%itemPath%", var2));
      SendMessage.sendMessage(var1, Lang.EDITSHOP_RELOAD_SHOP_TO_SEE_CHANGES.get());
   }

   private String getNextAvailablePosition() {
      Config var1 = ConfigManager.getShop(this.section);
      if (this.plugin.getShopType(this.section) == ShopType.CATEGORY) {
         int var8 = var1.getConfigurationSection("pages").getKeys(false).size();
         if (var8 == 0) {
            return "pages.page1.items.1";
         } else {
            LinkedList var9 = new LinkedList(var1.getConfigurationSection("pages").getKeys(false));
            String var4 = (String)var9.get(var8 - 1);
            int var5 = var1.getInt("pages." + var4 + ".gui-rows", 6) * 9 - (this.plugin.navBar.isEnabled(this.section) ? 9 : 0);
            if (var1.getConfigurationSection("pages." + var4 + ".items").getKeys(false).size() >= var5) {
               return "pages.page" + (var9.size() + 1) + ".items.1";
            } else {
               Set var6 = var1.getConfigurationSection("pages." + var4 + ".items").getKeys(false);
               int var7 = var6.size() + 1;

               while (var6.contains(String.valueOf(var7))) {
                  var7++;
               }

               return "pages." + var4 + ".items." + var7;
            }
         }
      } else if (this.plugin.getShopType(this.section) != ShopType.ROTATING) {
         return null;
      } else {
         Set var2 = var1.getConfigurationSection("items").getKeys(false);
         int var3 = var2.size() + 1;

         while (var2.contains(String.valueOf(var3))) {
            var3++;
         }

         return "items." + var3;
      }
   }
}
