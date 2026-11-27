package com.onyxshop.methodes;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.objects.User;
import com.onyxshop.util.ItemLayout;
import com.onyxshop.util.PermissionsCache;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class CalculateAmount {
   private static OnyxShop plugin;
   private static final DecimalFormat f = new DecimalFormat("0.00");

   public CalculateAmount(OnyxShop plugin2) {
      plugin = plugin2;
   }

   public List<String> loadItemsInOrder(ItemLayout itemLayout, String itemId, String text, Integer value, int value2) {
      LinkedList<String> values = new LinkedList<>();
      String text2 = "pages." + text + ".items";

      for (String shopId : ConfigManager.getShop(itemId).getConfigurationSection(text2).getKeys(false)) {
         if (!ConfigManager.getShop(itemId).getBoolean(text2 + "." + shopId + ".hidden")) {
            try {
               String text3 = ConfigManager.getShop(itemId).getString(text2 + "." + shopId + ".slot");
               ArrayList<Integer> slots = this.getSlots(text3);
               if (slots != null && slots.stream().allMatch(element -> element >= 0)) {
                  for (int value3 : slots) {
                     while (itemLayout.containsKey(value3)) {
                        value3++;
                     }

                     itemLayout.set(value3, text + ".items." + shopId);
                  }
               } else {
                  values.add(text + ".items." + shopId);
               }
            } catch (NumberFormatException exception) {
               SendMessage.warnMessage(
                  "Invalid slot index for format '"
                     + ConfigManager.getShop(itemId).getString(text2 + "." + shopId + ".slot")
                     + "' for item '"
                     + itemId
                     + "."
                     + shopId
                     + "'"
               );
            }
         }
      }

      for (String text3 : values) {
         itemLayout.add(text3);
      }

      return itemLayout.build(value, value2);
   }

   public ArrayList<Integer> getSlots(String text) {
      if (text == null) {
         return null;
      } else {
         try {
            return new ArrayList<>(Collections.singletonList(Integer.parseInt(text) - 1));
         } catch (NumberFormatException exception) {
            ArrayList<Integer> values = new ArrayList<>();
            text = StringUtils.deleteWhitespace(text);

            for (String text2 : text.split(",")) {
               String[] values2 = text2.split("-");
               if (values2.length == 2) {
                  for (int index = Integer.parseInt(values2[0]) - 1; index < Integer.parseInt(values2[1]); index++) {
                     values.add(index);
                  }
               } else if (values2.length == 1) {
                  values.add(Integer.parseInt(values2[0]) - 1);
               }
            }

            if (!values.isEmpty()) {
               return values;
            } else {
               throw new NumberFormatException();
            }
         }
      }
   }

   public static double calculateDiscount(Player player, String playerName, double amount) {
      double amount2 = 0.0;

      for (String key : plugin.getDiscounts(playerName).keySet()) {
         if (PermissionsCache.hasPermission(player, "OnyxShop.discounts." + key)) {
            double amount3 = plugin.getDiscounts(playerName).get(key);
            if (amount2 == 0.0 || amount2 < amount3) {
               amount2 = amount3;
            }
         }
      }

      return Math.max(0.0, amount - amount / 100.0 * amount2);
   }

   public static double calculateMultiplier(Player player, String playerName, double amount) {
      double amount2 = 0.0;

      for (String key : plugin.getMultipliers(playerName).keySet()) {
         if (PermissionsCache.hasPermission(player, "OnyxShop.sell-multipliers." + key)) {
            double amount3 = plugin.getMultipliers(playerName).get(key);
            if (amount2 == 0.0 || amount2 < amount3) {
               amount2 = amount3;
            }
         }
      }

      return Math.max(0.0, amount + amount / 100.0 * amount2);
   }

   public Map<Integer, String> getMainMenuItemSlots() {
      int size = plugin.mainMenuSize;
      if (plugin.navBar.isEnableMainNav()) {
         size -= 9;
      }

      ArrayList<Integer> values = new ArrayList<>();
      ArrayList<String> values2 = new ArrayList<>();
      HashMap<Integer, String> map = new HashMap<>();

      for (String itemId : plugin.getShopSections()) {
         if (!ConfigManager.getSection(itemId).getBoolean("hidden") && !ConfigManager.getSection(itemId).getBoolean("sub-section")) {
            String text = ConfigManager.getSection(itemId).getString("slot");
            if (text == null) {
               values2.add(itemId);
            } else {
               ArrayList<Integer> numbers;
               try {
                  numbers = this.getSlots(text);
               } catch (NumberFormatException exception) {
                  values2.add(itemId);
                  continue;
               }

               ArrayList<Integer> values3 = new ArrayList<>();

               for (Integer value : numbers) {
                  if (value >= 0 && value < size) {
                     if (values.contains(value)) {
                        if (numbers.size() != 1 && !values3.isEmpty()) {
                           SendMessage.errorMessage(
                              "Slot '%slot%' for shop %section% is already used, skipping slot..."
                                 .replace("%section%", itemId)
                                 .replace("%slot%", String.valueOf(value))
                           );
                        } else {
                           values2.add(itemId);
                        }
                     } else {
                        values.add(value);
                        map.put(value, itemId);
                        values3.add(value);
                     }
                  } else if (numbers.size() != 1 && !values3.isEmpty()) {
                     SendMessage.errorMessage(
                        "Slot '%slot%' for shop %section% out of bounds for 0-%maxInvSize%, skipping slot..."
                           .replace("%section%", itemId)
                           .replace("%slot%", String.valueOf(value))
                           .replace("%maxInvSize%", String.valueOf(size))
                     );
                  } else {
                     SendMessage.errorMessage(
                        Lang.CANNOT_DISPLAY_SHOPSECTION.get().replace("%shopsection%", itemId).replace("%maxInvSize%", String.valueOf(size))
                     );
                     values2.add(itemId);
                  }
               }
            }
         }
      }

      for (String text : values2) {
         int value = 0;

         while (values.contains(value)) {
            value++;
         }

         if (value >= size) {
            SendMessage.errorMessage(Lang.CANNOT_DISPLAY_SHOPSECTION.get().replace("%shopsection%", text).replace("%maxInvSize%", String.valueOf(size)));
         } else {
            map.put(value, text);
            values.add(value);
            SendMessage.errorMessage(
               "The place of shop "
                  + text
                  + " in the main shop-inventory has automatically been changed to '"
                  + value
                  + "' because it was already used or could not be found."
            );
         }
      }

      return map;
   }

   public static int getInvSlots(int value) {
      int[] value2 = new int[]{1, 2, 3, 4, 5, 6};
      if (value != 1 && value != 2 && value != 3 && value != 4 && value != 5 && value != 6) {
         int value3 = 0;
         int value4 = Math.abs(value2[0] - value);

         for (int index = 1; index < value2.length; index++) {
            int value6 = Math.abs(value2[index] - value);
            if (value6 < value4) {
               value3 = index;
               value4 = value6;
            }
         }

         value = value2[value3];
      }

      return value * 9;
   }

   public static int getMainInvSlots(User user) {
      return plugin.resizeGUI && user.isBedrock() ? 54 : plugin.mainMenuSize;
   }

   public static int getInvSlots(User user, int value) {
      return plugin.resizeGUI && user.isBedrock() ? 54 : getSlots(value);
   }

   public static int getInvSlotsDef(User user, int value) {
      return plugin.resizeGUI && user.isBedrock() ? 54 : value;
   }

   public static List<String> splitLongString(String text) {
      ArrayList values = new ArrayList();
      String[] values2 = text.split(" ");
      String[] values3 = ChatColor.stripColor(text).split(" ");
      int value = 0;
      StringBuilder builder = new StringBuilder();

      for (int index = 0; index < values3.length; index++) {
         String text2 = values3[index];
         value += text2.length() + 1;
         if (value < 46 && !text2.contains("\n")) {
            if (builder.length() != 0) {
               builder.append(" ");
            }

            builder.append(values2[index]);
         } else {
            String text3 = builder.toString();
            values.add(text3);
            value = text2.length();
            builder = new StringBuilder();
            if (!text2.contains("\n")) {
               builder.append(ChatColor.getLastColors(text3)).append(values2[index]);
            }
         }
      }

      values.add(builder.toString());
      return values;
   }

   public static int getSlots(int value) {
      byte value2 = 54;
      if (value <= 9 && value > 0) {
         value2 = 18;
      } else if (value <= 18 && value > 9) {
         value2 = 27;
      } else if (value <= 27 && value > 18) {
         value2 = 36;
      } else if (value <= 36 && value > 27) {
         value2 = 45;
      }

      return value2;
   }
}
