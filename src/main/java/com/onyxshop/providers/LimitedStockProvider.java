package com.onyxshop.providers;

import com.google.common.collect.ImmutableMap;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import com.onyxshop.OnyxShop;
import com.onyxshop.database.Database;
import com.onyxshop.database.SQLBatch;
import com.onyxshop.database.queries.ClearPlayerStockQuery;
import com.onyxshop.database.queries.ClearStockQuery;
import com.onyxshop.database.queries.GlobalRestockTimerQuery;
import com.onyxshop.database.queries.ItemStockQuery;
import com.onyxshop.database.queries.PlayerRestockTimerQuery;
import com.onyxshop.database.queries.PlayerStockQuery;
import com.onyxshop.database.queries.RemovePlayerStockQuery;
import com.onyxshop.database.queries.RemovePlayerStocksQuery;
import com.onyxshop.database.queries.RemoveStockQuery;
import com.onyxshop.database.queries.RestockScheduleQuery;
import com.onyxshop.database.queries.UpdatePlayerStockQuery;
import com.onyxshop.database.types.MySQL;
import com.onyxshop.database.types.PostgreSQL;
import com.onyxshop.database.types.SQLite;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.files.Translatable;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.LoreFormatter;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.objects.jobs.RestockItemStock;
import com.onyxshop.objects.shops.ShopType;
import com.onyxshop.util.TimeUtils;
import org.bukkit.entity.Player;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

public class LimitedStockProvider {
   private final ConcurrentHashMap<String, Integer> itemStocks = new ConcurrentHashMap<>();
   private ConcurrentHashMap<String, Integer> CASHED_GLOBAL_STOCK = new ConcurrentHashMap<>();
   private ConcurrentHashMap<String, Map<UUID, Integer>> CASHED_PLAYER_STOCK = new ConcurrentHashMap<>();
   private ConcurrentHashMap<String, Long> GLOBAL_RESTOCK_TIMER = new ConcurrentHashMap<>();
   private ConcurrentHashMap<String, Map<UUID, Long>> PLAYER_RESTOCK_TIMER = new ConcurrentHashMap<>();
   private final Object STOCK_LOCK = new Object();
   private final ThreadLocal<LimitedStockProvider.Snapshot> PAGE_STOCK_SNAPSHOT = new ThreadLocal<>();
   private ConcurrentHashMap<ScheduleManager.Item, Long> restockSchedulers = new ConcurrentHashMap<>();
   private final OnyxShop plugin;
   private final Database database;
   private SQLBatch sqlBatch;
   private final boolean cache;

   public LimitedStockProvider(OnyxShop plugin, Database database) {
      this.plugin = plugin;
      this.database = database;
      this.cache = database instanceof SQLite;
      SendMessage.infoMessage(Lang.LOADING_ITEM_STOCK.get());
      this.loadItemStockModes();
      this.loadRestockTimes();
      this.startRestockTimers();
      if (this.cache) {
         this.sqlBatch = new SQLBatch(this.plugin, this.database, SQLBatch.Type.STOCK);
      }

      if (this.cache || this.plugin.displayCache) {
         this.loadItemStock();
      }

      this.reloadOriginals();
      this.plugin.itemStock = true;
   }

   public void reload() {
      SendMessage.infoMessage(Lang.RELOADING_ITEM_STOCK.get());
      if (!this.itemStocks.isEmpty()) {
         this.itemStocks.clear();
      }

      this.loadItemStockModes();
      this.reloadOriginals();
      this.reloadRestockTimers();
   }

   private void startRestockTimers() {
      Map cachedSchedulers = this.getCachedSchedulers();
      int restockTime = this.loadRestockTimers(cachedSchedulers);
      if (restockTime != 0) {
         SendMessage.infoMessage("Completed loading " + restockTime + " item restock cron schedulers");
      }
   }

   private void reloadRestockTimers() {
      int restockTime = this.loadRestockTimers(this.restockSchedulers);
      if (restockTime != 0) {
         SendMessage.infoMessage("Successfully reloaded " + restockTime + " item restock cron schedulers");
      }
   }

   private int loadRestockTimers(Map<ScheduleManager.Item, Long> restockTimes) {
      AtomicInteger counter = new AtomicInteger(0);
      this.itemStocks
         .forEach(
            (key, value) -> {
               ScheduleManager.Item item = new ScheduleManager.Item(key, value);

               try {
                  Date date = this.scheduleJob(item);
                  if (date == null) {
                     return;
                  }

                  if (restockTimes.containsKey(item)) {
                     if ((Long)restockTimes.get(item) <= 0L && !this.restockSchedulers.containsKey(item)) {
                        this.restockItem(key, value, date.getTime());
                        SendMessage.infoMessage(
                           "Successfully restocked item '"
                              + key
                              + "' for "
                              + (value == 1 ? "global" : "player")
                              + " stock while cron schedule ended at "
                              + TimeUtils.getReadableTime((Long)restockTimes.get(item))
                              + " when server was offline"
                        );
                     }

                     if ((Long)restockTimes.get(item) != date.getTime()) {
                        SendMessage.infoMessage("Detected cron schedule change for item " + key + ", updating schedule with next fire time at " + date);
                        restockTimes.put(item, date.getTime());
                     }
                  } else {
                     restockTimes.put(item, date.getTime());
                  }

                  SendMessage.logDebugMessage(
                     "Successfully loaded cron restock schedule for item '"
                        + key
                        + "' for "
                        + (value == 1 ? "global" : "player")
                        + " stock with next schedule date at "
                        + date
                  );
                  counter.incrementAndGet();
               } catch (RuntimeException exception) {
                  SendMessage.warnMessage("Invalid cron expression format with reason: '" + exception.getCause().getMessage());
                  SendMessage.errorShops(item.section, item.path);
               } catch (SchedulerException exception) {
                  SendMessage.warnMessage("A unknown error occurred while creating a new cron schedule");
                  SendMessage.errorShops(item.section, item.path);
                  exception.printStackTrace();
               } catch (Exception exception) {
                  exception.printStackTrace();
               }
            }
         );
      this.restockSchedulers = new ConcurrentHashMap<>(restockTimes);
      return counter.get();
   }

   private Date scheduleJob(ScheduleManager.Item item) throws SchedulerException {
      String text = null;
      if (item.mode == 2) {
         text = ConfigManager.getShop(item.section).getString(this.getItemLoc(item.getItemPath()) + ".auto-restock-player-frequency");
         if (text == null) {
            return null;
         }

         if (ConfigManager.getShop(item.section).getString(this.getItemLoc(item.getItemPath()) + ".auto-restock-player") != null) {
            SendMessage.warnMessage("A item cannot have a restock timeframe aswell as a restock frequency, using frequency as default...");
            SendMessage.errorShops(item.section, item.path);
         }
      } else if (item.mode == 1) {
         text = ConfigManager.getShop(item.section).getString(this.getItemLoc(item.getItemPath()) + ".auto-restock-global-frequency");
         if (text == null) {
            return null;
         }

         if (ConfigManager.getShop(item.section).getString(this.getItemLoc(item.getItemPath()) + ".auto-restock-global") != null) {
            SendMessage.warnMessage("A item cannot have a restock timeframe aswell as a restock frequency, using frequency as default...");
            SendMessage.errorShops(item.section, item.path);
         }
      }

      JobDetail jobDetail = JobBuilder.newJob(RestockItemStock.class)
         .setJobData(new JobDataMap(ImmutableMap.of("item", item.getItemPath(), "mode", item.mode)))
         .withIdentity(item.getItemPath() + "_JOB", "ONYX_RESTOCK_ITEM_STOCK")
         .build();
      Trigger trigger = TriggerBuilder.newTrigger()
         .withIdentity(item.getItemPath() + "_TRIGGER", "ONYX_RESTOCK_ITEM_STOCK")
         .withSchedule(CronScheduleBuilder.cronSchedule(text))
         .build();
      if (!this.restockSchedulers.containsKey(item)) {
         this.plugin.getScheduleManager().schedule(jobDetail, trigger);
         if (trigger.getNextFireTime() == null) {
            SendMessage.warnMessage("Failed to schedule cron expression as it will never fire again, for item " + item + " and schedule " + text);
            SendMessage.errorShops(item.section, item.path);
            return null;
         }
      }

      return trigger.getNextFireTime();
   }

   private Map<ScheduleManager.Item, Long> getCachedSchedulers() {
      Object value = new HashMap();

      try {
         value = this.database.getItemStockSchedulers();
      } catch (SQLException exception) {
         SendMessage.errorMessage("Error occurred while getting item restock schedules from database");
         exception.printStackTrace();
      }

      return (Map<ScheduleManager.Item, Long>)value;
   }

   public int getItemStockMode(String itemId) {
      return this.itemStocks.get(itemId) != null ? this.itemStocks.get(itemId) : 0;
   }

   private void loadItemStockModes() {
      for (String itemId : ConfigManager.getShops()) {
         for (String itemId2 : this.plugin.getConfigManager().getItemsRaw(itemId)) {
            if (!ConfigManager.getShop(itemId)
               .contains((this.plugin.getConfigManager().getShopType(itemId) == ShopType.ROTATING ? "pages." : "") + itemId2 + ".action")) {
               String text = itemId + "." + itemId2;
               if (ConfigManager.getShop(itemId).contains(this.getItemLoc(text) + ".stock-limit-player")) {
                  this.itemStocks.put(text, 2);
               } else if (ConfigManager.getShop(itemId).contains(this.getItemLoc(text) + ".stock-limit-global")) {
                  this.itemStocks.put(text, 1);
               }
            }
         }
      }
   }

   private void loadItemStock() {
      this.refreshStockCache();
   }

   public void reloadDisplayCache() {
      this.refreshStockCache();
   }

   private void refreshStockCache() {
      Map<String, Integer> stocks;
      Map<String, Map<UUID, Integer>> map;
      try {
         stocks = this.database.getAllItemStocks();
         map = this.database.getAllPlayerStocks();
      } catch (SQLException exception) {
         SendMessage.errorMessage("Exception occurred while loading the item stock display cache");
         SendMessage.errorMessage(exception.getClass().getName() + ": " + exception.getMessage());
         return;
      }

      this.itemStocks.forEach((key, value) -> {
         if (value == 2) {
            Map<UUID, Integer> map2 = new ConcurrentHashMap<>();

            for (Entry entry : map.getOrDefault(key, Collections.emptyMap()).entrySet()) {
               map2.put((UUID)entry.getKey(), this.getPlayerStock((UUID)entry.getKey(), key, (Integer)entry.getValue()));
            }

            this.CASHED_PLAYER_STOCK.put(key, map2);
         } else if (value == 1) {
            Integer value2 = (Integer)stocks.get(key);
            if (value2 == null) {
               value2 = this.getGlobalStock(key);
               this.setItemStock(key, value2);
            }

            this.CASHED_GLOBAL_STOCK.put(key, this.getGlobalStock(key, value2));
         }
      });
   }

   private void reloadOriginals() {
      try {
         Map originalStocks = this.database.getOriginalStocks();
         this.itemStocks.forEach((key, value) -> {
            Integer value2 = (Integer)originalStocks.get(key);
            if (value2 != null && value2 != 0) {
               if (value == 2) {
                  int playerStock = this.getPlayerStock(key);
                  if (value2 != playerStock) {
                     SendMessage.logDebugMessage(Lang.PLAYER_ITEM_STOCK_CHANGE.get().replace("%path%", key));
                     this.database.setOriginalStock(key, playerStock);
                     this.database.setPlayerStockAmount(key, playerStock);
                     if (this.cache || this.plugin.displayCache) {
                        Map<UUID, Integer> map = new ConcurrentHashMap<>();
                        this.CASHED_PLAYER_STOCK.get(key).keySet().forEach(entry -> map.put(entry, playerStock));
                        this.CASHED_PLAYER_STOCK.put(key, map);
                     }
                  }
               } else if (value == 1) {
                  int globalStock = this.getGlobalStock(key);
                  if (value2 != globalStock) {
                     SendMessage.logDebugMessage(Lang.GLOBAL_ITEM_STOCK_CHANGE.get().replace("%path%", key));
                     this.database.setOriginalStock(key, globalStock);
                     this.database.setItemStock(key, globalStock);
                     this.CASHED_GLOBAL_STOCK.put(key, globalStock);
                  }
               }
            } else {
               this.database.setOriginalStock(key, value == 2 ? this.getPlayerStock(key) : this.getGlobalStock(key));
            }
         });
      } catch (Exception exception) {
         SendMessage.warnMessage("Failed to check for stock updates:");
         exception.printStackTrace();
      }
   }

   private void loadRestockTimes() {
      try {
         this.PLAYER_RESTOCK_TIMER = copyRestockTimes(this.database.getPlayerRestockTimes());
      } catch (SQLException exception) {
         SendMessage.errorMessage("Error occurred while loading the restock times for player stock");
         exception.printStackTrace();
      }

      try {
         this.GLOBAL_RESTOCK_TIMER = new ConcurrentHashMap<>(this.database.getItemRestockTime());
      } catch (SQLException exception) {
         SendMessage.errorMessage("Error occurred while loading the restock times for global stock");
         exception.printStackTrace();
      }
   }

   private static ConcurrentHashMap<String, Map<UUID, Long>> copyRestockTimes(Map<String, Map<UUID, Long>> restockTimes) {
      ConcurrentHashMap<String, Map<UUID, Long>> copy = new ConcurrentHashMap<>();
      restockTimes.forEach((itemId, timers) -> copy.put(itemId, timers == null ? new ConcurrentHashMap<>() : new ConcurrentHashMap<>(timers)));
      return copy;
   }

   public void stop() {
      if (this.cache) {
         this.sqlBatch.drain();
      } else {
         long startTime = System.currentTimeMillis();
         SendMessage.logDebugMessage("Saving item stock to database, please wait...");
         this.saveRestockTimes();
         this.saveSchedulers();
         SendMessage.logDebugMessage("Finished saving limit stock to database in " + (System.currentTimeMillis() - startTime) + "ms");
      }
   }

   private void saveSchedulers() {
      try {
         this.database.setItemStockSchedulers(this.restockSchedulers);
      } catch (SQLException exception) {
         SendMessage.errorMessage("Error occurred while saving the item restock schedulers");
         exception.printStackTrace();
      }
   }

   private void saveRestockTimes() {
      try {
         this.database.setPlayerRestockTimes(this.PLAYER_RESTOCK_TIMER);
      } catch (SQLException exception) {
         SendMessage.errorMessage("Error occurred while saving the player item restock times");
         exception.printStackTrace();
      }

      try {
         this.database.setItemRestockTimes(this.GLOBAL_RESTOCK_TIMER);
      } catch (SQLException exception) {
         SendMessage.errorMessage("Error occurred while saving the global item restock times");
         exception.printStackTrace();
      }
   }

   public Translatable getStockLore(ShopItem shopItem, Player player, int stock) {
      int id = Math.min(stock, this.getItemStock(shopItem, player.getUniqueId(), true));
      if (id >= 0) {
         return Lang.ITEM_STOCK.get(player).replace("%stock%", String.valueOf(id));
      } else if (id == -1) {
         return Lang.RESTOCKING_ITEM_STOCK.get(player).replace("%time%", this.getTimeLeftToRestock(shopItem.getItemPath()));
      } else if (id == -2) {
         return Lang.RESTOCKING_ITEM_STOCK.get(player).replace("%time%", this.getTimeLeftToRestock(player.getUniqueId(), shopItem.getItemPath()));
      } else {
         return id != -3 && id != -4 ? SendMessage.fromConfig("∞") : Lang.OUT_OF_STOCK.get(player);
      }
   }

   private void restockGlobalItem(String itemId) throws ParseException {
      long restockTime = TimeUtils.getTime(ConfigManager.getShop(itemId.split("\\.")[0]).getString(this.getItemLoc(itemId) + ".auto-restock-global"))
         + System.currentTimeMillis();
      this.GLOBAL_RESTOCK_TIMER.put(itemId, restockTime);
      if (this.cache) {
         this.sqlBatch.addQuery(new GlobalRestockTimerQuery(itemId, restockTime));
      }
   }

   private void restockPlayerItem(UUID playerId, String itemId) throws ParseException {
      long restockTime = TimeUtils.getTime(ConfigManager.getShop(itemId.split("\\.")[0]).getString(this.getItemLoc(itemId) + ".auto-restock-player"))
         + System.currentTimeMillis();
      this.PLAYER_RESTOCK_TIMER.computeIfAbsent(itemId, key -> new ConcurrentHashMap<>()).put(playerId, restockTime);

      if (this.cache) {
         this.sqlBatch.addQuery(new PlayerRestockTimerQuery(itemId, playerId.toString(), restockTime));
      }
   }

   private long getLongLeftToRestock(String text) {
      return this.restockSchedulers.getOrDefault(new ScheduleManager.Item(text, 1), this.GLOBAL_RESTOCK_TIMER.get(text)) - System.currentTimeMillis();
   }

   public String getTimeLeftToRestock(String text) {
      return TimeUtils.getReadableTime(this.getLongLeftToRestock(text));
   }

   private long getLongLeftToRestock(UUID uniqueId, String text) {
      return this.restockSchedulers.containsKey(new ScheduleManager.Item(text, 2))
         ? this.restockSchedulers.get(new ScheduleManager.Item(text, 2)) - System.currentTimeMillis()
         : this.PLAYER_RESTOCK_TIMER.get(text).get(uniqueId) - System.currentTimeMillis();
   }

   public String getTimeLeftToRestock(UUID uniqueId, String text) {
      return TimeUtils.getReadableTime(this.getLongLeftToRestock(uniqueId, text));
   }

   public long getTimeLeftToRestock(ShopItem shopItem, UUID uniqueId) {
      if (shopItem.getLimitedStockMode() == 1) {
         return this.getLongLeftToRestock(shopItem.getItemPath());
      } else {
         return shopItem.getLimitedStockMode() == 2 ? this.getLongLeftToRestock(uniqueId, shopItem.getItemPath()) : -1L;
      }
   }

   private int getGlobalStock(String itemId) {
      return ConfigManager.getShop(itemId.split("\\.")[0]).getInt(this.getItemLoc(itemId) + ".stock-limit-global", 0);
   }

   public int getPlayerStock(String itemId) {
      return ConfigManager.getShop(itemId.split("\\.")[0]).getInt(this.getItemLoc(itemId) + ".stock-limit-player", 0);
   }

   public void emptyCache() {
      this.PLAYER_RESTOCK_TIMER.clear();
      this.GLOBAL_RESTOCK_TIMER.clear();
   }

   private LimitedStockProvider.Snapshot loadPageStock(LoreFormatter.PageIndex pageIndex, UUID uniqueId) {
      ArrayList itemIds = new ArrayList();
      ArrayList values = new ArrayList();

      for (ShopItem shopItem : pageIndex.getItems()) {
         int limitedStockMode = shopItem.getLimitedStockMode();
         if (limitedStockMode == 1) {
            itemIds.add(shopItem.getItemPath());
         } else if (limitedStockMode == 2) {
            values.add(shopItem.getItemPath());
         }
      }

      if (!itemIds.isEmpty() || !values.isEmpty()) {
         try {
            HashMap map = new HashMap();
            if (!itemIds.isEmpty()) {
               map.putAll(this.database.getItemStocks(itemIds));
            }

            if (!values.isEmpty()) {
               map.putAll(this.database.getPlayerStockAmounts(values, uniqueId));
            }

            LimitedStockProvider.Snapshot snapshot = new LimitedStockProvider.Snapshot(pageIndex, map);
            this.PAGE_STOCK_SNAPSHOT.set(snapshot);
            return snapshot;
         } catch (SQLException exception) {
            SendMessage.errorMessage("Failed to batch load item stock for a shop page");
            exception.printStackTrace();
         }
      }

      return new LimitedStockProvider.Snapshot(pageIndex, Collections.EMPTY_MAP);
   }

   private void setItemStock(String itemId, int stock) {
      this.updateCachedGlobalStock(itemId, stock);
      this.persistItemStock(itemId, stock);
   }

   private void updateCachedGlobalStock(String itemId, int value) {
      if (this.cache || this.plugin.displayCache) {
         this.CASHED_GLOBAL_STOCK.put(itemId, value);
      }
   }

   private void updateCachedPlayerStock(UUID playerId, String itemId, int value) {
      if (this.cache || this.plugin.displayCache) {
         this.updateCachedPlayerStock(itemId, playerId, value);
      }
   }

   private void persistItemStock(String itemId, int stock) {
      if (this.cache) {
         this.sqlBatch.addQuery(new ItemStockQuery(itemId, stock));
      } else {
         this.database.setItemStock(itemId, stock);
      }
   }

   public int getItemStock(String itemId) {
      return this.getItemStock(itemId, false, null, null);
   }

   public int getItemStock(String itemId, boolean flag, LoreFormatter.PageIndex pageIndex, UUID uniqueId) {
      return this.getGlobalStock(itemId, this.getCashedGlobalStock(itemId, flag, pageIndex, uniqueId));
   }

   private int getGlobalStock(String itemId, int value) {
      if (value <= 0) {
         try {
            if (this.getLongLeftToRestock(itemId) <= 0L) {
               this.GLOBAL_RESTOCK_TIMER.remove(itemId);
               int globalStock = this.getGlobalStock(itemId);
               this.setItemStock(itemId, globalStock);
               SendMessage.logDebugMessage(Lang.RESTOCKED_GLOBAL_ITEM_STOCK.get().replace("%path%", itemId));
               return globalStock;
            } else {
               return -1;
            }
         } catch (NullPointerException exception) {
            try {
               this.restockGlobalItem(itemId);
               SendMessage.logDebugMessage(Lang.RESTOCKING_GLOBAL_ITEM_STOCK.get().replace("%path%", itemId));
               return -1;
            } catch (ParseException exception2) {
               SendMessage.errorMessage("Could not get the global restock time of item '" + itemId + "', you need to specify a format like '00:05:00'(HH:mm:ss)");
               return -3;
            } catch (NullPointerException exception2) {
               return -4;
            }
         }
      } else {
         return value;
      }
   }

   public void sellItems(ShopItem shopItem, UUID uniqueId, int value) {
      if (shopItem.getLimitedStockMode() == 2) {
         int value2;
         synchronized (this.STOCK_LOCK) {
            value2 = Math.max(this.getCashedPlayerStock(shopItem.getItemPath(), uniqueId, false), 0) + Math.max(0, value);
            int playerCap = this.getPlayerStock(shopItem.getItemPath());
            if (playerCap > 0 && value2 > playerCap) {
               value2 = playerCap;
            }

            this.updateCachedPlayerStock(uniqueId, shopItem.getItemPath(), value2);
            this.persistPlayerStock(uniqueId, shopItem.getItemPath(), value2);
         }
      } else if (shopItem.getLimitedStockMode() == 1) {
         int value2;
         synchronized (this.STOCK_LOCK) {
            value2 = Math.max(this.getCashedGlobalStock(shopItem.getItemPath(), false), 0) + Math.max(0, value);
            int globalCap = this.getGlobalStock(shopItem.getItemPath());
            if (globalCap > 0 && value2 > globalCap) {
               value2 = globalCap;
            }

            this.updateCachedGlobalStock(shopItem.getItemPath(), value2);
            this.persistItemStock(shopItem.getItemPath(), value2);
         }
      }
   }

   public void buyItems(ShopItem shopItem, UUID uniqueId, int value) {
      if (shopItem.getLimitedStockMode() == 2) {
         int value2;
         synchronized (this.STOCK_LOCK) {
            value2 = this.getCashedPlayerStock(shopItem.getItemPath(), uniqueId, false) - value;
            this.updateCachedPlayerStock(uniqueId, shopItem.getItemPath(), value2);
            this.persistPlayerStock(uniqueId, shopItem.getItemPath(), value2);
         }
      } else if (shopItem.getLimitedStockMode() == 1) {
         int value2;
         synchronized (this.STOCK_LOCK) {
            value2 = this.getCashedGlobalStock(shopItem.getItemPath(), false) - value;
            this.updateCachedGlobalStock(shopItem.getItemPath(), value2);
            this.persistItemStock(shopItem.getItemPath(), value2);
         }
      }
   }

   public int buyItemsSafe(ShopItem shopItem, UUID uniqueId, int value) {
      int value2 = -5;
      if (shopItem.getLimitedStockMode() == 2) {
         synchronized (this.STOCK_LOCK) {
            value2 = this.getCashedPlayerStock(shopItem.getItemPath(), uniqueId, false);
            if (value2 < value) {
               value = value2;
            }

            this.updateCachedPlayerStock(uniqueId, shopItem.getItemPath(), value2 - value);
            this.persistPlayerStock(uniqueId, shopItem.getItemPath(), value2 - value);
         }
      } else if (shopItem.getLimitedStockMode() == 1) {
         synchronized (this.STOCK_LOCK) {
            value2 = this.getCashedGlobalStock(shopItem.getItemPath(), false);
            if (value2 < value) {
               value = value2;
            }

            this.updateCachedGlobalStock(shopItem.getItemPath(), value2 - value);
            this.persistItemStock(shopItem.getItemPath(), value2 - value);
         }
      }

      return value2;
   }

   public void resetItemStock(String section, String itemLoc, UUID playerId, int mode) {
      if (section != null && itemLoc != null) {
         if (mode == 2) {
            this.setPlayerStock(playerId, section + "." + itemLoc, this.getPlayerStock(section + "." + itemLoc));
            if (this.PLAYER_RESTOCK_TIMER.containsKey(section + "." + itemLoc)) {
               if (playerId == null) {
                  this.PLAYER_RESTOCK_TIMER.remove(section + "." + itemLoc);
               } else {
                  this.PLAYER_RESTOCK_TIMER.get(section + "." + itemLoc).remove(playerId);
               }
            }
         } else if (mode == 1) {
            this.setItemStock(section + "." + itemLoc, this.getGlobalStock(section + "." + itemLoc));
            this.GLOBAL_RESTOCK_TIMER.remove(section + "." + itemLoc);
         }
      } else {
         if (section == null) {
            if (mode == 2) {
               if (playerId != null) {
                  RemovePlayerStockQuery removePlayerStockQuery = new RemovePlayerStockQuery(playerId.toString(), null);
                  if (this.cache) {
                     this.sqlBatch.addQuery(removePlayerStockQuery);
                  } else {
                     this.database.executeQuery(SQLBatch.Type.STOCK, removePlayerStockQuery);
                  }

                  if (this.cache || this.plugin.displayCache) {
                     for (String key : this.CASHED_PLAYER_STOCK.keySet()) {
                        this.CASHED_PLAYER_STOCK.get(key).remove(playerId);
                     }
                  }

                  for (String text : this.PLAYER_RESTOCK_TIMER.keySet()) {
                     this.PLAYER_RESTOCK_TIMER.get(text).remove(playerId);
                  }
               } else {
                  ClearPlayerStockQuery clearPlayerStockQuery = new ClearPlayerStockQuery();
                  if (this.cache) {
                     this.sqlBatch.addQuery(clearPlayerStockQuery);
                  } else {
                     this.database.executeQuery(SQLBatch.Type.STOCK, clearPlayerStockQuery);
                  }

                  if (this.cache || this.plugin.displayCache) {
                     this.CASHED_PLAYER_STOCK.clear();
                  }

                  this.PLAYER_RESTOCK_TIMER.clear();
               }
            } else if (mode == 1) {
               ClearStockQuery clearStockQuery = new ClearStockQuery();
               if (this.cache) {
                  this.sqlBatch.addQuery(clearStockQuery);
               } else {
                  this.database.executeQuery(SQLBatch.Type.STOCK, clearStockQuery);
               }

               if (this.cache || this.plugin.displayCache) {
                  this.CASHED_GLOBAL_STOCK.clear();
               }

               this.GLOBAL_RESTOCK_TIMER.clear();
            }
         } else if (itemLoc == null) {
            if (mode == 2) {
               if (playerId != null) {
                  RemovePlayerStockQuery removePlayerStockQuery = new RemovePlayerStockQuery(playerId.toString(), section);
                  if (this.cache) {
                     this.sqlBatch.addQuery(removePlayerStockQuery);
                  } else {
                     this.database.executeQuery(SQLBatch.Type.STOCK, removePlayerStockQuery);
                  }

                  if (this.cache || this.plugin.displayCache) {
                     for (String text : this.CASHED_PLAYER_STOCK.keySet()) {
                        if (text.split("\\.")[0].equals(section)) {
                           this.CASHED_PLAYER_STOCK.get(text).remove(playerId);
                        }
                     }
                  }

                  for (String text : this.PLAYER_RESTOCK_TIMER.keySet()) {
                     if (text.split("\\.")[0].equals(section)) {
                        this.PLAYER_RESTOCK_TIMER.get(text).remove(playerId);
                     }
                  }
               } else {
                  RemovePlayerStocksQuery removePlayerStocksQuery = new RemovePlayerStocksQuery(section);
                  if (this.cache) {
                     this.sqlBatch.addQuery(removePlayerStocksQuery);
                  } else {
                     this.database.executeQuery(SQLBatch.Type.STOCK, removePlayerStocksQuery);
                  }

                  if (this.cache || this.plugin.displayCache) {
                     for (String text : this.CASHED_PLAYER_STOCK.keySet()) {
                        if (text.split("\\.")[0].equals(section)) {
                           this.CASHED_PLAYER_STOCK.get(text).clear();
                        }
                     }
                  }

                  for (String text : this.PLAYER_RESTOCK_TIMER.keySet()) {
                     if (text.split("\\.")[0].equals(section)) {
                        this.PLAYER_RESTOCK_TIMER.get(text).clear();
                     }
                  }
               }
            } else if (mode == 1) {
               RemoveStockQuery removeStockQuery = new RemoveStockQuery(section);
               if (this.cache) {
                  this.sqlBatch.addQuery(removeStockQuery);
               } else {
                  this.database.executeQuery(SQLBatch.Type.STOCK, removeStockQuery);
               }

               if (this.cache || this.plugin.displayCache) {
                  for (String text : new ArrayList<>(this.CASHED_GLOBAL_STOCK.keySet())) {
                     if (text.split("\\.")[0].equals(section)) {
                        this.CASHED_GLOBAL_STOCK.remove(text);
                     }
                  }
               }

               for (String text : new ArrayList<>(this.GLOBAL_RESTOCK_TIMER.keySet())) {
                  if (text.split("\\.")[0].equals(section)) {
                     this.GLOBAL_RESTOCK_TIMER.remove(text);
                  }
               }
            }
         }

         if (this.cache) {
            this.sqlBatch.saveLast();
         }
      }
   }

   public void setItemStock(String itemId, UUID uniqueId, int stock, int value) {
      if (stock == 2) {
         this.setPlayerStock(uniqueId, itemId, value);
      } else if (stock == 1) {
         this.setItemStock(itemId, value);
      }

      if (value == 0) {
         this.restockItems(itemId, uniqueId, stock);
      }
   }

   public void restockItem(String itemId, int restockTime, long value) {
      ScheduleManager.Item item = new ScheduleManager.Item(itemId, restockTime);
      this.restockSchedulers.put(item, value);
      if (this.cache) {
         this.sqlBatch.addQuery(new RestockScheduleQuery(item, value));
      }

      if (restockTime == 2) {
         int playerStock = this.getPlayerStock(itemId);
         this.setPlayerStock(null, itemId, playerStock);
      } else if (restockTime == 1) {
         int globalStock = this.getGlobalStock(itemId);
         this.setItemStock(itemId, globalStock);
      }
   }

   private void restockItems(String text, UUID uniqueId, int restockTime) {
      try {
         if (restockTime == 2) {
            if (uniqueId == null) {
               for (UUID uniqueId2 : this.CASHED_PLAYER_STOCK.get(text).keySet()) {
                  this.restockPlayerItem(uniqueId2, text);
               }
            } else {
               this.restockPlayerItem(uniqueId, text);
            }
         } else if (restockTime == 1) {
            this.restockGlobalItem(text);
         }
      } catch (ParseException exception) {
         SendMessage.errorMessage("Could not get the restock time of item '" + text + "', you need to specify a format like '00:05:00'(HH:mm:ss)");
      } catch (NullPointerException exception) {
      }
   }

   private void updateCachedPlayerStock(String itemId, UUID playerId, int value) {
      if (playerId == null) {
         try {
            if (this.cache) {
               this.sqlBatch.addQuery(new UpdatePlayerStockQuery(itemId, value));
            }

            this.CASHED_PLAYER_STOCK.get(itemId).replaceAll((key, stock) -> value);
         } catch (NullPointerException exception) {
         }
      } else {
         if (this.cache) {
            this.sqlBatch.addQuery(new PlayerStockQuery(itemId, playerId.toString(), value));
         }

         try {
            this.CASHED_PLAYER_STOCK.get(itemId).put(playerId, value);
         } catch (NullPointerException exception) {
            Map<UUID, Integer> map = new ConcurrentHashMap<>();
            map.put(playerId, value);
            this.CASHED_PLAYER_STOCK.put(itemId, map);
         }
      }
   }

   private int getCashedPlayerStock(String itemId, UUID uniqueId, boolean flag) {
      return this.getCashedPlayerStock(itemId, uniqueId, flag, null);
   }

   private int getCashedPlayerStock(String itemId, UUID uniqueId, boolean flag, LoreFormatter.PageIndex pageIndex) {
      if (this.cache || flag && this.plugin.displayCache) {
         try {
            if (this.CASHED_PLAYER_STOCK.get(itemId).containsKey(uniqueId)) {
               return this.CASHED_PLAYER_STOCK.get(itemId).get(uniqueId);
            }
         } catch (NullPointerException exception) {
         }
      }

      if (flag && pageIndex != null) {
         LimitedStockProvider.Snapshot snapshot = this.PAGE_STOCK_SNAPSHOT.get();
         if (snapshot == null || snapshot.pageIndex != pageIndex) {
            snapshot = this.loadPageStock(pageIndex, uniqueId);
         }

         if (snapshot.snapshot.containsKey(itemId)) {
            return snapshot.snapshot.get(itemId);
         }
      }

      try {
         return this.database.getPlayerStockAmount(uniqueId, itemId);
      } catch (SQLException exception) {
         SendMessage.errorMessage("Failed to read stored player stock for " + itemId + ": " + exception.getMessage());
         Map<UUID, Integer> stocks = this.CASHED_PLAYER_STOCK.get(itemId);
         return stocks != null && stocks.containsKey(uniqueId) ? stocks.get(uniqueId) : 0;
      }
   }

   private int getCashedGlobalStock(String itemId, boolean flag) {
      return this.getCashedGlobalStock(itemId, flag, null, null);
   }

   private int getCashedGlobalStock(String itemId, boolean flag, LoreFormatter.PageIndex pageIndex, UUID uniqueId) {
      if (this.cache || flag && this.plugin.displayCache) {
         Integer value = this.CASHED_GLOBAL_STOCK.get(itemId);
         if (value != null) {
            return value;
         }
      }

      if (flag && pageIndex != null) {
         LimitedStockProvider.Snapshot snapshot = this.PAGE_STOCK_SNAPSHOT.get();
         if (snapshot == null || snapshot.pageIndex != pageIndex) {
            snapshot = this.loadPageStock(pageIndex, uniqueId);
         }

         if (snapshot.snapshot.containsKey(itemId)) {
            return snapshot.snapshot.get(itemId);
         }
      }

      try {
         return this.database.getItemStock(itemId);
      } catch (SQLException exception) {
         SendMessage.errorMessage("Failed to read stored item stock for " + itemId + ": " + exception.getMessage());
         Integer value = this.CASHED_GLOBAL_STOCK.get(itemId);
         return value != null ? value : 0;
      }
   }

   public int getItemStock(String itemId, UUID uniqueId) {
      int stock = this.itemStocks.get(itemId);
      if (stock == 2) {
         return this.getPlayerStock(uniqueId, itemId);
      } else {
         return stock == 1 ? this.getItemStock(itemId) : 123456789;
      }
   }

   public int getItemStock(ShopItem shopItem, UUID uniqueId, boolean flag) {
      return this.getItemStock(shopItem, uniqueId, flag, false, null);
   }

   public int getItemStock(ShopItem shopItem, UUID uniqueId, boolean flag, boolean flag2, LoreFormatter.PageIndex pageIndex) {
      if (shopItem.getLimitedStockMode() == 2) {
         return flag ? this.getPlayerStock(uniqueId, shopItem.getItemPath(), flag2, pageIndex) : this.getCashedPlayerStock(shopItem.getItemPath(), uniqueId, flag2);
      } else if (shopItem.getLimitedStockMode() == 1) {
         return flag ? this.getItemStock(shopItem.getItemPath(), flag2, pageIndex, uniqueId) : this.getCashedGlobalStock(shopItem.getItemPath(), flag2);
      } else {
         return 123456789;
      }
   }

   private void setPlayerStock(UUID playerId, String itemId, int value) {
      if (this.cache || this.plugin.displayCache) {
         this.updateCachedPlayerStock(itemId, playerId, value);
      }

      this.persistPlayerStock(playerId, itemId, value);
   }

   private void persistPlayerStock(UUID playerId, String itemId, int value) {
      if (this.database instanceof MySQL || this.database instanceof PostgreSQL) {
         if (playerId == null) {
            this.database.setPlayerStockAmount(itemId, value);
         } else {
            this.database.setPlayerStockAmount(playerId, itemId, value);
         }
      }
   }

   public int getPlayerStock(UUID playerId, String itemId) {
      return this.getPlayerStock(playerId, itemId, false, null);
   }

   public int getPlayerStock(UUID playerId, String itemId, boolean flag, LoreFormatter.PageIndex pageIndex) {
      return this.getPlayerStock(playerId, itemId, this.getCashedPlayerStock(itemId, playerId, flag, pageIndex));
   }

   private int getPlayerStock(UUID playerId, String itemId, int value) {
      if (value <= 0) {
         try {
            if (this.getLongLeftToRestock(playerId, itemId) <= 0L) {
               this.PLAYER_RESTOCK_TIMER.get(itemId).remove(playerId);
               int playerStock = this.getPlayerStock(itemId);
               this.setPlayerStock(playerId, itemId, playerStock);
               SendMessage.logDebugMessage(Lang.RESTOCKED_PLAYER_ITEM_STOCK.get().replace("%path%", itemId).replace("%playerUUID%", String.valueOf(playerId)));
               return playerStock;
            } else {
               return -2;
            }
         } catch (NullPointerException exception) {
            try {
               this.restockPlayerItem(playerId, itemId);
               SendMessage.logDebugMessage(Lang.RESTOCKING_PLAYER_ITEM_STOCK.get().replace("%path%", itemId).replace("%playerUUID%", String.valueOf(playerId)));
               return -2;
            } catch (ParseException exception2) {
               SendMessage.errorMessage("Could not get the player restock time of item '" + itemId + "', you need to specify a format like '00:05:00'(HH:mm:ss)");
               return -3;
            } catch (NullPointerException exception2) {
               return -4;
            }
         }
      } else {
         return value;
      }
   }

   private String getItemLoc(String itemId) {
      String[] values = itemId.split("\\.", 2);
      return (this.plugin.getShopType(values[0]) == ShopType.CATEGORY ? "pages." : "") + values[1];
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
