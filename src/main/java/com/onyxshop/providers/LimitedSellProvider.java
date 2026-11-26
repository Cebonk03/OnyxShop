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
   private Map<String, Integer> CASHED_GLOBAL_LIMITS = new HashMap<>();
   private Map<String, Map<UUID, Integer>> CASHED_PLAYER_LIMITS = new HashMap<>();
   private Map<String, Long> GLOBAL_RESTOCK_TIMER = new HashMap<>();
   private Map<String, Map<UUID, Long>> PLAYER_RESTOCK_TIMER = new HashMap<>();
   private final ThreadLocal<LimitedSellProvider.Snapshot> PAGE_LIMIT_SNAPSHOT = new ThreadLocal<>();
   private Map<ScheduleManager.Item, Long> restockSchedulers = new HashMap<>();
   private final Database database;
   private final OnyxShop plugin;
   private SQLBatch sqlBatch;
   private final boolean cache;

   public LimitedSellProvider(OnyxShop var1, Database var2) {
      this.plugin = var1;
      this.database = var2;
      this.cache = var2 instanceof SQLite;
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
      Map var1 = this.getCachedSchedulers();
      int var2 = this.loadRestockTimers(var1);
      if (var2 != 0) {
         SendMessage.infoMessage("Completed loading " + var2 + " sell limit restock cron schedulers");
      }
   }

   private void reloadRestockTimers() {
      int var1 = this.loadRestockTimers(this.restockSchedulers);
      if (var1 != 0) {
         SendMessage.infoMessage("Successfully reloaded " + var1 + " sell limit restock cron schedulers");
      }
   }

   private int loadRestockTimers(Map<ScheduleManager.Item, Long> var1) {
      AtomicInteger var2 = new AtomicInteger(0);
      this.itemModes
         .forEach(
            (var3, var4) -> {
               ScheduleManager.Item var5 = new ScheduleManager.Item(var3, var4);

               try {
                  Date var6 = this.scheduleJob(var5);
                  if (var6 == null) {
                     return;
                  }

                  if (var1.containsKey(var5)) {
                     if ((Long)var1.get(var5) <= 0L && !this.restockSchedulers.containsKey(var5)) {
                        this.restockItem(var3, var4, var6.getTime());
                        SendMessage.infoMessage(
                           "Successfully restocked item '"
                              + var3
                              + "' for "
                              + (var4 == 1 ? "global" : "player")
                              + " sell limit while cron schedule ended at "
                              + TimeUtils.getReadableTime((Long)var1.get(var5))
                              + " when server was offline"
                        );
                     }

                     if ((Long)var1.get(var5) != var6.getTime()) {
                        SendMessage.infoMessage("Detected cron schedule change for item " + var3 + ", updating schedule with next fire time at " + var6);
                        var1.put(var5, var6.getTime());
                     }
                  } else {
                     var1.put(var5, var6.getTime());
                  }

                  SendMessage.logDebugMessage(
                     "Successfully loaded cron restock schedule for item '"
                        + var3
                        + "' for "
                        + (var4 == 1 ? "global" : "player")
                        + " sell limit with next schedule date at "
                        + var6
                  );
                  var2.incrementAndGet();
               } catch (RuntimeException var7) {
                  SendMessage.warnMessage("Invalid cron expression format with reason: '" + var7.getCause().getMessage());
                  SendMessage.errorItemShops(var3);
               } catch (SchedulerException var8) {
                  SendMessage.warnMessage("A unknown error occurred while creating a new cron schedule");
                  SendMessage.errorShops(var5.section, var5.path);
                  var8.printStackTrace();
               } catch (Exception var9) {
                  var9.printStackTrace();
               }
            }
         );
      this.restockSchedulers = var1;
      return var2.get();
   }

   private Date scheduleJob(ScheduleManager.Item var1) throws SchedulerException {
      String var2 = null;
      if (var1.mode == 2) {
         var2 = ConfigManager.getShop(var1.section).getString(this.getItemLoc(var1.getItemPath()) + ".auto-restock-player-sell-frequency");
         if (var2 == null) {
            return null;
         }

         if (ConfigManager.getShop(var1.section).getString(this.getItemLoc(var1.getItemPath()) + ".auto-restock-player-sell") != null) {
            SendMessage.warnMessage("A item cannot have a restock timeframe aswell as a restock frequency, using frequency as default...");
            SendMessage.errorShops(var1.section, var1.path);
         }
      } else if (var1.mode == 1) {
         var2 = ConfigManager.getShop(var1.section).getString(this.getItemLoc(var1.getItemPath()) + ".auto-restock-global-sell-frequency");
         if (var2 == null) {
            return null;
         }

         if (ConfigManager.getShop(var1.section).getString(this.getItemLoc(var1.getItemPath()) + ".auto-restock-global-sell") != null) {
            SendMessage.warnMessage("A item cannot have a restock timeframe aswell as a restock frequency, using frequency as default...");
            SendMessage.errorShops(var1.section, var1.path);
         }
      }

      JobDetail var3 = JobBuilder.newJob(RestockItemLimit.class)
         .setJobData(new JobDataMap(ImmutableMap.of("item", var1.getItemPath(), "mode", var1.mode)))
         .withIdentity(var1.getItemPath() + "_JOB", "ONYX_RESTOCK_ITEM_LIMIT")
         .build();
      Trigger var4 = TriggerBuilder.newTrigger()
         .withIdentity(var1.getItemPath() + "_TRIGGER", "ONYX_RESTOCK_ITEM_LIMIT")
         .withSchedule(CronScheduleBuilder.cronSchedule(var2))
         .build();
      if (!this.restockSchedulers.containsKey(var1)) {
         this.plugin.getScheduleManager().schedule(var3, var4);
         if (var4.getNextFireTime() == null) {
            SendMessage.warnMessage("Failed to schedule cron expression as it will never fire again, for item " + var1 + " and schedule " + var2);
            SendMessage.errorShops(var1.section, var1.path);
            return null;
         }
      }

      return var4.getNextFireTime();
   }

   private Map<ScheduleManager.Item, Long> getCachedSchedulers() {
      Object var1 = new HashMap();

      try {
         var1 = this.database.getSellLimitSchedulers();
      } catch (SQLException var3) {
         SendMessage.errorMessage("Error occurred while getting item sell limit schedules from database");
         var3.printStackTrace();
      }

      return (Map<ScheduleManager.Item, Long>)var1;
   }

   public int getSellLimitMode(String var1) {
      return this.itemModes.get(var1) != null ? this.itemModes.get(var1) : 0;
   }

   private void loadLimitedSellModes() {
      for (String var2 : ConfigManager.getShops()) {
         for (String var4 : this.plugin.getConfigManager().getItemsRaw(var2)) {
            if (!ConfigManager.getShop(var2)
               .contains((this.plugin.getConfigManager().getShopType(var2) == ShopType.ROTATING ? "pages." : "") + var4 + ".action")) {
               String var5 = var2 + "." + var4;
               if (ConfigManager.getShop(var2).contains(this.getItemLoc(var5) + ".sell-limit-player")) {
                  this.itemModes.put(var5, 2);
               } else if (ConfigManager.getShop(var2).contains(this.getItemLoc(var5) + ".sell-limit-global")) {
                  this.itemModes.put(var5, 1);
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
      Map<String, Integer> var1;
      Map<String, Map<UUID, Integer>> var2;
      try {
         var1 = this.database.getAllSellLimits();
         var2 = this.database.getAllPlayerSellLimits();
      } catch (SQLException var4) {
         SendMessage.errorMessage("Exception occurred while loading the sell limit display cache");
         SendMessage.errorMessage(var4.getClass().getName() + ": " + var4.getMessage());
         return;
      }

      this.itemModes.forEach((var3, var4x) -> {
         if (var4x == 2) {
            HashMap var5 = new HashMap();

            for (Entry var7 : var2.getOrDefault(var3, Collections.emptyMap()).entrySet()) {
               var5.put((UUID)var7.getKey(), this.getPlayerLimit((UUID)var7.getKey(), var3, (Integer)var7.getValue()));
            }

            this.CASHED_PLAYER_LIMITS.put(var3, var5);
         } else if (var4x == 1) {
            Integer var8 = (Integer)var1.get(var3);
            if (var8 == null) {
               var8 = this.getGlobalSellLimit(var3);
               this.setItemLimit(var3, var8);
            }

            this.CASHED_GLOBAL_LIMITS.put(var3, this.getGlobalSellLimit(var3, var8));
         }
      });
   }

   private void reloadOriginals() {
      try {
         Map var1 = this.database.getOriginalLimits();
         this.itemModes.forEach((var2x, var3) -> {
            Integer var4 = (Integer)var1.get(var2x);
            if (var4 != null && var4 != 0) {
               if (var3 == 2) {
                  int var5 = this.getPlayerSellLimit(var2x);
                  if (var4 != var5) {
                     SendMessage.logDebugMessage(Lang.PLAYER_SELL_LIMIT_CHANGE.get().replace("%path%", var2x));
                     this.database.setOriginalLimit(var2x, var5);
                     this.database.setPlayerSellLimit(var2x, var5);
                     if (this.cache || this.plugin.displayCache) {
                        HashMap var6 = new HashMap();
                        this.CASHED_PLAYER_LIMITS.get(var2x).keySet().forEach(var2xx -> var6.put(var2xx, var5));
                        this.CASHED_PLAYER_LIMITS.put(var2x, var6);
                     }
                  }
               } else if (var3 == 1) {
                  int var7 = this.getGlobalSellLimit(var2x);
                  if (var4 != var7) {
                     SendMessage.logDebugMessage(Lang.GLOBAL_SELL_LIMIT_CHANGE.get().replace("%path%", var2x));
                     this.database.setOriginalLimit(var2x, var7);
                     this.database.setSellLimit(var2x, var7);
                     this.CASHED_GLOBAL_LIMITS.put(var2x, var7);
                  }
               }
            } else {
               this.database.setOriginalLimit(var2x, var3 == 2 ? this.getPlayerSellLimit(var2x) : this.getGlobalSellLimit(var2x));
            }
         });
      } catch (Exception var2) {
         SendMessage.warnMessage("Failed to check for sell limit updates:");
         var2.printStackTrace();
      }
   }

   private void loadRestockTimes() {
      try {
         this.PLAYER_RESTOCK_TIMER = this.database.getPlayerSellLimitRestockTimes();
      } catch (SQLException var3) {
         SendMessage.errorMessage("Error occurred while loading the restock times for player sell limits");
         var3.printStackTrace();
      }

      try {
         this.GLOBAL_RESTOCK_TIMER = this.database.getSellLimitRestockTimes();
      } catch (SQLException var2) {
         SendMessage.errorMessage("Error occurred while loading the restock times for global sell limits");
         var2.printStackTrace();
      }
   }

   public void stop() {
      if (this.cache) {
         this.sqlBatch.drain();
      } else {
         long var1 = System.currentTimeMillis();
         SendMessage.logDebugMessage("Saving item sell limits to database, please wait...");
         this.saveRestockTimes();
         this.saveSchedulers();
         SendMessage.logDebugMessage("Finished saving sell limits to database in " + (System.currentTimeMillis() - var1) + "ms");
      }
   }

   private void saveSchedulers() {
      try {
         this.database.setItemStockSchedulers(this.restockSchedulers);
      } catch (SQLException var2) {
         SendMessage.errorMessage("Error occurred while saving the item restock schedulers");
         var2.printStackTrace();
      }
   }

   private void saveRestockTimes() {
      try {
         this.database.setPlayerSellLimitRestockTimes(this.PLAYER_RESTOCK_TIMER);
      } catch (SQLException var3) {
         SendMessage.errorMessage("Error occurred while saving the player item sell restock times");
         var3.printStackTrace();
      }

      try {
         this.database.setSellLimitRestockTimes(this.GLOBAL_RESTOCK_TIMER);
      } catch (SQLException var2) {
         SendMessage.errorMessage("Error occurred while saving the global item sell restock times");
         var2.printStackTrace();
      }
   }

   private void restockGlobalItem(String var1) throws ParseException {
      long var2 = TimeUtils.getTime(ConfigManager.getShop(var1.split("\\.")[0]).getString(this.getItemLoc(var1) + ".auto-restock-global-sell"))
         + System.currentTimeMillis();
      this.GLOBAL_RESTOCK_TIMER.put(var1, var2);
      if (this.cache) {
         this.sqlBatch.addQuery(new GlobalRestockTimerQuery(var1, var2));
      }
   }

   private void restockPlayerItem(UUID var1, String var2) throws ParseException {
      long var3 = TimeUtils.getTime(ConfigManager.getShop(var2.split("\\.")[0]).getString(this.getItemLoc(var2) + ".auto-restock-player-sell"))
         + System.currentTimeMillis();
      if (this.PLAYER_RESTOCK_TIMER.containsKey(var2)) {
         this.PLAYER_RESTOCK_TIMER.get(var2).put(var1, var3);
      } else {
         HashMap var5 = new HashMap();
         var5.put(var1, var3);
         this.PLAYER_RESTOCK_TIMER.put(var2, var5);
      }

      if (this.cache) {
         this.sqlBatch.addQuery(new PlayerRestockTimerQuery(var2, var1.toString(), var3));
      }
   }

   public void emptyCache() {
      this.PLAYER_RESTOCK_TIMER.clear();
      this.GLOBAL_RESTOCK_TIMER.clear();
   }

   private LimitedSellProvider.Snapshot loadPageLimit(LoreFormatter.PageIndex var1, UUID var2) {
      ArrayList var3 = new ArrayList();
      ArrayList var4 = new ArrayList();

      for (ShopItem var6 : var1.getItems()) {
         int var7 = var6.getLimitedSellMode();
         if (var7 == 1) {
            var3.add(var6.getItemPath());
         } else if (var7 == 2) {
            var4.add(var6.getItemPath());
         }
      }

      if (!var3.isEmpty() || !var4.isEmpty()) {
         try {
            HashMap var9 = new HashMap();
            if (!var3.isEmpty()) {
               var9.putAll(this.database.getSellLimits(var3));
            }

            if (!var4.isEmpty()) {
               var9.putAll(this.database.getPlayerSellLimitAmounts(var4, var2));
            }

            LimitedSellProvider.Snapshot var10 = new LimitedSellProvider.Snapshot(var1, var9);
            this.PAGE_LIMIT_SNAPSHOT.set(var10);
            return var10;
         } catch (SQLException var8) {
            SendMessage.errorMessage("Failed to batch load sell limits for a shop page");
            var8.printStackTrace();
         }
      }

      return new LimitedSellProvider.Snapshot(var1, Collections.EMPTY_MAP);
   }

   private long getLongLeftToRestock(String var1) {
      return this.restockSchedulers.getOrDefault(new ScheduleManager.Item(var1, 1), this.GLOBAL_RESTOCK_TIMER.get(var1)) - System.currentTimeMillis();
   }

   public String getTimeLeftToRestock(String var1) {
      return TimeUtils.getReadableTime(this.getLongLeftToRestock(var1));
   }

   private long getLongLeftToRestock(UUID var1, String var2) {
      return this.PLAYER_RESTOCK_TIMER.containsKey(var2)
         ? this.PLAYER_RESTOCK_TIMER.get(var2).get(var1) - System.currentTimeMillis()
         : this.restockSchedulers.get(new ScheduleManager.Item(var2, 2)) - System.currentTimeMillis();
   }

   public String getTimeLeftToRestock(UUID var1, String var2) {
      return TimeUtils.getReadableTime(this.getLongLeftToRestock(var1, var2));
   }

   public long getTimeLeftToRestock(ShopItem var1, UUID var2) {
      if (var1.getLimitedStockMode() == 1) {
         return this.getLongLeftToRestock(var1.getItemPath());
      } else {
         return var1.getLimitedStockMode() == 2 ? this.getLongLeftToRestock(var2, var1.getItemPath()) : -1L;
      }
   }

   private int getGlobalSellLimit(String var1) {
      return ConfigManager.getShop(var1.split("\\.")[0]).getInt(this.getItemLoc(var1) + ".sell-limit-global", 0);
   }

   public int getPlayerSellLimit(String var1) {
      return ConfigManager.getShop(var1.split("\\.")[0]).getInt(this.getItemLoc(var1) + ".sell-limit-player", 0);
   }

   private void setItemLimit(String var1, int var2) {
      if (this.cache || this.plugin.displayCache) {
         this.CASHED_GLOBAL_LIMITS.put(var1, var2);
      }

      if (this.cache) {
         this.sqlBatch.addQuery(new ItemStockQuery(var1, var2));
      } else {
         this.database.setSellLimit(var1, var2);
      }
   }

   public int getSellLimit(String var1) {
      return this.getSellLimit(var1, false, null, null);
   }

   public int getSellLimit(String var1, boolean var2) {
      return this.getSellLimit(var1, var2, null, null);
   }

   public int getSellLimit(String var1, boolean var2, LoreFormatter.PageIndex var3, UUID var4) {
      return this.getGlobalSellLimit(var1, this.getCashedGlobalLimit(var1, var2, var3, var4));
   }

   private int getGlobalSellLimit(String var1, int var2) {
      if (var2 <= 0) {
         try {
            if (this.getLongLeftToRestock(var1) <= 0L) {
               this.GLOBAL_RESTOCK_TIMER.remove(var1);
               int var3 = this.getGlobalSellLimit(var1);
               this.setItemLimit(var1, var3);
               SendMessage.logDebugMessage(Lang.RESTOCKED_GLOBAL_SELL_LIMIT.get().replace("%path%", var1));
               return var3;
            } else {
               return -1;
            }
         } catch (NullPointerException var7) {
            try {
               this.restockGlobalItem(var1);
               SendMessage.logDebugMessage(Lang.RESTOCKING_GLOBAL_SELL_LIMIT.get().replace("%path%", var1));
               return -1;
            } catch (ParseException var5) {
               SendMessage.errorMessage(
                  "Invalid time format for global sell restock time of item '" + var1 + "', you need to specify a format like '00:05:00'(HH:mm:ss)"
               );
               return -3;
            } catch (NullPointerException var6) {
               return -4;
            }
         }
      } else {
         return var2;
      }
   }

   public void setLimit(ShopItem var1, UUID var2, int var3) {
      if (var1.getLimitedSellMode() == 2) {
         this.setPlayerLimit(var2, var1.getItemPath(), var3);
      } else if (var1.getLimitedSellMode() == 1) {
         this.setItemLimit(var1.getItemPath(), var3);
      }

      if (var3 == 0) {
         this.restockItems(var1, var2);
      }
   }

   public void restockItem(String var1, int var2, long var3) {
      ScheduleManager.Item var5 = new ScheduleManager.Item(var1, var2);
      this.restockSchedulers.put(var5, var3);
      if (this.cache) {
         this.sqlBatch.addQuery(new RestockScheduleQuery(var5, var3));
      }

      if (var2 == 2) {
         int var6 = this.getPlayerSellLimit(var1);
         this.setPlayerLimit(null, var1, var6);
      } else if (var2 == 1) {
         int var7 = this.getGlobalSellLimit(var1);
         this.setItemLimit(var1, var7);
      }
   }

   private void restockItems(ShopItem var1, UUID var2) {
      try {
         if (var1.getLimitedSellMode() == 2) {
            if (var2 == null) {
               for (UUID var4 : this.CASHED_PLAYER_LIMITS.get(var1.getItemPath()).keySet()) {
                  this.restockPlayerItem(var4, var1.getItemPath());
               }
            } else {
               this.restockPlayerItem(var2, var1.getItemPath());
            }
         } else if (var1.getLimitedSellMode() == 1) {
            this.restockGlobalItem(var1.getItemPath());
         }
      } catch (ParseException var5) {
         SendMessage.errorMessage("Could not get the restock time of item '" + var1.getItemPath() + "', you need to specify a format like '00:05:00'(HH:mm:ss)");
      } catch (NullPointerException var6) {
      }
   }

   public void sellItems(ShopItem var1, UUID var2, int var3, boolean var4) {
      if (var1.getLimitedSellMode() == 2) {
         this.setPlayerLimit(var2, var1.getItemPath(), Math.max(0, this.getCashedPlayerLimit(var1.getItemPath(), var2, false) - var3));
         if (var4) {
            this.getPlayerLimit(var2, var1.getItemPath());
         }
      } else if (var1.getLimitedSellMode() == 1) {
         this.setItemLimit(var1.getItemPath(), Math.max(0, this.getCashedGlobalLimit(var1.getItemPath(), false) - var3));
         if (var4) {
            this.getSellLimit(var1.getItemPath());
         }
      }
   }

   public int sellItemsSafe(ShopItem var1, UUID var2, int var3, boolean var4) {
      int var5 = -5;
      if (var1.getLimitedSellMode() == 2) {
         var5 = this.getCashedPlayerLimit(var1.getItemPath(), var2, false);
         if (var5 < var3) {
            var3 = var5;
         }

         this.setPlayerLimit(var2, var1.getItemPath(), var5 - var3);
         if (var4) {
            this.getPlayerLimit(var2, var1.getItemPath());
         }
      } else if (var1.getLimitedSellMode() == 1) {
         var5 = this.getCashedGlobalLimit(var1.getItemPath(), false);
         if (var5 < var3) {
            var3 = var5;
         }

         this.setItemLimit(var1.getItemPath(), var5 - var3);
         if (var4) {
            this.getSellLimit(var1.getItemPath());
         }
      }

      return var5;
   }

   public void resetItemLinit(String var1, String var2, UUID var3, int var4) {
      if (var1 != null && var2 != null) {
         if (var4 == 2) {
            this.setPlayerLimit(var3, var1 + "." + var2, this.getPlayerSellLimit(var1 + "." + var2));
            if (this.PLAYER_RESTOCK_TIMER.containsKey(var1 + "." + var2)) {
               if (var3 == null) {
                  this.PLAYER_RESTOCK_TIMER.remove(var1 + "." + var2);
               } else {
                  this.PLAYER_RESTOCK_TIMER.get(var1 + "." + var2).remove(var3);
               }
            }
         } else if (var4 == 1) {
            this.setItemLimit(var1 + "." + var2, this.getGlobalSellLimit(var1 + "." + var2));
            this.GLOBAL_RESTOCK_TIMER.remove(var1 + "." + var2);
         }
      } else {
         if (var1 == null) {
            if (var4 == 2) {
               if (var3 != null) {
                  RemovePlayerStockQuery var12 = new RemovePlayerStockQuery(var3.toString(), null);
                  if (this.cache) {
                     this.sqlBatch.addQuery(var12);
                  } else {
                     this.database.executeQuery(SQLBatch.Type.SELL_LIMIT, var12);
                  }

                  if (this.cache || this.plugin.displayCache) {
                     for (String var25 : this.CASHED_PLAYER_LIMITS.keySet()) {
                        this.CASHED_PLAYER_LIMITS.get(var25).remove(var3);
                     }
                  }

                  for (String var26 : this.PLAYER_RESTOCK_TIMER.keySet()) {
                     this.PLAYER_RESTOCK_TIMER.get(var26).remove(var3);
                  }
               } else {
                  ClearPlayerStockQuery var11 = new ClearPlayerStockQuery();
                  if (this.cache) {
                     this.sqlBatch.addQuery(var11);
                  } else {
                     this.database.executeQuery(SQLBatch.Type.SELL_LIMIT, var11);
                  }

                  if (this.cache || this.plugin.displayCache) {
                     this.CASHED_PLAYER_LIMITS.clear();
                  }

                  this.PLAYER_RESTOCK_TIMER.clear();
               }
            } else if (var4 == 1) {
               ClearStockQuery var10 = new ClearStockQuery();
               if (this.cache) {
                  this.sqlBatch.addQuery(var10);
               } else {
                  this.database.executeQuery(SQLBatch.Type.SELL_LIMIT, var10);
               }

               if (this.cache || this.plugin.displayCache) {
                  this.CASHED_GLOBAL_LIMITS.clear();
               }

               this.GLOBAL_RESTOCK_TIMER.clear();
            }
         } else if (var2 == null) {
            if (var4 == 2) {
               if (var3 != null) {
                  RemovePlayerStockQuery var9 = new RemovePlayerStockQuery(var3.toString(), var1);
                  if (this.cache) {
                     this.sqlBatch.addQuery(var9);
                  } else {
                     this.database.executeQuery(SQLBatch.Type.SELL_LIMIT, var9);
                  }

                  if (this.cache || this.plugin.displayCache) {
                     for (String var23 : this.CASHED_PLAYER_LIMITS.keySet()) {
                        if (var23.split("\\.")[0].equals(var1)) {
                           this.CASHED_PLAYER_LIMITS.get(var23).remove(var3);
                        }
                     }
                  }

                  for (String var24 : this.PLAYER_RESTOCK_TIMER.keySet()) {
                     if (var24.split("\\.")[0].equals(var1)) {
                        this.PLAYER_RESTOCK_TIMER.get(var24).remove(var3);
                     }
                  }
               } else {
                  RemovePlayerStocksQuery var8 = new RemovePlayerStocksQuery(var1);
                  if (this.cache) {
                     this.sqlBatch.addQuery(var8);
                  } else {
                     this.database.executeQuery(SQLBatch.Type.SELL_LIMIT, var8);
                  }

                  if (this.cache || this.plugin.displayCache) {
                     for (String var21 : this.CASHED_PLAYER_LIMITS.keySet()) {
                        if (var21.split("\\.")[0].equals(var1)) {
                           this.CASHED_PLAYER_LIMITS.get(var21).clear();
                        }
                     }
                  }

                  for (String var22 : this.PLAYER_RESTOCK_TIMER.keySet()) {
                     if (var22.split("\\.")[0].equals(var1)) {
                        this.PLAYER_RESTOCK_TIMER.get(var22).clear();
                     }
                  }
               }
            } else if (var4 == 1) {
               RemoveStockQuery var5 = new RemoveStockQuery(var1);
               if (this.cache) {
                  this.sqlBatch.addQuery(var5);
               } else {
                  this.database.executeQuery(SQLBatch.Type.SELL_LIMIT, var5);
               }

               if (this.cache || this.plugin.displayCache) {
                  for (String var7 : new ArrayList<>(this.CASHED_GLOBAL_LIMITS.keySet())) {
                     if (var7.split("\\.")[0].equals(var1)) {
                        this.CASHED_GLOBAL_LIMITS.remove(var7);
                     }
                  }
               }

               for (String var20 : new ArrayList<>(this.GLOBAL_RESTOCK_TIMER.keySet())) {
                  if (var20.split("\\.")[0].equals(var1)) {
                     this.GLOBAL_RESTOCK_TIMER.remove(var20);
                  }
               }
            }
         }

         if (this.cache) {
            this.sqlBatch.saveLast();
         }
      }
   }

   private void updateCashedPlayerLimit(String var1, UUID var2, int var3) {
      if (var2 == null) {
         if (this.cache) {
            this.sqlBatch.addQuery(new UpdatePlayerStockQuery(var1, var3));
         }

         try {
            this.CASHED_PLAYER_LIMITS.get(var1).replaceAll((var1x, var2x) -> var3);
         } catch (NullPointerException var7) {
         }
      } else {
         if (this.cache) {
            this.sqlBatch.addQuery(new PlayerStockQuery(var1, var2.toString(), var3));
         }

         try {
            this.CASHED_PLAYER_LIMITS.get(var1).put(var2, var3);
         } catch (NullPointerException var6) {
            HashMap var5 = new HashMap();
            var5.put(var2, var3);
            this.CASHED_PLAYER_LIMITS.put(var1, var5);
         }
      }
   }

   private int getCashedPlayerLimit(String var1, UUID var2, boolean var3) {
      return this.getCashedPlayerLimit(var1, var2, var3, null);
   }

   private int getCashedPlayerLimit(String var1, UUID var2, boolean var3, LoreFormatter.PageIndex var4) {
      if (this.cache || var3 && this.plugin.displayCache) {
         try {
            if (this.CASHED_PLAYER_LIMITS.get(var1).containsKey(var2)) {
               return this.CASHED_PLAYER_LIMITS.get(var1).get(var2);
            }
         } catch (NullPointerException var8) {
         }
      }

      if (var3 && var4 != null) {
         LimitedSellProvider.Snapshot var5 = this.PAGE_LIMIT_SNAPSHOT.get();
         if (var5 == null || var5.pageIndex != var4) {
            var5 = this.loadPageLimit(var4, var2);
         }

         if (var5.snapshot.containsKey(var1)) {
            return var5.snapshot.get(var1);
         }
      }

      try {
         return this.database.getPlayerSellLimit(var2, var1);
      } catch (SQLException var7) {
         SendMessage.errorMessage("Failed to read stored player sell limit for " + var1 + ": " + var7.getMessage());
         Map<UUID, Integer> var6 = this.CASHED_PLAYER_LIMITS.get(var1);
         return var6 != null && var6.containsKey(var2) ? var6.get(var2) : 0;
      }
   }

   private int getCashedGlobalLimit(String var1, boolean var2) {
      return this.getCashedGlobalLimit(var1, var2, null, null);
   }

   private int getCashedGlobalLimit(String var1, boolean var2, LoreFormatter.PageIndex var3, UUID var4) {
      if (this.cache || var2 && this.plugin.displayCache) {
         Integer var5 = this.CASHED_GLOBAL_LIMITS.get(var1);
         if (var5 != null) {
            return var5;
         }
      }

      if (var2 && var3 != null) {
         LimitedSellProvider.Snapshot var8 = this.PAGE_LIMIT_SNAPSHOT.get();
         if (var8 == null || var8.pageIndex != var3) {
            var8 = this.loadPageLimit(var3, var4);
         }

         if (var8.snapshot.containsKey(var1)) {
            return var8.snapshot.get(var1);
         }
      }

      try {
         return this.database.getSellLimit(var1);
      } catch (SQLException var7) {
         SendMessage.errorMessage("Failed to read stored global sell limit for " + var1 + ": " + var7.getMessage());
         Integer var6 = this.CASHED_GLOBAL_LIMITS.get(var1);
         return var6 != null ? var6 : 0;
      }
   }

   public Translatable getStockLore(ShopItem var1, Player var2, int var3) {
      int var4 = Math.min(var3, this.getItemLimit(var1, var2.getUniqueId(), true));
      if (var4 >= 0) {
         return Lang.CURRENT_SELL_LIMIT.get(var2).replace("%limit%", String.valueOf(var4));
      } else if (var4 == -1) {
         return Lang.RESTOCKING_SELL_LIMIT.get(var2).replace("%time%", this.getTimeLeftToRestock(var1.getItemPath()));
      } else if (var4 == -2) {
         return Lang.RESTOCKING_SELL_LIMIT.get(var2).replace("%time%", this.getTimeLeftToRestock(var2.getUniqueId(), var1.getItemPath()));
      } else {
         return var4 != -3 && var4 != -4 ? SendMessage.fromConfig("∞") : Lang.REACHED_SELL_LIMIT.get(var2);
      }
   }

   public int getItemLimit(ShopItem var1, UUID var2, boolean var3) {
      return this.getItemLimit(var1, var2, var3, false, null);
   }

   public int getItemLimit(ShopItem var1, UUID var2, boolean var3, boolean var4, LoreFormatter.PageIndex var5) {
      if (var1.getLimitedSellMode() == 2) {
         return var3 ? this.getPlayerLimit(var2, var1.getItemPath(), var4, var5) : this.getCashedPlayerLimit(var1.getItemPath(), var2, var4);
      } else if (var1.getLimitedSellMode() == 1) {
         return var3 ? this.getSellLimit(var1.getItemPath(), var4, var5, var2) : this.getCashedGlobalLimit(var1.getItemPath(), var4);
      } else {
         return 123456789;
      }
   }

   private void setPlayerLimit(UUID var1, String var2, int var3) {
      if (this.cache || this.plugin.displayCache) {
         this.updateCashedPlayerLimit(var2, var1, var3);
      }

      if (this.database instanceof MySQL || this.database instanceof PostgreSQL) {
         if (var1 == null) {
            this.database.setPlayerSellLimit(var2, var3);
         } else {
            this.database.setPlayerSellLimit(var1, var2, var3);
         }
      }
   }

   public int getPlayerLimit(UUID var1, String var2) {
      return this.getPlayerLimit(var1, var2, false, null);
   }

   public int getPlayerLimit(UUID var1, String var2, boolean var3, LoreFormatter.PageIndex var4) {
      return this.getPlayerLimit(var1, var2, this.getCashedPlayerLimit(var2, var1, var3, var4));
   }

   private int getPlayerLimit(UUID var1, String var2, int var3) {
      if (var3 <= 0) {
         try {
            if (this.getLongLeftToRestock(var1, var2) <= 0L) {
               this.PLAYER_RESTOCK_TIMER.get(var2).remove(var1);
               int var4 = this.getPlayerSellLimit(var2);
               this.setPlayerLimit(var1, var2, var4);
               SendMessage.logDebugMessage(Lang.RESTOCKED_PLAYER_SELL_LIMIT.get().replace("%path%", var2).replace("%playerUUID%", String.valueOf(var1)));
               return var4;
            } else {
               return -2;
            }
         } catch (NullPointerException var8) {
            try {
               this.restockPlayerItem(var1, var2);
               SendMessage.logDebugMessage(Lang.RESTOCKING_PLAYER_SELL_LIMIT.get().replace("%path%", var2).replace("%playerUUID%", String.valueOf(var1)));
               return -2;
            } catch (ParseException var6) {
               SendMessage.errorMessage(
                  "Invalid time format for player sell restock time of item '" + var2 + "', you need to specify a format like '00:05:00'(HH:mm:ss)"
               );
               return -3;
            } catch (NullPointerException var7) {
               return -4;
            }
         }
      } else {
         return var3;
      }
   }

   private String getItemLoc(String var1) {
      String[] var2 = var1.split("\\.", 2);
      return (this.plugin.getShopType(var2[0]) == ShopType.CATEGORY ? "pages." : "") + var2[1];
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
