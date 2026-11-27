package com.onyxshop.providers;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.database.Database;
import com.onyxshop.database.types.MySQL;
import com.onyxshop.database.types.PostgreSQL;
import com.onyxshop.database.types.SQLite;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.DynamicPricingCache;
import com.onyxshop.files.Lang;
import com.onyxshop.files.Translatable;
import com.onyxshop.files.config.Config;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.LoreFormatter;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.objects.shops.ShopSection;
import com.onyxshop.objects.shops.ShopType;
import com.onyxshop.util.TimeUtils;
import com.onyxshop.util.scheduler.ScheduledTask;
import org.bukkit.entity.Player;

public class DynamicPricingProvider {
   private final Database database;
   private final OnyxShop plugin;
   private DynamicPricingProvider.PriceRestoration priceRestorationTicker;
   private volatile ConcurrentHashMap<String, Integer> EXPIRED_STOCK = new ConcurrentHashMap<>();
   private volatile ConcurrentHashMap<String, Integer> CASHED_STOCK = new ConcurrentHashMap<>();
   private Map<String, Integer> maxStocks = new HashMap<>();
   private final DynamicPricingCache settingsCache;
   private Map<String, Integer> buyPriceScaling;
   private Map<String, Integer> sellPriceScaling;
   private final boolean cache;
   private final ThreadLocal<DynamicPricingProvider.Snapshot> PAGE_STOCK_SNAPSHOT = new ThreadLocal<>();
   private ScheduledTask task;
   private int maxStock;
   private int minStock;
   private final int magicNumber;

   public DynamicPricingProvider(OnyxShop plugin, Database database) {
      this.plugin = plugin;
      SendMessage.infoMessage(Lang.LOADING_DYNAMIC_PRICING.get());
      this.database = database;
      this.settingsCache = new DynamicPricingCache(plugin);
      this.cache = database instanceof SQLite;
      this.magicNumber = this.maxStock * 2;
      this.initStocks();
      if (this.cache) {
         this.startSaveTask();
      }

      if (ConfigManager.getConfig().getBoolean("dynamic-pricing.price-restoration.enabled", true)) {
         this.priceRestorationTicker = new DynamicPricingProvider.PriceRestoration();
      }

      this.plugin.dynamicPricing = true;
   }

   private void initStocks() {
      this.maxStock = Math.max(20, ConfigManager.getConfig().getInt("dynamic-pricing.max-stock", 250));
      this.minStock = ConfigManager.getConfig().getInt("dynamic-pricing.min-stock", 10);
      this.buyPriceScaling = this.getBuyPriceScaling();
      this.sellPriceScaling = this.getSellPriceScaling();
      this.loadCashedStock();
      Map map = this.loadPrices();
      this.checkForMaxStockUpdates(map);
      this.loadReversibleMaxStocks(map);
   }

   private void checkForMaxStockUpdates(Map<String, Integer> stocks) {
      int value = 2000;

      try {
         value = Integer.parseInt(ConfigManager.getConfig().getString("config-version", "2.0.0.0").replace(".", ""));
      } catch (NumberFormatException exception) {
      }

      if (value == 2024) {
         try {
            this.updateOldMaxStocks(stocks);
            this.maxStock = 250;
            ConfigManager.getConfig().set("dynamic-pricing.max-stock", 250);
            ConfigManager.getConfig().set("config-version", "2.0.3.0");
            ConfigManager.saveConfig();
         } catch (Exception exception) {
            SendMessage.errorMessage("Exception while updating maxStock entries with current DynamicPricing");
            exception.printStackTrace();
         }
      }

      Set<String> values = new HashSet<>();

      for (String text : ConfigManager.getShops()) {
         int staticMaxStock = this.getStaticMaxStock(text, null, 0);
         if (staticMaxStock > 0) {
            Integer cachedMaxStock = this.settingsCache.getCachedMaxStock(text);
            if (cachedMaxStock == null || cachedMaxStock != staticMaxStock) {
               this.settingsCache.saveCachedMaxStock(text, staticMaxStock);
               values.add(text);
            }
         } else if (this.settingsCache.getCachedMaxStock(text) != null) {
            this.settingsCache.removeCachedMaxStock(text);
         }

         int maxPrice = this.getMaxPrice(stocks, text);
         if (maxPrice != -1) {
            Integer cachedMaxPrice = this.settingsCache.getCachedMaxPrice(text);
            if (cachedMaxPrice == null || cachedMaxPrice != maxPrice) {
               this.settingsCache.saveCachedMaxPrice(text, maxPrice);
               values.add(text);
            }
         } else if (this.settingsCache.getCachedMaxPrice(text) != null) {
            this.settingsCache.removeCachedMaxPrice(text);
         }
      }

      Integer cachedMaxStock = this.settingsCache.getCachedMaxStock(null);
      if (cachedMaxStock == null || cachedMaxStock != this.maxStock) {
         this.settingsCache.saveCachedMaxStock(null, this.maxStock);
         values = ConfigManager.getShops();
      }

      if (!values.isEmpty()) {
         this.updateMaxStocks(stocks, (Set<String>)values);
      }
   }

   private void updateMaxStocks(Map<String, Integer> map, Set<String> values) {
      SendMessage.infoMessage("Detected maxStock change, automatically updating dynamic pricing from database with new maxStock data...");

      try {
         Map itemIds = !this.cache && !this.plugin.displayCache ? this.database.loadDynamicPricingStock() : this.CASHED_STOCK;
         Map map2 = this.database.loadDynamicMaxStocks();

         for (String text : values) {
            int staticMaxStock = this.getStaticMaxStock(text, null, 0);
            if (staticMaxStock <= 0) {
               Map<String, Integer> invertedStocks = this.getInvertedStocks(map, text);

               for (String key : invertedStocks.keySet()) {
                  int staticMaxStock2 = this.getStaticMaxStock(text, key.split("\\.", 2)[1], 0);
                  if (staticMaxStock2 <= 0) {
                     int value = (Integer)invertedStocks.get(key);
                     if (itemIds.containsKey(key) && map2.containsKey(key) && !((Integer)map2.get(key)).equals(value)) {
                        this.database.setDynamicMaxStock(key, value);
                        double amount = ((Integer)itemIds.get(key)).intValue() * 200.0 / ((Integer)map2.get(key)).intValue();
                        if (amount > 200.0 || amount < 0.0) {
                           amount = 100.0;
                        }

                        int value2 = (int)Math.round(value / 200.0 * amount);
                        this.setDynamicStock(key, Math.max(0, Math.min(value, value2)));
                     }
                  }
               }
            }
         }
      } catch (SQLException exception) {
         SendMessage.warnMessage("Failed to automatically update dynamic pricing with new maxStock data, resetting dynamic pricing instead...");
         exception.printStackTrace();
      }
   }

   private void updateOldMaxStocks(Map<String, Integer> map) throws SQLException {
      SendMessage.infoMessage("Automatically updating current DynamicPricing from database with new maxStock data...");
      OnyxShop.getInstance().createFileBackup("database.db");
      Map itemIds = this.database.loadDynamicPricingStock();
      Map maxStocksOld = this.getMaxStocksOld(itemIds);

      for (String text : ConfigManager.getShops()) {
         Map<String, Integer> invertedStocks = this.getInvertedStocks(map, text);

         for (String key : invertedStocks.keySet()) {
            int staticMaxStock = this.getStaticMaxStock(text, key.split("\\.", 2)[1], 0);
            if (staticMaxStock >= 0 && staticMaxStock <= 0) {
               int value = (Integer)invertedStocks.get(key);
               if (itemIds.containsKey(key) && maxStocksOld.containsKey(key) && !((Integer)maxStocksOld.get(key)).equals(value)) {
                  this.database.setDynamicMaxStock(key, value);
                  double amount = ((Integer)itemIds.get(key)).intValue() * 200.0 / ((Integer)maxStocksOld.get(key)).intValue();
                  if (amount > 200.0 || amount < 0.0) {
                     amount = 100.0;
                  }

                  int value2 = (int)Math.round(value / 200.0 * amount);
                  this.setDynamicStock(key, Math.max(0, Math.min(value, value2)));
               }
            }
         }
      }
   }

   private int getMaxPrice(Map<String, Integer> prices, String text) {
      return prices.entrySet()
         .stream()
         .filter(entry -> ((String)entry.getKey()).split("\\.")[0].equals(text))
         .map(Entry::getValue)
         .mapToInt(Integer::intValue)
         .max()
         .orElse(-1);
   }

   private void setDynamicStockSafe(String itemId, int value) {
      if (this.cache || this.plugin.displayCache) {
         this.CASHED_STOCK.put(itemId, Math.max(0, Math.min(this.maxStocks.get(itemId) / 2, value)));
      }

      if (this.database instanceof MySQL || this.database instanceof PostgreSQL) {
         this.database.setDynamicStock(itemId, Math.max(0, Math.min(this.maxStocks.get(itemId) / 2, value)));
      }
   }

   private void setDynamicStock(String itemId, int value) {
      if (this.cache || this.plugin.displayCache) {
         this.CASHED_STOCK.put(itemId, value);
      }

      if (this.database instanceof MySQL || this.database instanceof PostgreSQL) {
         this.database.setDynamicStock(itemId, value);
      }
   }

   private Map<String, Integer> loadPrices() {
      HashMap map = new HashMap();

      for (String text : ConfigManager.getShops()) {
         for (String text2 : this.plugin.getConfigManager().getItemsRaw(text)) {
            if (!ConfigManager.getShop(text)
               .contains((this.plugin.getConfigManager().getShopType(text) == ShopType.ROTATING ? "pages." : "") + text2 + ".action")) {
               double itemPrice = this.getItemPrice(text, text2);
               if (!(itemPrice <= 0.0)) {
                  map.put(text + "." + text2, (int)(Math.log(itemPrice + 1.0) * 100.0));
               }
            }
         }
      }

      return map;
   }

   private Map<String, Integer> getBuyPriceScaling() {
      int price = ConfigManager.getConfig().getInt("dynamic-pricing.buyPriceScaling");
      if (price > 100 || price < 0) {
         Translatable text = Lang.INVALID_BUY_PRICE_SCALING.get().replace("%scale%", String.valueOf(price));
         price = 30;
         SendMessage.warnMessage(text.replace("%default%", String.valueOf(30)));
      }

      HashMap map = new HashMap();

      for (String text : ConfigManager.getSections()) {
         int value = ConfigManager.getSection(text).getInt("dynamic-pricing.buyPriceScaling", price);
         if (value > 100 || value < 0) {
            Translatable text2 = Lang.INVALID_BUY_PRICE_SCALING.get().replace("%scale%", String.valueOf(price));
            value = price;
            SendMessage.warnMessage(text2.replace("%default%", String.valueOf(price)));
         }

         map.put(text, value);
      }

      return map;
   }

   private Map<String, Integer> getSellPriceScaling() {
      int price = ConfigManager.getConfig().getInt("dynamic-pricing.sellPriceScaling");
      if (price > 100 || price < 0) {
         Translatable text = Lang.INVALID_SELL_PRICE_SCALING.get().replace("%scale%", String.valueOf(price));
         price = 20;
         SendMessage.warnMessage(text.replace("%default%", String.valueOf(20)));
      }

      HashMap map = new HashMap();

      for (String text : ConfigManager.getSections()) {
         int value = ConfigManager.getSection(text).getInt("dynamic-pricing.sellPriceScaling", price);
         if (value > 100 || value < 0) {
            Translatable text2 = Lang.INVALID_BUY_PRICE_SCALING.get().replace("%scale%", String.valueOf(price));
            value = price;
            SendMessage.warnMessage(text2.replace("%default%", String.valueOf(price)));
         }

         map.put(text, value);
      }

      return map;
   }

   private void startSaveTask() {
      long value;
      try {
         value = TimeUtils.getTime(ConfigManager.getConfig().getString("dynamic-pricing.save-period", "1h")) / 1000L * 20L;
      } catch (ParseException exception) {
         value = 72000L;
      }

      this.task = this.plugin.runTaskAsyncTimer(this::saveDynamicStock, value, value);
   }

   public void saveDynamicStock() {
      SendMessage.infoMessage(Lang.SAVING_DYNAMIC_PRICING.get());
      long startTime = System.currentTimeMillis();
      Map<String, Integer> map = new HashMap<>(this.CASHED_STOCK);
      this.database
         .saveDynamicStock(
            map.entrySet()
               .stream()
               .filter(entry -> !Objects.equals(this.EXPIRED_STOCK.get(entry.getKey()), entry.getValue()))
               .collect(Collectors.toMap(Entry::getKey, Entry::getValue))
         );
      this.EXPIRED_STOCK = new ConcurrentHashMap<>(map);
      SendMessage.logDebugMessage(Lang.SAVED_DYNAMIC_PRICING.get().replace("%millis%", String.valueOf(System.currentTimeMillis() - startTime)));
   }

   private void loadCashedStock() {
      try {
         this.CASHED_STOCK = new ConcurrentHashMap<>(this.database.loadDynamicPricingStock());
         if (this.cache) {
            this.EXPIRED_STOCK = new ConcurrentHashMap<>(this.CASHED_STOCK);
         }
      } catch (SQLException exception) {
         SendMessage.errorMessage("Exception while loading the DynamicPricing from database");
         exception.printStackTrace();
      }
   }

   public void reloadDisplayCache() {
      try {
         this.CASHED_STOCK = new ConcurrentHashMap<>(this.database.loadDynamicPricingStock());
      } catch (SQLException exception) {
         SendMessage.errorMessage("Exception while reloading the DynamicPricing display cache from database");
         exception.printStackTrace();
      }
   }

   public double getBuyPrice(String price, double amount, boolean flag) {
      return this.getBuyPrice(price, amount, 1, flag);
   }

   public double getBuyPrice(String price, double amount, int value, boolean flag) {
      return this.getBuyPrice(price, amount, value, flag, null);
   }

   public double getBuyPrice(String price, double page, int index, boolean flag, LoreFormatter.PageIndex pageIndex) {
      double buyPriceDifference = this.getBuyPriceDifference(price, page);
      double amount = 0.0;
      int stock = this.getStock(price, flag, pageIndex);

      for (int index2 = 0; index2 < index; index2++) {
         amount += page + buyPriceDifference / 100.0 * (100.0 - this.getStockScaleMin(price, stock--));
      }

      return amount;
   }

   public void buyItem(String itemId, int value) {
      this.setDynamicStock(itemId, Math.max(this.getStock(itemId, false) - value, 0));
   }

   public double getSellPrice(String price, double amount, boolean flag) {
      return this.getSellPrice(price, amount, 0, 1, flag);
   }

   public double getSellPrice(String price, double amount, int value, int value2, boolean flag) {
      return this.getSellPrice(price, amount, value, value2, flag, null);
   }

   public double getSellPrice(String price, double page, int index, int value, boolean flag, LoreFormatter.PageIndex pageIndex) {
      double sellPriceDifference = this.getSellPriceDifference(price, page);
      double amount = 0.0;
      int stock = this.getStock(price, flag, pageIndex);

      for (int index2 = 0; index2 < index; index2++) {
         amount += page + sellPriceDifference / 100.0 * (100.0 - this.getStockScaleAdd(price, value + stock++));
      }

      return amount;
   }

   public void sellItem(String itemId, int value) {
      this.setDynamicStock(itemId, Math.min(this.getStock(itemId, false) + value, this.maxStocks.get(itemId)));
   }

   private double getBuyPriceDifference(String price, double amount) {
      return amount / 100.0 * this.buyPriceScaling.get(price.split("\\.", 2)[0]).intValue();
   }

   private double getSellPriceDifference(String price, double amount) {
      return amount / 100.0 * this.sellPriceScaling.get(price.split("\\.", 2)[0]).intValue();
   }

   private double getStockScaleAdd(String itemId, int value) {
      return Math.min(value * 200.0 / this.maxStocks.get(itemId).intValue(), 200.0);
   }

   private double getStockScaleMin(String itemId, int value) {
      return Math.max(value * 200.0 / this.maxStocks.get(itemId).intValue(), 0.0);
   }

   private int getStock(String itemId, boolean flag) {
      return this.getStock(itemId, flag, null);
   }

   private int getStock(String itemId, boolean flag, LoreFormatter.PageIndex pageIndex) {
      if (!this.cache && (!flag || !this.plugin.displayCache)) {
         if (flag && pageIndex != null) {
            DynamicPricingProvider.Snapshot snapshot = this.PAGE_STOCK_SNAPSHOT.get();
            if (snapshot == null || snapshot.pageIndex != pageIndex) {
               snapshot = this.loadPageStock(pageIndex);
            }

            if (snapshot.snapshot.containsKey(itemId)) {
               return snapshot.snapshot.get(itemId);
            }
         }

         Integer dynamicStock = this.database.getDynamicStock(itemId);
         return dynamicStock == null ? this.loadStockForItem(itemId) : dynamicStock;
      } else {
         Integer value = this.CASHED_STOCK.get(itemId);
         return value == null ? this.loadStockForItem(itemId) : value;
      }
   }

   private DynamicPricingProvider.Snapshot loadPageStock(LoreFormatter.PageIndex pageIndex) {
      ArrayList itemIds = new ArrayList();

      for (ShopItem shopItem : pageIndex.getItems()) {
         if (shopItem.isDynamicPricing()) {
            itemIds.add(shopItem.getItemPath());
         }
      }

      if (!itemIds.isEmpty()) {
         try {
            DynamicPricingProvider.Snapshot snapshot = new DynamicPricingProvider.Snapshot(pageIndex, this.database.getDynamicStocks(itemIds));
            this.PAGE_STOCK_SNAPSHOT.set(snapshot);
            return snapshot;
         } catch (SQLException exception) {
            SendMessage.errorMessage("Failed to batch load dynamic pricing stock for a shop page");
            exception.printStackTrace();
         }
      }

      DynamicPricingProvider.Snapshot snapshot = new DynamicPricingProvider.Snapshot(pageIndex, Collections.EMPTY_MAP);
      this.PAGE_STOCK_SNAPSHOT.set(snapshot);
      return snapshot;
   }

   private double getItemPrice(String itemId, String price) {
      if (this.plugin.getConfigManager().getShopType(itemId) == ShopType.CATEGORY) {
         price = "pages." + price;
      }

      try {
         if (ConfigManager.getShop(itemId).getDouble(price + ".buy", -1.0) > 0.0) {
            return ConfigManager.getShop(itemId).getDouble(price + ".buy", -1.0);
         } else if (!ConfigManager.getShop(itemId).getStringList(price + ".buy-prices.prices").isEmpty()) {
            return ConfigManager.getShop(itemId).getBoolean(price + ".buy-prices.require-all")
               ? ConfigManager.getShop(itemId)
                  .getStringList(price + ".buy-prices.prices")
                  .stream()
                  .mapToDouble(value -> Double.parseDouble(value.split("::")[1]))
                  .sum()
               : Double.parseDouble(((String)ConfigManager.getShop(itemId).getStringList(price + ".buy-prices.prices").get(0)).split("::")[1]);
         } else if (ConfigManager.getShop(itemId).getDouble(price + ".sell", -1.0) > 0.0) {
            return ConfigManager.getShop(itemId).getDouble(price + ".sell");
         } else if (!ConfigManager.getShop(itemId).getStringList(price + ".sell-prices.prices").isEmpty()) {
            return ConfigManager.getShop(itemId).getBoolean(price + ".sell-prices.give-all")
               ? ConfigManager.getShop(itemId)
                  .getStringList(price + ".sell-prices.prices")
                  .stream()
                  .mapToDouble(value -> Double.parseDouble(value.split("::")[1]))
                  .sum()
               : Double.parseDouble(((String)ConfigManager.getShop(itemId).getStringList(price + ".sell-prices.prices").get(0)).split("::")[1]);
         } else {
            return 0.0;
         }
      } catch (NumberFormatException exception) {
         SendMessage.warnMessage("Failed to load dynamic pricing for item '" + price + "' for section '" + itemId + "' while loading prices, skipping...");
         exception.printStackTrace();
         return 0.0;
      }
   }

   private double getItemPriceOld(String itemId, String price) {
      if (this.plugin.getConfigManager().getShopType(itemId) == ShopType.CATEGORY) {
         price = "pages." + price;
      }

      double amount = ConfigManager.getShop(itemId).getDouble(price + ".buy", -1.0);
      if (amount <= 0.0) {
         try {
            if (!ConfigManager.getShop(itemId).getStringList(price + ".buy-prices.prices").isEmpty()) {
               if (ConfigManager.getShop(itemId).getBoolean(price + ".buy-prices.require-all")) {
                  amount = ConfigManager.getShop(itemId)
                     .getStringList(price + ".buy-prices.prices")
                     .stream()
                     .map(element -> Double.parseDouble(element.split("::")[1]))
                     .count();
               } else {
                  amount = Double.parseDouble(((String)ConfigManager.getShop(itemId).getStringList(price + ".buy-prices.prices").get(0)).split("::")[1]);
               }
            } else if (ConfigManager.getShop(itemId).getDouble(price + ".sell", -1.0) >= 0.0) {
               amount = ConfigManager.getShop(itemId).getDouble(price + ".sell");
            } else if (!ConfigManager.getShop(itemId).getStringList(price + ".sell-prices.prices").isEmpty()) {
               if (ConfigManager.getShop(itemId).getBoolean(price + ".sell-prices.give-all")) {
                  amount = ConfigManager.getShop(itemId)
                     .getStringList(price + ".sell-prices.prices")
                     .stream()
                     .map(element -> Double.parseDouble(element.split("::")[1]))
                     .count();
               } else {
                  amount = Double.parseDouble(((String)ConfigManager.getShop(itemId).getStringList(price + ".sell-prices.prices").get(0)).split("::")[1]);
               }
            }
         } catch (NumberFormatException exception) {
            SendMessage.warnMessage("Failed to load dynamic pricing for item '" + price + "' for section '" + itemId + "' while loading prices, skipping...");
            exception.printStackTrace();
            return 0.0;
         }
      }

      return amount;
   }

   private Integer getCachedStock(String itemId) {
      Integer value = this.CASHED_STOCK.get(itemId);
      if (value == null) {
         value = this.database.getDynamicStock(itemId);
         if (value != null) {
            this.CASHED_STOCK.put(itemId, value);
         }
      }

      return value;
   }

   private int loadStockForItem(String itemId) {
      int value = this.maxStocks.get(itemId) / 2;
      if (this.cache || this.plugin.displayCache) {
         this.CASHED_STOCK.put(itemId, value);
      }

      if (this.database instanceof MySQL || this.database instanceof PostgreSQL) {
         this.database.setDynamicStock(itemId, value);
      }

      return value;
   }

   private Map<String, Integer> reverse(Map<String, Integer> map) {
      HashMap map2 = new HashMap();
      ArrayList values = new ArrayList(map.values());
      Collections.reverse(values);
      int value = 0;

      for (String key : map.keySet()) {
         map2.put(key, (Integer)values.get(value));
         value++;
      }

      return map2;
   }

   private Map<String, Integer> sortValues(Map<String, Integer> map) {
      LinkedList<Entry<String, Integer>> values = new LinkedList<>(map.entrySet());
      values.sort((element, arg2) -> ((Comparable)((Entry)element).getValue()).compareTo(((Entry)arg2).getValue()));
      LinkedHashMap map2 = new LinkedHashMap();

      for (Entry entry : values) {
         map2.put((String)entry.getKey(), (Integer)entry.getValue());
      }

      return map2;
   }

   public Map<String, Integer> getMaxStocksOld(Map<String, Integer> map) throws SQLException {
      HashMap map2 = new HashMap();
      HashMap map3 = new HashMap();
      Map map4 = this.database.loadDynamicMaxStocks();

      for (String text : ConfigManager.getShops()) {
         int staticMaxStock = this.getStaticMaxStock(text, null, 0);

         for (String text2 : this.plugin.getConfigManager().getItemsRaw(text)) {
            String text3 = text + "." + text2;
            if (!(this.getItemPriceOld(text, text2) <= 0.0)) {
               int staticMaxStock2 = this.getStaticMaxStock(text, text2, staticMaxStock);
               if (staticMaxStock2 > 0) {
                  if (map4.containsKey(text3) && !((Integer)map4.get(text3)).equals(staticMaxStock2)) {
                     SendMessage.warnMessage(Lang.DYNAMIC_PRICING_MAX_STOCK_CHANGE.get().replace("%path%", text3).replace("%shop%", text));
                     map.put(text3, staticMaxStock2 / 2);
                  }

                  map3.put(text3, staticMaxStock2);
               } else {
                  int maxStockForPrice = this.getMaxStockForPrice(this.getItemPriceOld(text, text2));
                  if (map4.containsKey(text3) && !((Integer)map4.get(text3)).equals(maxStockForPrice)) {
                     SendMessage.warnMessage(Lang.DYNAMIC_PRICING_PRICE_CHANGE.get().replace("%path%", text3).replace("%shop%", text));
                     map.put(text3, maxStockForPrice / 2);
                  }

                  map2.put(text3, maxStockForPrice);
               }
            }
         }
      }

      map3.putAll(this.reverse(this.sortValues(map2)));
      return map3;
   }

   public void loadReversibleMaxStocks(Map<String, Integer> map) {
      try {
         Map itemIds = !this.cache && !this.plugin.displayCache ? this.database.loadDynamicPricingStock() : this.CASHED_STOCK;
         Map map2 = this.database.loadDynamicMaxStocks();

         for (String text : ConfigManager.getShops()) {
            int staticMaxStock = this.getStaticMaxStock(text, null, 0);
            Map<String, Integer> invertedStocks = this.getInvertedStocks(map, text);

            for (String key : invertedStocks.keySet()) {
               int staticMaxStock2 = this.getStaticMaxStock(text, key.split("\\.", 2)[1], staticMaxStock);
               if (staticMaxStock2 > 0) {
                  if (map2.containsKey(key)) {
                     if (!((Integer)map2.get(key)).equals(staticMaxStock2)) {
                        SendMessage.warnMessage(Lang.DYNAMIC_PRICING_MAX_STOCK_CHANGE.get().replace("%path%", key).replace("%shop%", text));
                        this.database.setDynamicMaxStock(key, staticMaxStock2);
                        this.setDynamicStock(key, staticMaxStock2 / 2);
                     }
                  } else if (itemIds.containsKey(key)) {
                     this.database.setDynamicMaxStock(key, staticMaxStock2);
                  }

                  this.maxStocks.put(key, staticMaxStock2);
               } else {
                  int value = (Integer)invertedStocks.get(key);
                  if (map2.containsKey(key)) {
                     if (!((Integer)map2.get(key)).equals(value)) {
                        SendMessage.warnMessage(Lang.DYNAMIC_PRICING_PRICE_CHANGE.get().replace("%path%", key).replace("%shop%", text));
                        this.database.setDynamicMaxStock(key, value);
                        this.setDynamicStock(key, value / 2);
                     }
                  } else if (itemIds.containsKey(key)) {
                     this.database.setDynamicMaxStock(key, value);
                  }

                  this.maxStocks.put(key, value);
               }
            }
         }

         this.validateStock(itemIds);
      } catch (SQLException exception) {
         SendMessage.errorMessage("Exception while loading the Dynamic Stock from database");
         exception.printStackTrace();
      }
   }

   private void validateStock(Map<String, Integer> stocks) {
      this.maxStocks.forEach((key, value) -> {
         if (stocks.containsKey(key) && ((Integer)stocks.get(key) > value || (Integer)stocks.get(key) < 0)) {
            this.CASHED_STOCK.remove(key);
            if (this.database instanceof MySQL || this.database instanceof PostgreSQL) {
               this.database.setDynamicStock(key, value / 2);
            }
         }
      });
   }

   private Map<String, Integer> getInvertedStocks(Map<String, Integer> map, String text) {
      HashMap map2 = new HashMap();
      boolean flag = false;
      int stock = Math.min(this.minStock, this.maxStock - 10);
      double amount = Math.max(
            1,
            map.entrySet()
               .stream()
               .filter(entry -> ((String)entry.getKey()).split("\\.")[0].equals(text))
               .map(Entry::getValue)
               .mapToInt(Integer::intValue)
               .min()
               .orElse(10)
         )
         - 1;
      double amount2 = map.entrySet()
         .stream()
         .filter(entry -> ((String)entry.getKey()).split("\\.")[0].equals(text))
         .map(Entry::getValue)
         .mapToInt(Integer::intValue)
         .max()
         .orElse(this.maxStock);

      for (Entry entry : map.entrySet().stream().filter(entry2 -> ((String)entry2.getKey()).split("\\.")[0].equals(text)).collect(Collectors.toList())) {
         int value = (int)((((Integer)entry.getValue()).intValue() - amount) / (amount2 - amount) * (this.maxStock - stock - 0) + 0.0);
         int value2 = this.maxStock - value;
         map2.put((String)entry.getKey(), value2 % 2 == 0 ? value2 : value2 + 1);
      }

      return map2;
   }

   public String getStatus(ShopItem shopItem, Player player, boolean flag) {
      double amount = flag
         ? (
            shopItem.isABuyPricing()
               ? shopItem.getABuyPrice().getFirstPrice(player, shopItem.getStackSize(), shopItem.getBuyModifiers(), true)
               : shopItem.getBuyPrice(player, shopItem.getStackSize(), true)
         )
         : (
            shopItem.isASellPricing()
               ? shopItem.getASellPrice().getFirstPrice(player, shopItem.getStackSize(), shopItem.getSellModifiers(), true)
               : shopItem.getSellPrice(player, shopItem.getStackSize(), true)
         );
      double amount2 = flag
         ? (shopItem.isABuyPricing() ? shopItem.getABuyPrice().getFirstPrice() : shopItem.getBuyPrice())
         : (shopItem.isASellPricing() ? shopItem.getASellPrice().getFirstPrice() : shopItem.getSellPrice());
      double amount3 = (amount - amount2) / amount2 * 100.0;
      return amount3 >= 0.0
         ? Lang.RISING_DYNAMIC_PRICE_PREFIX.get(player).getLegacy().replace("%percent%", String.format("%.2f", amount3))
         : Lang.DROPPING_DYNAMIC_PRICE_PREFIX.get(player).getLegacy().replace("%percent%", String.format("%.2f", Math.abs(amount3)));
   }

   public int getMaxStock(String itemId) {
      return this.maxStocks.get(itemId);
   }

   public void reload() {
      SendMessage.infoMessage(Lang.RELOADING_DYNAMIC_PRICING.get());
      if (this.cache) {
         this.saveDynamicStock();
      }

      this.initStocks();
      if (this.priceRestorationTicker != null) {
         this.priceRestorationTicker.stop();
      }

      if (ConfigManager.getConfig().getBoolean("dynamic-pricing.price-restoration.enabled", true)) {
         this.priceRestorationTicker = new DynamicPricingProvider.PriceRestoration();
      }
   }

   public void resetDynamicPricing(String text) {
      if (this.cache || this.plugin.displayCache) {
         this.CASHED_STOCK.put(text, this.maxStocks.get(text) / 2);
      }

      if (this.database instanceof MySQL || this.database instanceof PostgreSQL) {
         this.database.setDynamicStock(text, this.maxStocks.get(text) / 2);
      }
   }

   private int getMaxStockForPrice(double stock) {
      int value = (int)(stock * 100.0);
      if (value <= 0) {
         return 0;
      } else if (value > this.maxStock) {
         return this.maxStock;
      } else {
         return value % 2 == 0 ? value : value + 1;
      }
   }

   private int getStaticMaxStock(String itemId, String text, int value) {
      if (text != null && this.plugin.getConfigManager().getShopType(itemId) == ShopType.CATEGORY) {
         text = "pages." + text;
      }

      return text != null
         ? ConfigManager.getShop(itemId).getInt(text + ".dynamic-pricing.max-stock", value)
         : ConfigManager.getSection(itemId).getInt("dynamic-pricing.max-stock", value);
   }

   public void stop() {
      if (this.cache) {
         this.task.cancel();
         this.saveDynamicStock();
      }

      if (this.priceRestorationTicker != null) {
         this.priceRestorationTicker.stop();
      }

      this.settingsCache.saveCachedMaxStock(null, this.maxStock);
      this.settingsCache.write();
   }

   private class PriceRestoration {
      private long frequency;
      private double percent;
      private boolean logging;
      private ScheduledTask task;
      private final Map<String, Integer> itemRestoration = new HashMap<>();

      public PriceRestoration() {
         try {
            String text = ConfigManager.getConfig().getString("dynamic-pricing.price-restoration.frequency");
            if (text == null) {
               SendMessage.errorMessage("Cannot enable dynamic price restoration because no frequency defined in config.yml");
               return;
            }

            this.frequency = TimeUtils.getTime(text) / 1000L * 20L;
            this.percent = ConfigManager.getConfig().getDouble("dynamic-pricing.price-restoration.percent");
            this.logging = ConfigManager.getConfig().getBoolean("dynamic-pricing.price-restoration.logging", false);
            this.loadItemRestorations();
            this.task = DynamicPricingProvider.this.plugin
               .runKillableTaskTimer(
                  () -> this.restorePrices(
                        DynamicPricingProvider.this.plugin
                           .getSections()
                           .values()
                           .stream()
                           .map(ShopSection::getShopItems)
                           .flatMap(Collection::stream)
                           .filter(ShopItem::isDynamicPricing)
                           .map(ShopItem::getItemPath)
                           .collect(Collectors.toSet())
                     )
                     .run(),
                  this.frequency,
                  this.frequency
               );
            SendMessage.infoMessage("Enabled dynamic pricing restoration timer with a frequency of " + text + "...");
         } catch (ParseException exception) {
            SendMessage.errorMessage("Failed to parse dynamic price restoration frequency with reason: " + exception.getMessage());
         } catch (Exception exception) {
            SendMessage.errorMessage("Failed to enable dynamic price restoration timer");
            exception.printStackTrace();
         }
      }

      private Runnable restorePrices(Set<String> values) {
         return () -> {
            for (String text : values) {
               int stock = DynamicPricingProvider.this.getStock(text, false);
               int stock2 = DynamicPricingProvider.this.getMaxStock(text) / 2;
               int orDefault = this.itemRestoration.getOrDefault(text, stock2);
               if (stock != orDefault) {
                  int value = (int)Math.max(1.0, Math.ceil(stock2 / 100.0 * this.percent));
                  int value2 = stock - orDefault;
                  if (value2 >= 0) {
                     stock -= Math.min(value, value2);
                  } else {
                     stock += Math.min(value, Math.abs(value2));
                  }

                  if (this.logging && stock == orDefault) {
                     SendMessage.infoMessage("Completed DynamicPricing restoration for item " + text);
                  }

                  DynamicPricingProvider.this.setDynamicStock(text, stock);
               }
            }
         };
      }

      private void loadItemRestorations() {
         for (String itemId : ConfigManager.getShops()) {
            Config config = ConfigManager.getShop(itemId);

            for (String shopId : OnyxShop.getInstance().getConfigManager().getItemsRaw(itemId)) {
               if (config.contains("pages." + shopId + ".dynamic-pricing.price-restoration.target-value")) {
                  String text = config.getString("pages." + shopId + ".dynamic-pricing.price-restoration.target-value");

                  try {
                     int value = Integer.parseInt(text.replace("%", ""));
                     if (value < -100 || value > 100) {
                        SendMessage.errorMessage("Dynamic pricing restoration target should be lower as 100% and higher as -100%");
                        SendMessage.errorItemShops(itemId + "." + shopId);
                     }

                     if (value > 0) {
                        value = -1 * value;
                     } else if (value < 0) {
                        value = Math.abs(value);
                     }

                     int stock = DynamicPricingProvider.this.getMaxStock(itemId + "." + shopId) / 2;
                     int value2 = (int)Math.round(stock + stock / 100.0 * value);
                     this.itemRestoration.put(itemId + "." + shopId, value2);
                     SendMessage.logDebugMessage(
                        "Successfully loaded dynamic pricing restoration target for item "
                           + itemId
                           + "."
                           + shopId
                           + " with default value of '"
                           + stock
                           + "' and a overridden target value of '"
                           + value2
                           + "'"
                     );
                  } catch (NumberFormatException exception) {
                     SendMessage.errorMessage("Invalid dynamic pricing price restoration value found for " + text);
                     SendMessage.errorItemShops(itemId + "." + shopId);
                  }
               }
            }
         }
      }

      private void stop() {
         this.task.cancel();
      }
   }

   private static final class Snapshot {
      public final LoreFormatter.PageIndex pageIndex;
      public final Map<String, Integer> snapshot;

      private Snapshot(LoreFormatter.PageIndex pageIndex, Map<String, Integer> pages) {
         this.pageIndex = pageIndex;
         this.snapshot = pages;
      }
   }
}
