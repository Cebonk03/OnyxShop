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

   public AddItem(OnyxShop plugin, Methods methods) {
      this.plugin = plugin;
      this.methods = methods;
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
   public boolean hasPermission(CommandSender sender) {
      return PermissionsCache.hasPermission(sender, "OnyxShop.editshop." + this.getName());
   }

   @Override
   public void perform(Object value, String[] values) {
      if (values.length > 1) {
         this.section = this.methods.getSection(value, values[1]);
         if (this.section != null) {
            if (values.length > 2) {
               this.material = this.methods.getMaterial(value, values[2]);
               if (this.material != null) {
                  if (values.length > 3) {
                     this.buyPrice = this.methods.getPrice(value, values[3]);
                     if (this.buyPrice != null) {
                        if (values.length > 4) {
                           this.sellPrice = this.methods.getPrice(value, values[4]);
                           if (this.sellPrice != null) {
                              if (values.length > 5) {
                                 StringBuilder builder = new StringBuilder();

                                 for (int index = 5; index < values.length; index++) {
                                    builder.append(values[index]).append(" ");
                                 }

                                 this.displayname = builder.substring(0, builder.length() - 1);
                              }

                              this.addItemToShopsConfig(value);
                           }
                        } else {
                           SendMessage.sendMessage(value, this.getSyntax());
                        }
                     }
                  } else {
                     SendMessage.sendMessage(value, this.getSyntax());
                  }
               }
            } else {
               SendMessage.sendMessage(value, this.getSyntax());
            }
         }
      } else {
         SendMessage.sendMessage(value, this.getSyntax());
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
         case 3:
            if (!values[2].isEmpty()) {
               ArrayList values2 = new ArrayList();
               StringUtil.copyPartialMatches(values[2], this.plugin.getSupportedMatNames(), values2);
               Collections.sort(values2);
               return values2;
            }

            return this.plugin.getSupportedMatNames();
         case 4:
         case 5:
            String text = values[values.length - 1];
            if (!text.isEmpty()) {
               ArrayList values2 = new ArrayList();
               StringUtil.copyPartialMatches(text, this.plugin.getExamplePrices(), values2);
               Collections.sort(values2);
               return values2;
            }

            return this.plugin.getExamplePrices();
         case 6:
            if (!values[5].isEmpty()) {
               ArrayList values2 = new ArrayList();
               StringUtil.copyPartialMatches(values[0], this.methods.getExampleItemNames(), values2);
               Collections.sort(values2);
               return values2;
            }

            return this.methods.getExampleItemNames();
         default:
            return null;
      }
   }

   private void addItemToShopsConfig(Object value) {
      SendMessage.sendMessage(value, Lang.EDITSHOP_ADDING_ITEM.get().replace("%shop%", this.section));
      String nextAvailablePosition = this.getNextAvailablePosition();
      ConfigManager.getShop(this.section).set(nextAvailablePosition + ".material", this.material);
      if (this.displayname != null) {
         ConfigManager.getShop(this.section).set(nextAvailablePosition + ".displayname", this.displayname);
      }

      ConfigManager.getShop(this.section).set(nextAvailablePosition + ".buy", this.buyPrice);
      ConfigManager.getShop(this.section).set(nextAvailablePosition + ".sell", this.sellPrice);
      ConfigManager.saveShop(this.section);
      SendMessage.sendMessage(value, Lang.EDITSHOP_ADD_ITEM_SUCCESSFUL.get().replace("%shop%", this.section).replace("%itemPath%", nextAvailablePosition));
      SendMessage.sendMessage(value, Lang.EDITSHOP_RELOAD_SHOP_TO_SEE_CHANGES.get());
   }

   private String getNextAvailablePosition() {
      Config config = ConfigManager.getShop(this.section);
      if (this.plugin.getShopType(this.section) == ShopType.CATEGORY) {
         int size = config.getConfigurationSection("pages").getKeys(false).size();
         if (size == 0) {
            return "pages.page1.items.1";
         } else {
            LinkedList sectionIds = new LinkedList(config.getConfigurationSection("pages").getKeys(false));
            String text = (String)sectionIds.get(size - 1);
            int value = config.getInt("pages." + text + ".gui-rows", 6) * 9 - (this.plugin.navBar.isEnabled(this.section) ? 9 : 0);
            if (config.getConfigurationSection("pages." + text + ".items").getKeys(false).size() >= value) {
               return "pages.page" + (sectionIds.size() + 1) + ".items.1";
            } else {
               Set keys = config.getConfigurationSection("pages." + text + ".items").getKeys(false);
               int value2 = keys.size() + 1;

               while (keys.contains(String.valueOf(value2))) {
                  value2++;
               }

               return "pages." + text + ".items." + value2;
            }
         }
      } else if (this.plugin.getShopType(this.section) != ShopType.ROTATING) {
         return null;
      } else {
         Set keys = config.getConfigurationSection("items").getKeys(false);
         int value = keys.size() + 1;

         while (keys.contains(String.valueOf(value))) {
            value++;
         }

         return "items." + value;
      }
   }
}
