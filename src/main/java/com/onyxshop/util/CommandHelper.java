package com.onyxshop.util;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ScheduleCache;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.DisplayItem;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.providers.PlaceholderAPIHook;
import com.onyxshop.providers.UserManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class CommandHelper {
   private static OnyxShop plugin;

   public CommandHelper(OnyxShop var1) {
      plugin = var1;
   }

   public static void executeCommandItem(ShopItem var0, Player var1, CommandType var2, Map<EcoType, Double> var3, int var4) {
      executeCommandItem(
         var0,
         var1,
         var2,
         !var0.isABuyPricing() && !var0.isASellPricing()
            ? var3.values().toArray()[0].toString()
            : plugin.formatPrices(
               null, var3, var2 == CommandType.BUY && var0.getABuyPrice().isAll() || var2 == CommandType.SELL && var0.getASellPrice().isAll()
            ),
         var4
      );
   }

   public static void executeCommandItem(ShopItem var0, Player var1, CommandType var2, String var3, int var4) {
      Iterator var5 = plugin.versionHandler.getNBTCommands(var0.getShopItem(), var2).iterator();

      while (true) {
         String var10;
         while (true) {
            if (!var5.hasNext()) {
               return;
            }

            var10 = (String)var5.next();
            var10 = fixCommand(var10);
            if (!var10.contains("::")) {
               break;
            }

            try {
               long var7 = Long.parseLong(var10.split("::")[0]);
               executeDelayedCommand(var0, var1, getCommand(var10.split("::")[1], var0, var4, var3), var7);
               plugin.getDelayedCommandProvider().addDelayedCommand(var10.split("::")[1], var7 / 20L * 1000L, var0, var1, var3, var4);
            } catch (NumberFormatException var9) {
               break;
            }
         }

         executeDelayedCommand(var0, var1, getCommand(var10, var0, var4, var3), 1L);
      }
   }

   public static void executeCommandItem(DisplayItem var0, Player var1, CommandType var2) {
      Iterator var3 = plugin.versionHandler.getNBTCommands(var0.getShopItem(), var2).iterator();

      while (true) {
         String var8;
         while (true) {
            if (!var3.hasNext()) {
               return;
            }

            var8 = (String)var3.next();
            var8 = fixCommand(var8);
            if (!var8.contains("::")) {
               break;
            }

            try {
               long var5 = Long.parseLong(var8.split("::")[0]);
               executeDelayedCommand(var0, var1, var0 instanceof ShopItem ? getCommand(var8.split("::")[1], (ShopItem)var0) : var8.split("::")[1], var5);
               plugin.getDelayedCommandProvider().addDelayedCommand(var8.split("::")[1], var5 / 20L * 1000L, var0, var1);
            } catch (NumberFormatException var7) {
               break;
            }
         }

         executeDelayedCommand(var0, var1, var0 instanceof ShopItem ? getCommand(var8, (ShopItem)var0) : var8, 1L);
      }
   }

   public static void executeClickCommands(List<String> var0, Player var1) {
      Iterator var2 = var0.iterator();

      while (true) {
         String var3;
         while (true) {
            if (!var2.hasNext()) {
               return;
            }

            var3 = (String)var2.next();
            if (!var3.contains("::")) {
               break;
            }

            try {
               long var4 = Long.parseLong(var3.split("::")[0]);
               executeDelayedCommand(null, var1, var3.split("::")[1], var4);
               plugin.getDelayedCommandProvider().addDelayedCommand(var3.split("::")[1], var4 / 20L * 1000L, null, var1);
            } catch (NumberFormatException var6) {
               break;
            }
         }

         executeDelayedCommand(null, var1, var3, 1L);
      }
   }

   public static void executeCommand(DisplayItem var0, Player var1, String var2) {
      var2 = var2.replace("%player%", var1.getName()).replace("%uuid%", var1.getUniqueId().toString());
      if (var2.startsWith("/")) {
         var2 = var2.substring(1);
      } else if (var2.charAt(2) == '/') {
         var2 = var2.replaceFirst("/", "");
      }

      var2 = PlaceholderAPIHook.translate(var1, var2);
      if (var0 != null) {
         SendMessage.logDebugMessage("Executing command '" + var2 + "' from command item " + var0.getItemPath());
      } else {
         SendMessage.logDebugMessage("Executing command item with command '" + var2 + "'");
      }

      UserManager.getUser(var1).setOpenNewGUI(true);
      if (var2.startsWith("p:")) {
         String var3 = var2.substring(2);
         executeCommand(() -> Bukkit.dispatchCommand(var1, var3), var1);
      } else if (var2.startsWith("o:")) {
         String var6 = var2.substring(2);
         executeCommand(() -> {
            boolean var2x = var1.isOp();
            if (!var2x) {
               try {
                  var1.setOp(true);
                  Bukkit.dispatchCommand(var1, var6);
               } finally {
                  var1.setOp(false);
               }
            } else {
               Bukkit.dispatchCommand(var1, var6);
            }
         }, var1);
      } else {
         String var9 = var2;
         executeCommand(() -> Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), var9), null);
      }
   }

   public static void executeDelayedCommand(ScheduleCache.DelayedCommand var0) {
      Map<String, String> var1 = var0.getArgs();
      DisplayItem var2 = var1.containsKey("item") ? plugin.getDisplayItem((String)var1.get("item")) : null;
      int var3 = 1;

      try {
         var3 = Integer.parseInt(var1.getOrDefault("amount", "1"));
      } catch (NumberFormatException var7) {
      }

      String var4 = var2 != null
         ? (var2 instanceof ShopItem ? getCommand(var0.getTask(), (ShopItem)var2, var3, var1.getOrDefault("price", "null")) : var0.getTask())
         : var0.getTask().replace("%quantity%", String.valueOf(var3)).replace("%price%", var1.getOrDefault("price", "null"));
      OfflinePlayer var5 = var1.containsKey("uuid")
         ? Bukkit.getOfflinePlayer(UUID.fromString((String)var1.get("uuid")))
         : (var1.containsKey("player") ? Bukkit.getOfflinePlayer((String)var1.get("player")) : null);
      var4 = PlaceholderAPIHook.translate(var5, var4);
      if (var5 != null && var5.isOnline()) {
         executeCommand(var2, var5.getPlayer(), var4);
      } else if (!var4.startsWith("p:") && !var4.startsWith("o:")) {
         var4 = var4.replace("%player%", var5.getName()).replace("%uuid%", var5.getUniqueId().toString());
         if (var2 != null) {
            SendMessage.logDebugMessage("Executing cached delayed command '" + var4 + "' from command item " + var2.getItemPath());
         } else {
            SendMessage.logDebugMessage("Executing cached delayed command item with command '" + var4 + "'");
         }

         if (var4.startsWith("/")) {
            var4 = var4.substring(1);
         }

         String var6 = var4;
         executeCommand(() -> Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), var6), null);
      } else {
         SendMessage.errorMessage(
            "Failed to execute cached delayed command '" + var4 + "' as player '" + var5.getName() + "' because the target is not online, skipping..."
         );
      }
   }

   private static void executeDelayedCommand(DisplayItem var0, Player var1, String var2, long var3) {
      plugin.runTaskLater(() -> executeCommand(var0, var1, var2), var3);
   }

   private static String getCommand(String var0, ShopItem var1, int var2, String var3) {
      return getCommand(var0, var1).replace("%quantity%", String.valueOf(var2)).replace("%price%", var3);
   }

   private static String getCommand(String var0, ShopItem var1) {
      return var0.replace("%buyPrice%", String.valueOf(var1.isABuyPricing() ? var1.getABuyPrice().getBuyPrices() : var1.getBuyPrice()))
         .replace("%sellPrice%", String.valueOf(var1.isASellPricing() ? var1.getASellPrice().getSellPrices() : var1.getSellPrice()));
   }

   private static String fixCommand(String var0) {
      return var0.startsWith("\"") && var0.endsWith("\"")
         ? var0.substring(1, var0.length() - 1)
         : (var0.startsWith("'") && var0.endsWith("'") ? var0.substring(1, var0.length() - 1) : var0);
   }

   private static void executeCommand(Runnable var0, Player var1) {
      if (ServerInfo.isFolia()) {
         if (var1 != null) {
            plugin.runTaskFor(var0, var1);
         } else {
            plugin.runTask(var0);
         }
      } else {
         var0.run();
      }
   }
}
