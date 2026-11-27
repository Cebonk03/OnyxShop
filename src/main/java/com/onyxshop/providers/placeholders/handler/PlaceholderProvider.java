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

   public PlaceholderProvider(OnyxShop plugin) {
      this.plugin = plugin;
      this.defaultMessage = this.format(Lang.TRANSACTION_PLACEHOLDER_DEFAULT.get().getLegacy());
      int value = ConfigManager.getConfig().getInt("placeholders.dynamic-prices-placeholder-cache", 20);
      this.DYNAMIC_BUY_PRICES_CACHE = CacheBuilder.newBuilder().expireAfterWrite(value, TimeUnit.SECONDS).build();
      this.DYNAMIC_SELL_PRICES_CACHE = CacheBuilder.newBuilder().expireAfterWrite(value, TimeUnit.SECONDS).build();
      this.expansion = new PAPIExpansion(this);
      this.expansion.register();
   }

   public String getItemBuyPrice(String[] itemIds) {
      ShopSection section = this.plugin.getSection(itemIds[0]);
      if (section == null) {
         return "§cNo shop found for '" + itemIds[0] + "'";
      } else {
         ShopItem shopItem = section.getShopItem(itemIds[0] + "." + itemIds[1]);
         if (shopItem == null) {
            return "§cNo item found in shop '" + itemIds[0] + "' at index '" + itemIds[1] + "'";
         } else if (shopItem.hasItemError()) {
            return "§cCannot get price of invalid item";
         } else if (!shopItem.isBuyAble()) {
            return Lang.CANNOT_PURCHASE_ITEM.get().getLegacy();
         } else if (!shopItem.isDynamicPricing()) {
            return shopItem.isABuyPricing()
               ? shopItem.getABuyPrice().formatBuyPrices(null, null, 1)
               : OnyxShop.getInstance().formatPrice(shopItem.getEcoType(), shopItem.getBuyPrice());
         } else {
            String text = (String)this.DYNAMIC_BUY_PRICES_CACHE.getIfPresent(itemIds[0] + "." + itemIds[1]);
            if (text != null) {
               return text;
            } else {
               text = shopItem.isABuyPricing()
                  ? shopItem.getABuyPrice().formatBuyPrices(null, null, 1)
                  : OnyxShop.getInstance().formatPrice(shopItem.getEcoType(), shopItem.getBuyPrice(1));
               this.DYNAMIC_BUY_PRICES_CACHE.put(itemIds[0] + "." + itemIds[1], text);
               return text;
            }
         }
      }
   }

   public String getItemSellPrice(String[] itemIds) {
      ShopSection section = this.plugin.getSection(itemIds[0]);
      if (section == null) {
         return "§cNo shop found for '" + itemIds[0] + "'";
      } else {
         ShopItem shopItem = section.getShopItem(itemIds[0] + "." + itemIds[1]);
         if (shopItem == null) {
            return "§cNo item found in shop '" + itemIds[0] + "' at index '" + itemIds[1] + "'";
         } else if (shopItem.hasItemError()) {
            return "§cCannot get price of invalid item";
         } else if (!shopItem.isSellAble()) {
            return Lang.CANNOT_SELL_ITEM.get().getLegacy();
         } else if (!shopItem.isDynamicPricing()) {
            return shopItem.isASellPricing()
               ? shopItem.getASellPrice().formatSellPrices(null, null, 1)
               : OnyxShop.getInstance().formatPrice(shopItem.getEcoType(), shopItem.getSellPrice());
         } else {
            String text = (String)this.DYNAMIC_SELL_PRICES_CACHE.getIfPresent(itemIds[0] + "." + itemIds[1]);
            if (text != null) {
               return text;
            } else {
               text = shopItem.isASellPricing()
                  ? shopItem.getASellPrice().formatSellPrices(null, null, 1)
                  : OnyxShop.getInstance().formatPrice(shopItem.getEcoType(), shopItem.getSellPrice(1));
               this.DYNAMIC_SELL_PRICES_CACHE.put(itemIds[0] + "." + itemIds[1], text);
               return text;
            }
         }
      }
   }

   public String getTotalTransactions(Transaction.Mode mode, TransactionLog.TimeFrame timeFrame, String text) {
      String text2 = null;
      String text3 = null;
      if (!text.isEmpty()) {
         if (!text.contains(".")) {
            if (this.plugin.getSection(text) == null) {
               return "§cNo shop found for '" + text + "'";
            }

            text2 = text;
         } else {
            String[] values = text.split("\\.", 2);
            ShopSection section = this.plugin.getSection(values[0]);
            if (section == null) {
               return "§cNo shop found for '" + values[0] + "'";
            }

            ShopItem shopItem = section.getShopItem(values[0] + "." + values[1]);
            if (shopItem == null) {
               return "§cNo item found in shop '" + values[0] + "' at index '" + values[1] + "'";
            }

            text2 = values[0];
            text3 = values[1];
         }
      }

      PlaceholderProvider.PlaceholderItemCache placeholderItemCache = this.getPlaceholderItemCache();
      return placeholderItemCache == null ? "§cLoading cache..." : String.valueOf(placeholderItemCache.getTotalTransactions(mode, timeFrame, text2, text3));
   }

   public String getTotalTransactions(Transaction.Mode mode, TransactionLog.TimeFrame timeFrame, String text, UUID uniqueId) {
      String text2 = null;
      String text3 = null;
      if (!text.isEmpty()) {
         if (!text.contains(".")) {
            if (this.plugin.getSection(text) == null) {
               return "§cNo shop found for '" + text + "'";
            }

            text2 = text;
         } else {
            String[] values = text.split("\\.", 2);
            ShopSection section = this.plugin.getSection(values[0]);
            if (section == null) {
               return "§cNo shop found for '" + values[0] + "'";
            }

            ShopItem shopItem = section.getShopItem(values[0] + "." + values[1]);
            if (shopItem == null) {
               return "§cNo item found in shop '" + values[0] + "' at index '" + values[1] + "'";
            }

            text2 = values[0];
            text3 = values[1];
         }
      }

      PlaceholderProvider.PlaceholderPlayerItemCache placeholderPlayerItemCache = this.getPlaceholderPlayerItemCache();
      return placeholderPlayerItemCache == null ? "§cLoading cache..." : String.valueOf(placeholderPlayerItemCache.getTotalTransactions(mode, timeFrame, uniqueId, text2, text3));
   }

   public String getDiscount(Player player, String sectionId) {
      if (this.plugin.getSection(sectionId) == null) {
         return "§cNo shop found for '" + sectionId + "'";
      } else if (!this.plugin.hasDiscount(sectionId)) {
         return "0";
      } else {
         double amount = 0.0;

         for (String key : this.plugin.getDiscounts(sectionId).keySet()) {
            if (PermissionsCache.hasPermission(player, "OnyxShop.discounts." + key)) {
               double amount2 = this.plugin.getDiscounts(sectionId).get(key);
               if (amount == 0.0 || amount < amount2) {
                  amount = amount2;
               }
            }
         }

         return String.valueOf(amount);
      }
   }

   public String getMultiplier(Player player, String sectionId) {
      if (this.plugin.getSection(sectionId) == null) {
         return "§cNo shop found for '" + sectionId + "'";
      } else if (!this.plugin.hasMultiplier(sectionId)) {
         return "0";
      } else {
         double amount = 0.0;

         for (String key : this.plugin.getMultipliers(sectionId).keySet()) {
            if (PermissionsCache.hasPermission(player, "OnyxShop.sell-multipliers." + key)) {
               double amount2 = this.plugin.getMultipliers(sectionId).get(key);
               if (amount == 0.0 || amount < amount2) {
                  amount = amount2;
               }
            }
         }

         return String.valueOf(amount);
      }
   }

   public String getShopRotation(String shopId) {
      ShopSection section = this.plugin.getSection(shopId);
      return section != null && section.getType() == ShopType.ROTATING ? ((RotatingShop)section).getNextRotation() : "§cNo rotating shop found for '" + shopId + "'";
   }

   private String format(String text) {
      if (text.contains("%translations-")) {
         try {
            Lang lang = Lang.valueOf(text.split("%translations-")[1].split("%")[0].toUpperCase(Locale.ENGLISH).replace("-", "_"));
            text = text.replace("%translations-" + lang.getKey() + "%", lang.get().getLegacy());
         } catch (IllegalArgumentException exception) {
         }
      }

      return ChatUtil.formatColors(text);
   }

   private void enablePlaceholderStore() {
      int value = ConfigManager.getConfig().getInt("placeholders.transaction-cache") * 20;
      if (value < 200) {
         value = 200;
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
      }, 0L, value);
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

   private String getTopPlayer(TransactionLog.TimeFrame timeFrame, Transaction.Mode mode) {
      HashMap<UUID, Integer> map = new HashMap<>();

      for (TransactionLog.SimpleTransaction simpleTransaction : this.plugin.getTransactionLog().getTransactions(timeFrame, mode)) {
         UUID playerId = simpleTransaction.uuid;
         int amount = simpleTransaction.amount;
         map.put(playerId, map.getOrDefault(playerId, 0) + amount);
      }

      if (map.isEmpty()) {
         return this.defaultMessage;
      } else {
         UUID uniqueId = null;
         int value = 0;

         for (Entry entry : map.entrySet()) {
            UUID uniqueId2 = (UUID)entry.getKey();
            int value2 = (Integer)entry.getValue();
            if (value2 > value) {
               value = value2;
               uniqueId = uniqueId2;
            }
         }

         return this.getPlayerName(uniqueId);
      }
   }

   private String getTopItem(TransactionLog.TimeFrame timeFrame, Transaction.Mode mode) {
      HashMap<String, Integer> map = new HashMap<>();

      for (TransactionLog.SimpleTransaction simpleTransaction : this.plugin.getTransactionLog().getTransactions(timeFrame, mode)) {
         String itemId = simpleTransaction.itemPath;
         int amount = simpleTransaction.amount;
         map.put(itemId, map.getOrDefault(itemId, 0) + amount);
      }

      if (map.isEmpty()) {
         return this.defaultMessage;
      } else {
         String text = null;
         int value = 0;

         for (Entry entry : map.entrySet()) {
            String text2 = (String)entry.getKey();
            int value2 = (Integer)entry.getValue();
            if (value2 > value) {
               value = value2;
               text = text2;
            }
         }

         return this.getItemName(text);
      }
   }

   private String getPlayerName(UUID playerId) {
      return this.plugin.getServer().getOfflinePlayer(playerId).getName();
   }

   private String getItemName(String itemId) {
      ShopSection section = this.plugin.getSections().get(itemId.split("\\.")[0]);
      if (section == null) {
         return "Unknown";
      } else {
         DisplayItem displayItem = section.getDisplayItem(itemId);
         return displayItem != null ? displayItem.getDisplayname() : "Unknown";
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

      public Item(Transaction.Mode mode, TransactionLog.TimeFrame timeFrame, String itemId, String text) {
         this.mode = mode;
         this.timeFrame = timeFrame;
         this.section = itemId;
         this.index = text;
      }

      @Override
      public boolean equals(Object value) {
         if (this == value) {
            return true;
         } else if (!(value instanceof PlaceholderProvider.Item)) {
            return false;
         } else {
            PlaceholderProvider.Item item = (PlaceholderProvider.Item)value;
            return this.mode == item.mode && this.timeFrame == item.timeFrame && this.section == null
               ? item.section == null
               : this.section.equals(item.section) && Objects.equals(this.index, item.index);
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
               (key, value) -> PlaceholderProvider.this.plugin
                  .getTransactionLog()
                  .getItemTransactions(key.mode, key.timeFrame, key.section, key.index, null)
            );
      }

      public int getTotalTransactions(Transaction.Mode mode, TransactionLog.TimeFrame timeFrame, String text, String text2) {
         return this.CACHE
            .computeIfAbsent(
               new PlaceholderProvider.Item(mode, timeFrame, text, text2),
               key -> PlaceholderProvider.this.plugin.getTransactionLog().getItemTransactions(key.mode, key.timeFrame, key.section, key.index, null)
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
               (key, value) -> PlaceholderProvider.this.plugin
                  .getTransactionLog()
                  .getItemTransactions(key.mode, key.timeFrame, key.section, key.index, key.playerUUID)
            );
      }

      public int getTotalTransactions(Transaction.Mode mode, TransactionLog.TimeFrame timeFrame, UUID uniqueId, String text, String text2) {
         return this.CACHE
            .computeIfAbsent(
               new PlaceholderProvider.PlayerItem(mode, timeFrame, uniqueId, text, text2),
               key -> PlaceholderProvider.this.plugin
                  .getTransactionLog()
                  .getItemTransactions(key.mode, key.timeFrame, key.section, key.index, key.playerUUID)
            );
      }
   }

   private static final class PlayerItem {
      final Transaction.Mode mode;
      final TransactionLog.TimeFrame timeFrame;
      final UUID playerUUID;
      final String section;
      final String index;

      public PlayerItem(Transaction.Mode mode, TransactionLog.TimeFrame timeFrame, UUID playerId, String itemId, String text) {
         this.mode = mode;
         this.timeFrame = timeFrame;
         this.playerUUID = playerId;
         this.section = itemId;
         this.index = text;
      }

      @Override
      public boolean equals(Object value) {
         if (this == value) {
            return true;
         } else if (!(value instanceof PlaceholderProvider.PlayerItem)) {
            return false;
         } else {
            PlaceholderProvider.PlayerItem playerItem = (PlaceholderProvider.PlayerItem)value;
            return this.mode == playerItem.mode && this.timeFrame == playerItem.timeFrame && this.playerUUID == playerItem.playerUUID && this.section == null
               ? playerItem.section == null
               : this.section.equals(playerItem.section) && Objects.equals(this.index, playerItem.index);
         }
      }

      @Override
      public int hashCode() {
         return Objects.hash(this.mode, this.timeFrame, this.playerUUID, this.section, this.index);
      }
   }
}
