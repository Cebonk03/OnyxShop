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

   public CommandHelper(OnyxShop plugin2) {
      plugin = plugin2;
   }

   public static void executeCommandItem(ShopItem shopItem, Player player, CommandType commandType, Map<EcoType, Double> map, int value) {
      executeCommandItem(
         shopItem,
         player,
         commandType,
         !shopItem.isABuyPricing() && !shopItem.isASellPricing()
            ? map.values().toArray()[0].toString()
            : plugin.formatPrices(
               null, map, commandType == CommandType.BUY && shopItem.getABuyPrice().isAll() || commandType == CommandType.SELL && shopItem.getASellPrice().isAll()
            ),
         value
      );
   }

   public static void executeCommandItem(ShopItem shopItem, Player player, CommandType commandType, String command, int value) {
      Iterator iterator = plugin.versionHandler.getNBTCommands(shopItem.getShopItem(), commandType).iterator();

      while (true) {
         String itemId;
         while (true) {
            if (!iterator.hasNext()) {
               return;
            }

            itemId = (String)iterator.next();
            itemId = fixCommand(itemId);
            if (!itemId.contains("::")) {
               break;
            }

            try {
               long value2 = Long.parseLong(itemId.split("::")[0]);
               executeDelayedCommand(shopItem, player, getCommand(itemId.split("::")[1], shopItem, value, command), value2);
               plugin.getDelayedCommandProvider().addDelayedCommand(itemId.split("::")[1], value2 / 20L * 1000L, shopItem, player, command, value);
            } catch (NumberFormatException exception) {
               break;
            }
         }

         executeDelayedCommand(shopItem, player, getCommand(itemId, shopItem, value, command), 1L);
      }
   }

   public static void executeCommandItem(DisplayItem displayItem, Player player, CommandType commandType) {
      Iterator iterator = plugin.versionHandler.getNBTCommands(displayItem.getShopItem(), commandType).iterator();

      while (true) {
         String command;
         while (true) {
            if (!iterator.hasNext()) {
               return;
            }

            command = (String)iterator.next();
            command = fixCommand(command);
            if (!command.contains("::")) {
               break;
            }

            try {
               long value = Long.parseLong(command.split("::")[0]);
               executeDelayedCommand(displayItem, player, displayItem instanceof ShopItem ? getCommand(command.split("::")[1], (ShopItem)displayItem) : command.split("::")[1], value);
               plugin.getDelayedCommandProvider().addDelayedCommand(command.split("::")[1], value / 20L * 1000L, displayItem, player);
            } catch (NumberFormatException exception) {
               break;
            }
         }

         executeDelayedCommand(displayItem, player, displayItem instanceof ShopItem ? getCommand(command, (ShopItem)displayItem) : command, 1L);
      }
   }

   public static void executeClickCommands(List<String> playerNames, Player player) {
      Iterator iterator = playerNames.iterator();

      while (true) {
         String text;
         while (true) {
            if (!iterator.hasNext()) {
               return;
            }

            text = (String)iterator.next();
            if (!text.contains("::")) {
               break;
            }

            try {
               long value = Long.parseLong(text.split("::")[0]);
               executeDelayedCommand(null, player, text.split("::")[1], value);
               plugin.getDelayedCommandProvider().addDelayedCommand(text.split("::")[1], value / 20L * 1000L, null, player);
            } catch (NumberFormatException exception) {
               break;
            }
         }

         executeDelayedCommand(null, player, text, 1L);
      }
   }

   public static void executeCommand(DisplayItem displayItem, Player player, String command) {
      command = command.replace("%player%", player.getName()).replace("%uuid%", player.getUniqueId().toString());
      if (command.startsWith("/")) {
         command = command.substring(1);
      } else if (command.charAt(2) == '/') {
         command = command.replaceFirst("/", "");
      }

      command = PlaceholderAPIHook.translate(player, command);
      if (displayItem != null) {
         SendMessage.logDebugMessage("Executing command '" + command + "' from command item " + displayItem.getItemPath());
      } else {
         SendMessage.logDebugMessage("Executing command item with command '" + command + "'");
      }

      UserManager.getUser(player).setOpenNewGUI(true);
      if (command.startsWith("p:")) {
         String text = command.substring(2);
         executeCommand(() -> Bukkit.dispatchCommand(player, text), player);
      } else if (command.startsWith("o:")) {
         String text = command.substring(2);
         executeCommand(() -> {
            boolean flag = player.isOp();
            if (!flag) {
               try {
                  player.setOp(true);
                  Bukkit.dispatchCommand(player, text);
               } finally {
                  player.setOp(false);
               }
            } else {
               Bukkit.dispatchCommand(player, text);
            }
         }, player);
      } else {
         String text = command;
         executeCommand(() -> Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), text), null);
      }
   }

   public static void executeDelayedCommand(ScheduleCache.DelayedCommand delayedCommand) {
      Map<String, String> args = delayedCommand.getArgs();
      DisplayItem displayItem = args.containsKey("item") ? plugin.getDisplayItem((String)args.get("item")) : null;
      int value = 1;

      try {
         value = Integer.parseInt(args.getOrDefault("amount", "1"));
      } catch (NumberFormatException exception) {
      }

      String command = displayItem != null
         ? (displayItem instanceof ShopItem ? getCommand(delayedCommand.getTask(), (ShopItem)displayItem, value, args.getOrDefault("price", "null")) : delayedCommand.getTask())
         : delayedCommand.getTask().replace("%quantity%", String.valueOf(value)).replace("%price%", args.getOrDefault("price", "null"));
      OfflinePlayer offlinePlayer = args.containsKey("uuid")
         ? Bukkit.getOfflinePlayer(UUID.fromString((String)args.get("uuid")))
         : (args.containsKey("player") ? Bukkit.getOfflinePlayer((String)args.get("player")) : null);
      command = PlaceholderAPIHook.translate(offlinePlayer, command);
      if (offlinePlayer != null && offlinePlayer.isOnline()) {
         executeCommand(displayItem, offlinePlayer.getPlayer(), command);
      } else if (!command.startsWith("p:") && !command.startsWith("o:")) {
         command = command.replace("%player%", offlinePlayer.getName()).replace("%uuid%", offlinePlayer.getUniqueId().toString());
         if (displayItem != null) {
            SendMessage.logDebugMessage("Executing cached delayed command '" + command + "' from command item " + displayItem.getItemPath());
         } else {
            SendMessage.logDebugMessage("Executing cached delayed command item with command '" + command + "'");
         }

         if (command.startsWith("/")) {
            command = command.substring(1);
         }

         String text = command;
         executeCommand(() -> Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), text), null);
      } else {
         SendMessage.errorMessage(
            "Failed to execute cached delayed command '" + command + "' as player '" + offlinePlayer.getName() + "' because the target is not online, skipping..."
         );
      }
   }

   private static void executeDelayedCommand(DisplayItem displayItem, Player player, String command, long value) {
      plugin.runTaskLater(() -> executeCommand(displayItem, player, command), value);
   }

   private static String getCommand(String command, ShopItem shopItem, int value, String shopId) {
      return getCommand(command, shopItem).replace("%quantity%", String.valueOf(value)).replace("%price%", shopId);
   }

   private static String getCommand(String command, ShopItem shopItem) {
      return command.replace("%buyPrice%", String.valueOf(shopItem.isABuyPricing() ? shopItem.getABuyPrice().getBuyPrices() : shopItem.getBuyPrice()))
         .replace("%sellPrice%", String.valueOf(shopItem.isASellPricing() ? shopItem.getASellPrice().getSellPrices() : shopItem.getSellPrice()));
   }

   private static String fixCommand(String command) {
      return command.startsWith("\"") && command.endsWith("\"")
         ? command.substring(1, command.length() - 1)
         : (command.startsWith("'") && command.endsWith("'") ? command.substring(1, command.length() - 1) : command);
   }

   private static void executeCommand(Runnable task, Player player) {
      if (ServerInfo.isFolia()) {
         if (player != null) {
            plugin.runTaskFor(task, player);
         } else {
            plugin.runTask(task);
         }
      } else {
         task.run();
      }
   }
}
