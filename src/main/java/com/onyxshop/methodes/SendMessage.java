package com.onyxshop.methodes;

import github.scarsz.discordsrv.DiscordSRV;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.files.Translatable;
import com.onyxshop.files.lang.TranslatableRaw;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.providers.DiscordSRVProvider;
import com.onyxshop.util.BukkitMeta;
import com.onyxshop.util.ChatUtil;
import com.onyxshop.util.EcoType;
import com.onyxshop.util.ReflectUtil;
import com.onyxshop.util.Transaction;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class SendMessage {
   private static OnyxShop plugin;
   private static boolean logPlayerTransactions;
   private static boolean logTransactions;
   private static boolean devDebug;
   private static boolean advancedTransactionStore;
   private static boolean onScreenTitle;
   private static boolean debug;
   private static DiscordSRVProvider discordSRV;
   public static final Translatable EMPTY = new TranslatableRaw("");
   private static String RESET;

   public SendMessage(OnyxShop plugin2) {
      plugin = plugin2;
   }

   public static Translatable fromConfig(String text) {
      return (Translatable)(plugin.getMetaUtils() instanceof BukkitMeta
         ? new TranslatableRaw(text)
         : (Translatable)ReflectUtil.getPaperFeature("files.lang.TranslatableComponent", text));
   }

   public static void init() {
      if (ConfigManager.getConfig().getBoolean("log-player-transactions", true)) {
         logPlayerTransactions = true;
      }

      if (ConfigManager.getConfig().getBoolean("transaction-log", true)) {
         logTransactions = true;
      }

      if (ConfigManager.getConfig().getBoolean("advanced-transaction-log", true)) {
         advancedTransactionStore = true;
      }

      if (ConfigManager.getConfig().getBoolean("on-screen-title", true)) {
         onScreenTitle = true;
      }

      if (ConfigManager.getConfig().getBoolean("enable-discordsrv-hook") && plugin.getServer().getPluginManager().getPlugin("DiscordSRV") != null) {
         if (DiscordSRV.isReady) {
            discordSRV = new DiscordSRVProvider(plugin, ConfigManager.getConfig().getConfigurationSection("discordsrv-transactions"));
         } else {
            DiscordSRV.api.subscribe(discordSRV = new DiscordSRVProvider(plugin));
            infoMessage("Waiting for DiscordSRV to initialize...");
         }
      }

      if (ConfigManager.getConfig().getBoolean("debug")) {
         debug = true;
      }

      if (ConfigManager.getConfig().getBoolean("dev-debug")) {
         devDebug = true;
      }

      RESET = ChatUtil.getAdventureUtils() != null ? "<white>" : "§f";
      plugin.getTransactionLog().setup();
   }

   public static void sendTransactionMessage(Player player, int value, double amount, ShopItem shopItem, Transaction.Mode mode, Transaction.Type type, boolean flag) {
      sendTransactionMessage(player, value, Collections.singletonMap(shopItem.getEcoType(), amount), shopItem, mode, type, flag);
   }

   public static void sendTransactionMessage(
      Player player, int value, Map<EcoType, Double> map, ShopItem shopItem, Transaction.Mode mode, Transaction.Type type, boolean flag
   ) {
      if (shopItem.transactionMessage()) {
         plugin.getMetaUtils().sendTransactionMessage(player, value, map, shopItem, mode, type, flag);
      }

      Translatable text = Lang.PLAYER_MADE_TRANSACTION.get();
      String lastColors = ChatUtil.getLastColors(text.toString().split("%material%")[0]);
      if (lastColors.isEmpty()) {
         lastColors = RESET;
      }

      String message = lastColors;
      logPlayerTransaction(
         text.replace("%playername%", player.getName())
            .replace("%player_displayname%", plugin.getDisplayName(player))
            .replace("%bought/sold%", mode.getName())
            .replace("%amount%", String.valueOf(value))
            .replace(
               "%material%",
               shopItem instanceof ShopItem.Shulker
                  ? shopItem.getDisplayname()
                     + message
                     + "("
                     + shopItem.getItemPath()
                     + ")"
                     + ((ShopItem.Shulker)shopItem)
                        .getContents()
                        .entrySet()
                        .stream()
                        .map(entry -> entry.getValue() + "x " + entry.getKey().getDisplayname() + message + "(" + entry.getKey().getItemPath() + ")")
                        .collect(Collectors.toList())
                  : shopItem.getDisplayname() + message + "(" + shopItem.getItemPath() + ")"
            )
            .replace("%amountofmoney%", getCurrencyDisplay(message, map, true))
            .replace("%buy/sell-method%", type.getName())
      );
      if (discordSRV != null && discordSRV.enabled && shopItem.transactionMessage()) {
         plugin.runTaskAsync(() -> discordSRV.logTransaction(player, value, mode.getName(), shopItem.getDisplayname(), getCurrencyDisplay(message, map, false)));
      }

      if (onScreenTitle && (type == Transaction.Type.SELL_ALL_COMMAND || type == Transaction.Type.SELL_GUI_SCREEN || shopItem.isCloseMenu())) {
         sendTitle(player, mode, map);
      }

      if (advancedTransactionStore) {
         plugin.getTransactionLog().log(player.getUniqueId(), shopItem, map, value, mode, type);
      }

      plugin.getSoundEffects().completeTransaction(player, mode);
   }

   public static void sendTransactionMessage(Player player, int value, Map<EcoType, Double> map, Map<ShopItem, Integer> map2, Transaction.Type type) {
      plugin.getMetaUtils().sendTransactionMessage(player, value, map, map2, type);
      Translatable text = Lang.PLAYER_MADE_TRANSACTION_MULTIPLE_ITEMS.get();
      String lastColors = ChatUtil.getLastColors(text.toString().split("%items%")[0]);
      if (lastColors.isEmpty()) {
         lastColors = RESET;
      }

      String message = lastColors;
      logPlayerTransaction(
         text.replace("%playername%", player.getName())
            .replace("%player_displayname%", plugin.getDisplayName(player))
            .replace("%bought/sold%", Lang.SOLD.get().toString())
            .replace("%items%", getItemDisplay(message, map2, true))
            .replace("%amountofmoney%", getCurrencyDisplay(message, map, true))
            .replace("%buy/sell-method%", type.getName())
      );
      if (discordSRV != null && discordSRV.enabled) {
         plugin.runTaskAsync(
            () -> discordSRV.logTransaction(player, value, Lang.SOLD.get().getLegacy(), getItemDisplay(message, map2, false), getCurrencyDisplay(message, map, false))
         );
      }

      if (onScreenTitle) {
         sendTitle(player, map);
      }

      if (advancedTransactionStore) {
         plugin.getTransactionLog().log(player.getUniqueId(), map2, map, value, Transaction.Mode.SELL, type);
      }

      plugin.getSoundEffects().completeTransaction(player, Transaction.Mode.getFromType(type));
   }

   private static String getItemDisplay(String itemId, Map<ShopItem, Integer> map, boolean flag) {
      StringBuilder builder = new StringBuilder();
      int value = 0;

      for (Entry<ShopItem, Integer> entry : map.entrySet()) {
         if (entry.getKey() instanceof ShopItem.Shulker) {
            Map<ShopItem, Integer> contents = ((ShopItem.Shulker)entry.getKey()).getContents();
            builder.append(entry.getValue()).append("x ").append(((ShopItem)entry.getKey()).getDisplayname());
            builder.append(
               contents.entrySet().stream().map(entry2 -> entry2.getValue() + "x " + ((ShopItem)entry2.getKey()).getDisplayname()).collect(Collectors.toList())
            );
         } else {
            builder.append(entry.getValue()).append("x ").append(((ShopItem)entry.getKey()).getDisplayname());
         }

         if (flag) {
            if (itemId != null) {
               builder.append(itemId);
            }

            builder.append("(").append(((ShopItem)entry.getKey()).getItemPath()).append(")");
         }

         if (value != map.size() - 1) {
            builder.append(", ");
         }

         value++;
      }

      return builder.toString();
   }

   private static String getCurrencyDisplay(String type, Map<EcoType, Double> map, boolean flag) {
      StringBuilder builder = new StringBuilder();
      int value = 0;

      for (Entry entry : map.entrySet()) {
         builder.append(plugin.formatPrice((EcoType)entry.getKey(), (Double)entry.getValue()));
         if (flag) {
            builder.append(type);
         }

         if (value != map.size() - 1) {
            builder.append(", ");
         }

         value++;
      }

      return builder.toString();
   }

   private static void sendTitle(Player player, Map<EcoType, Double> map) {
      sendTitle(player, null, map);
   }

   private static void sendTitle(Player player, Transaction.Mode mode, Map<EcoType, Double> map) {
      String title = mode != null && mode != Transaction.Mode.SELL
         ? Lang.ON_SCREEN_TITLE_BOUGHT.get(player).getLegacy()
         : Lang.ON_SCREEN_TITLE_SOLD.get(player).getLegacy();
      String lastColors = ChatColor.getLastColors(title.split("%price%")[0]);
      if (lastColors.isEmpty()) {
         lastColors = "§r";
      }

      player.sendTitle(title.replace("%price%", getCurrencyDisplay(lastColors, map, true)), "");
   }

   public static void sendMessage(Object value, Translatable text) {
      plugin.getMetaUtils().send(value, text);
   }

   public static void sendMessage(Object value, String message) {
      plugin.getMetaUtils().sendLegacy(value, message);
   }

   public static void chatToPlayer(Player player, Translatable text) {
      plugin.getMetaUtils().send(player, text.get(player));
   }

   public static void chatToPlayer(Player player, String playerName) {
      plugin.getMetaUtils().sendLegacy(player, playerName);
   }

   public static void chatToPlayer(Player player, TextComponent textComponent) {
      TextComponent textComponent2 = new TextComponent(TextComponent.fromLegacyText(Lang.SHOP_PREFIX.get(player).getLegacy()));
      textComponent2.addExtra(textComponent);
      player.spigot().sendMessage(textComponent2);
   }

   public static void broadcastMessage(Translatable text) {
      plugin.getMetaUtils().broadcast(text);
   }

   public static void logDebugMessage(String message) {
      if (debug) {
         plugin.getMetaUtils().logLegacyDebugMessage(message);
      }
   }

   public static void logDebugMessage(Translatable text) {
      if (debug) {
         plugin.getMetaUtils().logDebugMessage(text);
      }
   }

   public static void logDevDebugMessage(String message) {
      if (devDebug) {
         plugin.getMetaUtils().logLegacyDebugMessage(message);
      }
   }

   public static void logDevDebugMessage(Translatable text) {
      if (devDebug) {
         plugin.getMetaUtils().logDevDebugMessage(text);
      }
   }

   public static void logPlayerTransaction(Translatable text) {
      if (logPlayerTransactions) {
         plugin.getMetaUtils().logPlayerTransaction(text);
      }

      if (logTransactions) {
         plugin.getTransactionLog().log(ChatUtil.stripColor(text.toString()));
      }
   }

   public static void infoMessage(String message) {
      plugin.getMetaUtils().infoLegacyMessage(message);
   }

   public static void infoMessage(Translatable text) {
      plugin.getMetaUtils().infoMessage(text);
   }

   public static void infoMessage(Object value, String message) {
      plugin.getMetaUtils().infoLegacyMessage(value, message);
   }

   public static void infoMessageWithoutPrefix(Object value, String message) {
      plugin.getMetaUtils().infoLegacyMessageWithoutPrefix(value, message);
   }

   public static void infoMessage(Object value, Translatable text) {
      plugin.getMetaUtils().infoMessage(value, text);
   }

   public static void warnMessage(String message) {
      plugin.getMetaUtils().warnLegacyMessage(message);
   }

   public static void warnMessage(Translatable text) {
      plugin.getMetaUtils().warnMessage(text);
   }

   public static void warnMessage(Object value, String message) {
      plugin.getMetaUtils().warnLegacyMessage(value, message);
   }

   public static void warnMessage(Object value, Translatable text) {
      plugin.getMetaUtils().warnMessage(value, text);
   }

   public static void errorMessage(String message) {
      plugin.getMetaUtils().errorLegacyMessage(message);
   }

   public static void errorMessage(Translatable text) {
      plugin.getMetaUtils().errorMessage(text);
   }

   public static void errorMessage(Object value, String message) {
      plugin.getMetaUtils().errorLegacyMessage(value, message);
   }

   public static void errorMessage(Object value, Translatable text) {
      plugin.getMetaUtils().errorMessage(value, text);
   }

   public static void errorItemShops(String itemId) {
      if (debug) {
         warnMessage(Lang.ITEMS_PATH_IN_SHOPS_CONFIG.get().replace("%location%", itemId));
      }
   }

   public static void errorShops(String text, String text2) {
      warnMessage(Lang.ITEMS_LOCATION_IN_SHOPS.get().replace("%section%", text).replace("%location%", text2));
   }

   public static void errorItemConfig(String itemId) {
      warnMessage(Lang.ITEMS_PATH_IN_CONFIG.get().replace("%location%", itemId));
   }

   public static void errorItemSections(String itemId) {
      if (debug) {
         warnMessage(Lang.ITEMS_PATH_IN_SECTIONS_CONFIG.get().replace("%location%", itemId));
      }
   }

   public static void errorSections(String text, String text2) {
      warnMessage(Lang.ITEMS_LOCATION_IN_SECTIONS.get().replace("%section%", text).replace("%location%", text2));
   }
}
