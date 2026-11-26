package com.onyxshop.providers.placeholders.handler;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.files.TransactionLog;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.DisplayItem;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.objects.shops.ShopSection;
import com.onyxshop.objects.shops.ShopType;
import com.onyxshop.objects.shops.types.RotatingShop;
import com.onyxshop.providers.placeholders.PAPIExpansion;
import com.onyxshop.util.ChatUtil;
import com.onyxshop.util.PermissionsCache;
import com.onyxshop.util.Transaction;
import org.bukkit.entity.Player;

public class PlaceholderProvider {
   private final OnyxShop plugin;
   private final PAPIExpansion expansion;
   private final Cache<String, String> DYNAMIC_BUY_PRICES_CACHE;
   private final Cache<String, String> DYNAMIC_SELL_PRICES_CACHE;
   private final String defaultMessage;
   private PlaceholderProvider.PlaceholderPlayerItemCache playerItemCache;
   private PlaceholderProvider.PlaceholderItemCache itemCache;
   private PlaceholderProvider.PlaceholderCache cache;
   private boolean placeholderCache = false;

   public PlaceholderProvider(OnyxShop var1) {
      this.plugin = var1;
      this.defaultMessage = this.format(Lang.TRANSACTION_PLACEHOLDER_DEFAULT.get().getLegacy());
      int var2 = ConfigManager.getConfig().getInt("placeholders.dynamic-prices-placeholder-cache", 20);
      this.DYNAMIC_BUY_PRICES_CACHE = CacheBuilder.newBuilder().expireAfterWrite(var2, TimeUnit.SECONDS).build();
      this.DYNAMIC_SELL_PRICES_CACHE = CacheBuilder.newBuilder().expireAfterWrite(var2, TimeUnit.SECONDS).build();
      this.expansion = new PAPIExpansion(this);
      this.expansion.register();
   }

   public String getItemBuyPrice(String[] var1) {
      ShopSection var2 = this.plugin.getSection(var1[0]);
      if (var2 == null) {
         return "§cNo shop found for '" + var1[0] + "'";
      } else {
         ShopItem var3 = var2.getShopItem(var1[0] + "." + var1[1]);
         if (var3 == null) {
            return "§cNo item found in shop '" + var1[0] + "' at index '" + var1[1] + "'";
         } else if (var3.hasItemError()) {
            return "§cCannot get price of invalid item";
         } else if (!var3.isBuyAble()) {
            return Lang.CANNOT_PURCHASE_ITEM.get().getLegacy();
         } else if (!var3.isDynamicPricing()) {
            return var3.isABuyPricing()
               ? var3.getABuyPrice().formatBuyPrices(null, null, 1)
               : OnyxShop.getInstance().formatPrice(var3.getEcoType(), var3.getBuyPrice());
         } else {
            String var4 = (String)this.DYNAMIC_BUY_PRICES_CACHE.getIfPresent(var1[0] + "." + var1[1]);
            if (var4 != null) {
               return var4;
            } else {
               var4 = var3.isABuyPricing()
                  ? var3.getABuyPrice().formatBuyPrices(null, null, 1)
                  : OnyxShop.getInstance().formatPrice(var3.getEcoType(), var3.getBuyPrice(1));
               this.DYNAMIC_BUY_PRICES_CACHE.put(var1[0] + "." + var1[1], var4);
               return var4;
            }
         }
      }
   }

   public String getItemSellPrice(String[] var1) {
      ShopSection var2 = this.plugin.getSection(var1[0]);
      if (var2 == null) {
         return "§cNo shop found for '" + var1[0] + "'";
      } else {
         ShopItem var3 = var2.getShopItem(var1[0] + "." + var1[1]);
         if (var3 == null) {
            return "§cNo item found in shop '" + var1[0] + "' at index '" + var1[1] + "'";
         } else if (var3.hasItemError()) {
            return "§cCannot get price of invalid item";
         } else if (!var3.isSellAble()) {
            return Lang.CANNOT_SELL_ITEM.get().getLegacy();
         } else if (!var3.isDynamicPricing()) {
            return var3.isASellPricing()
               ? var3.getASellPrice().formatSellPrices(null, null, 1)
               : OnyxShop.getInstance().formatPrice(var3.getEcoType(), var3.getSellPrice());
         } else {
            String var4 = (String)this.DYNAMIC_SELL_PRICES_CACHE.getIfPresent(var1[0] + "." + var1[1]);
            if (var4 != null) {
               return var4;
            } else {
               var4 = var3.isASellPricing()
                  ? var3.getASellPrice().formatSellPrices(null, null, 1)
                  : OnyxShop.getInstance().formatPrice(var3.getEcoType(), var3.getSellPrice(1));
               this.DYNAMIC_SELL_PRICES_CACHE.put(var1[0] + "." + var1[1], var4);
               return var4;
            }
         }
      }
   }

   public String getTotalTransactions(Transaction.Mode var1, TransactionLog.TimeFrame var2, String var3) {
      String var4 = null;
      String var5 = null;
      if (!var3.isEmpty()) {
         if (!var3.contains(".")) {
            if (this.plugin.getSection(var3) == null) {
               return "§cNo shop found for '" + var3 + "'";
            }

            var4 = var3;
         } else {
            String[] var6 = var3.split("\\.", 2);
            ShopSection var7 = this.plugin.getSection(var6[0]);
            if (var7 == null) {
               return "§cNo shop found for '" + var6[0] + "'";
            }

            ShopItem var8 = var7.getShopItem(var6[0] + "." + var6[1]);
            if (var8 == null) {
               return "§cNo item found in shop '" + var6[0] + "' at index '" + var6[1] + "'";
            }

            var4 = var6[0];
            var5 = var6[1];
         }
      }

      PlaceholderProvider.PlaceholderItemCache var9 = this.getPlaceholderItemCache();
      return var9 == null ? "§cLoading cache..." : String.valueOf(var9.getTotalTransactions(var1, var2, var4, var5));
   }

   public String getTotalTransactions(Transaction.Mode var1, TransactionLog.TimeFrame var2, String var3, UUID var4) {
      String var5 = null;
      String var6 = null;
      if (!var3.isEmpty()) {
         if (!var3.contains(".")) {
            if (this.plugin.getSection(var3) == null) {
               return "§cNo shop found for '" + var3 + "'";
            }

            var5 = var3;
         } else {
            String[] var7 = var3.split("\\.", 2);
            ShopSection var8 = this.plugin.getSection(var7[0]);
            if (var8 == null) {
               return "§cNo shop found for '" + var7[0] + "'";
            }

            ShopItem var9 = var8.getShopItem(var7[0] + "." + var7[1]);
            if (var9 == null) {
               return "§cNo item found in shop '" + var7[0] + "' at index '" + var7[1] + "'";
            }

            var5 = var7[0];
            var6 = var7[1];
         }
      }

      PlaceholderProvider.PlaceholderPlayerItemCache var10 = this.getPlaceholderPlayerItemCache();
      return var10 == null ? "§cLoading cache..." : String.valueOf(var10.getTotalTransactions(var1, var2, var4, var5, var6));
   }

   public String getDiscount(Player var1, String var2) {
      if (this.plugin.getSection(var2) == null) {
         return "§cNo shop found for '" + var2 + "'";
      } else if (!this.plugin.hasDiscount(var2)) {
         return "0";
      } else {
         double var3 = 0.0;

         for (String var6 : this.plugin.getDiscounts(var2).keySet()) {
            if (PermissionsCache.hasPermission(var1, "OnyxShop.discounts." + var6)) {
               double var7 = this.plugin.getDiscounts(var2).get(var6);
               if (var3 == 0.0 || var3 < var7) {
                  var3 = var7;
               }
            }
         }

         return String.valueOf(var3);
      }
   }

   public String getMultiplier(Player var1, String var2) {
      if (this.plugin.getSection(var2) == null) {
         return "§cNo shop found for '" + var2 + "'";
      } else if (!this.plugin.hasMultiplier(var2)) {
         return "0";
      } else {
         double var3 = 0.0;

         for (String var6 : this.plugin.getMultipliers(var2).keySet()) {
            if (PermissionsCache.hasPermission(var1, "OnyxShop.sell-multipliers." + var6)) {
               double var7 = this.plugin.getMultipliers(var2).get(var6);
               if (var3 == 0.0 || var3 < var7) {
                  var3 = var7;
               }
            }
         }

         return String.valueOf(var3);
      }
   }

   public String getShopRotation(String var1) {
      ShopSection var2 = this.plugin.getSection(var1);
      return var2 != null && var2.getType() == ShopType.ROTATING ? ((RotatingShop)var2).getNextRotation() : "§cNo rotating shop found for '" + var1 + "'";
   }

   private String format(String var1) {
      if (var1.contains("%translations-")) {
         try {
            Lang var2 = Lang.valueOf(var1.split("%translations-")[1].split("%")[0].toUpperCase(Locale.ENGLISH).replace("-", "_"));
            var1 = var1.replace("%translations-" + var2.getKey() + "%", var2.get().getLegacy());
         } catch (IllegalArgumentException var3) {
         }
      }

      return ChatUtil.formatColors(var1);
   }

   private void enablePlaceholderStore() {
      int var1 = ConfigManager.getConfig().getInt("placeholders.transaction-cache") * 20;
      if (var1 < 200) {
         var1 = 200;
      }

      this.plugin.runTaskAsyncTimer(() -> {
         if (this.cache == null || this.itemCache == null || this.playerItemCache == null) {
            this.plugin.getTransactionLog().loadCache();
         }

         if (this.cache == null) {
            this.cache = new PlaceholderProvider.PlaceholderCache();
         } else {
            this.getPlaceholderCache().reloadActivePlaceholders();
         }

         if (this.itemCache == null) {
            this.itemCache = new PlaceholderProvider.PlaceholderItemCache();
         } else {
            this.getPlaceholderItemCache().reloadActive();
         }

         if (this.playerItemCache == null) {
            this.playerItemCache = new PlaceholderProvider.PlaceholderPlayerItemCache();
         } else {
            this.getPlaceholderPlayerItemCache().reloadActive();
         }
      }, 0L, var1);
      this.placeholderCache = true;
   }

   public PlaceholderProvider.PlaceholderCache getPlaceholderCache() {
      if (!this.placeholderCache && this.plugin.placeholders) {
         SendMessage.infoMessage("Received placeholder request, loading transactions...");
         this.enablePlaceholderStore();
         return null;
      } else {
         return this.cache;
      }
   }

   public PlaceholderProvider.PlaceholderItemCache getPlaceholderItemCache() {
      if (!this.placeholderCache && this.plugin.placeholders) {
         SendMessage.infoMessage("Received placeholder request, loading transactions...");
         this.enablePlaceholderStore();
         return null;
      } else {
         return this.itemCache;
      }
   }

   public PlaceholderProvider.PlaceholderPlayerItemCache getPlaceholderPlayerItemCache() {
      if (!this.placeholderCache) {
         SendMessage.infoMessage("Received placeholder request, loading transactions...");
         this.enablePlaceholderStore();
         return null;
      } else {
         return this.playerItemCache;
      }
   }

   private String getTopPlayer(TransactionLog.TimeFrame var1, Transaction.Mode var2) {
      HashMap<UUID, Integer> var3 = new HashMap<>();

      for (TransactionLog.SimpleTransaction var5 : this.plugin.getTransactionLog().getTransactions(var1, var2)) {
         UUID var6 = var5.uuid;
         int var7 = var5.amount;
         var3.put(var6, var3.getOrDefault(var6, 0) + var7);
      }

      if (var3.isEmpty()) {
         return this.defaultMessage;
      } else {
         UUID var10 = null;
         int var11 = 0;

         for (Entry var13 : var3.entrySet()) {
            UUID var8 = (UUID)var13.getKey();
            int var9 = (Integer)var13.getValue();
            if (var9 > var11) {
               var11 = var9;
               var10 = var8;
            }
         }

         return this.getPlayerName(var10);
      }
   }

   private String getTopItem(TransactionLog.TimeFrame var1, Transaction.Mode var2) {
      HashMap<String, Integer> var3 = new HashMap<>();

      for (TransactionLog.SimpleTransaction var5 : this.plugin.getTransactionLog().getTransactions(var1, var2)) {
         String var6 = var5.itemPath;
         int var7 = var5.amount;
         var3.put(var6, var3.getOrDefault(var6, 0) + var7);
      }

      if (var3.isEmpty()) {
         return this.defaultMessage;
      } else {
         String var10 = null;
         int var11 = 0;

         for (Entry var13 : var3.entrySet()) {
            String var8 = (String)var13.getKey();
            int var9 = (Integer)var13.getValue();
            if (var9 > var11) {
               var11 = var9;
               var10 = var8;
            }
         }

         return this.getItemName(var10);
      }
   }

   private String getPlayerName(UUID var1) {
      return this.plugin.getServer().getOfflinePlayer(var1).getName();
   }

   private String getItemName(String var1) {
      ShopSection var2 = this.plugin.getSections().get(var1.split("\\.")[0]);
      if (var2 == null) {
         return "Unknown";
      } else {
         DisplayItem var3 = var2.getDisplayItem(var1);
         return var3 != null ? var3.getDisplayname() : "Unknown";
      }
   }

   private final class ActivePlaceholders {
      boolean top_player_bought_items_current_month = false;
      boolean top_player_bought_items_current_week = false;
      boolean top_player_bought_items_current_day = false;
      boolean top_player_sold_items_current_month = false;
      boolean top_player_sold_items_current_week = false;
      boolean top_player_sold_items_current_day = false;
      boolean top_bought_items_current_month = false;
      boolean top_bought_items_current_week = false;
      boolean top_bought_items_current_day = false;
      boolean top_sold_items_current_month = false;
      boolean top_sold_items_current_week = false;
      boolean top_sold_items_current_day = false;

      private ActivePlaceholders() {
      }
   }

   private static final class Item {
      final Transaction.Mode mode;
      final TransactionLog.TimeFrame timeFrame;
      final String section;
      final String index;

      public Item(Transaction.Mode var1, TransactionLog.TimeFrame var2, String var3, String var4) {
         this.mode = var1;
         this.timeFrame = var2;
         this.section = var3;
         this.index = var4;
      }

      @Override
      public boolean equals(Object var1) {
         if (this == var1) {
            return true;
         } else if (!(var1 instanceof PlaceholderProvider.Item)) {
            return false;
         } else {
            PlaceholderProvider.Item var2 = (PlaceholderProvider.Item)var1;
            return this.mode == var2.mode && this.timeFrame == var2.timeFrame && this.section == null
               ? var2.section == null
               : this.section.equals(var2.section) && Objects.equals(this.index, var2.index);
         }
      }

      @Override
      public int hashCode() {
         return Objects.hash(this.mode, this.timeFrame, this.section, this.index);
      }
   }

   public final class PlaceholderCache {
      private final PlaceholderProvider.ActivePlaceholders activePlaceholders = PlaceholderProvider.this.new ActivePlaceholders();
      private ArrayList<TransactionLog.TopPlayer> top_player_bought_items_current_month;
      private ArrayList<TransactionLog.TopPlayer> top_player_bought_items_current_week;
      private ArrayList<TransactionLog.TopPlayer> top_player_bought_items_current_day;
      private ArrayList<TransactionLog.TopPlayer> top_player_sold_items_current_month;
      private ArrayList<TransactionLog.TopPlayer> top_player_sold_items_current_week;
      private ArrayList<TransactionLog.TopPlayer> top_player_sold_items_current_day;
      private ArrayList<TransactionLog.TopItem> top_bought_items_current_month;
      private ArrayList<TransactionLog.TopItem> top_bought_items_current_week;
      private ArrayList<TransactionLog.TopItem> top_bought_items_current_day;
      private ArrayList<TransactionLog.TopItem> top_sold_items_current_month;
      private ArrayList<TransactionLog.TopItem> top_sold_items_current_week;
      private ArrayList<TransactionLog.TopItem> top_sold_items_current_day;

      public PlaceholderCache() {
         this.reloadActivePlaceholders();
      }

      public void reloadActivePlaceholders() {
         if (this.activePlaceholders.top_player_bought_items_current_month) {
            this.loadTopPlayerBoughtItemsCurrentMonth();
         }

         if (this.activePlaceholders.top_player_bought_items_current_week) {
            this.loadTopPlayerBoughtItemsCurrentWeek();
         }

         if (this.activePlaceholders.top_player_bought_items_current_day) {
            this.loadTopPlayerBoughtItemsCurrentDay();
         }

         if (this.activePlaceholders.top_player_sold_items_current_month) {
            this.loadTopPlayerSoldItemsCurrentMonth();
         }

         if (this.activePlaceholders.top_player_sold_items_current_week) {
            this.loadTopPlayerSoldItemsCurrentWeek();
         }

         if (this.activePlaceholders.top_player_sold_items_current_day) {
            this.loadTopPlayerSoldItemsCurrentDay();
         }

         if (this.activePlaceholders.top_bought_items_current_month) {
            this.loadTopBoughtItemsCurrentMonth();
         }

         if (this.activePlaceholders.top_bought_items_current_week) {
            this.loadTopBoughtItemsCurrentWeek();
         }

         if (this.activePlaceholders.top_bought_items_current_day) {
            this.loadTopBoughtItemsCurrentDay();
         }

         if (this.activePlaceholders.top_sold_items_current_month) {
            this.loadTopSoldItemsCurrentMonth();
         }

         if (this.activePlaceholders.top_sold_items_current_week) {
            this.loadTopSoldItemsCurrentWeek();
         }

         if (this.activePlaceholders.top_sold_items_current_day) {
            this.loadTopSoldItemsCurrentDay();
         }
      }

      public ArrayList<TransactionLog.TopPlayer> getTopPlayerBoughtItemsCurrentMonth() {
         if (!this.activePlaceholders.top_player_bought_items_current_month) {
            this.loadTopPlayerBoughtItemsCurrentMonth();
            this.activePlaceholders.top_player_bought_items_current_month = true;
         }

         return this.top_player_bought_items_current_month;
      }

      private void loadTopPlayerBoughtItemsCurrentMonth() {
         this.top_player_bought_items_current_month = PlaceholderProvider.this.plugin
            .getTransactionLog()
            .getTopThreePlayers(TransactionLog.TimeFrame.MONTH, Transaction.Mode.BUY);
      }

      public ArrayList<TransactionLog.TopPlayer> getTopPlayerBoughtItemsCurrentWeek() {
         if (!this.activePlaceholders.top_player_bought_items_current_week) {
            this.loadTopPlayerBoughtItemsCurrentWeek();
            this.activePlaceholders.top_player_bought_items_current_week = true;
         }

         return this.top_player_bought_items_current_week;
      }

      private void loadTopPlayerBoughtItemsCurrentWeek() {
         this.top_player_bought_items_current_week = PlaceholderProvider.this.plugin
            .getTransactionLog()
            .getTopThreePlayers(TransactionLog.TimeFrame.WEEK, Transaction.Mode.BUY);
      }

      public ArrayList<TransactionLog.TopPlayer> getTopPlayerBoughtItemsCurrentDay() {
         if (!this.activePlaceholders.top_player_bought_items_current_day) {
            this.loadTopPlayerBoughtItemsCurrentDay();
            this.activePlaceholders.top_player_bought_items_current_day = true;
         }

         return this.top_player_bought_items_current_day;
      }

      private void loadTopPlayerBoughtItemsCurrentDay() {
         this.top_player_bought_items_current_day = PlaceholderProvider.this.plugin
            .getTransactionLog()
            .getTopThreePlayers(TransactionLog.TimeFrame.DAY, Transaction.Mode.BUY);
      }

      public ArrayList<TransactionLog.TopPlayer> getTopPlayerSoldItemsCurrentMonth() {
         if (!this.activePlaceholders.top_player_sold_items_current_month) {
            this.loadTopPlayerSoldItemsCurrentMonth();
            this.activePlaceholders.top_player_sold_items_current_month = true;
         }

         return this.top_player_sold_items_current_month;
      }

      private void loadTopPlayerSoldItemsCurrentMonth() {
         this.top_player_sold_items_current_month = PlaceholderProvider.this.plugin
            .getTransactionLog()
            .getTopThreePlayers(TransactionLog.TimeFrame.MONTH, Transaction.Mode.SELL);
      }

      public ArrayList<TransactionLog.TopPlayer> getTopPlayerSoldItemsCurrentWeek() {
         if (!this.activePlaceholders.top_player_sold_items_current_week) {
            this.loadTopPlayerSoldItemsCurrentWeek();
            this.activePlaceholders.top_player_sold_items_current_week = true;
         }

         return this.top_player_sold_items_current_week;
      }

      private void loadTopPlayerSoldItemsCurrentWeek() {
         this.top_player_sold_items_current_week = PlaceholderProvider.this.plugin
            .getTransactionLog()
            .getTopThreePlayers(TransactionLog.TimeFrame.WEEK, Transaction.Mode.SELL);
      }

      public ArrayList<TransactionLog.TopPlayer> getTopPlayerSoldItemsCurrentDay() {
         if (!this.activePlaceholders.top_player_sold_items_current_day) {
            this.loadTopPlayerSoldItemsCurrentDay();
            this.activePlaceholders.top_player_sold_items_current_day = true;
         }

         return this.top_player_sold_items_current_day;
      }

      private void loadTopPlayerSoldItemsCurrentDay() {
         this.top_player_sold_items_current_day = PlaceholderProvider.this.plugin
            .getTransactionLog()
            .getTopThreePlayers(TransactionLog.TimeFrame.DAY, Transaction.Mode.SELL);
      }

      public ArrayList<TransactionLog.TopItem> getTopBoughtItemsCurrentMonth() {
         if (!this.activePlaceholders.top_bought_items_current_month) {
            this.loadTopBoughtItemsCurrentMonth();
            this.activePlaceholders.top_bought_items_current_month = true;
         }

         return this.top_bought_items_current_month;
      }

      private void loadTopBoughtItemsCurrentMonth() {
         this.top_bought_items_current_month = PlaceholderProvider.this.plugin
            .getTransactionLog()
            .getTopThreeItems(TransactionLog.TimeFrame.MONTH, Transaction.Mode.BUY);
      }

      public ArrayList<TransactionLog.TopItem> getTopBoughtItemsCurrentWeek() {
         if (!this.activePlaceholders.top_bought_items_current_week) {
            this.loadTopBoughtItemsCurrentWeek();
            this.activePlaceholders.top_bought_items_current_week = true;
         }

         return this.top_bought_items_current_week;
      }

      private void loadTopBoughtItemsCurrentWeek() {
         this.top_bought_items_current_week = PlaceholderProvider.this.plugin
            .getTransactionLog()
            .getTopThreeItems(TransactionLog.TimeFrame.WEEK, Transaction.Mode.BUY);
      }

      public ArrayList<TransactionLog.TopItem> getTopBoughtItemsCurrentDay() {
         if (!this.activePlaceholders.top_bought_items_current_day) {
            this.loadTopBoughtItemsCurrentDay();
            this.activePlaceholders.top_bought_items_current_day = true;
         }

         return this.top_bought_items_current_day;
      }

      private void loadTopBoughtItemsCurrentDay() {
         this.top_bought_items_current_day = PlaceholderProvider.this.plugin
            .getTransactionLog()
            .getTopThreeItems(TransactionLog.TimeFrame.DAY, Transaction.Mode.BUY);
      }

      public ArrayList<TransactionLog.TopItem> getTopSoldItemsCurrentMonth() {
         if (!this.activePlaceholders.top_sold_items_current_month) {
            this.loadTopSoldItemsCurrentMonth();
            this.activePlaceholders.top_sold_items_current_month = true;
         }

         return this.top_sold_items_current_month;
      }

      private void loadTopSoldItemsCurrentMonth() {
         this.top_sold_items_current_month = PlaceholderProvider.this.plugin
            .getTransactionLog()
            .getTopThreeItems(TransactionLog.TimeFrame.MONTH, Transaction.Mode.SELL);
      }

      public ArrayList<TransactionLog.TopItem> getTopSoldItemsCurrentWeek() {
         if (!this.activePlaceholders.top_sold_items_current_week) {
            this.loadTopSoldItemsCurrentWeek();
            this.activePlaceholders.top_sold_items_current_week = true;
         }

         return this.top_sold_items_current_week;
      }

      private void loadTopSoldItemsCurrentWeek() {
         this.top_sold_items_current_week = PlaceholderProvider.this.plugin
            .getTransactionLog()
            .getTopThreeItems(TransactionLog.TimeFrame.WEEK, Transaction.Mode.SELL);
      }

      public ArrayList<TransactionLog.TopItem> getTopSoldItemsCurrentDay() {
         if (!this.activePlaceholders.top_sold_items_current_day) {
            this.loadTopSoldItemsCurrentDay();
            this.activePlaceholders.top_sold_items_current_day = true;
         }

         return this.top_sold_items_current_day;
      }

      private void loadTopSoldItemsCurrentDay() {
         this.top_sold_items_current_day = PlaceholderProvider.this.plugin
            .getTransactionLog()
            .getTopThreeItems(TransactionLog.TimeFrame.DAY, Transaction.Mode.SELL);
      }
   }

   public final class PlaceholderItemCache {
      private final Map<PlaceholderProvider.Item, Integer> CACHE = new ConcurrentHashMap<>();

      public PlaceholderItemCache() {
      }

      private void reloadActive() {
         this.CACHE
            .replaceAll(
               (var1, var2) -> PlaceholderProvider.this.plugin
                  .getTransactionLog()
                  .getItemTransactions(var1.mode, var1.timeFrame, var1.section, var1.index, null)
            );
      }

      public int getTotalTransactions(Transaction.Mode var1, TransactionLog.TimeFrame var2, String var3, String var4) {
         return this.CACHE
            .computeIfAbsent(
               new PlaceholderProvider.Item(var1, var2, var3, var4),
               var1x -> PlaceholderProvider.this.plugin.getTransactionLog().getItemTransactions(var1x.mode, var1x.timeFrame, var1x.section, var1x.index, null)
            );
      }
   }

   public final class PlaceholderPlayerItemCache {
      private final Map<PlaceholderProvider.PlayerItem, Integer> CACHE = new ConcurrentHashMap<>();

      public PlaceholderPlayerItemCache() {
      }

      private void reloadActive() {
         this.CACHE
            .replaceAll(
               (var1, var2) -> PlaceholderProvider.this.plugin
                  .getTransactionLog()
                  .getItemTransactions(var1.mode, var1.timeFrame, var1.section, var1.index, var1.playerUUID)
            );
      }

      public int getTotalTransactions(Transaction.Mode var1, TransactionLog.TimeFrame var2, UUID var3, String var4, String var5) {
         return this.CACHE
            .computeIfAbsent(
               new PlaceholderProvider.PlayerItem(var1, var2, var3, var4, var5),
               var1x -> PlaceholderProvider.this.plugin
                  .getTransactionLog()
                  .getItemTransactions(var1x.mode, var1x.timeFrame, var1x.section, var1x.index, var1x.playerUUID)
            );
      }
   }

   private static final class PlayerItem {
      final Transaction.Mode mode;
      final TransactionLog.TimeFrame timeFrame;
      final UUID playerUUID;
      final String section;
      final String index;

      public PlayerItem(Transaction.Mode var1, TransactionLog.TimeFrame var2, UUID var3, String var4, String var5) {
         this.mode = var1;
         this.timeFrame = var2;
         this.playerUUID = var3;
         this.section = var4;
         this.index = var5;
      }

      @Override
      public boolean equals(Object var1) {
         if (this == var1) {
            return true;
         } else if (!(var1 instanceof PlaceholderProvider.PlayerItem)) {
            return false;
         } else {
            PlaceholderProvider.PlayerItem var2 = (PlaceholderProvider.PlayerItem)var1;
            return this.mode == var2.mode && this.timeFrame == var2.timeFrame && this.playerUUID == var2.playerUUID && this.section == null
               ? var2.section == null
               : this.section.equals(var2.section) && Objects.equals(this.index, var2.index);
         }
      }

      @Override
      public int hashCode() {
         return Objects.hash(this.mode, this.timeFrame, this.playerUUID, this.section, this.index);
      }
   }
}
