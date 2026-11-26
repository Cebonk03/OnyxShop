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

   public Methods(OnyxShop var1) {
      this.plugin = var1;
   }

   public String getSection(Object var1, String var2) {
      if (this.plugin.getShopSections().contains(var2)) {
         return var2;
      } else {
         SendMessage.sendMessage(var1, Lang.NO_SHOP_FOUND.get());
         return null;
      }
   }

   public String getMaterial(Object var1, String var2) {
      Optional var3 = XMaterial.matchXMaterial((String)var2);
      if (var3.isPresent()) {
         if (((XMaterial)var3.get()).parseItem() != null) {
            return (String)var2;
         }

         SendMessage.warnMessage(var1, Lang.MATERIAL_NOT_SUPPORTED.get());
      } else {
         SendMessage.warnMessage(var1, Lang.ITEM_MATERIAL_NULL.get().replace("%material%", ChatColor.DARK_RED + var2));
      }

      return null;
   }

   public Double getPrice(Object var1, Object var2) {
      try {
         return Double.parseDouble(var2.toString());
      } catch (NumberFormatException var4) {
         SendMessage.sendMessage(var1, Lang.NO_VALID_AMOUNT.get());
         return null;
      }
   }

   public String getShopItemLoc(Object var1, String var2, String var3) {
      if (this.plugin.getSection(var2).getItemLocs().contains(var3)) {
         if (this.plugin.getSection(var2).isActionItem(var2, var3)) {
            SendMessage.sendMessage(var1, Lang.CANNOT_DO_THAT_ACTION_ITEM.get());
            return null;
         } else {
            return var3;
         }
      } else {
         SendMessage.sendMessage(var1, Lang.SECTION_DOES_NOT_CONTAIN_ITEM.get().replace("%shopsection%", var2).replace("%itemLoc%", var3));
         return null;
      }
   }

   public String getItemLoc(Object var1, String var2, String var3) {
      if (this.plugin.getSection(var2).getItemLocs().contains(var3)) {
         return var3;
      } else {
         SendMessage.sendMessage(var1, Lang.SECTION_DOES_NOT_CONTAIN_ITEM.get().replace("%shopsection%", var2).replace("%itemLoc%", var3));
         return null;
      }
   }

   public final List<String> getExampleRGBColors() {
      return Arrays.asList("0x0000FF", "0x008000", "0xFF0000", "0xFFFF00");
   }

   public final List<String> getExampleItemNames() {
      return Arrays.asList("&aGreen carpet", "&bDiamond", "&c&lRedstone dust", "&0Bedrock", "#979797Gunpowder", "&aRed flower");
   }

   public Integer getMainMenuSlot(Object var1, String var2) {
      try {
         int var3 = Integer.parseInt(var2);
         if (this.plugin.getMainMenuItemSlots().containsKey(var3)) {
            SendMessage.sendMessage(
               var1, Lang.SLOT_ALREADY_IN_USE.get().replace("%available-slots%", Arrays.toString(this.getAvailableMainMenuSlots().toArray()))
            );
            return null;
         } else {
            return var3;
         }
      } catch (NumberFormatException var4) {
         SendMessage.sendMessage(var1, Lang.SLOT_NEEDS_TO_BE_NUMBER.get());
         return null;
      }
   }

   public List<String> getAvailableMainMenuSlots() {
      ArrayList var1 = new ArrayList();

      for (int var2 = 1; var2 < 46; var2++) {
         var1.add(String.valueOf(var2));
      }

      this.plugin.getMainMenuItemSlots().keySet().forEach(var1x -> var1.remove(String.valueOf(var1x)));
      return var1;
   }
}
