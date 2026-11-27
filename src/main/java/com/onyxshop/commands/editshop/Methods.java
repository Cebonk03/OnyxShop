package com.onyxshop.commands.editshop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.util.XMaterial;
import org.bukkit.ChatColor;

public class Methods {
   private final OnyxShop plugin;

   public Methods(OnyxShop plugin) {
      this.plugin = plugin;
   }

   public String getSection(Object value, String sectionId) {
      if (this.plugin.getShopSections().contains(sectionId)) {
         return sectionId;
      } else {
         SendMessage.sendMessage(value, Lang.NO_SHOP_FOUND.get());
         return null;
      }
   }

   public String getMaterial(Object value, String materialName) {
      Optional optional = XMaterial.matchXMaterial((String)materialName);
      if (optional.isPresent()) {
         if (((XMaterial)optional.get()).parseItem() != null) {
            return (String)materialName;
         }

         SendMessage.warnMessage(value, Lang.MATERIAL_NOT_SUPPORTED.get());
      } else {
         SendMessage.warnMessage(value, Lang.ITEM_MATERIAL_NULL.get().replace("%material%", ChatColor.DARK_RED + materialName));
      }

      return null;
   }

   public Double getPrice(Object value, Object value2) {
      try {
         return Double.parseDouble(value2.toString());
      } catch (NumberFormatException exception) {
         SendMessage.sendMessage(value, Lang.NO_VALID_AMOUNT.get());
         return null;
      }
   }

   public String getShopItemLoc(Object value, String shopId, String itemId) {
      if (this.plugin.getSection(shopId).getItemLocs().contains(itemId)) {
         if (this.plugin.getSection(shopId).isActionItem(shopId, itemId)) {
            SendMessage.sendMessage(value, Lang.CANNOT_DO_THAT_ACTION_ITEM.get());
            return null;
         } else {
            return itemId;
         }
      } else {
         SendMessage.sendMessage(value, Lang.SECTION_DOES_NOT_CONTAIN_ITEM.get().replace("%shopsection%", shopId).replace("%itemLoc%", itemId));
         return null;
      }
   }

   public String getItemLoc(Object value, String shopId, String itemLoc) {
      if (this.plugin.getSection(shopId).getItemLocs().contains(itemLoc)) {
         return itemLoc;
      } else {
         SendMessage.sendMessage(value, Lang.SECTION_DOES_NOT_CONTAIN_ITEM.get().replace("%shopsection%", shopId).replace("%itemLoc%", itemLoc));
         return null;
      }
   }

   public final List<String> getExampleRGBColors() {
      return Arrays.asList("0x0000FF", "0x008000", "0xFF0000", "0xFFFF00");
   }

   public final List<String> getExampleItemNames() {
      return Arrays.asList("&aGreen carpet", "&bDiamond", "&c&lRedstone dust", "&0Bedrock", "#979797Gunpowder", "&aRed flower");
   }

   public Integer getMainMenuSlot(Object value, String text) {
      try {
         int slot = Integer.parseInt(text);
         if (this.plugin.getMainMenuItemSlots().containsKey(slot)) {
            SendMessage.sendMessage(
               value, Lang.SLOT_ALREADY_IN_USE.get().replace("%available-slots%", Arrays.toString(this.getAvailableMainMenuSlots().toArray()))
            );
            return null;
         } else {
            return slot;
         }
      } catch (NumberFormatException exception) {
         SendMessage.sendMessage(value, Lang.SLOT_NEEDS_TO_BE_NUMBER.get());
         return null;
      }
   }

   public List<String> getAvailableMainMenuSlots() {
      ArrayList values = new ArrayList();

      for (int index = 1; index < 46; index++) {
         values.add(String.valueOf(index));
      }

      this.plugin.getMainMenuItemSlots().keySet().forEach(entry -> values.remove(String.valueOf(entry)));
      return values;
   }
}
