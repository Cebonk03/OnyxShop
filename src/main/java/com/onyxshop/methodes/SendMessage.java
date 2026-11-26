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

   public SendMessage(OnyxShop var1) {
      plugin = var1;
   }

   public static Translatable fromConfig(String var0) {
      return (Translatable)(plugin.getMetaUtils() instanceof BukkitMeta
         ? new TranslatableRaw(var0)
         : (Translatable)ReflectUtil.getPaperFeature("files.lang.TranslatableComponent", var0));
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

   public static void sendTransactionMessage(Player var0, int var1, double var2, ShopItem var4, Transaction.Mode var5, Transaction.Type var6, boolean var7) {
      sendTransactionMessage(var0, var1, Collections.singletonMap(var4.getEcoType(), var2), var4, var5, var6, var7);
   }

   public static void sendTransactionMessage(
      Player var0, int var1, Map<EcoType, Double> var2, ShopItem var3, Transaction.Mode var4, Transaction.Type var5, boolean var6
   ) {
      if (var3.transactionMessage()) {
         plugin.getMetaUtils().sendTransactionMessage(var0, var1, var2, var3, var4, var5, var6);
      }

      Translatable var7 = Lang.PLAYER_MADE_TRANSACTION.get();
      String var8 = ChatUtil.getLastColors(var7.toString().split("%material%")[0]);
      if (var8.isEmpty()) {
         var8 = RESET;
      }

      String var9 = var8;
      logPlayerTransaction(
         var7.replace("%playername%", var0.getName())
            .replace("%player_displayname%", plugin.getDisplayName(var0))
            .replace("%bought/sold%", var4.getName())
            .replace("%amount%", String.valueOf(var1))
            .replace(
               "%material%",
               var3 instanceof ShopItem.Shulker
                  ? var3.getDisplayname()
                     + var9
                     + "("
                     + var3.getItemPath()
                     + ")"
                     + ((ShopItem.Shulker)var3)
                        .getContents()
                        .entrySet()
                        .stream()
                        .map(var1x -> var1x.getValue() + "x " + var1x.getKey().getDisplayname() + var9 + "(" + var1x.getKey().getItemPath() + ")")
                        .collect(Collectors.toList())
                  : var3.getDisplayname() + var9 + "(" + var3.getItemPath() + ")"
            )
            .replace("%amountofmoney%", getCurrencyDisplay(var9, var2, true))
            .replace("%buy/sell-method%", var5.getName())
      );
      if (discordSRV != null && discordSRV.enabled && var3.transactionMessage()) {
         plugin.runTaskAsync(() -> discordSRV.logTransaction(var0, var1, var4.getName(), var3.getDisplayname(), getCurrencyDisplay(var9, var2, false)));
      }

      if (onScreenTitle && (var5 == Transaction.Type.SELL_ALL_COMMAND || var5 == Transaction.Type.SELL_GUI_SCREEN || var3.isCloseMenu())) {
         sendTitle(var0, var4, var2);
      }

      if (advancedTransactionStore) {
         plugin.getTransactionLog().log(var0.getUniqueId(), var3, var2, var1, var4, var5);
      }

      plugin.getSoundEffects().completeTransaction(var0, var4);
   }

   public static void sendTransactionMessage(Player var0, int var1, Map<EcoType, Double> var2, Map<ShopItem, Integer> var3, Transaction.Type var4) {
      plugin.getMetaUtils().sendTransactionMessage(var0, var1, var2, var3, var4);
      Translatable var5 = Lang.PLAYER_MADE_TRANSACTION_MULTIPLE_ITEMS.get();
      String var6 = ChatUtil.getLastColors(var5.toString().split("%items%")[0]);
      if (var6.isEmpty()) {
         var6 = RESET;
      }

      String var7 = var6;
      logPlayerTransaction(
         var5.replace("%playername%", var0.getName())
            .replace("%player_displayname%", plugin.getDisplayName(var0))
            .replace("%bought/sold%", Lang.SOLD.get().toString())
            .replace("%items%", getItemDisplay(var7, var3, true))
            .replace("%amountofmoney%", getCurrencyDisplay(var7, var2, true))
            .replace("%buy/sell-method%", var4.getName())
      );
      if (discordSRV != null && discordSRV.enabled) {
         plugin.runTaskAsync(
            () -> discordSRV.logTransaction(var0, var1, Lang.SOLD.get().getLegacy(), getItemDisplay(var7, var3, false), getCurrencyDisplay(var7, var2, false))
         );
      }

      if (onScreenTitle) {
         sendTitle(var0, var2);
      }

      if (advancedTransactionStore) {
         plugin.getTransactionLog().log(var0.getUniqueId(), var3, var2, var1, Transaction.Mode.SELL, var4);
      }

      plugin.getSoundEffects().completeTransaction(var0, Transaction.Mode.getFromType(var4));
   }

   private static String getItemDisplay(String var0, Map<ShopItem, Integer> var1, boolean var2) {
      StringBuilder var3 = new StringBuilder();
      int var4 = 0;

      for (Entry<ShopItem, Integer> var6 : var1.entrySet()) {
         if (var6.getKey() instanceof ShopItem.Shulker) {
            Map<ShopItem, Integer> var7 = ((ShopItem.Shulker)var6.getKey()).getContents();
            var3.append(var6.getValue()).append("x ").append(((ShopItem)var6.getKey()).getDisplayname());
            var3.append(
               var7.entrySet().stream().map(var0x -> var0x.getValue() + "x " + ((ShopItem)var0x.getKey()).getDisplayname()).collect(Collectors.toList())
            );
         } else {
            var3.append(var6.getValue()).append("x ").append(((ShopItem)var6.getKey()).getDisplayname());
         }

         if (var2) {
            if (var0 != null) {
               var3.append(var0);
            }

            var3.append("(").append(((ShopItem)var6.getKey()).getItemPath()).append(")");
         }

         if (var4 != var1.size() - 1) {
            var3.append(", ");
         }

         var4++;
      }

      return var3.toString();
   }

   private static String getCurrencyDisplay(String var0, Map<EcoType, Double> var1, boolean var2) {
      StringBuilder var3 = new StringBuilder();
      int var4 = 0;

      for (Entry var6 : var1.entrySet()) {
         var3.append(plugin.formatPrice((EcoType)var6.getKey(), (Double)var6.getValue()));
         if (var2) {
            var3.append(var0);
         }

         if (var4 != var1.size() - 1) {
            var3.append(", ");
         }

         var4++;
      }

      return var3.toString();
   }

   private static void sendTitle(Player var0, Map<EcoType, Double> var1) {
      sendTitle(var0, null, var1);
   }

   private static void sendTitle(Player var0, Transaction.Mode var1, Map<EcoType, Double> var2) {
      String var3 = var1 != null && var1 != Transaction.Mode.SELL
         ? Lang.ON_SCREEN_TITLE_BOUGHT.get(var0).getLegacy()
         : Lang.ON_SCREEN_TITLE_SOLD.get(var0).getLegacy();
      String var4 = ChatColor.getLastColors(var3.split("%price%")[0]);
      if (var4.isEmpty()) {
         var4 = "§r";
      }

      var0.sendTitle(var3.replace("%price%", getCurrencyDisplay(var4, var2, true)), "");
   }

   public static void sendMessage(Object var0, Translatable var1) {
      plugin.getMetaUtils().send(var0, var1);
   }

   public static void sendMessage(Object var0, String var1) {
      plugin.getMetaUtils().sendLegacy(var0, var1);
   }

   public static void chatToPlayer(Player var0, Translatable var1) {
      plugin.getMetaUtils().send(var0, var1.get(var0));
   }

   public static void chatToPlayer(Player var0, String var1) {
      plugin.getMetaUtils().sendLegacy(var0, var1);
   }

   public static void chatToPlayer(Player var0, TextComponent var1) {
      TextComponent var2 = new TextComponent(TextComponent.fromLegacyText(Lang.SHOP_PREFIX.get(var0).getLegacy()));
      var2.addExtra(var1);
      var0.spigot().sendMessage(var2);
   }

   public static void broadcastMessage(Translatable var0) {
      plugin.getMetaUtils().broadcast(var0);
   }

   public static void logDebugMessage(String var0) {
      if (debug) {
         plugin.getMetaUtils().logLegacyDebugMessage(var0);
      }
   }

   public static void logDebugMessage(Translatable var0) {
      if (debug) {
         plugin.getMetaUtils().logDebugMessage(var0);
      }
   }

   public static void logDevDebugMessage(String var0) {
      if (devDebug) {
         plugin.getMetaUtils().logLegacyDebugMessage(var0);
      }
   }

   public static void logDevDebugMessage(Translatable var0) {
      if (devDebug) {
         plugin.getMetaUtils().logDevDebugMessage(var0);
      }
   }

   public static void logPlayerTransaction(Translatable var0) {
      if (logPlayerTransactions) {
         plugin.getMetaUtils().logPlayerTransaction(var0);
      }

      if (logTransactions) {
         plugin.getTransactionLog().log(ChatUtil.stripColor(var0.toString()));
      }
   }

   public static void infoMessage(String var0) {
      plugin.getMetaUtils().infoLegacyMessage(var0);
   }

   public static void infoMessage(Translatable var0) {
      plugin.getMetaUtils().infoMessage(var0);
   }

   public static void infoMessage(Object var0, String var1) {
      plugin.getMetaUtils().infoLegacyMessage(var0, var1);
   }

   public static void infoMessageWithoutPrefix(Object var0, String var1) {
      plugin.getMetaUtils().infoLegacyMessageWithoutPrefix(var0, var1);
   }

   public static void infoMessage(Object var0, Translatable var1) {
      plugin.getMetaUtils().infoMessage(var0, var1);
   }

   public static void warnMessage(String var0) {
      plugin.getMetaUtils().warnLegacyMessage(var0);
   }

   public static void warnMessage(Translatable var0) {
      plugin.getMetaUtils().warnMessage(var0);
   }

   public static void warnMessage(Object var0, String var1) {
      plugin.getMetaUtils().warnLegacyMessage(var0, var1);
   }

   public static void warnMessage(Object var0, Translatable var1) {
      plugin.getMetaUtils().warnMessage(var0, var1);
   }

   public static void errorMessage(String var0) {
      plugin.getMetaUtils().errorLegacyMessage(var0);
   }

   public static void errorMessage(Translatable var0) {
      plugin.getMetaUtils().errorMessage(var0);
   }

   public static void errorMessage(Object var0, String var1) {
      plugin.getMetaUtils().errorLegacyMessage(var0, var1);
   }

   public static void errorMessage(Object var0, Translatable var1) {
      plugin.getMetaUtils().errorMessage(var0, var1);
   }

   public static void errorItemShops(String var0) {
      if (debug) {
         warnMessage(Lang.ITEMS_PATH_IN_SHOPS_CONFIG.get().replace("%location%", var0));
      }
   }

   public static void errorShops(String var0, String var1) {
      warnMessage(Lang.ITEMS_LOCATION_IN_SHOPS.get().replace("%section%", var0).replace("%location%", var1));
   }

   public static void errorItemConfig(String var0) {
      warnMessage(Lang.ITEMS_PATH_IN_CONFIG.get().replace("%location%", var0));
   }

   public static void errorItemSections(String var0) {
      if (debug) {
         warnMessage(Lang.ITEMS_PATH_IN_SECTIONS_CONFIG.get().replace("%location%", var0));
      }
   }

   public static void errorSections(String var0, String var1) {
      warnMessage(Lang.ITEMS_LOCATION_IN_SECTIONS.get().replace("%section%", var0).replace("%location%", var1));
   }
}
