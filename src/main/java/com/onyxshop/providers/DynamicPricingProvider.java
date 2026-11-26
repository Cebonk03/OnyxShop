package com.onyxshop.providers;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
   private Map<String, Integer> EXPIRED_STOCK;
   private Map<String, Integer> CASHED_STOCK = new HashMap<>();
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

   public DynamicPricingProvider(OnyxShop var1, Database var2) {
      this.plugin = var1;
      SendMessage.infoMessage(Lang.LOADING_DYNAMIC_PRICING.get());
      this.database = var2;
      this.settingsCache = new DynamicPricingCache(var1);
      this.cache = var2 instanceof SQLite;
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
      Map var1 = this.loadPrices();
      this.checkForMaxStockUpdates(var1);
      this.loadReversibleMaxStocks(var1);
   }

   private void checkForMaxStockUpdates(Map<String, Integer> var1) {
      int var2 = 2000;

      try {
         var2 = Integer.parseInt(ConfigManager.getConfig().getString("config-version", "2.0.0.0").replace(".", ""));
      } catch (NumberFormatException var10) {
      }

      if (var2 == 2024) {
         try {
            this.updateOldMaxStocks(var1);
            this.maxStock = 250;
            ConfigManager.getConfig().set("dynamic-pricing.max-stock", 250);
            ConfigManager.getConfig().set("config-version", "2.0.3.0");
            ConfigManager.saveConfig();
         } catch (Exception var9) {
            SendMessage.errorMessage("Exception while updating maxStock entries with current DynamicPricing");
            var9.printStackTrace();
         }
      }

      Set<String> var3 = new HashSet<>();

      for (String var5 : ConfigManager.getShops()) {
         int var6 = this.getStaticMaxStock(var5, null, 0);
         if (var6 > 0) {
            Integer var7 = this.settingsCache.getCachedMaxStock(var5);
            if (var7 == null || var7 != var6) {
               this.settingsCache.saveCachedMaxStock(var5, var6);
               var3.add(var5);
            }
         } else if (this.settingsCache.getCachedMaxStock(var5) != null) {
            this.settingsCache.removeCachedMaxStock(var5);
         }

         int var12 = this.getMaxPrice(var1, var5);
         if (var12 != -1) {
            Integer var8 = this.settingsCache.getCachedMaxPrice(var5);
            if (var8 == null || var8 != var12) {
               this.settingsCache.saveCachedMaxPrice(var5, var12);
               var3.add(var5);
            }
         } else if (this.settingsCache.getCachedMaxPrice(var5) != null) {
            this.settingsCache.removeCachedMaxPrice(var5);
         }
      }

      Integer var11 = this.settingsCache.getCachedMaxStock(null);
      if (var11 == null || var11 != this.maxStock) {
         this.settingsCache.saveCachedMaxStock(null, this.maxStock);
         var3 = ConfigManager.getShops();
      }

      if (!var3.isEmpty()) {
         this.updateMaxStocks(var1, (Set<String>)var3);
      }
   }

   private void updateMaxStocks(Map<String, Integer> var1, Set<String> var2) {
      SendMessage.infoMessage("Detected maxStock change, automatically updating dynamic pricing from database with new maxStock data...");

      try {
         Map var3 = !this.cache && !this.plugin.displayCache ? this.database.loadDynamicPricingStock() : this.CASHED_STOCK;
         Map var4 = this.database.loadDynamicMaxStocks();

         for (String var6 : var2) {
            int var7 = this.getStaticMaxStock(var6, null, 0);
            if (var7 <= 0) {
               Map<String, Integer> var8 = this.getInvertedStocks(var1, var6);

               for (String var10 : var8.keySet()) {
                  int var11 = this.getStaticMaxStock(var6, var10.split("\\.", 2)[1], 0);
                  if (var11 <= 0) {
                     int var12 = (Integer)var8.get(var10);
                     if (var3.containsKey(var10) && var4.containsKey(var10) && !((Integer)var4.get(var10)).equals(var12)) {
                        this.database.setDynamicMaxStock(var10, var12);
                        double var13 = ((Integer)var3.get(var10)).intValue() * 200.0 / ((Integer)var4.get(var10)).intValue();
                        if (var13 > 200.0 || var13 < 0.0) {
                           var13 = 100.0;
                        }

                        int var15 = (int)Math.round(var12 / 200.0 * var13);
                        this.setDynamicStock(var10, Math.max(0, Math.min(var12, var15)));
                     }
                  }
               }
            }
         }
      } catch (SQLException var16) {
         SendMessage.warnMessage("Failed to automatically update dynamic pricing with new maxStock data, resetting dynamic pricing instead...");
         var16.printStackTrace();
      }
   }

   private void updateOldMaxStocks(Map<String, Integer> var1) throws SQLException {
      SendMessage.infoMessage("Automatically updating current DynamicPricing from database with new maxStock data...");
      OnyxShop.getInstance().createFileBackup("database.db");
      Map var2 = this.database.loadDynamicPricingStock();
      Map var3 = this.getMaxStocksOld(var2);

      for (String var5 : ConfigManager.getShops()) {
         Map<String, Integer> var6 = this.getInvertedStocks(var1, var5);

         for (String var8 : var6.keySet()) {
            int var9 = this.getStaticMaxStock(var5, var8.split("\\.", 2)[1], 0);
            if (var9 >= 0 && var9 <= 0) {
               int var10 = (Integer)var6.get(var8);
               if (var2.containsKey(var8) && var3.containsKey(var8) && !((Integer)var3.get(var8)).equals(var10)) {
                  this.database.setDynamicMaxStock(var8, var10);
                  double var11 = ((Integer)var2.get(var8)).intValue() * 200.0 / ((Integer)var3.get(var8)).intValue();
                  if (var11 > 200.0 || var11 < 0.0) {
                     var11 = 100.0;
                  }

                  int var13 = (int)Math.round(var10 / 200.0 * var11);
                  this.setDynamicStock(var8, Math.max(0, Math.min(var10, var13)));
               }
            }
         }
      }
   }

   private int getMaxPrice(Map<String, Integer> var1, String var2) {
      return var1.entrySet()
         .stream()
         .filter(var1x -> ((String)var1x.getKey()).split("\\.")[0].equals(var2))
         .map(Entry::getValue)
         .mapToInt(Integer::intValue)
         .max()
         .orElse(-1);
   }

   private void setDynamicStockSafe(String var1, int var2) {
      if (this.cache || this.plugin.displayCache) {
         this.CASHED_STOCK.put(var1, Math.max(0, Math.min(this.maxStocks.get(var1) / 2, var2)));
      }

      if (this.database instanceof MySQL || this.database instanceof PostgreSQL) {
         this.database.setDynamicStock(var1, Math.max(0, Math.min(this.maxStocks.get(var1) / 2, var2)));
      }
   }

   private void setDynamicStock(String var1, int var2) {
      if (this.cache || this.plugin.displayCache) {
         this.CASHED_STOCK.put(var1, var2);
      }

      if (this.database instanceof MySQL || this.database instanceof PostgreSQL) {
         this.database.setDynamicStock(var1, var2);
      }
   }

   private Map<String, Integer> loadPrices() {
      HashMap var1 = new HashMap();

      for (String var3 : ConfigManager.getShops()) {
         for (String var5 : this.plugin.getConfigManager().getItemsRaw(var3)) {
            if (!ConfigManager.getShop(var3)
               .contains((this.plugin.getConfigManager().getShopType(var3) == ShopType.ROTATING ? "pages." : "") + var5 + ".action")) {
               double var6 = this.getItemPrice(var3, var5);
               if (!(var6 <= 0.0)) {
                  var1.put(var3 + "." + var5, (int)(Math.log(var6 + 1.0) * 100.0));
               }
            }
         }
      }

      return var1;
   }

   private Map<String, Integer> getBuyPriceScaling() {
      int var1 = ConfigManager.getConfig().getInt("dynamic-pricing.buyPriceScaling");
      if (var1 > 100 || var1 < 0) {
         Translatable var10000 = Lang.INVALID_BUY_PRICE_SCALING.get().replace("%scale%", String.valueOf(var1));
         var1 = 30;
         SendMessage.warnMessage(var10000.replace("%default%", String.valueOf(30)));
      }

      HashMap var2 = new HashMap();

      for (String var4 : ConfigManager.getSections()) {
         int var5 = ConfigManager.getSection(var4).getInt("dynamic-pricing.buyPriceScaling", var1);
         if (var5 > 100 || var5 < 0) {
            Translatable var6 = Lang.INVALID_BUY_PRICE_SCALING.get().replace("%scale%", String.valueOf(var1));
            var5 = var1;
            SendMessage.warnMessage(var6.replace("%default%", String.valueOf(var1)));
         }

         var2.put(var4, var5);
      }

      return var2;
   }

   private Map<String, Integer> getSellPriceScaling() {
      int var1 = ConfigManager.getConfig().getInt("dynamic-pricing.sellPriceScaling");
      if (var1 > 100 || var1 < 0) {
         Translatable var10000 = Lang.INVALID_SELL_PRICE_SCALING.get().replace("%scale%", String.valueOf(var1));
         var1 = 20;
         SendMessage.warnMessage(var10000.replace("%default%", String.valueOf(20)));
      }

      HashMap var2 = new HashMap();

      for (String var4 : ConfigManager.getSections()) {
         int var5 = ConfigManager.getSection(var4).getInt("dynamic-pricing.sellPriceScaling", var1);
         if (var5 > 100 || var5 < 0) {
            Translatable var6 = Lang.INVALID_BUY_PRICE_SCALING.get().replace("%scale%", String.valueOf(var1));
            var5 = var1;
            SendMessage.warnMessage(var6.replace("%default%", String.valueOf(var1)));
         }

         var2.put(var4, var5);
      }

      return var2;
   }

   private void startSaveTask() {
      long var1;
      try {
         var1 = TimeUtils.getTime(ConfigManager.getConfig().getString("dynamic-pricing.save-period", "1h")) / 1000L * 20L;
      } catch (ParseException var4) {
         var1 = 72000L;
      }

      this.task = this.plugin.runTaskAsyncTimer(this::saveDynamicStock, var1, var1);
   }

   public void saveDynamicStock() {
      SendMessage.infoMessage(Lang.SAVING_DYNAMIC_PRICING.get());
      long var1 = System.currentTimeMillis();
      this.database
         .saveDynamicStock(
            this.CASHED_STOCK
               .entrySet()
               .stream()
               .filter(var1x -> !Objects.equals(this.EXPIRED_STOCK.get(var1x.getKey()), var1x.getValue()))
               .collect(Collectors.toMap(Entry::getKey, Entry::getValue))
         );
      this.EXPIRED_STOCK = new HashMap<>(this.CASHED_STOCK);
      SendMessage.logDebugMessage(Lang.SAVED_DYNAMIC_PRICING.get().replace("%millis%", String.valueOf(System.currentTimeMillis() - var1)));
   }

   private void loadCashedStock() {
      try {
         this.CASHED_STOCK = this.database.loadDynamicPricingStock();
         if (this.cache) {
            this.EXPIRED_STOCK = new HashMap<>(this.CASHED_STOCK);
         }
      } catch (SQLException var2) {
         SendMessage.errorMessage("Exception while loading the DynamicPricing from database");
         var2.printStackTrace();
      }
   }

   public void reloadDisplayCache() {
      try {
         this.CASHED_STOCK = this.database.loadDynamicPricingStock();
      } catch (SQLException var2) {
         SendMessage.errorMessage("Exception while reloading the DynamicPricing display cache from database");
         var2.printStackTrace();
      }
   }

   public double getBuyPrice(String var1, double var2, boolean var4) {
      return this.getBuyPrice(var1, var2, 1, var4);
   }

   public double getBuyPrice(String var1, double var2, int var4, boolean var5) {
      return this.getBuyPrice(var1, var2, var4, var5, null);
   }

   public double getBuyPrice(String var1, double var2, int var4, boolean var5, LoreFormatter.PageIndex var6) {
      double var7 = this.getBuyPriceDifference(var1, var2);
      double var9 = 0.0;
      int var11 = this.getStock(var1, var5, var6);

      for (int var12 = 0; var12 < var4; var12++) {
         var9 += var2 + var7 / 100.0 * (100.0 - this.getStockScaleMin(var1, var11--));
      }

      return var9;
   }

   public void buyItem(String var1, int var2) {
      this.setDynamicStock(var1, Math.max(this.getStock(var1, false) - var2, 0));
   }

   public double getSellPrice(String var1, double var2, boolean var4) {
      return this.getSellPrice(var1, var2, 0, 1, var4);
   }

   public double getSellPrice(String var1, double var2, int var4, int var5, boolean var6) {
      return this.getSellPrice(var1, var2, var4, var5, var6, null);
   }

   public double getSellPrice(String var1, double var2, int var4, int var5, boolean var6, LoreFormatter.PageIndex var7) {
      double var8 = this.getSellPriceDifference(var1, var2);
      double var10 = 0.0;
      int var12 = this.getStock(var1, var6, var7);

      for (int var13 = 0; var13 < var4; var13++) {
         var10 += var2 + var8 / 100.0 * (100.0 - this.getStockScaleAdd(var1, var5 + var12++));
      }

      return var10;
   }

   public void sellItem(String var1, int var2) {
      this.setDynamicStock(var1, Math.min(this.getStock(var1, false) + var2, this.maxStocks.get(var1)));
   }

   private double getBuyPriceDifference(String var1, double var2) {
      return var2 / 100.0 * this.buyPriceScaling.get(var1.split("\\.", 2)[0]).intValue();
   }

   private double getSellPriceDifference(String var1, double var2) {
      return var2 / 100.0 * this.sellPriceScaling.get(var1.split("\\.", 2)[0]).intValue();
   }

   private double getStockScaleAdd(String var1, int var2) {
      return Math.min(var2 * 200.0 / this.maxStocks.get(var1).intValue(), 200.0);
   }

   private double getStockScaleMin(String var1, int var2) {
      return Math.max(var2 * 200.0 / this.maxStocks.get(var1).intValue(), 0.0);
   }

   private int getStock(String var1, boolean var2) {
      return this.getStock(var1, var2, null);
   }

   private int getStock(String var1, boolean var2, LoreFormatter.PageIndex var3) {
      if (!this.cache && (!var2 || !this.plugin.displayCache)) {
         if (var2 && var3 != null) {
            DynamicPricingProvider.Snapshot var5 = this.PAGE_STOCK_SNAPSHOT.get();
            if (var5 == null || var5.pageIndex != var3) {
               var5 = this.loadPageStock(var3);
            }

            if (var5.snapshot.containsKey(var1)) {
               return var5.snapshot.get(var1);
            }
         }

         Integer var6 = this.database.getDynamicStock(var1);
         return var6 == null ? this.loadStockForItem(var1) : var6;
      } else {
         Integer var4 = this.CASHED_STOCK.get(var1);
         return var4 == null ? this.loadStockForItem(var1) : var4;
      }
   }

   private DynamicPricingProvider.Snapshot loadPageStock(LoreFormatter.PageIndex var1) {
      ArrayList var2 = new ArrayList();

      for (ShopItem var4 : var1.getItems()) {
         if (var4.isDynamicPricing()) {
            var2.add(var4.getItemPath());
         }
      }

      if (!var2.isEmpty()) {
         try {
            DynamicPricingProvider.Snapshot var7 = new DynamicPricingProvider.Snapshot(var1, this.database.getDynamicStocks(var2));
            this.PAGE_STOCK_SNAPSHOT.set(var7);
            return var7;
         } catch (SQLException var5) {
            SendMessage.errorMessage("Failed to batch load dynamic pricing stock for a shop page");
            var5.printStackTrace();
         }
      }

      DynamicPricingProvider.Snapshot var6 = new DynamicPricingProvider.Snapshot(var1, Collections.EMPTY_MAP);
      this.PAGE_STOCK_SNAPSHOT.set(var6);
      return var6;
   }

   private double getItemPrice(String var1, String var2) {
      if (this.plugin.getConfigManager().getShopType(var1) == ShopType.CATEGORY) {
         var2 = "pages." + var2;
      }

      try {
         if (ConfigManager.getShop(var1).getDouble(var2 + ".buy", -1.0) > 0.0) {
            return ConfigManager.getShop(var1).getDouble(var2 + ".buy", -1.0);
         } else if (!ConfigManager.getShop(var1).getStringList(var2 + ".buy-prices.prices").isEmpty()) {
            return ConfigManager.getShop(var1).getBoolean(var2 + ".buy-prices.require-all")
               ? ConfigManager.getShop(var1)
                  .getStringList(var2 + ".buy-prices.prices")
                  .stream()
                  .mapToDouble(var0 -> Double.parseDouble(var0.split("::")[1]))
                  .sum()
               : Double.parseDouble(((String)ConfigManager.getShop(var1).getStringList(var2 + ".buy-prices.prices").get(0)).split("::")[1]);
         } else if (ConfigManager.getShop(var1).getDouble(var2 + ".sell", -1.0) > 0.0) {
            return ConfigManager.getShop(var1).getDouble(var2 + ".sell");
         } else if (!ConfigManager.getShop(var1).getStringList(var2 + ".sell-prices.prices").isEmpty()) {
            return ConfigManager.getShop(var1).getBoolean(var2 + ".sell-prices.give-all")
               ? ConfigManager.getShop(var1)
                  .getStringList(var2 + ".sell-prices.prices")
                  .stream()
                  .mapToDouble(var0 -> Double.parseDouble(var0.split("::")[1]))
                  .sum()
               : Double.parseDouble(((String)ConfigManager.getShop(var1).getStringList(var2 + ".sell-prices.prices").get(0)).split("::")[1]);
         } else {
            return 0.0;
         }
      } catch (NumberFormatException var4) {
         SendMessage.warnMessage("Failed to load dynamic pricing for item '" + var2 + "' for section '" + var1 + "' while loading prices, skipping...");
         var4.printStackTrace();
         return 0.0;
      }
   }

   private double getItemPriceOld(String var1, String var2) {
      if (this.plugin.getConfigManager().getShopType(var1) == ShopType.CATEGORY) {
         var2 = "pages." + var2;
      }

      double var3 = ConfigManager.getShop(var1).getDouble(var2 + ".buy", -1.0);
      if (var3 <= 0.0) {
         try {
            if (!ConfigManager.getShop(var1).getStringList(var2 + ".buy-prices.prices").isEmpty()) {
               if (ConfigManager.getShop(var1).getBoolean(var2 + ".buy-prices.require-all")) {
                  var3 = ConfigManager.getShop(var1)
                     .getStringList(var2 + ".buy-prices.prices")
                     .stream()
                     .map(var0 -> Double.parseDouble(var0.split("::")[1]))
                     .count();
               } else {
                  var3 = Double.parseDouble(((String)ConfigManager.getShop(var1).getStringList(var2 + ".buy-prices.prices").get(0)).split("::")[1]);
               }
            } else if (ConfigManager.getShop(var1).getDouble(var2 + ".sell", -1.0) >= 0.0) {
               var3 = ConfigManager.getShop(var1).getDouble(var2 + ".sell");
            } else if (!ConfigManager.getShop(var1).getStringList(var2 + ".sell-prices.prices").isEmpty()) {
               if (ConfigManager.getShop(var1).getBoolean(var2 + ".sell-prices.give-all")) {
                  var3 = ConfigManager.getShop(var1)
                     .getStringList(var2 + ".sell-prices.prices")
                     .stream()
                     .map(var0 -> Double.parseDouble(var0.split("::")[1]))
                     .count();
               } else {
                  var3 = Double.parseDouble(((String)ConfigManager.getShop(var1).getStringList(var2 + ".sell-prices.prices").get(0)).split("::")[1]);
               }
            }
         } catch (NumberFormatException var6) {
            SendMessage.warnMessage("Failed to load dynamic pricing for item '" + var2 + "' for section '" + var1 + "' while loading prices, skipping...");
            var6.printStackTrace();
            return 0.0;
         }
      }

      return var3;
   }

   private Integer getCachedStock(String var1) {
      Integer var2 = this.CASHED_STOCK.get(var1);
      if (var2 == null) {
         var2 = this.database.getDynamicStock(var1);
         if (var2 != null) {
            this.CASHED_STOCK.put(var1, var2);
         }
      }

      return var2;
   }

   private int loadStockForItem(String var1) {
      int var2 = this.maxStocks.get(var1) / 2;
      if (this.cache || this.plugin.displayCache) {
         this.CASHED_STOCK.put(var1, var2);
      }

      if (this.database instanceof MySQL || this.database instanceof PostgreSQL) {
         this.database.setDynamicStock(var1, var2);
      }

      return var2;
   }

   private Map<String, Integer> reverse(Map<String, Integer> var1) {
      HashMap var2 = new HashMap();
      ArrayList var3 = new ArrayList(var1.values());
      Collections.reverse(var3);
      int var4 = 0;

      for (String var6 : var1.keySet()) {
         var2.put(var6, (Integer)var3.get(var4));
         var4++;
      }

      return var2;
   }

   private Map<String, Integer> sortValues(Map<String, Integer> var1) {
      LinkedList<Entry<String, Integer>> var2 = new LinkedList<>(var1.entrySet());
      var2.sort((var0, var1x) -> ((Comparable)((Entry)var0).getValue()).compareTo(((Entry)var1x).getValue()));
      LinkedHashMap var3 = new LinkedHashMap();

      for (Entry var5 : var2) {
         var3.put((String)var5.getKey(), (Integer)var5.getValue());
      }

      return var3;
   }

   public Map<String, Integer> getMaxStocksOld(Map<String, Integer> var1) throws SQLException {
      HashMap var2 = new HashMap();
      HashMap var3 = new HashMap();
      Map var4 = this.database.loadDynamicMaxStocks();

      for (String var6 : ConfigManager.getShops()) {
         int var7 = this.getStaticMaxStock(var6, null, 0);

         for (String var9 : this.plugin.getConfigManager().getItemsRaw(var6)) {
            String var10 = var6 + "." + var9;
            if (!(this.getItemPriceOld(var6, var9) <= 0.0)) {
               int var11 = this.getStaticMaxStock(var6, var9, var7);
               if (var11 > 0) {
                  if (var4.containsKey(var10) && !((Integer)var4.get(var10)).equals(var11)) {
                     SendMessage.warnMessage(Lang.DYNAMIC_PRICING_MAX_STOCK_CHANGE.get().replace("%path%", var10).replace("%shop%", var6));
                     var1.put(var10, var11 / 2);
                  }

                  var3.put(var10, var11);
               } else {
                  int var12 = this.getMaxStockForPrice(this.getItemPriceOld(var6, var9));
                  if (var4.containsKey(var10) && !((Integer)var4.get(var10)).equals(var12)) {
                     SendMessage.warnMessage(Lang.DYNAMIC_PRICING_PRICE_CHANGE.get().replace("%path%", var10).replace("%shop%", var6));
                     var1.put(var10, var12 / 2);
                  }

                  var2.put(var10, var12);
               }
            }
         }
      }

      var3.putAll(this.reverse(this.sortValues(var2)));
      return var3;
   }

   public void loadReversibleMaxStocks(Map<String, Integer> var1) {
      try {
         Map var2 = !this.cache && !this.plugin.displayCache ? this.database.loadDynamicPricingStock() : this.CASHED_STOCK;
         Map var3 = this.database.loadDynamicMaxStocks();

         for (String var5 : ConfigManager.getShops()) {
            int var6 = this.getStaticMaxStock(var5, null, 0);
            Map<String, Integer> var7 = this.getInvertedStocks(var1, var5);

            for (String var9 : var7.keySet()) {
               int var10 = this.getStaticMaxStock(var5, var9.split("\\.", 2)[1], var6);
               if (var10 > 0) {
                  if (var3.containsKey(var9)) {
                     if (!((Integer)var3.get(var9)).equals(var10)) {
                        SendMessage.warnMessage(Lang.DYNAMIC_PRICING_MAX_STOCK_CHANGE.get().replace("%path%", var9).replace("%shop%", var5));
                        this.database.setDynamicMaxStock(var9, var10);
                        this.setDynamicStock(var9, var10 / 2);
                     }
                  } else if (var2.containsKey(var9)) {
                     this.database.setDynamicMaxStock(var9, var10);
                  }

                  this.maxStocks.put(var9, var10);
               } else {
                  int var11 = (Integer)var7.get(var9);
                  if (var3.containsKey(var9)) {
                     if (!((Integer)var3.get(var9)).equals(var11)) {
                        SendMessage.warnMessage(Lang.DYNAMIC_PRICING_PRICE_CHANGE.get().replace("%path%", var9).replace("%shop%", var5));
                        this.database.setDynamicMaxStock(var9, var11);
                        this.setDynamicStock(var9, var11 / 2);
                     }
                  } else if (var2.containsKey(var9)) {
                     this.database.setDynamicMaxStock(var9, var11);
                  }

                  this.maxStocks.put(var9, var11);
               }
            }
         }

         this.validateStock(var2);
      } catch (SQLException var12) {
         SendMessage.errorMessage("Exception while loading the Dynamic Stock from database");
         var12.printStackTrace();
      }
   }

   private void validateStock(Map<String, Integer> var1) {
      this.maxStocks.forEach((var2, var3) -> {
         if (var1.containsKey(var2) && ((Integer)var1.get(var2) > var3 || (Integer)var1.get(var2) < 0)) {
            this.CASHED_STOCK.remove(var2);
            if (this.database instanceof MySQL || this.database instanceof PostgreSQL) {
               this.database.setDynamicStock(var2, var3 / 2);
            }
         }
      });
   }

   private Map<String, Integer> getInvertedStocks(Map<String, Integer> var1, String var2) {
      HashMap var3 = new HashMap();
      boolean var4 = false;
      int var5 = Math.min(this.minStock, this.maxStock - 10);
      double var6 = Math.max(
            1,
            var1.entrySet()
               .stream()
               .filter(var1x -> ((String)var1x.getKey()).split("\\.")[0].equals(var2))
               .map(Entry::getValue)
               .mapToInt(Integer::intValue)
               .min()
               .orElse(10)
         )
         - 1;
      double var8 = var1.entrySet()
         .stream()
         .filter(var1x -> ((String)var1x.getKey()).split("\\.")[0].equals(var2))
         .map(Entry::getValue)
         .mapToInt(Integer::intValue)
         .max()
         .orElse(this.maxStock);

      for (Entry var11 : var1.entrySet().stream().filter(var1x -> ((String)var1x.getKey()).split("\\.")[0].equals(var2)).collect(Collectors.toList())) {
         int var12 = (int)((((Integer)var11.getValue()).intValue() - var6) / (var8 - var6) * (this.maxStock - var5 - 0) + 0.0);
         int var13 = this.maxStock - var12;
         var3.put((String)var11.getKey(), var13 % 2 == 0 ? var13 : var13 + 1);
      }

      return var3;
   }

   public String getStatus(ShopItem var1, Player var2, boolean var3) {
      double var4 = var3
         ? (
            var1.isABuyPricing()
               ? var1.getABuyPrice().getFirstPrice(var2, var1.getStackSize(), var1.getBuyModifiers(), true)
               : var1.getBuyPrice(var2, var1.getStackSize(), true)
         )
         : (
            var1.isASellPricing()
               ? var1.getASellPrice().getFirstPrice(var2, var1.getStackSize(), var1.getSellModifiers(), true)
               : var1.getSellPrice(var2, var1.getStackSize(), true)
         );
      double var6 = var3
         ? (var1.isABuyPricing() ? var1.getABuyPrice().getFirstPrice() : var1.getBuyPrice())
         : (var1.isASellPricing() ? var1.getASellPrice().getFirstPrice() : var1.getSellPrice());
      double var8 = (var4 - var6) / var6 * 100.0;
      return var8 >= 0.0
         ? Lang.RISING_DYNAMIC_PRICE_PREFIX.get(var2).getLegacy().replace("%percent%", String.format("%.2f", var8))
         : Lang.DROPPING_DYNAMIC_PRICE_PREFIX.get(var2).getLegacy().replace("%percent%", String.format("%.2f", Math.abs(var8)));
   }

   public int getMaxStock(String var1) {
      return this.maxStocks.get(var1);
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

   public void resetDynamicPricing(String var1) {
      if (this.cache || this.plugin.displayCache) {
         this.CASHED_STOCK.put(var1, this.maxStocks.get(var1) / 2);
      }

      if (this.database instanceof MySQL || this.database instanceof PostgreSQL) {
         this.database.setDynamicStock(var1, this.maxStocks.get(var1) / 2);
      }
   }

   private int getMaxStockForPrice(double var1) {
      int var3 = (int)(var1 * 100.0);
      if (var3 <= 0) {
         return 0;
      } else if (var3 > this.maxStock) {
         return this.maxStock;
      } else {
         return var3 % 2 == 0 ? var3 : var3 + 1;
      }
   }

   private int getStaticMaxStock(String var1, String var2, int var3) {
      if (var2 != null && this.plugin.getConfigManager().getShopType(var1) == ShopType.CATEGORY) {
         var2 = "pages." + var2;
      }

      return var2 != null
         ? ConfigManager.getShop(var1).getInt(var2 + ".dynamic-pricing.max-stock", var3)
         : ConfigManager.getSection(var1).getInt("dynamic-pricing.max-stock", var3);
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
            String var2 = ConfigManager.getConfig().getString("dynamic-pricing.price-restoration.frequency");
            if (var2 == null) {
               SendMessage.errorMessage("Cannot enable dynamic price restoration because no frequency defined in config.yml");
               return;
            }

            this.frequency = TimeUtils.getTime(var2) / 1000L * 20L;
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
            SendMessage.infoMessage("Enabled dynamic pricing restoration timer with a frequency of " + var2 + "...");
         } catch (ParseException var3) {
            SendMessage.errorMessage("Failed to parse dynamic price restoration frequency with reason: " + var3.getMessage());
         } catch (Exception var4) {
            SendMessage.errorMessage("Failed to enable dynamic price restoration timer");
            var4.printStackTrace();
         }
      }

      private Runnable restorePrices(Set<String> var1) {
         return () -> {
            for (String var3 : var1) {
               int var4 = DynamicPricingProvider.this.getStock(var3, false);
               int var5 = DynamicPricingProvider.this.getMaxStock(var3) / 2;
               int var6 = this.itemRestoration.getOrDefault(var3, var5);
               if (var4 != var6) {
                  int var7 = (int)Math.max(1.0, Math.ceil(var5 / 100.0 * this.percent));
                  int var8 = var4 - var6;
                  if (var8 >= 0) {
                     var4 -= Math.min(var7, var8);
                  } else {
                     var4 += Math.min(var7, Math.abs(var8));
                  }

                  if (this.logging && var4 == var6) {
                     SendMessage.infoMessage("Completed DynamicPricing restoration for item " + var3);
                  }

                  DynamicPricingProvider.this.setDynamicStock(var3, var4);
               }
            }
         };
      }

      private void loadItemRestorations() {
         for (String var2 : ConfigManager.getShops()) {
            Config var3 = ConfigManager.getShop(var2);

            for (String var5 : OnyxShop.getInstance().getConfigManager().getItemsRaw(var2)) {
               if (var3.contains("pages." + var5 + ".dynamic-pricing.price-restoration.target-value")) {
                  String var6 = var3.getString("pages." + var5 + ".dynamic-pricing.price-restoration.target-value");

                  try {
                     int var7 = Integer.parseInt(var6.replace("%", ""));
                     if (var7 < -100 || var7 > 100) {
                        SendMessage.errorMessage("Dynamic pricing restoration target should be lower as 100% and higher as -100%");
                        SendMessage.errorItemShops(var2 + "." + var5);
                     }

                     if (var7 > 0) {
                        var7 = -1 * var7;
                     } else if (var7 < 0) {
                        var7 = Math.abs(var7);
                     }

                     int var8 = DynamicPricingProvider.this.getMaxStock(var2 + "." + var5) / 2;
                     int var9 = (int)Math.round(var8 + var8 / 100.0 * var7);
                     this.itemRestoration.put(var2 + "." + var5, var9);
                     SendMessage.logDebugMessage(
                        "Successfully loaded dynamic pricing restoration target for item "
                           + var2
                           + "."
                           + var5
                           + " with default value of '"
                           + var8
                           + "' and a overridden target value of '"
                           + var9
                           + "'"
                     );
                  } catch (NumberFormatException var10) {
                     SendMessage.errorMessage("Invalid dynamic pricing price restoration value found for " + var6);
                     SendMessage.errorItemShops(var2 + "." + var5);
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

      private Snapshot(LoreFormatter.PageIndex var1, Map<String, Integer> var2) {
         this.pageIndex = var1;
         this.snapshot = var2;
      }
   }
}
