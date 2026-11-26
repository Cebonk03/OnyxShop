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

   public CalculateAmount(OnyxShop var1) {
      plugin = var1;
   }

   public List<String> loadItemsInOrder(ItemLayout var1, String var2, String var3, Integer var4, int var5) {
      LinkedList<String> var6 = new LinkedList<>();
      String var7 = "pages." + var3 + ".items";

      for (String var9 : ConfigManager.getShop(var2).getConfigurationSection(var7).getKeys(false)) {
         if (!ConfigManager.getShop(var2).getBoolean(var7 + "." + var9 + ".hidden")) {
            try {
               String var10 = ConfigManager.getShop(var2).getString(var7 + "." + var9 + ".slot");
               ArrayList<Integer> var11 = this.getSlots(var10);
               if (var11 != null && var11.stream().allMatch(var0 -> var0 >= 0)) {
                  for (int var13 : var11) {
                     while (var1.containsKey(var13)) {
                        var13++;
                     }

                     var1.set(var13, var3 + ".items." + var9);
                  }
               } else {
                  var6.add(var3 + ".items." + var9);
               }
            } catch (NumberFormatException var14) {
               SendMessage.warnMessage(
                  "Invalid slot index for format '"
                     + ConfigManager.getShop(var2).getString(var7 + "." + var9 + ".slot")
                     + "' for item '"
                     + var2
                     + "."
                     + var9
                     + "'"
               );
            }
         }
      }

      for (String var16 : var6) {
         var1.add(var16);
      }

      return var1.build(var4, var5);
   }

   public ArrayList<Integer> getSlots(String var1) {
      if (var1 == null) {
         return null;
      } else {
         try {
            return new ArrayList<>(Collections.singletonList(Integer.parseInt(var1) - 1));
         } catch (NumberFormatException var10) {
            ArrayList<Integer> var3 = new ArrayList<>();
            var1 = StringUtils.deleteWhitespace(var1);

            for (String var7 : var1.split(",")) {
               String[] var8 = var7.split("-");
               if (var8.length == 2) {
                  for (int var9 = Integer.parseInt(var8[0]) - 1; var9 < Integer.parseInt(var8[1]); var9++) {
                     var3.add(var9);
                  }
               } else if (var8.length == 1) {
                  var3.add(Integer.parseInt(var8[0]) - 1);
               }
            }

            if (!var3.isEmpty()) {
               return var3;
            } else {
               throw new NumberFormatException();
            }
         }
      }
   }

   public static double calculateDiscount(Player var0, String var1, double var2) {
      double var4 = 0.0;

      for (String var7 : plugin.getDiscounts(var1).keySet()) {
         if (PermissionsCache.hasPermission(var0, "OnyxShop.discounts." + var7)) {
            double var8 = plugin.getDiscounts(var1).get(var7);
            if (var4 == 0.0 || var4 < var8) {
               var4 = var8;
            }
         }
      }

      return Math.max(0.0, var2 - var2 / 100.0 * var4);
   }

   public static double calculateMultiplier(Player var0, String var1, double var2) {
      double var4 = 0.0;

      for (String var7 : plugin.getMultipliers(var1).keySet()) {
         if (PermissionsCache.hasPermission(var0, "OnyxShop.sell-multipliers." + var7)) {
            double var8 = plugin.getMultipliers(var1).get(var7);
            if (var4 == 0.0 || var4 < var8) {
               var4 = var8;
            }
         }
      }

      return Math.max(0.0, var2 + var2 / 100.0 * var4);
   }

   public Map<Integer, String> getMainMenuItemSlots() {
      int var1 = plugin.mainMenuSize;
      if (plugin.navBar.isEnableMainNav()) {
         var1 -= 9;
      }

      ArrayList<Integer> var2 = new ArrayList<>();
      ArrayList<String> var3 = new ArrayList<>();
      HashMap<Integer, String> var4 = new HashMap<>();

      for (String var6 : plugin.getShopSections()) {
         if (!ConfigManager.getSection(var6).getBoolean("hidden") && !ConfigManager.getSection(var6).getBoolean("sub-section")) {
            String var7 = ConfigManager.getSection(var6).getString("slot");
            if (var7 == null) {
               var3.add(var6);
            } else {
               ArrayList<Integer> var8;
               try {
                  var8 = this.getSlots(var7);
               } catch (NumberFormatException var12) {
                  var3.add(var6);
                  continue;
               }

               ArrayList<Integer> var9 = new ArrayList<>();

               for (Integer var11 : var8) {
                  if (var11 >= 0 && var11 < var1) {
                     if (var2.contains(var11)) {
                        if (var8.size() != 1 && !var9.isEmpty()) {
                           SendMessage.errorMessage(
                              "Slot '%slot%' for shop %section% is already used, skipping slot..."
                                 .replace("%section%", var6)
                                 .replace("%slot%", String.valueOf(var11))
                           );
                        } else {
                           var3.add(var6);
                        }
                     } else {
                        var2.add(var11);
                        var4.put(var11, var6);
                        var9.add(var11);
                     }
                  } else if (var8.size() != 1 && !var9.isEmpty()) {
                     SendMessage.errorMessage(
                        "Slot '%slot%' for shop %section% out of bounds for 0-%maxInvSize%, skipping slot..."
                           .replace("%section%", var6)
                           .replace("%slot%", String.valueOf(var11))
                           .replace("%maxInvSize%", String.valueOf(var1))
                     );
                  } else {
                     SendMessage.errorMessage(
                        Lang.CANNOT_DISPLAY_SHOPSECTION.get().replace("%shopsection%", var6).replace("%maxInvSize%", String.valueOf(var1))
                     );
                     var3.add(var6);
                  }
               }
            }
         }
      }

      for (String var14 : var3) {
         int var15 = 0;

         while (var2.contains(var15)) {
            var15++;
         }

         if (var15 >= var1) {
            SendMessage.errorMessage(Lang.CANNOT_DISPLAY_SHOPSECTION.get().replace("%shopsection%", var14).replace("%maxInvSize%", String.valueOf(var1)));
         } else {
            var4.put(var15, var14);
            var2.add(var15);
            SendMessage.errorMessage(
               "The place of shop "
                  + var14
                  + " in the main shop-inventory has automatically been changed to '"
                  + var15
                  + "' because it was already used or could not be found."
            );
         }
      }

      return var4;
   }

   public static int getInvSlots(int var0) {
      int[] var1 = new int[]{1, 2, 3, 4, 5, 6};
      if (var0 != 1 && var0 != 2 && var0 != 3 && var0 != 4 && var0 != 5 && var0 != 6) {
         int var2 = 0;
         int var3 = Math.abs(var1[0] - var0);

         for (int var4 = 1; var4 < var1.length; var4++) {
            int var5 = Math.abs(var1[var4] - var0);
            if (var5 < var3) {
               var2 = var4;
               var3 = var5;
            }
         }

         var0 = var1[var2];
      }

      return var0 * 9;
   }

   public static int getMainInvSlots(User var0) {
      return plugin.resizeGUI && var0.isBedrock() ? 54 : plugin.mainMenuSize;
   }

   public static int getInvSlots(User var0, int var1) {
      return plugin.resizeGUI && var0.isBedrock() ? 54 : getSlots(var1);
   }

   public static int getInvSlotsDef(User var0, int var1) {
      return plugin.resizeGUI && var0.isBedrock() ? 54 : var1;
   }

   public static List<String> splitLongString(String var0) {
      ArrayList var1 = new ArrayList();
      String[] var2 = var0.split(" ");
      String[] var3 = ChatColor.stripColor(var0).split(" ");
      int var4 = 0;
      StringBuilder var5 = new StringBuilder();

      for (int var6 = 0; var6 < var3.length; var6++) {
         String var7 = var3[var6];
         var4 += var7.length() + 1;
         if (var4 < 46 && !var7.contains("\n")) {
            if (var5.length() != 0) {
               var5.append(" ");
            }

            var5.append(var2[var6]);
         } else {
            String var8 = var5.toString();
            var1.add(var8);
            var4 = var7.length();
            var5 = new StringBuilder();
            if (!var7.contains("\n")) {
               var5.append(ChatColor.getLastColors(var8)).append(var2[var6]);
            }
         }
      }

      var1.add(var5.toString());
      return var1;
   }

   public static int getSlots(int var0) {
      byte var1 = 54;
      if (var0 <= 9 && var0 > 0) {
         var1 = 18;
      } else if (var0 <= 18 && var0 > 9) {
         var1 = 27;
      } else if (var0 <= 27 && var0 > 18) {
         var1 = 36;
      } else if (var0 <= 36 && var0 > 27) {
         var1 = 45;
      }

      return var1;
   }
}
