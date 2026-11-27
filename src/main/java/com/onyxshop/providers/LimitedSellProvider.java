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
import com.onyxshop.objects.jobs.RestockItemLimit;
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

public class LimitedSellProvider {
   private final Map<String, Integer> itemModes = new HashMap<>();
   private volatile ConcurrentHashMap<String, Integer> CASHED_GLOBAL_LIMITS = new ConcurrentHashMap<>();
   private volatile ConcurrentHashMap<String, Map<UUID, Integer>> CASHED_PLAYER_LIMITS = new ConcurrentHashMap<>();
   private volatile ConcurrentHashMap<String, Long> GLOBAL_RESTOCK_TIMER = new ConcurrentHashMap<>();
   private volatile ConcurrentHashMap<String, Map<UUID, Long>> PLAYER_RESTOCK_TIMER = new ConcurrentHashMap<>();
   private final ThreadLocal<LimitedSellProvider.Snapshot> PAGE_LIMIT_SNAPSHOT = new ThreadLocal<>();
   private volatile ConcurrentHashMap<ScheduleManager.Item, Long> restockSchedulers = new ConcurrentHashMap<>();
   private final Object LIMIT_LOCK = new Object();
   private final Database database;
   private final OnyxShop plugin;
   private SQLBatch sqlBatch;
   private final boolean cache;

   public LimitedSellProvider(OnyxShop plugin, Database database) {
      this.plugin = plugin;
      this.database = database;
      this.cache = database instanceof SQLite;
      SendMessage.infoMessage(Lang.LOADING_LIMITED_SELL.get());
      this.loadLimitedSellModes();
      this.loadRestockTimes();
      this.startRestockTimers();
      if (this.cache) {
         this.sqlBatch = new SQLBatch(this.plugin, this.database, SQLBatch.Type.SELL_LIMIT);
      }

      if (this.cache || this.plugin.displayCache) {
         this.loadItemLimits();
      }

      this.reloadOriginals();
      this.plugin.sellLimits = true;
   }

   public void reload() {
      SendMessage.infoMessage(Lang.RELOADING_LIMITED_SELL.get());
      if (!this.itemModes.isEmpty()) {
         this.itemModes.clear();
      }

      this.loadLimitedSellModes();
      this.reloadOriginals();
      this.reloadRestockTimers();
   }

   private void startRestockTimers() {
      Map cachedSchedulers = this.getCachedSchedulers();
      int restockTime = this.loadRestockTimers(cachedSchedulers);
      if (restockTime != 0) {
         SendMessage.infoMessage("Completed loading " + restockTime + " sell limit restock cron schedulers");
      }
   }

   private void reloadRestockTimers() {
      int restockTime = this.loadRestockTimers(this.restockSchedulers);
      if (restockTime != 0) {
         SendMessage.infoMessage("Successfully reloaded " + restockTime + " sell limit restock cron schedulers");
      }
   }

   private int loadRestockTimers(Map<ScheduleManager.Item, Long> restockTimes) {
      AtomicInteger counter = new AtomicInteger(0);
      this.itemModes
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
                              + " sell limit while cron schedule ended at "
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
                        + " sell limit with next schedule date at "
                        + date
                  );
                  counter.incrementAndGet();
               } catch (RuntimeException exception) {
                  SendMessage.warnMessage("Invalid cron expression format with reason: '" + exception.getCause().getMessage());
                  SendMessage.errorItemShops(key);
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
         text = ConfigManager.getShop(item.section).getString(this.getItemLoc(item.getItemPath()) + ".auto-restock-player-sell-frequency");
         if (text == null) {
            return null;
         }

         if (ConfigManager.getShop(item.section).getString(this.getItemLoc(item.getItemPath()) + ".auto-restock-player-sell") != null) {
            SendMessage.warnMessage("A item cannot have a restock timeframe aswell as a restock frequency, using frequency as default...");
            SendMessage.errorShops(item.section, item.path);
         }
      } else if (item.mode == 1) {
         text = ConfigManager.getShop(item.section).getString(this.getItemLoc(item.getItemPath()) + ".auto-restock-global-sell-frequency");
         if (text == null) {
            return null;
         }

         if (ConfigManager.getShop(item.section).getString(this.getItemLoc(item.getItemPath()) + ".auto-restock-global-sell") != null) {
            SendMessage.warnMessage("A item cannot have a restock timeframe aswell as a restock frequency, using frequency as default...");
            SendMessage.errorShops(item.section, item.path);
         }
      }

      JobDetail jobDetail = JobBuilder.newJob(RestockItemLimit.class)
         .setJobData(new JobDataMap(ImmutableMap.of("item", item.getItemPath(), "mode", item.mode)))
         .withIdentity(item.getItemPath() + "_JOB", "ONYX_RESTOCK_ITEM_LIMIT")
         .build();
      Trigger trigger = TriggerBuilder.newTrigger()
         .withIdentity(item.getItemPath() + "_TRIGGER", "ONYX_RESTOCK_ITEM_LIMIT")
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
         value = this.database.getSellLimitSchedulers();
      } catch (SQLException exception) {
         SendMessage.errorMessage("Error occurred while getting item sell limit schedules from database");
         exception.printStackTrace();
      }

      return (Map<ScheduleManager.Item, Long>)value;
   }

   public int getSellLimitMode(String text) {
      return this.itemModes.get(text) != null ? this.itemModes.get(text) : 0;
   }

   private void loadLimitedSellModes() {
      for (String text : ConfigManager.getShops()) {
         for (String text2 : this.plugin.getConfigManager().getItemsRaw(text)) {
            if (!ConfigManager.getShop(text)
               .contains((this.plugin.getConfigManager().getShopType(text) == ShopType.ROTATING ? "pages." : "") + text2 + ".action")) {
               String text3 = text + "." + text2;
               if (ConfigManager.getShop(text).contains(this.getItemLoc(text3) + ".sell-limit-player")) {
                  this.itemModes.put(text3, 2);
               } else if (ConfigManager.getShop(text).contains(this.getItemLoc(text3) + ".sell-limit-global")) {
                  this.itemModes.put(text3, 1);
               }
            }
         }
      }
   }

   private void loadItemLimits() {
      this.refreshLimitCache();
   }

   public void reloadDisplayCache() {
      this.refreshLimitCache();
   }

   private void refreshLimitCache() {
      Map<String, Integer> limits;
      Map<String, Map<UUID, Integer>> map;
      try {
         limits = this.database.getAllSellLimits();
         map = this.database.getAllPlayerSellLimits();
      } catch (SQLException exception) {
         SendMessage.errorMessage("Exception occurred while loading the sell limit display cache");
         SendMessage.errorMessage(exception.getClass().getName() + ": " + exception.getMessage());
         return;
      }

      this.itemModes.forEach((key, value) -> {
         if (value == 2) {
            Map<UUID, Integer> map2 = new ConcurrentHashMap<>();

            for (Entry entry : map.getOrDefault(key, Collections.emptyMap()).entrySet()) {
               map2.put((UUID)entry.getKey(), this.getPlayerLimit((UUID)entry.getKey(), key, (Integer)entry.getValue()));
            }

            this.CASHED_PLAYER_LIMITS.put(key, map2);
         } else if (value == 1) {
            Integer value2 = (Integer)limits.get(key);
            if (value2 == null) {
               value2 = this.getGlobalSellLimit(key);
               this.setItemLimit(key, value2);
            }

            this.CASHED_GLOBAL_LIMITS.put(key, this.getGlobalSellLimit(key, value2));
         }
      });
   }

   private void reloadOriginals() {
      try {
         Map originalLimits = this.database.getOriginalLimits();
         this.itemModes.forEach((key, value) -> {
            Integer value2 = (Integer)originalLimits.get(key);
            if (value2 != null && value2 != 0) {
               if (value == 2) {
                  int playerSellLimit = this.getPlayerSellLimit(key);
                  if (value2 != playerSellLimit) {
                     SendMessage.logDebugMessage(Lang.PLAYER_SELL_LIMIT_CHANGE.get().replace("%path%", key));
                     this.database.setOriginalLimit(key, playerSellLimit);
                     this.database.setPlayerSellLimit(key, playerSellLimit);
                     if (this.cache || this.plugin.displayCache) {
                        Map<UUID, Integer> map = new ConcurrentHashMap<>();
                        this.CASHED_PLAYER_LIMITS.get(key).keySet().forEach(entry -> map.put(entry, playerSellLimit));
                        this.CASHED_PLAYER_LIMITS.put(key, map);
                     }
                  }
               } else if (value == 1) {
                  int globalSellLimit = this.getGlobalSellLimit(key);
                  if (value2 != globalSellLimit) {
                     SendMessage.logDebugMessage(Lang.GLOBAL_SELL_LIMIT_CHANGE.get().replace("%path%", key));
                     this.database.setOriginalLimit(key, globalSellLimit);
                     this.database.setSellLimit(key, globalSellLimit);
                     this.CASHED_GLOBAL_LIMITS.put(key, globalSellLimit);
                  }
               }
            } else {
               this.database.setOriginalLimit(key, value == 2 ? this.getPlayerSellLimit(key) : this.getGlobalSellLimit(key));
            }
         });
      } catch (Exception exception) {
         SendMessage.warnMessage("Failed to check for sell limit updates:");
         exception.printStackTrace();
      }
   }

   private static ConcurrentHashMap<String, Map<UUID, Long>> copyRestockTimes(Map<String, Map<UUID, Long>> restockTimes) {
      ConcurrentHashMap<String, Map<UUID, Long>> copy = new ConcurrentHashMap<>();
      restockTimes.forEach((itemId, timers) -> copy.put(itemId, timers == null ? new ConcurrentHashMap<>() : new ConcurrentHashMap<>(timers)));
      return copy;
   }

   private void loadRestockTimes() {
      try {
         this.PLAYER_RESTOCK_TIMER = copyRestockTimes(this.database.getPlayerSellLimitRestockTimes());
      } catch (SQLException exception) {
         SendMessage.errorMessage("Error occurred while loading the restock times for player sell limits");
         exception.printStackTrace();
      }

      try {
         this.GLOBAL_RESTOCK_TIMER = new ConcurrentHashMap<>(this.database.getSellLimitRestockTimes());
      } catch (SQLException exception) {
         SendMessage.errorMessage("Error occurred while loading the restock times for global sell limits");
         exception.printStackTrace();
      }
   }

   public void stop() {
      if (this.cache) {
         this.sqlBatch.drain();
      } else {
         long startTime = System.currentTimeMillis();
         SendMessage.logDebugMessage("Saving item sell limits to database, please wait...");
         this.saveRestockTimes();
         this.saveSchedulers();
         SendMessage.logDebugMessage("Finished saving sell limits to database in " + (System.currentTimeMillis() - startTime) + "ms");
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
         this.database.setPlayerSellLimitRestockTimes(this.PLAYER_RESTOCK_TIMER);
      } catch (SQLException exception) {
         SendMessage.errorMessage("Error occurred while saving the player item sell restock times");
         exception.printStackTrace();
      }

      try {
         this.database.setSellLimitRestockTimes(this.GLOBAL_RESTOCK_TIMER);
      } catch (SQLException exception) {
         SendMessage.errorMessage("Error occurred while saving the global item sell restock times");
         exception.printStackTrace();
      }
   }

   private void restockGlobalItem(String itemId) throws ParseException {
      long restockTime = TimeUtils.getTime(ConfigManager.getShop(itemId.split("\\.")[0]).getString(this.getItemLoc(itemId) + ".auto-restock-global-sell"))
         + System.currentTimeMillis();
      this.GLOBAL_RESTOCK_TIMER.put(itemId, restockTime);
      if (this.cache) {
         this.sqlBatch.addQuery(new GlobalRestockTimerQuery(itemId, restockTime));
      }
   }

   private void restockPlayerItem(UUID playerId, String itemId) throws ParseException {
      long restockTime = TimeUtils.getTime(ConfigManager.getShop(itemId.split("\\.")[0]).getString(this.getItemLoc(itemId) + ".auto-restock-player-sell"))
         + System.currentTimeMillis();
      if (this.PLAYER_RESTOCK_TIMER.containsKey(itemId)) {
         this.PLAYER_RESTOCK_TIMER.get(itemId).put(playerId, restockTime);
      } else {
         HashMap map = new HashMap();
         map.put(playerId, restockTime);
         this.PLAYER_RESTOCK_TIMER.put(itemId, map);
      }

      if (this.cache) {
         this.sqlBatch.addQuery(new PlayerRestockTimerQuery(itemId, playerId.toString(), restockTime));
      }
   }

   public void emptyCache() {
      this.PLAYER_RESTOCK_TIMER.clear();
      this.GLOBAL_RESTOCK_TIMER.clear();
   }

   private LimitedSellProvider.Snapshot loadPageLimit(LoreFormatter.PageIndex pageIndex, UUID uniqueId) {
      ArrayList values = new ArrayList();
      ArrayList values2 = new ArrayList();

      for (ShopItem shopItem : pageIndex.getItems()) {
         int limitedSellMode = shopItem.getLimitedSellMode();
         if (limitedSellMode == 1) {
            values.add(shopItem.getItemPath());
         } else if (limitedSellMode == 2) {
            values2.add(shopItem.getItemPath());
         }
      }

      if (!values.isEmpty() || !values2.isEmpty()) {
         try {
            HashMap map = new HashMap();
            if (!values.isEmpty()) {
               map.putAll(this.database.getSellLimits(values));
            }

            if (!values2.isEmpty()) {
               map.putAll(this.database.getPlayerSellLimitAmounts(values2, uniqueId));
            }

            LimitedSellProvider.Snapshot snapshot = new LimitedSellProvider.Snapshot(pageIndex, map);
            this.PAGE_LIMIT_SNAPSHOT.set(snapshot);
            return snapshot;
         } catch (SQLException exception) {
            SendMessage.errorMessage("Failed to batch load sell limits for a shop page");
            exception.printStackTrace();
         }
      }

      return new LimitedSellProvider.Snapshot(pageIndex, Collections.EMPTY_MAP);
   }

   private long getLongLeftToRestock(String text) {
      return this.restockSchedulers.getOrDefault(new ScheduleManager.Item(text, 1), this.GLOBAL_RESTOCK_TIMER.get(text)) - System.currentTimeMillis();
   }

   public String getTimeLeftToRestock(String text) {
      return TimeUtils.getReadableTime(this.getLongLeftToRestock(text));
   }

   private long getLongLeftToRestock(UUID uniqueId, String text) {
      return this.PLAYER_RESTOCK_TIMER.containsKey(text)
         ? this.PLAYER_RESTOCK_TIMER.get(text).get(uniqueId) - System.currentTimeMillis()
         : this.restockSchedulers.get(new ScheduleManager.Item(text, 2)) - System.currentTimeMillis();
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

   private int getGlobalSellLimit(String text) {
      return ConfigManager.getShop(text.split("\\.")[0]).getInt(this.getItemLoc(text) + ".sell-limit-global", 0);
   }

   public int getPlayerSellLimit(String itemId) {
      return ConfigManager.getShop(itemId.split("\\.")[0]).getInt(this.getItemLoc(itemId) + ".sell-limit-player", 0);
   }

   private void setItemLimit(String itemId, int limit) {
      if (this.cache || this.plugin.displayCache) {
         this.CASHED_GLOBAL_LIMITS.put(itemId, limit);
      }

      if (this.cache) {
         this.sqlBatch.addQuery(new ItemStockQuery(itemId, limit));
      } else {
         this.database.setSellLimit(itemId, limit);
      }
   }

   public int getSellLimit(String text) {
      return this.getSellLimit(text, false, null, null);
   }

   public int getSellLimit(String text, boolean flag) {
      return this.getSellLimit(text, flag, null, null);
   }

   public int getSellLimit(String lore, boolean flag, LoreFormatter.PageIndex pageIndex, UUID uniqueId) {
      return this.getGlobalSellLimit(lore, this.getCashedGlobalLimit(lore, flag, pageIndex, uniqueId));
   }

   private int getGlobalSellLimit(String text, int limit) {
      if (limit <= 0) {
         try {
            if (this.getLongLeftToRestock(text) <= 0L) {
               this.GLOBAL_RESTOCK_TIMER.remove(text);
               int globalSellLimit = this.getGlobalSellLimit(text);
               this.setItemLimit(text, globalSellLimit);
               SendMessage.logDebugMessage(Lang.RESTOCKED_GLOBAL_SELL_LIMIT.get().replace("%path%", text));
               return globalSellLimit;
            } else {
               return -1;
            }
         } catch (NullPointerException exception) {
            try {
               this.restockGlobalItem(text);
               SendMessage.logDebugMessage(Lang.RESTOCKING_GLOBAL_SELL_LIMIT.get().replace("%path%", text));
               return -1;
            } catch (ParseException exception2) {
               SendMessage.errorMessage(
                  "Invalid time format for global sell restock time of item '" + text + "', you need to specify a format like '00:05:00'(HH:mm:ss)"
               );
               return -3;
            } catch (NullPointerException exception2) {
               return -4;
            }
         }
      } else {
         return limit;
      }
   }

   public void setLimit(ShopItem shopItem, UUID uniqueId, int limit) {
      if (shopItem.getLimitedSellMode() == 2) {
         this.setPlayerLimit(uniqueId, shopItem.getItemPath(), limit);
      } else if (shopItem.getLimitedSellMode() == 1) {
         this.setItemLimit(shopItem.getItemPath(), limit);
      }

      if (limit == 0) {
         this.restockItems(shopItem, uniqueId);
      }
   }

   public void restockItem(String itemId, int restockTime, long value) {
      ScheduleManager.Item item = new ScheduleManager.Item(itemId, restockTime);
      this.restockSchedulers.put(item, value);
      if (this.cache) {
         this.sqlBatch.addQuery(new RestockScheduleQuery(item, value));
      }

      if (restockTime == 2) {
         int playerSellLimit = this.getPlayerSellLimit(itemId);
         this.setPlayerLimit(null, itemId, playerSellLimit);
      } else if (restockTime == 1) {
         int globalSellLimit = this.getGlobalSellLimit(itemId);
         this.setItemLimit(itemId, globalSellLimit);
      }
   }

   private void restockItems(ShopItem shopItem, UUID uniqueId) {
      try {
         if (shopItem.getLimitedSellMode() == 2) {
            if (uniqueId == null) {
               for (UUID uniqueId2 : this.CASHED_PLAYER_LIMITS.get(shopItem.getItemPath()).keySet()) {
                  this.restockPlayerItem(uniqueId2, shopItem.getItemPath());
               }
            } else {
               this.restockPlayerItem(uniqueId, shopItem.getItemPath());
            }
         } else if (shopItem.getLimitedSellMode() == 1) {
            this.restockGlobalItem(shopItem.getItemPath());
         }
      } catch (ParseException exception) {
         SendMessage.errorMessage("Could not get the restock time of item '" + shopItem.getItemPath() + "', you need to specify a format like '00:05:00'(HH:mm:ss)");
      } catch (NullPointerException exception) {
      }
   }

   public Object getSellLock() {
      return this.LIMIT_LOCK;
   }

   public void sellItems(ShopItem shopItem, UUID uniqueId, int value, boolean flag) {
      synchronized (this.LIMIT_LOCK) {
         if (shopItem.getLimitedSellMode() == 2) {
            this.setPlayerLimit(uniqueId, shopItem.getItemPath(), Math.max(0, this.getCashedPlayerLimit(shopItem.getItemPath(), uniqueId, false) - value));
            if (flag) {
               this.getPlayerLimit(uniqueId, shopItem.getItemPath());
            }
         } else if (shopItem.getLimitedSellMode() == 1) {
            this.setItemLimit(shopItem.getItemPath(), Math.max(0, this.getCashedGlobalLimit(shopItem.getItemPath(), false) - value));
            if (flag) {
               this.getSellLimit(shopItem.getItemPath());
            }
         }
      }
   }

   public int sellItemsSafe(ShopItem shopItem, UUID uniqueId, int value, boolean flag) {
      int value2 = -5;
      synchronized (this.LIMIT_LOCK) {
         if (shopItem.getLimitedSellMode() == 2) {
            value2 = this.getCashedPlayerLimit(shopItem.getItemPath(), uniqueId, false);
            if (value2 < value) {
               value = value2;
            }

            this.setPlayerLimit(uniqueId, shopItem.getItemPath(), value2 - value);
            if (flag) {
               this.getPlayerLimit(uniqueId, shopItem.getItemPath());
            }
         } else if (shopItem.getLimitedSellMode() == 1) {
            value2 = this.getCashedGlobalLimit(shopItem.getItemPath(), false);
            if (value2 < value) {
               value = value2;
            }

            this.setItemLimit(shopItem.getItemPath(), value2 - value);
            if (flag) {
               this.getSellLimit(shopItem.getItemPath());
            }
         }
      }

      return value2;
   }

   public void resetItemLinit(String itemId, String text, UUID uniqueId, int value) {
      if (itemId != null && text != null) {
         if (value == 2) {
            this.setPlayerLimit(uniqueId, itemId + "." + text, this.getPlayerSellLimit(itemId + "." + text));
            if (this.PLAYER_RESTOCK_TIMER.containsKey(itemId + "." + text)) {
               if (uniqueId == null) {
                  this.PLAYER_RESTOCK_TIMER.remove(itemId + "." + text);
               } else {
                  this.PLAYER_RESTOCK_TIMER.get(itemId + "." + text).remove(uniqueId);
               }
            }
         } else if (value == 1) {
            this.setItemLimit(itemId + "." + text, this.getGlobalSellLimit(itemId + "." + text));
            this.GLOBAL_RESTOCK_TIMER.remove(itemId + "." + text);
         }
      } else {
         if (itemId == null) {
            if (value == 2) {
               if (uniqueId != null) {
                  RemovePlayerStockQuery removePlayerStockQuery = new RemovePlayerStockQuery(uniqueId.toString(), null);
                  if (this.cache) {
                     this.sqlBatch.addQuery(removePlayerStockQuery);
                  } else {
                     this.database.executeQuery(SQLBatch.Type.SELL_LIMIT, removePlayerStockQuery);
                  }

                  if (this.cache || this.plugin.displayCache) {
                     for (String key : this.CASHED_PLAYER_LIMITS.keySet()) {
                        this.CASHED_PLAYER_LIMITS.get(key).remove(uniqueId);
                     }
                  }

                  for (String text2 : this.PLAYER_RESTOCK_TIMER.keySet()) {
                     this.PLAYER_RESTOCK_TIMER.get(text2).remove(uniqueId);
                  }
               } else {
                  ClearPlayerStockQuery clearPlayerStockQuery = new ClearPlayerStockQuery();
                  if (this.cache) {
                     this.sqlBatch.addQuery(clearPlayerStockQuery);
                  } else {
                     this.database.executeQuery(SQLBatch.Type.SELL_LIMIT, clearPlayerStockQuery);
                  }

                  if (this.cache || this.plugin.displayCache) {
                     this.CASHED_PLAYER_LIMITS.clear();
                  }

                  this.PLAYER_RESTOCK_TIMER.clear();
               }
            } else if (value == 1) {
               ClearStockQuery clearStockQuery = new ClearStockQuery();
               if (this.cache) {
                  this.sqlBatch.addQuery(clearStockQuery);
               } else {
                  this.database.executeQuery(SQLBatch.Type.SELL_LIMIT, clearStockQuery);
               }

               if (this.cache || this.plugin.displayCache) {
                  this.CASHED_GLOBAL_LIMITS.clear();
               }

               this.GLOBAL_RESTOCK_TIMER.clear();
            }
         } else if (text == null) {
            if (value == 2) {
               if (uniqueId != null) {
                  RemovePlayerStockQuery removePlayerStockQuery = new RemovePlayerStockQuery(uniqueId.toString(), itemId);
                  if (this.cache) {
                     this.sqlBatch.addQuery(removePlayerStockQuery);
                  } else {
                     this.database.executeQuery(SQLBatch.Type.SELL_LIMIT, removePlayerStockQuery);
                  }

                  if (this.cache || this.plugin.displayCache) {
                     for (String text2 : this.CASHED_PLAYER_LIMITS.keySet()) {
                        if (text2.split("\\.")[0].equals(itemId)) {
                           this.CASHED_PLAYER_LIMITS.get(text2).remove(uniqueId);
                        }
                     }
                  }

                  for (String text2 : this.PLAYER_RESTOCK_TIMER.keySet()) {
                     if (text2.split("\\.")[0].equals(itemId)) {
                        this.PLAYER_RESTOCK_TIMER.get(text2).remove(uniqueId);
                     }
                  }
               } else {
                  RemovePlayerStocksQuery removePlayerStocksQuery = new RemovePlayerStocksQuery(itemId);
                  if (this.cache) {
                     this.sqlBatch.addQuery(removePlayerStocksQuery);
                  } else {
                     this.database.executeQuery(SQLBatch.Type.SELL_LIMIT, removePlayerStocksQuery);
                  }

                  if (this.cache || this.plugin.displayCache) {
                     for (String text2 : this.CASHED_PLAYER_LIMITS.keySet()) {
                        if (text2.split("\\.")[0].equals(itemId)) {
                           this.CASHED_PLAYER_LIMITS.get(text2).clear();
                        }
                     }
                  }

                  for (String text2 : this.PLAYER_RESTOCK_TIMER.keySet()) {
                     if (text2.split("\\.")[0].equals(itemId)) {
                        this.PLAYER_RESTOCK_TIMER.get(text2).clear();
                     }
                  }
               }
            } else if (value == 1) {
               RemoveStockQuery removeStockQuery = new RemoveStockQuery(itemId);
               if (this.cache) {
                  this.sqlBatch.addQuery(removeStockQuery);
               } else {
                  this.database.executeQuery(SQLBatch.Type.SELL_LIMIT, removeStockQuery);
               }

               if (this.cache || this.plugin.displayCache) {
                  for (String text2 : new ArrayList<>(this.CASHED_GLOBAL_LIMITS.keySet())) {
                     if (text2.split("\\.")[0].equals(itemId)) {
                        this.CASHED_GLOBAL_LIMITS.remove(text2);
                     }
                  }
               }

               for (String text2 : new ArrayList<>(this.GLOBAL_RESTOCK_TIMER.keySet())) {
                  if (text2.split("\\.")[0].equals(itemId)) {
                     this.GLOBAL_RESTOCK_TIMER.remove(text2);
                  }
               }
            }
         }

         if (this.cache) {
            this.sqlBatch.saveLast();
         }
      }
   }

   private void updateCachedPlayerLimit(String itemId, UUID uniqueId, int limit) {
      if (uniqueId == null) {
         if (this.cache) {
            this.sqlBatch.addQuery(new UpdatePlayerStockQuery(itemId, limit));
         }

         try {
            this.CASHED_PLAYER_LIMITS.get(itemId).replaceAll((key, value) -> limit);
         } catch (NullPointerException exception) {
         }
      } else {
         if (this.cache) {
            this.sqlBatch.addQuery(new PlayerStockQuery(itemId, uniqueId.toString(), limit));
         }

         try {
            this.CASHED_PLAYER_LIMITS.get(itemId).put(uniqueId, limit);
         } catch (NullPointerException exception) {
            HashMap map = new HashMap();
            map.put(uniqueId, limit);
            this.CASHED_PLAYER_LIMITS.put(itemId, map);
         }
      }
   }

   private int getCashedPlayerLimit(String itemId, UUID uniqueId, boolean flag) {
      return this.getCashedPlayerLimit(itemId, uniqueId, flag, null);
   }

   private int getCashedPlayerLimit(String itemId, UUID uniqueId, boolean flag, LoreFormatter.PageIndex pageIndex) {
      if (this.cache || flag && this.plugin.displayCache) {
         try {
            if (this.CASHED_PLAYER_LIMITS.get(itemId).containsKey(uniqueId)) {
               return this.CASHED_PLAYER_LIMITS.get(itemId).get(uniqueId);
            }
         } catch (NullPointerException exception) {
         }
      }

      if (flag && pageIndex != null) {
         LimitedSellProvider.Snapshot snapshot = this.PAGE_LIMIT_SNAPSHOT.get();
         if (snapshot == null || snapshot.pageIndex != pageIndex) {
            snapshot = this.loadPageLimit(pageIndex, uniqueId);
         }

         if (snapshot.snapshot.containsKey(itemId)) {
            return snapshot.snapshot.get(itemId);
         }
      }

      try {
         return this.database.getPlayerSellLimit(uniqueId, itemId);
      } catch (SQLException exception) {
         SendMessage.errorMessage("Failed to read stored player sell limit for " + itemId + ": " + exception.getMessage());
         Map<UUID, Integer> limits = this.CASHED_PLAYER_LIMITS.get(itemId);
         return limits != null && limits.containsKey(uniqueId) ? limits.get(uniqueId) : 0;
      }
   }

   private int getCashedGlobalLimit(String text, boolean flag) {
      return this.getCashedGlobalLimit(text, flag, null, null);
   }

   private int getCashedGlobalLimit(String lore, boolean flag, LoreFormatter.PageIndex pageIndex, UUID uniqueId) {
      if (this.cache || flag && this.plugin.displayCache) {
         Integer limit = this.CASHED_GLOBAL_LIMITS.get(lore);
         if (limit != null) {
            return limit;
         }
      }

      if (flag && pageIndex != null) {
         LimitedSellProvider.Snapshot snapshot = this.PAGE_LIMIT_SNAPSHOT.get();
         if (snapshot == null || snapshot.pageIndex != pageIndex) {
            snapshot = this.loadPageLimit(pageIndex, uniqueId);
         }

         if (snapshot.snapshot.containsKey(lore)) {
            return snapshot.snapshot.get(lore);
         }
      }

      try {
         return this.database.getSellLimit(lore);
      } catch (SQLException exception) {
         SendMessage.errorMessage("Failed to read stored global sell limit for " + lore + ": " + exception.getMessage());
         Integer value = this.CASHED_GLOBAL_LIMITS.get(lore);
         return value != null ? value : 0;
      }
   }

   public Translatable getStockLore(ShopItem shopItem, Player player, int stock) {
      int limit = Math.min(stock, this.getItemLimit(shopItem, player.getUniqueId(), true));
      if (limit >= 0) {
         return Lang.CURRENT_SELL_LIMIT.get(player).replace("%limit%", String.valueOf(limit));
      } else if (limit == -1) {
         return Lang.RESTOCKING_SELL_LIMIT.get(player).replace("%time%", this.getTimeLeftToRestock(shopItem.getItemPath()));
      } else if (limit == -2) {
         return Lang.RESTOCKING_SELL_LIMIT.get(player).replace("%time%", this.getTimeLeftToRestock(player.getUniqueId(), shopItem.getItemPath()));
      } else {
         return limit != -3 && limit != -4 ? SendMessage.fromConfig("∞") : Lang.REACHED_SELL_LIMIT.get(player);
      }
   }

   public int getItemLimit(ShopItem shopItem, UUID uniqueId, boolean flag) {
      return this.getItemLimit(shopItem, uniqueId, flag, false, null);
   }

   public int getItemLimit(ShopItem shopItem, UUID uniqueId, boolean flag, boolean flag2, LoreFormatter.PageIndex pageIndex) {
      if (shopItem.getLimitedSellMode() == 2) {
         return flag ? this.getPlayerLimit(uniqueId, shopItem.getItemPath(), flag2, pageIndex) : this.getCashedPlayerLimit(shopItem.getItemPath(), uniqueId, flag2);
      } else if (shopItem.getLimitedSellMode() == 1) {
         return flag ? this.getSellLimit(shopItem.getItemPath(), flag2, pageIndex, uniqueId) : this.getCashedGlobalLimit(shopItem.getItemPath(), flag2);
      } else {
         return 123456789;
      }
   }

   private void setPlayerLimit(UUID playerId, String text, int limit) {
      if (this.cache || this.plugin.displayCache) {
         this.updateCachedPlayerLimit(text, playerId, limit);
      }

      if (this.database instanceof MySQL || this.database instanceof PostgreSQL) {
         if (playerId == null) {
            this.database.setPlayerSellLimit(text, limit);
         } else {
            this.database.setPlayerSellLimit(playerId, text, limit);
         }
      }
   }

   public int getPlayerLimit(UUID playerId, String text) {
      return this.getPlayerLimit(playerId, text, false, null);
   }

   public int getPlayerLimit(UUID playerId, String lore, boolean flag, LoreFormatter.PageIndex pageIndex) {
      return this.getPlayerLimit(playerId, lore, this.getCashedPlayerLimit(lore, playerId, flag, pageIndex));
   }

   private int getPlayerLimit(UUID playerId, String text, int limit) {
      if (limit <= 0) {
         try {
            if (this.getLongLeftToRestock(playerId, text) <= 0L) {
               this.PLAYER_RESTOCK_TIMER.get(text).remove(playerId);
               int playerSellLimit = this.getPlayerSellLimit(text);
               this.setPlayerLimit(playerId, text, playerSellLimit);
               SendMessage.logDebugMessage(Lang.RESTOCKED_PLAYER_SELL_LIMIT.get().replace("%path%", text).replace("%playerUUID%", String.valueOf(playerId)));
               return playerSellLimit;
            } else {
               return -2;
            }
         } catch (NullPointerException exception) {
            try {
               this.restockPlayerItem(playerId, text);
               SendMessage.logDebugMessage(Lang.RESTOCKING_PLAYER_SELL_LIMIT.get().replace("%path%", text).replace("%playerUUID%", String.valueOf(playerId)));
               return -2;
            } catch (ParseException exception2) {
               SendMessage.errorMessage(
                  "Invalid time format for player sell restock time of item '" + text + "', you need to specify a format like '00:05:00'(HH:mm:ss)"
               );
               return -3;
            } catch (NullPointerException exception2) {
               return -4;
            }
         }
      } else {
         return limit;
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
