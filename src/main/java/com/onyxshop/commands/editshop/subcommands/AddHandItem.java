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

   public AddHandItem(OnyxShop plugin, Methods methods) {
      this.plugin = plugin;
      this.methods = methods;
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
   public boolean hasPermission(CommandSender sender) {
      return PermissionsCache.hasPermission(sender, "OnyxShop.editshop." + this.getName());
   }

   @Override
   public void perform(Object value, String[] values) {
      if (value instanceof Player) {
         Player player = (Player)value;
         if (values.length > 1) {
            this.section = this.methods.getSection(player, values[1]);
            if (this.section != null) {
               if (values.length > 2) {
                  this.buyPrice = this.methods.getPrice(player, values[2]);
                  if (this.buyPrice != null) {
                     if (values.length > 3) {
                        this.sellPrice = this.methods.getPrice(player, values[3]);
                        if (this.sellPrice != null) {
                           this.addItemToShopsConfig(player, this.plugin.versionHandler.getItemInHand(player), Arrays.asList(values).contains("-unsafe"));
                        }
                     } else {
                        SendMessage.sendMessage(player, this.getSyntax());
                     }
                  }
               } else {
                  SendMessage.sendMessage(player, this.getSyntax());
               }
            }
         } else {
            SendMessage.sendMessage(player, this.getSyntax());
         }
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
         case 4:
            String text = values[values.length - 1];
            if (!text.isEmpty()) {
               ArrayList values2 = new ArrayList();
               StringUtil.copyPartialMatches(text, this.plugin.getExamplePrices(), values2);
               Collections.sort(values2);
               return values2;
            }

            return this.plugin.getExamplePrices();
         default:
            return null;
      }
   }

   private void addItemToShopsConfig(Player player, ItemStack itemStack, boolean flag) {
      SendMessage.chatToPlayer(player, Lang.EDITSHOP_ADDING_ITEM.get().replace("%shop%", this.section));
      String itemId = "pages." + this.getNextAvailablePosition();
      Map<String, Object> shopItem = this.plugin.createItem.getShopItem(itemStack, this.buyPrice, this.sellPrice, flag);

      for (String key : shopItem.keySet()) {
         ConfigManager.getShop(this.section).set(itemId + "." + key, shopItem.get(key));
      }

      ConfigManager.saveShop(this.section);
      SendMessage.chatToPlayer(player, Lang.EDITSHOP_ADD_ITEM_SUCCESSFUL.get().replace("%shop%", this.section).replace("%itemPath%", itemId));
      SendMessage.chatToPlayer(player, Lang.EDITSHOP_RELOAD_SHOP_TO_SEE_CHANGES.get());
   }

   private String getNextAvailablePosition() {
      Config config = ConfigManager.getShop(this.section);
      if (!config.contains("pages")) {
         return "page1.items.1";
      } else {
         int size = config.getConfigurationSection("pages").getKeys(false).size();
         if (size == 0) {
            return "page1.items.1";
         } else {
            LinkedList sectionIds = new LinkedList(config.getConfigurationSection("pages").getKeys(false));
            String text = (String)sectionIds.get(size - 1);
            int value = config.getInt("pages." + text + ".gui-rows", 6) * 9 - (this.plugin.navBar.isEnabled(this.section) ? 9 : 0);
            List stringList = ConfigManager.getSection(this.section).getStringList("item-layout");
            if (!stringList.isEmpty()) {
               ArrayList<Boolean> values = new ArrayList<>();

               for (int index = 0; index < value / 9; index++) {
                  String[] values2 = ((String)stringList.get(index)).split(" ");
                  if (values2.length != 9) {
                     SendMessage.errorMessage(
                        "Invalid item layout for " + this.section + " page '" + size + "': Row " + (index + 1) + " contains less or more symbols, expected 9"
                     );
                     return this.getNextAvailablePositionFromShop(config, text, value, sectionIds.size() + 1);
                  }

                  if (Arrays.stream(values2).anyMatch(element -> !element.equals("0") && !element.equals("1"))) {
                     SendMessage.errorMessage(
                        "Invalid item layout for "
                           + this.section
                           + " page '"
                           + size
                           + "': Items from row "
                           + (index + 1)
                           + " does not match expected values, expected 0 or 1"
                     );
                     return this.getNextAvailablePositionFromShop(config, text, value, sectionIds.size() + 1);
                  }

                  Arrays.stream(values2).forEach(entry -> values.add(entry.equals("1")));
               }

               return this.getNextAvailablePositionFromShop(config, text, (int)values.stream().filter(Boolean::booleanValue).count(), sectionIds.size() + 1);
            } else {
               return this.getNextAvailablePositionFromShop(config, text, value, sectionIds.size() + 1);
            }
         }
      }
   }

   private String getNextAvailablePositionFromShop(Config config, String shopId, int value, int value2) {
      ConfigurationSection section = config.getConfigurationSection("pages." + shopId + ".items");
      if (section == null) {
         return "page" + shopId + ".items.1";
      } else if (section.getKeys(false).size() >= value) {
         return "page" + value2 + ".items.1";
      } else {
         Set keys = section.getKeys(false);
         int size = keys.size() + 1;

         while (keys.contains(String.valueOf(size))) {
            size++;
         }

         return shopId + ".items." + size;
      }
   }
}
