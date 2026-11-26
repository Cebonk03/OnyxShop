package com.onyxshop.providers;

import java.util.concurrent.atomic.AtomicBoolean;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.ScheduledRestockJob;
import com.onyxshop.util.PoolJob;
import com.onyxshop.util.scheduler.ScheduledTask;

public class JobPoolProvider {
   private final OnyxShop plugin;
   private PoolJob currentPool;
   private ScheduledTask expireTask;
   private final AtomicBoolean isRunning = new AtomicBoolean(false);
   private int MAX_POOL_SIZE;
   private long delay;
   private int poolID = 0;

   public JobPoolProvider(OnyxShop var1) {
      this.plugin = var1;
   }

   public void init() {
      this.MAX_POOL_SIZE = ConfigManager.getConfig().getInt("max-pool-size", 50);
      this.delay = ConfigManager.getConfig().getLong("execution-delay", 2L);
      SendMessage.infoMessage("Created job pool provider with max size of " + this.MAX_POOL_SIZE + " and execution delay of " + this.delay + "ticks");
   }

   public synchronized void executeRestockItemLimit(String var1, int var2, long var3) {
      if (this.currentPool == null) {
         this.createNewPoolAndSchedule();
      }

      int var5 = this.currentPool
         .addAndGetSize(new ScheduledRestockJob(() -> this.plugin.getLimitedSellProvider().restockItem(var1, var2, var3), var1, var2, var3));
      if (var5 >= this.MAX_POOL_SIZE) {
         this.executePoolNow(this.currentPool, true);
      }
   }

   public synchronized void executeRestockItemStock(String var1, int var2, long var3) {
      if (this.currentPool == null) {
         this.createNewPoolAndSchedule();
      }

      int var5 = this.currentPool
         .addAndGetSize(new ScheduledRestockJob(() -> this.plugin.getLimitedStockProvider().restockItem(var1, var2, var3), var1, var2, var3));
      if (var5 >= this.MAX_POOL_SIZE) {
         this.executePoolNow(this.currentPool, true);
      }
   }

   private synchronized void createNewPoolAndSchedule() {
      if (this.currentPool != null) {
         this.executePoolNow(this.currentPool, true);
      }

      this.poolID++;
      this.currentPool = new PoolJob(this.poolID);
      this.expireTask = this.plugin.runTaskLater(() -> this.executePoolNow(this.currentPool, false), this.delay);
   }

   private synchronized void executePoolNow(PoolJob var1, boolean var2) {
      this.expireTask.cancel();
      this.currentPool = null;
      if (var2) {
         this.plugin.runTask(var1::execute);
      } else {
         var1.execute();
      }

      this.isRunning.lazySet(false);
   }
}
